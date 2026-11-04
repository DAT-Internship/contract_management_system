package com.cms.demo.repository;

import com.cms.demo.entity.ApprovalStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByContractContractIdOrderByCreatedAtDesc(Long contractId);
    List<ApprovalStep> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    Optional<ApprovalStep> findFirstByContractContractIdAndStatusInOrderByCreatedAtDesc(Long contractId, List<String> statuses);

    @Query("select distinct s from ApprovalStep s join fetch s.contract c left join fetch c.department "
            + "left join fetch c.party left join fetch c.currency left join fetch c.project "
            + "left join fetch c.createdBy left join fetch s.submittedBy left join fetch s.approver "
            + "where upper(s.status) in :statuses and (:allDepartments = true or c.department.departmentId = :departmentId) "
            + "order by s.createdAt desc")
    List<ApprovalStep> findForDepartments(@Param("statuses") List<String> statuses,
            @Param("allDepartments") boolean allDepartments, @Param("departmentId") Integer departmentId);
}
