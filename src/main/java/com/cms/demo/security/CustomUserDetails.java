package com.cms.demo.security;

import com.cms.demo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final String roleName; // 🔴 Role Name ကို သိမ်းမယ်

    public CustomUserDetails(User user) {
        this.user = user;
        // 🔴 Role Code အစား Role Name ကို ယူမယ်
        this.roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "USER";
    }

    public String getFullName() {
        return user.getFullName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 🔴 Role Name ကို ROLE_ နဲ့ ပေါင်းပြီး ပြန်ပေးမယ်
        // ဥပမာ - Role Name က "Admin" ဆိုရင် "ROLE_Admin" ဖြစ်သွားမယ်
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmployeeCode();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { 
        return user.getIsActive() != null && user.getIsActive(); 
    }
}