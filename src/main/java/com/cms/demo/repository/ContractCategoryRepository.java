package com.cms.demo.repository;


import com.cms.demo.entity.MasterContractCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractCategoryRepository
        extends JpaRepository<MasterContractCategory, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}