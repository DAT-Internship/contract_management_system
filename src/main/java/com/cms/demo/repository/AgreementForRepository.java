package com.cms.demo.repository;

import com.cms.demo.entity.MasterAgreementFor;


import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementForRepository
        extends JpaRepository<MasterAgreementFor, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}