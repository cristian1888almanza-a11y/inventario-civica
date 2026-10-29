package com.example.taskmanager.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskmanager.repository.AssetRepository;
import com.example.taskmanager.repository.AssetCategoryRepository;
import com.example.taskmanager.repository.DepreciationRecordRepository;
import com.example.taskmanager.repository.InventoryUserRepository;
import com.example.taskmanager.entity.InventoryUser;
import com.example.taskmanager.entity.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class AssetControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired AssetRepository repository;
    @Autowired AssetCategoryRepository categories;
    @Autowired DepreciationRecordRepository depreciationRecords;
    @Autowired InventoryUserRepository users;

    @BeforeEach
    void clearAssets() {
        depreciationRecords.deleteAll();
        repository.deleteAll();
    }

    @Test
    void registersIndividualAssetsWithUniqueGeneratedCodesAndListsThem() throws Exception {
        String request = """
            {
              "assetType": "Equipo de cómputo",
              "categoryId": %d,
              "name": "Computadora portátil",
              "serialNumber": "SERIAL-001",
              "brand": "Marca",
              "model": "Modelo",
              "material": "Madera",
              "color": "Roble",
              "dimensions": "120 x 60 x 75 cm",
              "itemDetails": "Con cajonera",
              "purchaseValue": 250000.00,
              "residualValue": 0,
              "startUseDate": "2024-04-01",
              "currency": "COP",
              "warrantyUntil": "2027-04-30",
              "processor": "Intel Core i5",
              "ram": "16",
              "storage": "SSD 512 GB",
              "operatingSystem": "Windows 11 Pro",
              "location": "Edificio principal",
              "area": "Administración",
              "condition": "Bueno",
              "responsible": "Ana Pérez"
            }
            """.formatted(categoryId());

        String firstResponse = mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.inventoryCode").isString())
            .andExpect(jsonPath("$.name").value("Computadora portátil"))
            .andExpect(jsonPath("$.material").value("Madera"))
            .andExpect(jsonPath("$.color").value("Roble"))
            .andExpect(jsonPath("$.dimensions").value("120 x 60 x 75 cm"))
            .andExpect(jsonPath("$.itemDetails").value("Con cajonera"))
            .andExpect(jsonPath("$.purchaseValue").value(250000.00))
            .andExpect(jsonPath("$.currency").value("COP"))
            .andExpect(jsonPath("$.warrantyUntil").value("2027-04-30"))
            .andExpect(jsonPath("$.processor").value("Intel Core i5"))
            .andExpect(jsonPath("$.ram").value("16"))
            .andExpect(jsonPath("$.storage").value("SSD 512 GB"))
            .andExpect(jsonPath("$.operatingSystem").value("Windows 11 Pro"))
            .andReturn().getResponse().getContentAsString();
        String secondResponse = mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String firstCode = mapper.readTree(firstResponse).get("inventoryCode").asText();
        String secondCode = mapper.readTree(secondResponse).get("inventoryCode").asText();
        org.junit.jupiter.api.Assertions.assertNotEquals(firstCode, secondCode);

        mockMvc.perform(get("/assets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].inventoryCode", not(equalTo(""))));
    }

    @Test
    void rejectsAssetsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetType\":\"Equipo de cómputo\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void providesCompanySitesAreasAndResponsibleOptionsFromRelatedRecords() throws Exception {
        mockMvc.perform(get("/assets/options"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyId").value(1))
            .andExpect(jsonPath("$.companyName").isNotEmpty())
            .andExpect(jsonPath("$.sites", hasSize(greaterThanOrEqualTo(5))))
            .andExpect(jsonPath("$.sites[0].areas", hasSize(8)));
    }

    @Test
    void validatesThatAreaBelongsToSelectedSiteAndPersistsTheRelationships() throws Exception {
        JsonNode options = mapper.readTree(mockMvc.perform(get("/assets/options"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        JsonNode firstSite = options.path("sites").get(0);
        JsonNode matchingArea = firstSite.path("areas").get(0);
        JsonNode otherSiteArea = options.path("sites").get(1).path("areas").get(0);

        var request = mapper.createObjectNode()
            .put("assetType", "Equipo de cómputo")
            .put("categoryId", categoryId())
            .put("name", "Equipo relacionado")
            .put("purchaseValue", 1000000)
            .put("residualValue", 0)
            .put("startUseDate", "2024-04-01")
            .put("companyId", options.path("companyId").asLong())
            .put("siteId", firstSite.path("id").asLong())
            .put("areaId", matchingArea.path("id").asLong())
            .put("location", firstSite.path("name").asText())
            .put("area", matchingArea.path("name").asText())
            .put("condition", "Activo");

        String created = mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.companyId").value(options.path("companyId").asLong()))
            .andExpect(jsonPath("$.siteId").value(firstSite.path("id").asLong()))
            .andExpect(jsonPath("$.areaId").value(matchingArea.path("id").asLong()))
            .andReturn().getResponse().getContentAsString();
        long assetId = mapper.readTree(created).path("id").asLong();

        request.put("areaId", otherSiteArea.path("id").asLong());
        mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/assets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].companyId").value(options.path("companyId").asLong()))
            .andExpect(jsonPath("$[0].siteId").value(firstSite.path("id").asLong()))
            .andExpect(jsonPath("$[0].areaId").value(matchingArea.path("id").asLong()));
    }

    @Test
    void linksTheSelectedResponsibleToAnActiveAccount() throws Exception {
        JsonNode options = mapper.readTree(mockMvc.perform(get("/assets/options"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
        JsonNode site = options.path("sites").get(0);
        JsonNode area = site.path("areas").get(0);
        InventoryUser user = new InventoryUser();
        user.setUsername("asset-resp-" + java.util.UUID.randomUUID().toString().substring(0, 12));
        user.setFullName("Responsable de bienes");
        user.setPasswordHash("test-password-hash");
        user.setRole(UserRole.EMPLOYEE);
        user.setEnabled(true);
        InventoryUser savedUser = users.save(user);

        var request = mapper.createObjectNode()
            .put("assetType", "Enser")
            .put("categoryId", categoryId())
            .put("name", "Silla asignada")
            .put("purchaseValue", 100000)
            .put("residualValue", 0)
            .put("startUseDate", "2024-04-01")
            .put("companyId", options.path("companyId").asLong())
            .put("siteId", site.path("id").asLong())
            .put("areaId", area.path("id").asLong())
            .put("responsibleId", savedUser.getId())
            .put("location", site.path("name").asText())
            .put("area", area.path("name").asText())
            .put("responsible", savedUser.getFullName())
            .put("condition", "Activo");

        mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.responsibleId").value(savedUser.getId()))
            .andExpect(jsonPath("$.responsible").value(savedUser.getFullName()));
    }

    @Test
    void updatesAssetStatusAndKeepsTheChangeReasonInObservations() throws Exception {
        String created = mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetType":"Equipo de cómputo","categoryId":%d,"name":"Teclado",
                     "purchaseValue":100000,"residualValue":0,"startUseDate":"2024-04-01",
                     "location":"Sede principal","area":"Tecnología","condition":"Activo"}
                    """.formatted(categoryId())))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        mockMvc.perform(patch("/assets/{id}/status", id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"condition":"Reemplazado","reason":"Dejó de funcionar y se instaló uno nuevo"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.condition").value("Reemplazado"))
            .andExpect(jsonPath("$.observations").value("Dejó de funcionar y se instaló uno nuevo"));
    }

            @Test
            void retiresAndReentersAssetWithoutDeletingItsHistory() throws Exception {
            String created = mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetType":"Enser","categoryId":%d,"name":"Silla antigua",
                     "purchaseValue":100000,"residualValue":0,"startUseDate":"2024-04-01",
                     "location":"Sede principal","area":"Administración","condition":"Activo"}
                    """.formatted(categoryId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
            long id = mapper.readTree(created).get("id").asLong();
            String inventoryCode = mapper.readTree(created).get("inventoryCode").asText();

            mockMvc.perform(patch("/assets/{id}/status", id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"condition":"Retirado","reason":"Se guarda temporalmente"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("Retirado"));

            mockMvc.perform(patch("/assets/{id}/status", id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"condition":"Activo","reason":"Regresa al inventario"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("Activo"))
                .andExpect(jsonPath("$.inventoryCode").value(inventoryCode))
                .andExpect(jsonPath("$.observations").value("Se guarda temporalmente\nRegresa al inventario"));
            }

                @Test
                void correctsSavedAssetWithoutChangingItsInventoryCode() throws Exception {
                String created = mockMvc.perform(post("/assets").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"assetType":"Equipo de cómputo","categoryId":%d,"name":"Monitr 24",
                         "purchaseValue":1500000,"residualValue":0,"startUseDate":"2024-04-01",
                         "location":"Sede principal","area":"Tecnología","condition":"Activo"}
                        """.formatted(categoryId())))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
                long id = mapper.readTree(created).get("id").asLong();
                String inventoryCode = mapper.readTree(created).get("inventoryCode").asText();

                mockMvc.perform(put("/assets/{id}", id).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"assetType":"Equipo de cómputo","categoryId":%d,"name":"Monitor 24 pulgadas",
                         "purchaseValue":1500000,"residualValue":0,"startUseDate":"2024-04-01",
                         "location":"Sede principal","area":"Tecnología","condition":"Activo","brand":"Dell"}
                        """.formatted(categoryId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Monitor 24 pulgadas"))
                    .andExpect(jsonPath("$.brand").value("Dell"))
                    .andExpect(jsonPath("$.inventoryCode").value(inventoryCode));
                }

    @Test
    void calculatesAndQueriesDepreciationWithoutChangingHistoricalValueOrRemovingAsset() throws Exception {
        String created = mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetType":"Equipo de cómputo","categoryId":%d,"name":"Portátil depreciable",
                     "purchaseValue":7000000,"residualValue":0,"purchaseDate":"2024-04-01",
                     "startUseDate":"2024-04-01","location":"Sede principal",
                     "area":"Tecnología","condition":"Activo"}
                    """.formatted(categoryId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usefulLifeYears").value(5))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).path("id").asLong();

        mockMvc.perform(get("/assets/depreciation/valuation").param("asOf", "2026-10-06"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].monthlyDepreciation").value(116666.67))
            .andExpect(jsonPath("$[0].accumulatedDepreciation").value(3500000.00))
            .andExpect(jsonPath("$[0].bookValue").value(3500000.00));

        String request = """
            {"assetId":%d,"depreciationDate":"2026-10-06","processedBy":"Prueba"}
            """.formatted(id);
        mockMvc.perform(post("/assets/{id}/depreciation/calculate", id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlyDepreciation").value(116666.67))
            .andExpect(jsonPath("$.accumulatedDepreciation").value(3500000.00))
            .andExpect(jsonPath("$.bookValue").value(3500000.00));
        mockMvc.perform(post("/assets/{id}/depreciation/calculate", id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk());
        mockMvc.perform(get("/assets/{id}/depreciation/history", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/assets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].purchaseValue").value(7000000.00))
            .andExpect(jsonPath("$[0].bookValue").value(3500000.00));
    }

    @Test
    void createdCategoryConfigurationIsAppliedToNewAssets() throws Exception {
        String categoryJson = mockMvc.perform(post("/asset-categories").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Instrumentos de medición","usefulLifeYears":8,
                     "annualDepreciationRate":0.125,"depreciationMethod":"STRAIGHT_LINE"}
                    """))
            .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.annualDepreciationRate").value(0.125))
            .andReturn().getResponse().getContentAsString();
        long categoryId = mapper.readTree(categoryJson).path("id").asLong();

        mockMvc.perform(post("/assets").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetType":"Otro","categoryId":%d,"name":"Calibrador",
                     "purchaseValue":800000,"residualValue":0,"startUseDate":"2024-04-01",
                     "location":"Sede principal","area":"Producción","condition":"Activo"}
                    """.formatted(categoryId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoryName").value("Instrumentos de medición"))
            .andExpect(jsonPath("$.usefulLifeYears").value(8));
    }

        private long categoryId() {
            return categories.findByNameIgnoreCase("Equipos de cómputo").orElseThrow().getId();
        }
}
