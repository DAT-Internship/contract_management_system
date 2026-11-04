package com.cms.demo.repository;

import com.cms.demo.entity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    List<PasswordResetRequest> findByStatus(String status);
}