package com.cms.demo.config;

import com.cms.demo.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    // ၁။ Password ကို Hash လုပ်ဖို့
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ၂။ Database ကနေ User ကို ရှာပြီး စစ်ဆေးမယ့် Provider
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // 🔴 ဒီနေရာမှာ CustomUserDetailsService ကို Constructor ထဲ ထည့်ပေးရပါမယ်
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(customUserDetailsService);
        
        authProvider.setPasswordEncoder(passwordEncoder()); 
        return authProvider;
    }
    // ၃။ Security စည်းမျဉ်းများ
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**").permitAll() 
                .anyRequest().authenticated() 
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true) 
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
}