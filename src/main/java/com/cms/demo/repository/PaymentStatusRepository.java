package com.cms.demo.repository;


import com.cms.demo.entity.MasterPaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusRepository
        extends JpaRepository<MasterPaymentStatus, Integer> {

    boolean existsByCodeIgnoreCase(String code);
}