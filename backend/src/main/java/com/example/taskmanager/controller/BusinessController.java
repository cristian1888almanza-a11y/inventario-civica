
package com.example.taskmanager.controller;

import com.example.taskmanager.dto.BusinessRequests.*;
import com.example.taskmanager.dto.BusinessResponses.*;
import com.example.taskmanager.entity.OperationStatus;
import com.example.taskmanager.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class BusinessController {
    private final BusinessService service;
    public BusinessController(BusinessService service) { this.service = service; }

    @GetMapping("/quotes") public List<Operation> quotes() { return service.quotes(); }
    @PostMapping("/quotes") @ResponseStatus(HttpStatus.CREATED)
    public Operation quote(@Valid @RequestBody Quote request) { return service.createQuote(request); }
    @PatchMapping("/quotes/{id}/status")
    public Operation quoteStatus(@PathVariable long id, @Valid @RequestBody Status request) { return service.quoteStatus(id, request.status()); }

    @GetMapping("/purchases") public List<Operation> purchases() { return service.purchases(); }
    @PostMapping("/purchases") @ResponseStatus(HttpStatus.CREATED)
    public Operation purchase(@Valid @RequestBody Purchase request) { return service.createPurchase(request); }
    @PostMapping("/purchases/{id}/receive")
    public Operation receivePurchase(@PathVariable long id) { return service.receivePurchase(id); }

    @GetMapping("/cash/active") public Cash activeCash() { return service.activeCash().orElse(null); }
    @GetMapping("/cash/movements") public List<CashMovement> cashMovements() { return service.cashMovements(); }
    @PostMapping("/cash/open") @ResponseStatus(HttpStatus.CREATED)
    public Cash openCash(@Valid @RequestBody OpenCash request, org.springframework.security.core.Authentication authentication) {
        return service.openCash(request, authentication.getName());
    }
    @PostMapping("/cash/close")
    public Cash closeCash(org.springframework.security.core.Authentication authentication) { return service.closeCash(authentication.getName()); }
    @PostMapping("/cash/movements/{direction}")
    public Cash movement(@PathVariable String direction, @Valid @RequestBody Movement request,
                         org.springframework.security.core.Authentication authentication) {
        return service.movement(direction.toUpperCase(), request, authentication.getName());
    }

    @GetMapping("/sales") public List<Operation> sales() { return service.sales(); }
    @PostMapping("/sales") @ResponseStatus(HttpStatus.CREATED)
    public Operation sale(@Valid @RequestBody Sale request) { return service.createSale(request); }

    @GetMapping("/reservations") public List<Operation> reservations() { return service.reservations(); }
    @PostMapping("/reservations") @ResponseStatus(HttpStatus.CREATED)
    public Operation reservation(@Valid @RequestBody Reservation request) { return service.createReservation(request); }
    @PatchMapping("/reservations/{id}/status")
    public Operation reservationStatus(@PathVariable long id, @Valid @RequestBody Status request) {
        return service.reservationStatus(id, request.status());
    }

    @GetMapping("/workshop/orders") public List<Operation> workshops() { return service.workshops(); }
    @PostMapping("/workshop/orders") @ResponseStatus(HttpStatus.CREATED)
    public Operation workshop(@Valid @RequestBody Workshop request) { return service.createWorkshop(request); }
    @PatchMapping("/workshop/orders/{id}/status")
    public Operation workshopStatus(@PathVariable long id, @Valid @RequestBody Status request) {
        return service.workshopStatus(id, request.status());
    }

    @GetMapping("/receipts") public List<Operation> receipts() { return service.receipts(); }
    @GetMapping("/receipts/{receiptNumber}") public Operation receipt(@PathVariable String receiptNumber) { return service.receipt(receiptNumber); }
    @GetMapping(value="/receipts/{receiptNumber}/pdf", produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receiptPdf(@PathVariable String receiptNumber) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("comprobante-" + receiptNumber + ".pdf").build().toString())
            .body(service.receiptPdf(receiptNumber));
    }
}
