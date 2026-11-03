package com.cms.demo.dto;

import java.time.LocalDateTime;

public record AuditLogEntry(
        Long id,
        String actorName,
        String employeeCode,
        String action,
        String entityType,
        Long entityId,
        String oldValue, // 🔴 သီးသန့် ခွဲထုတ်လိုက်တယ်
        String newValue, // 🔴 သီးသန့် ခွဲထုတ်လိုက်တယ်
        LocalDateTime createdAt) {
}