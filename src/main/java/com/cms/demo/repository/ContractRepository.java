package com.cms.demo.repository;

import com.cms.demo.entity.Contract;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNo(String contractNo);

    @EntityGraph(attributePaths = {
            "party", "category", "agreementFor", "department", "project",
            "paymentType", "currency", "status", "createdBy"
    })
    List<Contract> findAllByOrderByRegisterDateDescContractIdDesc();

    @EntityGraph(attributePaths = {
            "createdBy", "projectIncharge", "contractManager", "project.projectManager", "status"
    })
    @Query("""
            select c from Contract c
            where c.endDate = :endDate
              and upper(c.status.code) in ('CS001', 'ACTIVE')
            """)
    List<Contract> findActiveContractsEndingOn(@Param("endDate") LocalDate endDate);

    @EntityGraph(attributePaths = {
            "createdBy", "projectIncharge", "contractManager", "project.projectManager", "status"
    })
    @Query("select c from Contract c where c.contractId = :contractId")
    Optional<Contract> findNotificationDetailsByContractId(@Param("contractId") Long contractId);
}
