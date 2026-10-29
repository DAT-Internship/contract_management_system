package com.cms.demo.repository;

import com.cms.demo.entity.CustomerSupplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSupplierRepository
        extends JpaRepository<CustomerSupplier, Long> {

    boolean existsByPartyCodeIgnoreCase(String partyCode);
}