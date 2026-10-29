package com.example.taskmanager.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.dto.AssetOptionsResponse;
import com.example.taskmanager.dto.AssetAreaRequest;
import com.example.taskmanager.dto.AssetBatchRequest;
import com.example.taskmanager.dto.AssetRequest;
import com.example.taskmanager.dto.AssetSiteRequest;
import com.example.taskmanager.dto.AssetResponse;
import com.example.taskmanager.dto.AssetStatusRequest;
import com.example.taskmanager.entity.Asset;
import com.example.taskmanager.entity.AssetArea;
import com.example.taskmanager.entity.AssetCategory;
import com.example.taskmanager.entity.AssetSite;
import com.example.taskmanager.entity.BusinessSettingsRecord;
import com.example.taskmanager.entity.InventoryUser;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.AssetAreaRepository;
import com.example.taskmanager.repository.AssetCategoryRepository;
import com.example.taskmanager.repository.AssetRepository;
import com.example.taskmanager.repository.AssetSiteRepository;
import com.example.taskmanager.repository.BusinessSettingsRepository;
import com.example.taskmanager.repository.InventoryUserRepository;

@Service
public class AssetService {
    private static final List<String> DEFAULT_AREAS = List.of(
        "Administración", "Tecnología", "Caja", "Ventas", "Almacén", "Recepción", "Producción", "Otro"
    );
    private static final List<String> DEFAULT_SITES = List.of(
        "Sede principal", "Sede norte", "Sede sur", "Bodega central", "Punto de venta"
    );

    private final AssetRepository repository;
    private final ActivityLogService activityLog;
    private final BusinessSettingsRepository companies;
    private final AssetSiteRepository sites;
    private final AssetAreaRepository areas;
    private final InventoryUserRepository users;
    private final AssetCategoryRepository categories;

    public AssetService(
        AssetRepository repository,
        ActivityLogService activityLog,
        BusinessSettingsRepository companies,
        AssetSiteRepository sites,
        AssetAreaRepository areas,
        InventoryUserRepository users,
        AssetCategoryRepository categories
    ) {
        this.repository = repository;
        this.activityLog = activityLog;
        this.companies = companies;
        this.sites = sites;
        this.areas = areas;
        this.users = users;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
            .stream().map(AssetResponse::from).toList();
    }

    @Transactional
    public AssetOptionsResponse options() {
        BusinessSettingsRecord company = getOrCreateCompany();
        linkExistingAssets(company);
        ensureDefaultLocations(company);
        List<AssetOptionsResponse.SiteOption> siteOptions = sites.findAllByCompany_IdOrderByNameAsc(company.getId())
            .stream()
            .map(site -> new AssetOptionsResponse.SiteOption(
                site.getId(),
                site.getName(),
                site.getDepartment(),
                site.getCity(),
                areas.findAllBySite_IdOrderByNameAsc(site.getId()).stream()
                    .map(area -> new AssetOptionsResponse.AreaOption(area.getId(), area.getName()))
                    .toList()
            ))
            .toList();
        List<AssetOptionsResponse.ResponsibleOption> responsibleOptions = users.findAllByEnabledTrueOrderByFullNameAsc()
            .stream()
            .map(user -> new AssetOptionsResponse.ResponsibleOption(user.getId(), user.getFullName()))
            .toList();
        List<AssetOptionsResponse.CategoryOption> categoryOptions = categories.findAllByOrderByNameAsc()
            .stream()
            .map(cat -> new AssetOptionsResponse.CategoryOption(cat.getId(), cat.getName()))
            .toList();
        return new AssetOptionsResponse(company.getId(), company.getBusinessName(), siteOptions, responsibleOptions, categoryOptions);
    }

    @Transactional
    public AssetOptionsResponse.SiteOption createSite(AssetSiteRequest request) {
        BusinessSettingsRecord company = getOrCreateCompany();
        String name = clean(request.name());
        if (sites.findByCompany_IdAndNameIgnoreCase(company.getId(), name).isPresent()) {
            throw new IllegalArgumentException("Ya existe una sede con ese nombre.");
        }
        AssetSite site = createSite(company, "SITE-" + UUID.randomUUID(), name);
        site.setDepartment(clean(request.department()));
        site.setCity(clean(request.city()));
        site = sites.save(site);
        return new AssetOptionsResponse.SiteOption(site.getId(), site.getName(), site.getDepartment(), site.getCity(),
            List.of());
    }

    @Transactional
    public AssetOptionsResponse.AreaOption createArea(Long siteId, AssetAreaRequest request) {
        AssetSite site = sites.findById(siteId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sede seleccionada."));
        String name = clean(request.name());
        if (areas.findBySite_IdAndNameIgnoreCase(siteId, name).isPresent()) {
            throw new IllegalArgumentException("Ya existe esa área en la sede.");
        }
        AssetArea area = createArea(site, "AREA-" + UUID.randomUUID(), name);
        return new AssetOptionsResponse.AreaOption(area.getId(), area.getName());
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        Asset asset = new Asset();
        apply(request, asset);
        asset.setInventoryCode("TMP-" + UUID.randomUUID());
        Asset saved = repository.saveAndFlush(asset);
        saved.setInventoryCode("BIEN-%06d".formatted(saved.getId()));
        Asset registered = repository.save(saved);
        activityLog.record("ASSET_CREATED",
            "Se registró " + registered.getName() + " con código " + registered.getInventoryCode());
        return AssetResponse.from(registered);
    }

    @Transactional
    public List<AssetResponse> createBatch(AssetBatchRequest request) {
        if (request.assets().size() > 500) {
            throw new IllegalArgumentException("No se pueden registrar más de 500 unidades en un solo ingreso.");
        }
        return request.assets().stream().map(this::create).toList();
    }

    @Transactional
    public AssetResponse update(Long id, AssetRequest request) {
        Asset asset = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el bien " + id));
        apply(request, asset);
        Asset updated = repository.save(asset);
        activityLog.record("ASSET_UPDATED",
            "Se corrigió la información de " + updated.getName() + " (" + updated.getInventoryCode() + ")");
        return AssetResponse.from(updated);
    }

    @Transactional
    public AssetResponse updateStatus(Long id, AssetStatusRequest request) {
        Asset asset = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el bien " + id));
        String newCondition = clean(request.condition());
        String reason = clean(request.reason());
        asset.setCondition(newCondition);
        String observations = asset.getObservations();
        asset.setObservations(observations == null || observations.isBlank()
            ? reason
            : observations + "\n" + reason);
        Asset updated = repository.save(asset);
        String movementLocation = updated.getSite() == null
            ? updated.getLocation()
            : updated.getLocation() + " · " + updated.getSite().getDepartment() + " · " + updated.getSite().getCity()
                + " · " + updated.getArea();
        activityLog.record("ASSET_STATUS_UPDATED",
            "Se cambió el estado de " + updated.getName() + " a " + updated.getCondition()
                + " en " + movementLocation + ". Motivo: " + reason);
        return AssetResponse.from(updated);
    }

    private void apply(AssetRequest request, Asset asset) {
        BusinessSettingsRecord company = resolveCompany(request.companyId());
        AssetSite site = resolveSite(request.siteId(), request.location(), company);
        AssetArea area = resolveArea(request.areaId(), request.area(), site);
        InventoryUser responsible = resolveResponsible(request.responsibleId());
        AssetCategory category = resolveCategory(request.categoryId());
        if (request.startUseDate() != null && request.startUseDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de puesta en servicio no puede ser posterior a hoy.");
        }
        if (request.residualValue() != null && request.residualValue().signum() < 0) {
            throw new IllegalArgumentException("El valor residual no puede ser negativo.");
        }
        if (request.residualValue() != null && request.purchaseValue() != null
            && request.residualValue().compareTo(request.purchaseValue()) > 0) {
            throw new IllegalArgumentException("El valor residual no puede superar el valor de adquisición.");
        }
        if (request.residualValue() != null && request.purchaseValue() == null
            && request.residualValue().signum() > 0) {
            throw new IllegalArgumentException("Registra el valor de adquisición antes de definir el valor residual.");
        }

        asset.setCompany(company);
        asset.setSite(site);
        asset.setAreaRecord(area);
        asset.setResponsibleUser(responsible);
        asset.setCategory(category);
        asset.setAssetType(clean(request.assetType()));
        asset.setName(clean(request.name()));
        asset.setSerialNumber(blankToNull(request.serialNumber()));
        asset.setBrand(blankToNull(request.brand()));
        asset.setModel(blankToNull(request.model()));
        asset.setMaterial(blankToNull(request.material()));
        asset.setColor(blankToNull(request.color()));
        asset.setDimensions(blankToNull(request.dimensions()));
        asset.setItemDetails(blankToNull(request.itemDetails()));
        asset.setLocation(site.getName());
        asset.setArea(area.getName());
        asset.setCondition(clean(request.condition()));
        asset.setResponsible(responsible == null ? blankToNull(request.responsible()) : responsible.getFullName());
        asset.setProcessor(blankToNull(request.processor()));
        asset.setRam(blankToNull(request.ram()));
        asset.setStorage(blankToNull(request.storage()));
        asset.setOperatingSystem(blankToNull(request.operatingSystem()));
        asset.setPurchaseDate(request.purchaseDate());
        asset.setPurchaseValue(request.purchaseValue());
        asset.setCurrency(blankToNull(request.currency()));
        asset.setWarrantyUntil(request.warrantyUntil());
        asset.setVendor(blankToNull(request.vendor()));
        asset.setPurchaseReference(blankToNull(request.purchaseReference()));
        asset.setObservations(blankToNull(request.observations()));
        asset.setStartUseDate(request.startUseDate());
        asset.setResidualValue(request.residualValue());
        asset.setUsefulLifeYears(category.getUsefulLifeYears());
    }

    private AssetCategory resolveCategory(Long categoryId) {
        return categories.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("La categoría seleccionada no existe."));
    }

    private BusinessSettingsRecord getOrCreateCompany() {
        return companies.findById(1L).orElseGet(() -> companies.save(new BusinessSettingsRecord()));
    }

    private BusinessSettingsRecord resolveCompany(Long companyId) {
        BusinessSettingsRecord company = getOrCreateCompany();
        if (companyId != null && !companyId.equals(company.getId())) {
            throw new IllegalArgumentException("La empresa seleccionada no existe o ya no está disponible.");
        }
        return company;
    }

    private AssetSite createSite(BusinessSettingsRecord company, String code, String name) {
        AssetSite site = new AssetSite();
        site.setCompany(company);
        site.setCode(code);
        site.setName(name);
        return sites.save(site);
    }

    private AssetArea createArea(AssetSite site, String code, String name) {
        AssetArea area = new AssetArea();
        area.setSite(site);
        area.setCode(code);
        area.setName(name);
        return areas.save(area);
    }

    private void linkExistingAssets(BusinessSettingsRecord company) {
        List<Asset> unlinkedAssets = repository.findAll().stream()
            .filter(asset -> asset.getCompany() == null || asset.getSite() == null || asset.getAreaRecord() == null)
            .toList();
        for (Asset asset : unlinkedAssets) {
            if (asset.getCompany() == null) asset.setCompany(company);
            if (asset.getSite() == null) {
                AssetSite site = sites.findByCompany_IdAndNameIgnoreCase(company.getId(), asset.getLocation())
                    .orElseGet(() -> createSite(company, "LEGACY-" + UUID.randomUUID(), asset.getLocation()));
                asset.setSite(site);
            }
            if (asset.getSite() != null && asset.getAreaRecord() == null) {
                AssetSite site = asset.getSite();
                AssetArea area = areas.findBySite_IdAndNameIgnoreCase(site.getId(), asset.getArea())
                    .orElseGet(() -> createArea(site, "LEGACY-" + UUID.randomUUID(), asset.getArea()));
                asset.setAreaRecord(area);
            }
            if (asset.getResponsibleUser() == null && asset.getResponsible() != null) {
                List<InventoryUser> matches = users.findAllByFullNameIgnoreCase(asset.getResponsible());
                if (matches.size() == 1) asset.setResponsibleUser(matches.get(0));
            }
        }
        repository.saveAll(unlinkedAssets);
    }

    private void ensureDefaultLocations(BusinessSettingsRecord company) {
        for (String siteName : DEFAULT_SITES) {
            AssetSite site = sites.findByCompany_IdAndNameIgnoreCase(company.getId(), siteName)
                .orElseGet(() -> createSite(company, "DEFAULT-" + UUID.randomUUID(), siteName));
            for (String areaName : DEFAULT_AREAS) {
                if (areas.findBySite_IdAndNameIgnoreCase(site.getId(), areaName).isEmpty()) {
                    createArea(site, "DEFAULT-" + UUID.randomUUID(), areaName);
                }
            }
        }
    }

    private AssetSite resolveSite(Long siteId, String location, BusinessSettingsRecord company) {
        if (siteId != null) {
            AssetSite site = sites.findById(siteId)
                .orElseThrow(() -> new IllegalArgumentException("La sede seleccionada no existe."));
            if (!site.getCompany().getId().equals(company.getId())) {
                throw new IllegalArgumentException("La sede seleccionada no pertenece a la empresa.");
            }
            return site;
        }
        String siteName = clean(location);
        return sites.findByCompany_IdAndNameIgnoreCase(company.getId(), siteName)
            .orElseGet(() -> createSite(company, "LEGACY-" + UUID.randomUUID(), siteName));
    }

    private AssetArea resolveArea(Long areaId, String areaName, AssetSite site) {
        if (areaId != null) {
            AssetArea area = areas.findById(areaId)
                .orElseThrow(() -> new IllegalArgumentException("El área seleccionada no existe."));
            if (!area.getSite().getId().equals(site.getId())) {
                throw new IllegalArgumentException("El área seleccionada no pertenece a la sede.");
            }
            return area;
        }
        String normalizedArea = clean(areaName);
        return areas.findBySite_IdAndNameIgnoreCase(site.getId(), normalizedArea)
            .orElseGet(() -> createArea(site, "LEGACY-" + UUID.randomUUID(), normalizedArea));
    }

    private InventoryUser resolveResponsible(Long responsibleId) {
        if (responsibleId == null) return null;
        InventoryUser user = users.findById(responsibleId)
            .orElseThrow(() -> new IllegalArgumentException("El responsable seleccionado no existe."));
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("El responsable seleccionado está inactivo.");
        }
        return user;
    }

    private String clean(String value) {
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
