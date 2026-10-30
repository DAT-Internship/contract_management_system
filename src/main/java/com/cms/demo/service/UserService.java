package com.cms.demo.service;

import com.cms.demo.entity.Department;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.DepartmentRepository; // 🔴 ဒါလေး ထည့်ပြီ
import com.cms.demo.repository.RoleRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.security.StaffIdValidator;

import jakarta.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository; // 🔴 ဒါလေး ထည့်ပြီ
    private final PasswordEncoder passwordEncoder;

    // 🔴 Constructor ထဲမှာ DepartmentRepository ကို ထည့်ပြီ
    public UserService(UserRepository userRepository, RoleRepository roleRepository, 
                       DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User getUserById(Long id) { 
        return userRepository.findById(id).orElseThrow(); 
    }
    
    // Read (Get All)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Create
    public void createUser(User user, Integer roleId) {
        // ၁။ Staff ID Format စစ်ဆေးခြင်း
        if (!StaffIdValidator.isValid(user.getEmployeeCode())) {
            throw new RuntimeException("Invalid Staff ID format!");
        }
        
        // ၂။ ထပ်နေခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
        if (userRepository.existsByEmployeeCode(user.getEmployeeCode())) {
            throw new RuntimeException("Staff ID already exists!");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        // ၃။ Password ကို Hash လုပ်ခြင်း (Admin က ပို့ပေးတဲ့ Password ကို သုံးမယ်)
        // (မှတ်ချက် - Controller က password ကို လက်ခံပြီး Hash လုပ်ပေးထားပြီးသားဆိုရင် ဒါကို ပြင်ရမယ်)
        if(user.getPasswordHash() == null || user.getPasswordHash().isEmpty()){
             user.setPasswordHash(passwordEncoder.encode("admin123")); 
        }
        
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // ၄။ Role ကို ချိတ်ဆက်ခြင်း
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        userRepository.save(user);
    }

    // Update
    @Transactional
    public void updateUser(Long userId, User updatedUser, Integer roleId, Integer departmentId) {
        // ၁။ Database ထဲက မူရင်း User ကို ရှာမယ်
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ၂။ အချက်အလက်အသစ်တွေ ထည့်မယ် (Staff ID ကို မပြောင်းပါဘူး)
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setUpdatedAt(LocalDateTime.now());

        // ၃။ Role နဲ့ Department အသစ် ရှာပြီး ချိတ်မယ်
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        existingUser.setRole(role);

        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        existingUser.setDepartment(dept);

        // ၄။ သိမ်းမယ်
        userRepository.save(existingUser);
    }
    
    // Delete (Deactivate)
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false); // ဖျက်မပစ်ဘဲ ပိတ်ပစ်မယ်
        userRepository.save(user);
    }
    
 // Password ပြောင်းခြင်း
    @Transactional
    public void changePassword(String employeeCode, String currentPassword, String newPassword) {
        // ၁။ User ကို ရှာမယ်
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ၂။ Current Password ကို စစ်မယ် (ရိုက်ထည့်တဲ့ Password နဲ့ Database ထဲက Hash ကို နှိုင်းယှဉ်မယ်)
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect!");
        }

        // ၃။ New Password ကို Hash လုပ်ပြီး သိမ်းမယ်
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        userRepository.save(user);
    }
}