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

    private static final String DEFAULT_ADMIN_CODE = "25-00001";
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
    public void run(String... args) {
        System.out.println("Checking for default users...");

        Department management = ensureDepartment("MGMT", "Management");
        ensureUser(DEFAULT_ADMIN_CODE, "System Administrator", "admin@cms.com", "admin123",
                "ADMIN", "Administrator", "Full system access", management);
    }

    private Department ensureDepartment(String code, String name) {
        return departmentRepository.findByDeptCodeIgnoreCase(code).orElseGet(() -> {
            // parent_dept_id is NOT NULL; attach new departments to the seeded ROOT row.
            Department root = departmentRepository.findByDeptCodeIgnoreCase("ROOT")
                    .orElseThrow(() -> new IllegalStateException(
                            "ROOT department is missing; verify database migrations have run"));

            Department department = new Department();
            department.setDeptCode(code);
            department.setDeptName(name);
            department.setIsActive(true);
            department.setCreatedAt(LocalDateTime.now());
            department.setParentDepartment(root);
            return departmentRepository.save(department);
        });
    }

    private Role ensureRole(String code, String name, String description) {
        return roleRepository.findByRoleCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setRoleCode(code);
            role.setRoleName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void ensureUser(String employeeCode, String fullName, String email, String password,
                           String roleCode, String roleName, String roleDescription,
                           Department department) {
        if (userRepository.findByEmployeeCode(employeeCode).isPresent()) {
            System.out.println("Default user " + employeeCode + " already exists.");
            return;
        }

        User user = new User();
        user.setEmployeeCode(employeeCode);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDepartment(department);
        user.setRole(ensureRole(roleCode, roleName, roleDescription));
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        System.out.println("Default " + roleName + " user (" + employeeCode + ") created successfully.");
    }
}
