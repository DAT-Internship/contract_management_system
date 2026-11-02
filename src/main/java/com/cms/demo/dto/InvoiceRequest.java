package com.cms.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRequest(
        @NotBlank @Size(max = 4000) String description,
        @Size(max = 40) String invoiceNo,
        LocalDate invoiceDate,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin(value = "0.00") BigDecimal amount,
        @DecimalMin(value = "0.00") BigDecimal taxAmount,
        Integer currencyId,
        @Size(max = 500) String note) {
}
