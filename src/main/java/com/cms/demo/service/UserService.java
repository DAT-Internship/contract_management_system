package com.cms.demo.service;

import com.cms.demo.entity.Department;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService; // 🔴 Audit Log Service ကို ထည့်ပါ

    public static final String DEFAULT_PASSWORD = "DAT@2526"; 

    // Constructor မှာ AuditLogService ကို ပါ ချိတ်ပါ
    public UserService(UserRepository userRepository, RoleRepository roleRepository, 
                       DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }
    
    public User getUserById(Long id) { 
        return userRepository.findById(id).orElseThrow(); 
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Create
    public void createUser(User user, Integer roleId, String actorCode) { // 🔴 actorCode ထည့်ပါ
        if (!StaffIdValidator.isValid(user.getEmployeeCode())) {
            throw new RuntimeException("Invalid Staff ID format!");
        }
        if (userRepository.existsByEmployeeCode(user.getEmployeeCode())) {
            throw new RuntimeException("Staff ID already exists!");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        if(user.getPasswordHash() == null || user.getPasswordHash().isEmpty()){
            user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD)); 
        }
        
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        userRepository.save(user);

        // 🔴 Audit Log ထည့်မယ်
        auditLogService.record(
            actorCode, 
            "USER", 
            user.getUserId(), 
            "CREATE", 
            null, // old value မရှိဘူး
            "{\"name\":\"" + user.getFullName() + "\", \"email\":\"" + user.getEmail() + "\"}" 
        );
    }

    // Update
    @Transactional
    public void updateUser(Long userId, User updatedUser, Integer roleId, Integer departmentId, String actorCode) {
        // ၁။ Database ထဲက မူရင်း User ကို ရှာမယ်
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ၂။ Role နဲ့ Department အသစ်တွေကို ရှာမယ်
        Role newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Department newDept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // 🔴 ၃။ ပြောင်းလဲမှု ရှိ/မရှိ စစ်ဆေးပြီး JSON String ဆောက်မယ်
        StringBuilder oldValueBuilder = new StringBuilder();
        StringBuilder newValueBuilder = new StringBuilder();

        // (က) Full Name စစ်ဆေးခြင်း
        if (!existingUser.getFullName().equals(updatedUser.getFullName())) {
            oldValueBuilder.append("\"name\": \"").append(existingUser.getFullName()).append("\", ");
            newValueBuilder.append("\"name\": \"").append(updatedUser.getFullName()).append("\", ");
        }

        // (ခ) Email စစ်ဆေးခြင်း
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            oldValueBuilder.append("\"email\": \"").append(existingUser.getEmail()).append("\", ");
            newValueBuilder.append("\"email\": \"").append(updatedUser.getEmail()).append("\", ");
        }

        // (ဂ) Role စစ်ဆေးခြင်း
        if (existingUser.getRole() != null && !existingUser.getRole().getRoleId().equals(newRole.getRoleId())) {
            oldValueBuilder.append("\"role\": \"").append(existingUser.getRole().getRoleName()).append("\", ");
            newValueBuilder.append("\"role\": \"").append(newRole.getRoleName()).append("\", ");
        }

        // (ဃ) Department စစ်ဆေးခြင်း
        if (existingUser.getDepartment() != null && !existingUser.getDepartment().getDepartmentId().equals(newDept.getDepartmentId())) {
            oldValueBuilder.append("\"department\": \"").append(existingUser.getDepartment().getDeptName()).append("\", ");
            newValueBuilder.append("\"department\": \"").append(newDept.getDeptName()).append("\", ");
        }

        // ၄။ JSON String တွေကို သန့်ရှင်းအောင် လုပ်မယ် (အပိုကော်မာတွေ ဖျက်မယ်)
        String oldValueJson = null;
        String newValueJson = null;

        if (oldValueBuilder.length() > 0) {
            // အဆုံးက ", " ကို ဖျက်ပြီး { } ခွက်ထဲ ထည့်မယ်
            String oldStr = oldValueBuilder.toString();
            oldStr = oldStr.substring(0, oldStr.length() - 2); 
            oldValueJson = "{" + oldStr + "}";

            String newStr = newValueBuilder.toString();
            newStr = newStr.substring(0, newStr.length() - 2);
            newValueJson = "{" + newStr + "}";
        }

        // ၅။ User ကို Update လုပ်မယ်
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(newRole);
        existingUser.setDepartment(newDept);
        existingUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(existingUser);

        // 🔴 ၆။ ပြောင်းလဲမှု တကယ်ရှိမှ Audit Log ထည့်မယ်
        if (oldValueJson != null && newValueJson != null) {
            auditLogService.record(
                actorCode, 
                "USER", 
                userId, 
                "UPDATE", 
                oldValueJson, 
                newValueJson
            );
        }
    }
    
    // Delete (Deactivate)
    public void deactivateUser(Long userId, String actorCode) { // 🔴 actorCode ထည့်ပါ
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false); 
        userRepository.save(user);

        // 🔴 Audit Log ထည့်မယ်
        auditLogService.record(
            actorCode, 
            "USER", 
            userId, 
            "UPDATE", 
            "{\"isActive\":true}", 
            "{\"isActive\":false}"
        );
    }
    
    // Change Password
    @Transactional
    public void changePassword(String employeeCode, String currentPassword, String newPassword) {
        User user = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);

        // 🔴 Audit Log ထည့်မယ် (Password ကို လုံခြုံရေးအရ မထည့်ပါ)
        auditLogService.record(
            employeeCode, 
            "USER", 
            user.getUserId(), 
            "UPDATE", 
            null, 
            "{\"action\":\"Password changed\"}" 
        );
    }
}