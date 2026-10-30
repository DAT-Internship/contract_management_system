package com.cms.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(
        Long contractId,
        String contractNo,
        String contractPoNo,
        String poNoRef,
        String fiscalYear,
        String partyName,
        String description,
        LocalDate signedDate,
        LocalDate registerDate,
        LocalDate startDate,
        LocalDate endDate,
        Integer periodMonths,
        Long remainingDays,
        BigDecimal headcountPerMonth,
        BigDecimal totalManMonths,
        BigDecimal contractValue,
        BigDecimal commercialTaxRate,
        BigDecimal commercialTaxAmount,
        String currencyCode,
        String statusCode,
        String statusName,
        String clientIncharge,
        String projectCode,
        String categoryName,
        String departmentName) {
}
