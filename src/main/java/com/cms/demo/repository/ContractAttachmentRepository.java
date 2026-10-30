package com.cms.demo.repository;

import com.cms.demo.entity.Contract;
import com.cms.demo.entity.ContractAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContractAttachmentRepository extends JpaRepository<ContractAttachment, Long> {
    List<ContractAttachment> findByContractContractId(Long contractId);
    @Query("select a from ContractAttachment a left join fetch a.uploadedBy where a.contract.contractId = :contractId order by a.uploadedAt desc, a.attachmentId desc")
    List<ContractAttachment> findWithUploaderByContractId(@Param("contractId") Long contractId);
    List<ContractAttachment> findByContract(Contract contract);
}
