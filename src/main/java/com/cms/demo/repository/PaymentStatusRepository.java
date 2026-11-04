package com.cms.demo.repository;


import com.cms.demo.entity.MasterPaymentStatus;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusRepository
        extends JpaRepository<MasterPaymentStatus, Integer> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<MasterPaymentStatus> findByCodeIgnoreCase(String code);

    Optional<MasterPaymentStatus> findFirstByNameIgnoreCase(String name);
}
