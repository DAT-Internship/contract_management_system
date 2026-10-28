package com.cms.demo.repository;

import com.cms.demo.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    Optional<Department> findByDeptCode(String deptCode);
}