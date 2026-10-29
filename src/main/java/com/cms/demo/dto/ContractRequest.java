package com.cms.demo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(
        @Size(max = 30) String contractNo,
        @NotBlank @Size(max = 40) String contractPoNo,
        @Size(max = 40) String poNoRef,
        @NotNull Integer agreementForId,
        @NotNull Integer categoryId,
        @NotNull Integer contractTypeId,
        @NotNull Integer paymentTypeId,
        @NotNull Integer statusId,
        @NotNull Integer departmentId,
        @NotBlank @Size(max = 10) String fiscalYear,
        @NotNull Long partyId,
        @NotBlank String description,
        LocalDate signedDate,
        LocalDate registerDate,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Integer periodMonths,
        @DecimalMin("0.0") BigDecimal headcountPerMonth,
        @NotNull @DecimalMin("0.0") BigDecimal contractValue,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal commercialTaxRate,
        @NotNull Integer currencyId,
        Long projectId,
        Long projectInchargeId,
        Long contractManagerId,
        @Size(max = 120) String clientIncharge,
        Long renewedFromId) {
}
