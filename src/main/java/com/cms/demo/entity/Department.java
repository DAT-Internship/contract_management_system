package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@NoArgsConstructor
@Entity
@Data
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "dept_code", length=20, nullable=false, unique=true)
    private String deptCode;

    @Column(name = "dept_name", length=120, nullable=false)
    private String deptName;

    @Column(name = "is_active", nullable=false)
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_dept_id", nullable = false)
    private Department parentDepartment;
    
   

}

