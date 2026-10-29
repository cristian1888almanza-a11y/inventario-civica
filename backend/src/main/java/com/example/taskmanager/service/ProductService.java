package com.example.taskmanager.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.dto.InventorySummary;
import com.example.taskmanager.dto.ProductRequest;
import com.example.taskmanager.dto.ProductResponse;
import com.example.taskmanager.entity.Product;
import com.example.taskmanager.exception.DuplicateSkuException;
import com.example.taskmanager.exception.ProductNotFoundException;
import com.example.taskmanager.exception.ReportGenerationException;
import com.example.taskmanager.repository.ProductRepository;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

@Service
public class ProductService {
    private final ProductRepository repository;
    private final ActivityLogService activityLog;
    private final InventoryMovementService inventoryMovements;

    public ProductService(ProductRepository repository, ActivityLogService activityLog,
                          InventoryMovementService inventoryMovements) {
        this.repository = repository;
        this.activityLog = activityLog;
        this.inventoryMovements = inventoryMovements;
    }

    public List<ProductResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(get(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (repository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException(sku);
        }
        Product product = repository.save(toEntity(request, new Product()));
        inventoryMovements.record(product, "IN", product.getStock(), "Stock inicial del producto");
        activityLog.record("PRODUCT_CREATED", "Se creó el producto " + product.getName() + " (SKU " + product.getSku() + ")");
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = get(id);
        String sku = normalizeSku(request.sku());
        if (repository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateSkuException(sku);
        }
        String previousName = product.getName();
        int previousStock = product.getStock();
        Product updated = repository.save(toEntity(request, product));
        String detail = "Se actualizó " + updated.getName() + " (SKU " + updated.getSku() + ")";
        if (previousStock != updated.getStock()) {
            detail += "; existencias " + previousStock + " → " + updated.getStock();
        }
        if (!previousName.equals(updated.getName())) {
            detail += "; nombre anterior: " + previousName;
        }
        if (previousStock != updated.getStock()) {
            inventoryMovements.record(updated, "ADJUSTMENT", updated.getStock() - previousStock, "Existencias actualizadas desde el catálogo");
        }
        activityLog.record("PRODUCT_UPDATED", detail);
        return ProductResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        repository.delete(product);
        activityLog.record("PRODUCT_DELETED", "Se eliminó " + product.getName() + " (SKU " + product.getSku() + ")");
    }

    public InventorySummary summary() {
        List<Product> products = repository.findAll();
        long totalUnits = 0;
        long lowStock = 0;
        BigDecimal inventoryValue = BigDecimal.ZERO;
        for (Product product : products) {
            totalUnits += product.getStock();
            inventoryValue = inventoryValue.add(product.getPrice().multiply(BigDecimal.valueOf(product.getStock())));
            if (product.getStock() <= product.getMinStock()) {
                lowStock++;
            }
        }
        return new InventorySummary(products.size(), totalUnits, inventoryValue, lowStock);
    }

    public byte[] exportReport() {
        List<Map<String, ?>> rows = new ArrayList<>();
        repository.findAll(Sort.by(Sort.Direction.ASC, "name"))
            .forEach(product -> rows.add(toReportRow(product)));
        try (InputStream template = new ClassPathResource("reports/inventory-report.jrxml").getInputStream()) {
            var report = JasperCompileManager.compileReport(template);
            var dataSource = new JRMapCollectionDataSource(rows);
            var print = JasperFillManager.fillReport(report, new HashMap<>(), dataSource);
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            if (pdf != null && pdf.length > 4) {
                return pdf;
            }
        } catch (RuntimeException | JRException | IOException exception) {
            exception.printStackTrace();
        }

        try {
            return buildFallbackPdf(rows);
        } catch (RuntimeException fallbackException) {
            fallbackException.printStackTrace();
            throw new ReportGenerationException(fallbackException);
        }
    }

    private byte[] buildFallbackPdf(List<Map<String, ?>> rows) {
        List<String> lines = rows.isEmpty()
            ? List.of("No hay productos registrados en el inventario.")
            : rows.stream()
                .map(row -> String.format(Locale.ROOT, "%s - %s | %s | %s | %s",
                    String.valueOf(row.get("sku")),
                    String.valueOf(row.get("name")),
                    String.valueOf(row.get("category")),
                    String.valueOf(row.get("description")),
                    String.valueOf(row.get("supplier"))))
                .toList();

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 12 Tf\n50 750 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                stream.append("0 -15 Td\n");
            }
            stream.append('(').append(escapePdfString(lines.get(i))).append(") Tj\n");
        }
        stream.append("ET\n");

        byte[] content = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("4 0 obj\n<< /Length " + content.length + " >>\nstream\n" + stream + "\nendstream\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        objects.add("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        int[] offsets = new int[objects.size() + 1];
        offsets[0] = 0;
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = output.size();
            output.write(objects.get(i), 0, objects.get(i).length);
        }

        int startXref = output.size();
        output.writeBytes(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        output.writeBytes("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i <= objects.size(); i++) {
            output.writeBytes(String.format(Locale.ROOT, "%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.ISO_8859_1));
        }
        output.writeBytes(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + startXref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return output.toByteArray();
    }

    private String escapePdfString(String value) {
        return value == null ? "" : value
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)");
    }

    private Map<String, ?> toReportRow(Product product) {
        Map<String, Object> row = new HashMap<>();
        row.put("sku", product.getSku());
        row.put("name", product.getName());
        row.put("category", product.getCategory());
        row.put("description", product.getDescription() == null || product.getDescription().isBlank()
            ? "Sin descripción" : product.getDescription());
        row.put("supplier", product.getSupplier() == null || product.getSupplier().isBlank()
            ? "Sin proveedor" : product.getSupplier());
        return row;
    }

    private Product get(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product toEntity(ProductRequest request, Product product) {
        product.setSku(normalizeSku(request.sku()));
        product.setName(request.name().trim());
        product.setCategory(request.category().trim());
        product.setDescription(blankToNull(request.description()));
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setMinStock(request.minStock());
        product.setSupplier(blankToNull(request.supplier()));
        return product;
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
