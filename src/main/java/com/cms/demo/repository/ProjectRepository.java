package com.cms.demo.repository;

import com.cms.demo.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByProjectCodeIgnoreCase(String projectCode);
    Optional<Project> findByProjectCodeIgnoreCase(String projectCode);
    List<Project> findByStatusNotIgnoreCase(String status);
}
