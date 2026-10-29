package com.example.taskmanager.service;

import com.example.taskmanager.dto.BusinessRequests.*;
import com.example.taskmanager.dto.BusinessRequests;
import com.example.taskmanager.dto.BusinessResponses.Operation;
import com.example.taskmanager.dto.BusinessResponses.Cash;
import com.example.taskmanager.dto.BusinessResponses.CashMovement;
import com.example.taskmanager.entity.*;
import com.example.taskmanager.exception.ProductNotFoundException;
import com.example.taskmanager.exception.ResourceNotFoundException;
import com.example.taskmanager.repository.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class BusinessService {
    private final OperationRepository operations;
    private final ProductRepository products;
    private final CashRegisterRepository registers;
    private final CashMovementRepository movements;
    private final ActivityLogService activityLog;
    private final InventoryMovementService inventoryMovementLog;

    public BusinessService(OperationRepository operations, ProductRepository products,
                           CashRegisterRepository registers, CashMovementRepository movements,
                           ActivityLogService activityLog, InventoryMovementService inventoryMovementLog) {
        this.operations = operations;
        this.products = products;
        this.registers = registers;
        this.movements = movements;
        this.activityLog = activityLog;
        this.inventoryMovementLog = inventoryMovementLog;
    }

    @Transactional
    public Operation createQuote(Quote request) {
        OperationRecord record = newRecord(OperationType.QUOTE, OperationStatus.DRAFT, request.customer(), request.description());
        BigDecimal total = BigDecimal.ZERO;
        for (Line item : request.items()) {
            Product product = getProduct(item.productId());
            addLine(record, product, item.quantity(), product.getPrice());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        record.setTotal(total);
        Operation result = save(record);
        activityLog.record("QUOTE_CREATED", "Se creó la cotización #" + result.id() + " por " + total);
        return result;
    }

    @Transactional(readOnly = true) public List<Operation> quotes() { return list(OperationType.QUOTE); }
    @Transactional public Operation quoteStatus(long id, OperationStatus status) {
        OperationRecord record = getOperation(id, OperationType.QUOTE);
        if (!Set.of(OperationStatus.DRAFT, OperationStatus.SENT, OperationStatus.ACCEPTED, OperationStatus.REJECTED).contains(status))
            throw new IllegalArgumentException("Estado no válido para una cotización");
        record.setStatus(status);
        Operation result = save(record);
        activityLog.record("QUOTE_STATUS_CHANGED", "Cotización #" + id + " cambió a " + status);
        return result;
    }

    @Transactional
    public Operation createPurchase(Purchase request) {
        OperationRecord record = newRecord(OperationType.PURCHASE, OperationStatus.OPEN, request.supplier(), request.description());
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseLine item : request.items()) {
            Product product = getProduct(item.productId());
            addLine(record, product, item.quantity(), item.unitCost());
            total = total.add(item.unitCost().multiply(BigDecimal.valueOf(item.quantity())));
        }
        record.setTotal(total);
        Operation result = save(record);
        activityLog.record("PURCHASE_CREATED", "Se creó la orden de compra #" + result.id() + " por " + total);
        return result;
    }
    @Transactional(readOnly = true) public List<Operation> purchases() { return list(OperationType.PURCHASE); }
    @Transactional public Operation receivePurchase(long id) {
        OperationRecord record = getOperation(id, OperationType.PURCHASE);
        if (record.getStatus() != OperationStatus.OPEN) throw new IllegalStateException("La compra ya fue recibida o no está pendiente");
        for (OperationLine line : record.getLines()) {
            Product product = lockProduct(line.getProduct().getId());
            product.setStock(product.getStock() + line.getQuantity());
            inventoryMovementLog.record(product, "IN", line.getQuantity(), "Compra recibida #" + id);
        }
        record.setStatus(OperationStatus.RECEIVED);
        Operation result = save(record);
        activityLog.record("PURCHASE_RECEIVED", "Se recibió la compra #" + id + " por " + record.getTotal());
        return result;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Cash openCash(OpenCash request, String username) {
        if (activeRegister().isPresent()) throw new IllegalStateException("Ya hay una caja abierta");
        CashRegister register = new CashRegister();
        register.setStatus("OPEN");
        register.setOpeningAmount(request.openingAmount());
        register.setBalance(request.openingAmount());
        register.setOpenedBy(username);
        Cash opened = Cash.from(registers.save(register));
        activityLog.record("CASH_OPENED", "Se abrió la caja #" + opened.id() + " con fondo " + request.openingAmount());
        return opened;
    }
    @Transactional(readOnly = true) public Optional<Cash> activeCash() { return activeRegister().map(Cash::from); }
    @Transactional(readOnly = true)
    public List<CashMovement> cashMovements() {
        CashRegister register = activeRegister().orElseThrow(() -> new IllegalStateException("No hay una caja activa"));
        return movements.findAllByRegisterIdOrderByCreatedAtDesc(register.getId()).stream().map(CashMovement::from).toList();
    }
    @Transactional public Cash closeCash(String username) {
        CashRegister register = requireActiveCash();
        register.setStatus("CLOSED");
        register.setClosingAmount(register.getBalance());
        register.setClosedBy(username);
        register.setClosedAt(java.time.LocalDateTime.now());
        activityLog.record("CASH_CLOSED", "Se cerró la caja #" + register.getId() + " con saldo " + register.getBalance());
        return Cash.from(register);
    }
    @Transactional public Cash movement(String direction, BusinessRequests.Movement request, String username) {
        CashRegister register = requireActiveCash();
        if (!direction.equals("IN") && !direction.equals("OUT")) throw new IllegalArgumentException("Movimiento debe ser IN o OUT");
        if (direction.equals("OUT") && register.getBalance().compareTo(request.amount()) < 0)
            throw new IllegalArgumentException("La caja no tiene saldo suficiente");
        com.example.taskmanager.entity.CashMovement movement = new com.example.taskmanager.entity.CashMovement();
        movement.setRegister(register);
        movement.setDirection(direction);
        movement.setAmount(request.amount());
        movement.setDescription(request.description().trim());
        movement.setCreatedBy(username);
        movements.save(movement);
        register.setBalance(register.getBalance().add(direction.equals("IN") ? request.amount() : request.amount().negate()));
        activityLog.record("CASH_MOVEMENT_" + direction, "Movimiento de caja por " + request.amount() + ": " + request.description().trim());
        return Cash.from(register);
    }

    @Transactional
    public Operation createSale(Sale request) {
        CashRegister register = registers.findOpenForSale(PageRequest.of(0, 1)).stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de realizar esta operación"));
        OperationRecord sale = newRecord(OperationType.SALE, OperationStatus.PAID, null, request.paymentMethod());
        BigDecimal total = BigDecimal.ZERO;
        for (Line item : request.items()) {
            Product product = lockProduct(item.productId());
            if (product.getStock() < item.quantity()) throw new IllegalArgumentException("Stock insuficiente para " + product.getName());
            product.setStock(product.getStock() - item.quantity());
            addLine(sale, product, item.quantity(), product.getPrice());
            inventoryMovementLog.record(product, "OUT", -item.quantity(), "Venta en comprobante " + sale.getReceiptNumber());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        sale.setTotal(total);
        sale.setPaidTotal(total);
        sale.setReceiptNumber("R-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
        Operation result = save(sale);
        if (request.paymentMethod().equals("CASH")) {
            com.example.taskmanager.entity.CashMovement movement = new com.example.taskmanager.entity.CashMovement();
            movement.setRegister(register); movement.setDirection("IN"); movement.setAmount(total);
            movement.setDescription("Venta " + sale.getReceiptNumber()); movement.setCreatedBy(activityLog.currentUsername());
            movements.save(movement);
        }
        activityLog.record("SALE_CREATED", "Se registró la venta " + sale.getReceiptNumber() + " por " + total
            + " con pago " + request.paymentMethod());
        BigDecimal cashIncrease = request.paymentMethod().equals("CASH") ? total : BigDecimal.ZERO;
        if (registers.addBalanceIfOpen(register.getId(), cashIncrease) != 1)
            throw new IllegalStateException("La caja se cerró antes de completar la venta");
        return result;
    }
    @Transactional(readOnly = true) public List<Operation> sales() { return list(OperationType.SALE); }
    @Transactional(readOnly = true) public List<Operation> receipts() { return sales(); }
    @Transactional(readOnly = true)
    public Operation receipt(String number) {
        return operations.findByReceiptNumber(number).map(Operation::from)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró el comprobante " + number));
    }
    @Transactional(readOnly = true)
    public byte[] receiptPdf(String number) {
        Operation sale = receipt(number);
        List<Map<String, ?>> rows = sale.items().stream().<Map<String, ?>>map(line -> {
            Map<String, Object> row = new HashMap<>();
            row.put("sku", line.sku()); row.put("name", line.name()); row.put("quantity", line.quantity());
            row.put("unitPrice", line.unitPrice()); row.put("lineTotal", line.lineTotal());
            return row;
        }).toList();
        try (InputStream template = new ClassPathResource("reports/sales-receipt.jrxml").getInputStream()) {
            var report = JasperCompileManager.compileReport(template);
            Map<String, Object> params = new HashMap<>();
            params.put("receiptNumber", sale.receiptNumber()); params.put("createdAt", sale.createdAt().toString());
            params.put("total", sale.total()); params.put("paidTotal", sale.paidTotal());
            params.put("paymentMethod", sale.description());
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(report, params, new JRMapCollectionDataSource(rows)));
        } catch (Exception exception) {
            return buildFallbackReceiptPdf(sale);
        }
    }

    private byte[] buildFallbackReceiptPdf(Operation sale) {
        StringBuilder summary = new StringBuilder();
        summary.append("Comprobante: ").append(sale.receiptNumber()).append("\n");
        summary.append("Fecha: ").append(sale.createdAt()).append("\n");
        summary.append("Pago: ").append(sale.description()).append("\n\n");
        for (var line : sale.items()) {
            summary.append(line.sku()).append(" - ").append(line.name())
                .append(" x ").append(line.quantity())
                .append(" | ").append(line.lineTotal()).append("\n");
        }
        summary.append("\nTotal: ").append(sale.total());

        String escapedText = summary.toString().replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 12 Tf\n50 750 Td\n");
        String[] lines = escapedText.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                stream.append("0 -15 Td\n");
            }
            stream.append('(').append(lines[i]).append(") Tj\n");
        }
        stream.append("ET\n");

        byte[] content = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<String> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        objects.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");
        objects.add("4 0 obj\n<< /Length " + content.length + " >>\nstream\n");
        objects.add("endstream\nendobj\n");
        objects.add("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            if (i == 3) {
                output.writeBytes(objects.get(i).getBytes(StandardCharsets.ISO_8859_1));
                output.writeBytes(content);
            } else {
                output.writeBytes(objects.get(i).getBytes(StandardCharsets.ISO_8859_1));
            }
        }

        int startXref = output.size();
        output.writeBytes(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i < offsets.size(); i++) {
            output.writeBytes(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
        }
        output.writeBytes(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + startXref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        return output.toByteArray();
    }

    @Transactional public Operation createReservation(Reservation request) {
        OperationRecord record = newRecord(OperationType.RESERVATION, OperationStatus.RESERVED, request.customer(), null);
        BigDecimal total = BigDecimal.ZERO;
        for (Line item : request.items()) {
            Product product = lockProduct(item.productId());
            if (product.getStock() < item.quantity()) throw new IllegalArgumentException("Stock insuficiente para " + product.getName());
            product.setStock(product.getStock() - item.quantity());
            addLine(record, product, item.quantity(), product.getPrice());
            inventoryMovementLog.record(product, "OUT", -item.quantity(), "Apartado para " + request.customer());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        record.setTotal(total);
        Operation result = save(record);
        activityLog.record("RESERVATION_CREATED", "Se creó el apartado #" + result.id() + " por " + total);
        return result;
    }
    @Transactional(readOnly = true) public List<Operation> reservations() { return list(OperationType.RESERVATION); }
    @Transactional public Operation reservationStatus(long id, OperationStatus status) {
        OperationRecord record = getOperation(id, OperationType.RESERVATION);
        if (record.getStatus() != OperationStatus.RESERVED) throw new IllegalStateException("La reserva ya fue procesada");
        if (status == OperationStatus.CANCELLED) {
            for (OperationLine line : record.getLines()) {
                Product product = lockProduct(line.getProduct().getId());
                product.setStock(product.getStock() + line.getQuantity());
                inventoryMovementLog.record(product, "IN", line.getQuantity(), "Cancelación del apartado #" + id);
            }
        } else if (status != OperationStatus.FULFILLED) {
            throw new IllegalArgumentException("Una reserva solo se puede cancelar o entregar");
        }
        record.setStatus(status);
        Operation result = save(record);
        activityLog.record("RESERVATION_" + status, "El apartado #" + id + " cambió a " + status);
        return result;
    }

    @Transactional public Operation createWorkshop(Workshop request) {
        OperationRecord record = newRecord(OperationType.WORKSHOP, OperationStatus.OPEN, request.customer(), request.description());
        BigDecimal total = BigDecimal.ZERO;
        if (request.items() != null) for (Line item : request.items()) {
            Product product = getProduct(item.productId());
            addLine(record, product, item.quantity(), product.getPrice());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        record.setTotal(total);
        return save(record);
    }
    @Transactional(readOnly = true) public List<Operation> workshops() { return list(OperationType.WORKSHOP); }
    @Transactional public Operation workshopStatus(long id, OperationStatus status) {
        OperationRecord record = getOperation(id, OperationType.WORKSHOP);
        if (!Set.of(OperationStatus.OPEN, OperationStatus.IN_PROGRESS, OperationStatus.READY, OperationStatus.COMPLETED).contains(status))
            throw new IllegalArgumentException("Estado no válido para una orden de taller");
        record.setStatus(status);
        return save(record);
    }

    private OperationRecord newRecord(OperationType type, OperationStatus status, String customer, String description) {
        OperationRecord record = new OperationRecord();
        record.setType(type); record.setStatus(status);
        record.setCustomer(customer == null ? null : customer.trim());
        record.setDescription(description == null ? null : description.trim());
        return record;
    }
    private void addLine(OperationRecord record, Product product, int quantity, BigDecimal price) {
        OperationLine line = new OperationLine();
        line.setProduct(product); line.setProductSku(product.getSku()); line.setProductName(product.getName());
        line.setQuantity(quantity); line.setUnitPrice(price);
        record.addLine(line);
    }
    private Operation save(OperationRecord record) { return Operation.from(operations.save(record)); }
    private List<Operation> list(OperationType type) {
        return operations.findAllByTypeOrderByCreatedAtDesc(type).stream().map(Operation::from).toList();
    }
    private OperationRecord getOperation(long id, OperationType type) {
        OperationRecord record = operations.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("No se encontró la operación " + id));
        if (record.getType() != type) throw new IllegalArgumentException("La operación no corresponde a este módulo");
        return record;
    }
    private Product getProduct(long id) {
        return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
    private Product lockProduct(long id) {
        return products.findByIdForUpdate(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
    private Optional<CashRegister> activeRegister() { return registers.findFirstByStatusOrderByOpenedAtDesc("OPEN"); }
    private CashRegister requireActiveCash() {
        return activeRegister().orElseThrow(() -> new IllegalStateException("Debe abrir la caja antes de realizar esta operación"));
    }
}
