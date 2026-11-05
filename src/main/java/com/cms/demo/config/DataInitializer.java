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
        System.out.println("🔍 System Roles နှင့် Default Admin ကို စတင်ဖန်တီးနေပါသည်...");

        // ၁။ Migration ထဲက ROOT Department ကို ရှာမယ် (မရှိရင် Error ပြမယ်)
        Department rootDept = departmentRepository.findByDeptCodeIgnoreCase("ROOT")
                .orElseThrow(() -> new IllegalStateException(
                        "ROOT department မရှိပါ။ Database Migration ကို အရင် Run ပေးပါ။"));

        // ၂။ Management (MGMT) Department ကို ဆောက်မယ် (ROOT အောက်မှာ)
        Department mgmtDept = departmentRepository.findByDeptCodeIgnoreCase("MGMT").orElseGet(() -> {
            Department newDept = new Department();
            newDept.setDeptCode("MGMT");
            newDept.setDeptName("Management");
            newDept.setIsActive(true);
            newDept.setCreatedAt(LocalDateTime.now());
            newDept.setParentDepartment(rootDept); // Migration ထဲက ROOT အောက်မှာ ချိတ်မယ်
            return departmentRepository.save(newDept);
        });

        // ၃။ သင့်ပုံထဲက Roles ၆ ခုလုံးကို Database ထဲမှာ သေချာဆောက်မယ်
        createRoleIfNotFound("ROOT", "Root");
        createRoleIfNotFound("PMO", "Admin");
        createRoleIfNotFound("CM", "Admin");
        createRoleIfNotFound("APPROVER", "Approver");
        createRoleIfNotFound("FINANCE", "Finance");
        createRoleIfNotFound("PM", "Operator (PM)");

        // ၄။ Default ROOT User ကို ဖန်တီးမယ်
        if (userRepository.findByEmployeeCode("25-00001").isEmpty()) {
            
            // Database ထဲက ROOT Role ကို ပြန်ဆွဲထုတ်မယ်
            Role rootRole = roleRepository.findByRoleCode("ROOT")
                    .orElseThrow(() -> new IllegalStateException("ROOT role မရှိပါ"));

            User admin = new User();
            admin.setEmployeeCode("25-00001"); 
            admin.setFullName("System Administrator");
            admin.setEmail("admin@cms.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123")); 
            admin.setDepartment(mgmtDept); // Management ဌာနအောက်မှာ ထားမယ်
            admin.setRole(rootRole);       // ROOT Role ကို ပေးမယ်
            admin.setIsActive(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());

            userRepository.save(admin);
            System.out.println("✅ Default ROOT User (25-00001) ကို အောင်မြင်စွာ ဖန်တီးပြီးပါပြီ!");
        } else {
            System.out.println("ℹ️ Default Admin User ရှိပြီးသား ဖြစ်ပါသည်။");
        }
    }

    // Role တွေ မရှိသေးရင် ဆောက်ပေးမယ့် Helper Method
    private void createRoleIfNotFound(String code, String name) {
        if (roleRepository.findByRoleCode(code).isEmpty()) {
            Role role = new Role();
            role.setRoleCode(code);
            role.setRoleName(name);
            roleRepository.save(role);
            System.out.println("✅ Role အသစ်ဆောက်ပြီး: " + code + " - " + name);
        }
    }
}