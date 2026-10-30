package com.cms.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class ContractForm {
    private Long contractId;
    private String contractNo;
    @NotBlank @Size(max = 40)
    private String contractPoNo;
    private String poNoRef;
    @NotNull private Integer agreementForId;
    @NotNull private Integer categoryId;
    @NotNull private Integer contractTypeId;
    @NotNull private Integer paymentTypeId;
    @NotNull private Integer statusId;
    @NotNull private Integer departmentId;
    @NotBlank @Size(max = 10) private String fiscalYear;
    @NotNull private Long partyId;
    @NotBlank private String description;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate signedDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registerDate;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private Integer periodMonths;
    @DecimalMin("0.0")
    private BigDecimal headcountPerMonth;
    @NotNull @DecimalMin("0.0")
    private BigDecimal contractValue;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal commercialTaxRate;
    @NotNull private Integer currencyId;
    private Long projectId;
    private Long projectInchargeId;
    private Long contractManagerId;
    private String clientIncharge;
    private Long renewedFromId;

    public ContractRequest toRequest() {
        return new ContractRequest(
                contractNo, contractPoNo, poNoRef,
                agreementForId, categoryId, contractTypeId, paymentTypeId,
                statusId, departmentId, fiscalYear, partyId, description,
                signedDate, registerDate, startDate, endDate, periodMonths,
                headcountPerMonth, contractValue, commercialTaxRate,
                currencyId, projectId, projectInchargeId, contractManagerId,
                clientIncharge, renewedFromId);
    }
}
