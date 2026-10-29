package com.example.taskmanager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class BusinessRequests {
    private BusinessRequests() {}
    public record Line(@NotNull @Positive Long productId, @Positive int quantity) {}
    public record PurchaseLine(@NotNull @Positive Long productId, @Positive int quantity,
                               @NotNull @DecimalMin("0.00") BigDecimal unitCost) {}
    public record Quote(@NotBlank @Size(max=160) String customer, @Size(max=500) String description,
                        @NotEmpty List<@Valid Line> items) {}
    public record Purchase(@NotBlank @Size(max=160) String supplier, @Size(max=500) String description,
                           @NotEmpty List<@Valid PurchaseLine> items) {}
    public record Sale(@NotEmpty List<@Valid Line> items, @NotBlank @Pattern(regexp="CASH|CARD|TRANSFER", message="El pago debe ser CASH, CARD o TRANSFER") String paymentMethod) {}
    public record Reservation(@NotBlank @Size(max=160) String customer, @NotEmpty List<@Valid Line> items) {}
    public record Workshop(@NotBlank @Size(max=160) String customer, @NotBlank @Size(max=500) String description,
                           List<@Valid Line> items) {}
    public record Status(@NotNull com.example.taskmanager.entity.OperationStatus status) {}
    public record OpenCash(@NotNull @DecimalMin("0.00") BigDecimal openingAmount) {}
    public record Movement(@NotNull @DecimalMin(value="0.01") BigDecimal amount, @NotBlank @Size(max=240) String description) {}
}
