package com.cms.demo.repository;

import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.MasterContractType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractStatusRepository
        extends JpaRepository<MasterContractStatus, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}