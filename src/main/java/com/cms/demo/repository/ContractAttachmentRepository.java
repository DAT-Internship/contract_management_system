package com.cms.demo.repository;

import com.cms.demo.entity.Contract;
import com.cms.demo.entity.ContractAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractAttachmentRepository extends JpaRepository<ContractAttachment, Long> {
    List<ContractAttachment> findByContractContractId(Long contractId);
    List<ContractAttachment> findByContract(Contract contract);
}
