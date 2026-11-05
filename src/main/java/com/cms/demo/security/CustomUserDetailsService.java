package com.cms.demo.security;

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
        
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with Employee Code: " + employeeCode));

        // 🔴 ဒီနေရာမှာ ကိုယ်ပိုင် CustomUserDetails ကို return လုပ်လိုက်ပါ
        return new CustomUserDetails(user);
    }
}