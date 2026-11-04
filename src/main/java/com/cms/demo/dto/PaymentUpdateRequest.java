package com.cms.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentUpdateRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal paidAmount,
        LocalDate paidDate,
        @NotBlank @Size(max = 500) String note) {
}
