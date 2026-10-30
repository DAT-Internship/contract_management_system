package com.cms.demo.repository;


import com.cms.demo.entity.MasterCurrency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository
        extends JpaRepository<MasterCurrency, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}