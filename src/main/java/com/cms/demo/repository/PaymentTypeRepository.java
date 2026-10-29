package com.cms.demo.repository;


import com.cms.demo.entity.MasterPaymentType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTypeRepository
        extends JpaRepository<MasterPaymentType, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}