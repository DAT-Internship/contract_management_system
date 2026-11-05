package com.cms.demo.repository;

import com.cms.demo.entity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findByStatus(String status);
    
    Optional<PasswordResetRequest>
    findFirstByEmployeeCodeIgnoreCaseOrderByRequestedAtDesc(
            String employeeCode
    );
}