package com.cms.demo.repository;

import com.cms.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployeeCode(String employeeCode);
    Optional<User> findByEmployeeCodeAndIsActiveTrue(String employeeCode);
    List<User> findByDepartmentDepartmentIdAndRoleRoleCodeIgnoreCaseAndIsActiveTrue(Integer departmentId, String roleCode);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByEmail(String email);
}
