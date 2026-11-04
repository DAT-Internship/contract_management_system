package com.cms.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Thymeleaf form backing object for recording a cumulative payment.
 */
@Data
@NoArgsConstructor
public class PaymentForm {

    @NotNull
    private Long invoiceId;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal paidAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paidDate;

    @NotBlank
    @Size(max = 500)
    private String note;

    public PaymentUpdateRequest toRequest() {
        return new PaymentUpdateRequest(paidAmount, paidDate, note);
    }
}
