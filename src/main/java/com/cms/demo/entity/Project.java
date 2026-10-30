package com.cms.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@Entity
@Data
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_code", length=30, nullable=false, unique=true)
    private String projectCode;

    @Column(name = "project_name", length=200, nullable=false)
    private String projectName;

    @Pattern(regexp = "^[ABC]$", message = "Project rank must be A, B, or C")
    @Column(name = "project_rank", length=10)
    private String projectRank;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", length=20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager", nullable = true)
    private User projectManager;

    public void setProjectRank(String projectRank) {
        if (projectRank != null && !projectRank.isBlank()) {
            String rank = projectRank.trim().toUpperCase();
            if (!rank.equals("A") && !rank.equals("B") && !rank.equals("C")) {
                throw new IllegalArgumentException("Project rank must be A, B, or C");
            }
            this.projectRank = rank;
        } else {
            this.projectRank = null;
        }
    }
}
