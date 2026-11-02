package com.cms.demo.repository;

import com.cms.demo.entity.AuditLog;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    @EntityGraph(attributePaths = {"actor", "actor.role"})
    List<AuditLog> findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(String entityType, Collection<Long> entityIds);
}
