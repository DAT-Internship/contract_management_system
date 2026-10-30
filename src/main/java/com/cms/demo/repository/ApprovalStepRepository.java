package com.cms.demo.repository;

import com.cms.demo.entity.ApprovalStep;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    List<ApprovalStep> findByContractContractIdOrderByCreatedAtDesc(Long contractId);
}
