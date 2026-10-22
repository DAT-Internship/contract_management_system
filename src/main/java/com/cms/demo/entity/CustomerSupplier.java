package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@NoArgsConstructor
@Entity
@Data
@Table(name = "customers_suppliers")
public class CustomerSupplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long partyId;

    @Column(name = "party_code", length=20, nullable=false, unique=true)
    private String partyCode;

    @Column(name = "party_name", length=200, nullable=false)
    private String partyName;

    @Column(name = "party_type", length=20, nullable=false)
    private String partyType;

    @Column(name = "tax_id", length=40)
    private String taxId;

    @Column(name = "address", columnDefinition="text")
    private String address;

    @Column(name = "contact_person", length=120)
    private String contactPerson;

    @Column(name = "contact_email", length=120)
    private String contactEmail;

    @Column(name = "contact_phone", length=30)
    private String contactPhone;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
