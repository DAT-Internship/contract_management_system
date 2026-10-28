package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Entity
@Data
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_code", length=30, nullable=false, unique=true)
    private String roleCode;

    @Column(name = "role_name", length=80, nullable=false)
    private String roleName;

    @Column(name = "description", length=255)
    private String description;
    
    
}
