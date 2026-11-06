package com.cms.demo.repository;

import com.cms.demo.entity.ApprovalStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByContractContractIdOrderByCreatedAtDesc(Long contractId);
    List<ApprovalStep> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
    Optional<ApprovalStep> findFirstByContractContractIdAndStatusInOrderByCreatedAtDesc(Long contractId, List<String> statuses);

    @Query(value = "select distinct s from ApprovalStep s join fetch s.contract c "
            + "left join fetch c.department left join fetch c.party left join fetch c.currency "
            + "left join fetch c.project left join fetch c.createdBy left join fetch s.submittedBy "
            + "where s.status in :statuses "
            + "and (:allDepartments = true or c.department.departmentId = :departmentId) "
            + "and not exists (select newer.stepId from ApprovalStep newer "
            + "where newer.contract.contractId = c.contractId and newer.status in :statuses "
            + "and (newer.createdAt > s.createdAt or (newer.createdAt = s.createdAt and newer.stepId > s.stepId) "
            + "or (newer.createdAt is null and s.createdAt is null and newer.stepId > s.stepId))) "
            + "and (:query is null or lower(c.contractNo) like lower(concat('%', :query, '%')) "
            + "or lower(c.party.partyName) like lower(concat('%', :query, '%'))) "
            + "order by s.createdAt desc",
            countQuery = "select count(s) from ApprovalStep s join s.contract c left join c.party "
                    + "where s.status in :statuses "
                    + "and (:allDepartments = true or c.department.departmentId = :departmentId) "
                    + "and not exists (select newer.stepId from ApprovalStep newer "
                    + "where newer.contract.contractId = c.contractId and newer.status in :statuses "
                    + "and (newer.createdAt > s.createdAt or (newer.createdAt = s.createdAt and newer.stepId > s.stepId) "
                    + "or (newer.createdAt is null and s.createdAt is null and newer.stepId > s.stepId))) "
                    + "and (:query is null or lower(c.contractNo) like lower(concat('%', :query, '%')) "
                    + "or lower(c.party.partyName) like lower(concat('%', :query, '%')))")
    Page<ApprovalStep> searchQueue(@Param("statuses") List<String> statuses,
            @Param("allDepartments") boolean allDepartments, @Param("departmentId") Integer departmentId,
            @Param("query") String query, Pageable pageable);

    @Query("select distinct s from ApprovalStep s join fetch s.contract c left join fetch c.department "
            + "left join fetch c.party left join fetch c.currency left join fetch c.project "
            + "left join fetch c.createdBy left join fetch s.submittedBy left join fetch s.approver "
            + "where upper(s.status) in :statuses and (:allDepartments = true or c.department.departmentId = :departmentId) "
            + "order by s.createdAt desc")
    List<ApprovalStep> findForDepartments(@Param("statuses") List<String> statuses,
            @Param("allDepartments") boolean allDepartments, @Param("departmentId") Integer departmentId);
}
