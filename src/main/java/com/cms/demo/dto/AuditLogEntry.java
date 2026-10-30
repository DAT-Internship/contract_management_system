package com.cms.demo.dto;

import java.time.LocalDateTime;

public record AuditLogEntry(
        Long id,
        String actorName,
        String employeeCode,
        String action,
        String entityType,
        Long entityId,
        String details,
        String ipAddress,
        LocalDateTime createdAt) {
}
