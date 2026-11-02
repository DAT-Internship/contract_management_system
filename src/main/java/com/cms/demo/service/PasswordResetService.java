package com.cms.demo.service;

import com.cms.demo.entity.PasswordResetRequest;
import com.cms.demo.entity.User;
import com.cms.demo.repository.PasswordResetRequestRepository;
import com.cms.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordResetService {

    private final PasswordResetRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetRequestRepository requestRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Request တင်ခြင်း
    public void submitRequest(PasswordResetRequest request) {
        requestRepository.save(request);
    }

    // Admin အတွက် Request စာရင်းကြည့်ခြင်း
    public List<PasswordResetRequest> getPendingRequests() {
        return requestRepository.findByStatus("PENDING");
    }

    public void resetUserPassword(Long requestId) {
        PasswordResetRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
                
        User user = userRepository.findByEmployeeCode(request.getEmployeeCode())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 🔴 UserService ထဲက DAT@2526 ကို လှမ်းယူသုံးမယ်
        user.setPasswordHash(passwordEncoder.encode(UserService.DEFAULT_PASSWORD));
        userRepository.save(user);

        request.setStatus("RESOLVED");
        requestRepository.save(request);
    }
}