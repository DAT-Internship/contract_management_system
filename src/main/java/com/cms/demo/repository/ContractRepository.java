package com.cms.demo.repository;

import com.cms.demo.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNo(String contractNo);

    List<Contract> findAllByOrderByRegisterDateDescContractIdDesc();
}
