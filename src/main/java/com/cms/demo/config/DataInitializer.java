package com.cms.demo.config;

import com.cms.demo.entity.Department;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.RoleRepository;
import com.cms.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository, 
                           DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("🔍 Checking for Default User...");
        
        // ၁။ Admin User ရှိပြီးသားလား အရင်စစ်မယ်
        if (userRepository.findByEmployeeCode("25-00001").isEmpty()) {
            
            // ၂။ Department မရှိရင် "Management" ဆိုတဲ့ ဌာန ဆောက်မယ်
            Department dept = departmentRepository.findByDeptCodeIgnoreCase("MGMT").orElseGet(() -> {
                Department newDept = new Department();
                newDept.setDeptCode("MGMT");
                newDept.setDeptName("Management");
                newDept.setIsActive(true);
                newDept.setCreatedAt(LocalDateTime.now());
                newDept.setParentDepartment(null); // မိခင်ဌာန မရှိဘူးလို့ သတ်မှတ်မယ်
                return departmentRepository.save(newDept);
            });

            // ၃။ Role မရှိရင် "ADMIN" Role ဆောက်မယ်
            Role role = roleRepository.findByRoleCode("ADMIN").orElseGet(() -> {
                Role newRole = new Role();
                newRole.setRoleCode("ADMIN");
                newRole.setRoleName("Administrator");
                return roleRepository.save(newRole);
            });

            // ၄။ Default Admin User ကို ဖန်တီးမယ်
            User admin = new User();
            admin.setEmployeeCode("25-00001"); 
            admin.setFullName("System Administrator");
            admin.setEmail("admin@cms.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123")); 
            admin.setDepartment(dept);
            admin.setRole(role);
            admin.setIsActive(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());

            // ၅။ Database ထဲ သိမ်းမယ်
            userRepository.save(admin);
            System.out.println("✅ Default Admin User (25-00001) created successfully in Database!");
        } else {
            System.out.println("ℹ️ Default Admin User already exists in Database.");
        }
    }
}