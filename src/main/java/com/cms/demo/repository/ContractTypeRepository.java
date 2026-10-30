package com.cms.demo.repository;

import com.cms.demo.entity.MasterContractType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractTypeRepository
        extends JpaRepository<MasterContractType, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}