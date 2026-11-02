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
 * Thymeleaf form backing object for creating and editing invoices.
 */
@Data
@NoArgsConstructor
public class InvoiceForm {

    private Long invoiceId;

    @NotBlank
    @Size(max = 4000)
    private String description;

    @Size(max = 40)
    private String invoiceNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDate;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal amount;

    @DecimalMin("0.00")
    private BigDecimal taxAmount;

    private Integer currencyId;

    @Size(max = 500)
    private String note;

    public InvoiceRequest toRequest() {
        return new InvoiceRequest(
                description, invoiceNo, invoiceDate, dueDate,
                amount, taxAmount, currencyId, note);
    }
}
