package com.cms.demo.dto;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;


public record PaymentHistoryEntry(
        Long logId,
        String action,
        Long invoiceId,
        String actorName,
        String actorRole,
        String actorEmployeeCode,
        String ipAddress,
        LocalDateTime createdAt,
        JsonNode oldValue,
        JsonNode newValue) {
}
