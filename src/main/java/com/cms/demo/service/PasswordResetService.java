package com.cms.demo.service;

import com.cms.demo.entity.PasswordResetRequest;
import com.cms.demo.entity.User;
import com.cms.demo.repository.PasswordResetRequestRepository;
import com.cms.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PasswordResetService {

    private final PasswordResetRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService; // 🔴 AuditLogService ကို ထည့်ပါ

    public PasswordResetService(PasswordResetRequestRepository requestRepository, 
                                UserRepository userRepository, 
                                PasswordEncoder passwordEncoder,
                                AuditLogService auditLogService) { // 🔴 Constructor မှာ ချိတ်ပါ
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    // ၁။ User က Request တင်တဲ့အခါ (CREATE)
    public void submitRequest(PasswordResetRequest request) {
        requestRepository.save(request);

        // 🔴 Audit Log ထည့်မယ်
        String newValue = "{\"Employee Code\":\"" + request.getEmployeeCode() + 
                          "\", \"Reason\":\"" + request.getReason() + "\"}";

        auditLogService.record(
            request.getEmployeeCode(), // လုပ်ဆောင်သူ (တောင်းဆိုသူ)
            "FORGOT_PASSWORD",         // Entity အမျိုးအစား
            request.getRequestId(),    // ID
            "CREATE",                  // Action
            null,                      // Old Value
            newValue                   // New Value
        );
    }

    // Admin အတွက် Request စာရင်းကြည့်ခြင်း
    public List<PasswordResetRequest> getPendingRequests() {
        return requestRepository.findByStatus("PENDING");
    }

    // ၂။ Admin က Reset လုပ်ပေးတဲ့အခါ (UPDATE)
    @Transactional
    public void resetUserPassword(Long requestId, String actorCode) { // 🔴 actorCode ထည့်ပါ
        PasswordResetRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
                
        User user = userRepository.findByEmployeeCode(request.getEmployeeCode())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPasswordHash(passwordEncoder.encode(UserService.DEFAULT_PASSWORD));
        userRepository.save(user);

        request.setStatus("RESOLVED");
        requestRepository.save(request);

        // 🔴 Audit Log ထည့်မယ်
        String oldValue = "{\"Status\":\"PENDING\"}";
        String newValue = "{\"Status\":\"RESOLVED\", \"Password\":\"Reset to default\"}";

        auditLogService.record(
            actorCode,                 // လုပ်ဆောင်သူ (Admin)
            "FORGOT_PASSWORD",         // Entity အမျိုးအစား
            requestId,                 // ID
            "UPDATE",                  // Action
            oldValue,                  // Old Value
            newValue                   // New Value
        );
    }
}