package com.cms.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long invoiceId,
        Long contractId,
        String contractNo,
        String invoiceNo,
        String description,
        String partyName,
        String currencyCode,
        LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal amount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDate paidDate,
        String statusCode,
        String statusName,
        boolean overdue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String updatedBy) {
}
