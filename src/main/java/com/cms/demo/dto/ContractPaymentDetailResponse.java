package com.cms.demo.dto;

import java.util.List;

public record ContractPaymentDetailResponse(
        ContractPaymentSummaryResponse contract,
        List<InvoiceResponse> invoices,
        List<PaymentHistoryEntry> history) {
}
