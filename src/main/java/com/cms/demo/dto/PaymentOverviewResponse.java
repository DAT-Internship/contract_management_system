package com.cms.demo.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment KPI cards across all contracts visible to the current user.
 * Currency totals are grouped because contracts may use different currencies.
 */
public record PaymentOverviewResponse(
        int contractsTracked,
        int totalInvoices,
        int overdueInvoiceCount,
        int overdueContracts,
        int fullySettledContracts,
        Map<String, BigDecimal> totalInvoicedByCurrency,
        Map<String, BigDecimal> totalPaidByCurrency,
        Map<String, BigDecimal> outstandingByCurrency) {
}
