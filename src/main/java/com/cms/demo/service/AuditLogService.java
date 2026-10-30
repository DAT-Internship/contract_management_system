package com.cms.demo.service;

import com.cms.demo.dto.AuditLogEntry;
import com.cms.demo.entity.AuditLog;
import com.cms.demo.repository.AuditLogRepository;
import com.cms.demo.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String employeeCode, String entityType, Long entityId, String action,
            String method, String path, int responseStatus, String ipAddress) {
        if (!List.of("CREATE", "UPDATE", "DELETE").contains(action == null ? "" : action.toUpperCase(Locale.ROOT))) return;
        var actor = userRepository.findByEmployeeCode(employeeCode).orElse(null);
        if (actor == null) return;

        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        log.setNewValue("{\"method\":\"" + jsonEscape(method) + "\",\"path\":\""
                + jsonEscape(path) + "\",\"responseStatus\":" + responseStatus + "}");
        auditLogRepository.save(log);
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntry> search(String query, String action, String entityType, LocalDate from,
            LocalDate to, Pageable pageable) {
        Specification<AuditLog> spec = (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.upper(root.get("action")).in("CREATE", "UPDATE", "DELETE"));
            if (hasText(action)) {
                predicates.add(cb.equal(cb.lower(root.get("action")), action.trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(entityType)) {
                predicates.add(cb.equal(cb.lower(root.get("entityType")), entityType.trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                var actor = root.join("actor");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("entityType")), pattern),
                        cb.like(cb.lower(actor.get("fullName")), pattern),
                        cb.like(cb.lower(actor.get("employeeCode")), pattern)));
            }
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return auditLogRepository.findAll(spec, pageable).map(log -> new AuditLogEntry(
                log.getLogId(),
                log.getActor() == null ? "System" : log.getActor().getFullName(),
                log.getActor() == null ? "-" : log.getActor().getEmployeeCode(),
                log.getAction(), log.getEntityType(), log.getEntityId(), log.getNewValue(),
                log.getIpAddress(), log.getCreatedAt()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
