package com.cms.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractPaymentSummaryResponse(
        Long contractId,
        String contractNo,
        String contractPoNo,
        String poNoRef,
        String description,
        Long partyId,
        String partyName,
        Integer categoryId,
        String categoryName,
        String agreementForName,
        String departmentName,
        String projectCode,
        Integer paymentTypeId,
        String paymentTypeName,
        String currencyName,
        BigDecimal contractValue,
        BigDecimal nextInstallmentAmount,
        BigDecimal commercialTaxRate,
        BigDecimal contractGross,
        BigDecimal remainingInvoiceable,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal outstandingAmount,
        int invoiceCount,
        int invoiceLimit,
        LocalDate nextDueDate,
        boolean hasOverdue,
        String paymentStatus,
        String paymentStatusName,
        int paymentProgressPercent,
        String contractStatus,
        LocalDate startDate,
        LocalDate endDate,
        long remainingDays) {
}
