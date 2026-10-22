package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@Data
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "entity_type", length=40, nullable=false)
    private String entityType;

    @Column(name = "entity_id", nullable=false)
    private Long entityId;

    @Column(name = "action", length=40, nullable=false)
    private String action;

    @Column(name = "old_value", columnDefinition="json")
    private String oldValue;

    @Column(name = "new_value", columnDefinition="json")
    private String newValue;

    @Column(name = "ip_address", length=45)
    private String ipAddress;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /* Design notes:
     * For MySQL JSON columns, this version stores the JSON payload as String. If using Hibernate 6/7 JSON mapping, consider @JdbcTypeCode(SqlTypes.JSON).
     */
}
