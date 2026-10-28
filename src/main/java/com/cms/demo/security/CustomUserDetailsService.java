package com.cms.demo.security;

import com.cms.demo.entity.Role;
// ✅ မင်း ရဲ့ Entity ကို Import လုပ်ထားတယ်
import com.cms.demo.entity.User;
import com.cms.demo.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String employeeCode) throws UsernameNotFoundException {
        
        // ၁။ Database ထဲမှာ ရှာမယ် (ဒီနေရာမှာ User က မင်း Entity ပါ)
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with Employee Code: " + employeeCode));

     
        // ၂။ Role ကို စစ်မယ်
        String roleCode = (user.getRole() != null) ? user.getRole().getRoleCode() : "USER";

        // ၃။ Return ပြန်မယ် (ဒီနေရာမှာ Spring Security ရဲ့ User ကို Full Path နဲ့ ခေါ်ထားတယ်)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmployeeCode())
                .password(user.getPasswordHash())
                .roles(roleCode)
                .disabled(user.getIsActive() == null || !user.getIsActive()) 
                .build();
    }
}