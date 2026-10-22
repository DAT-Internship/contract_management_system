package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;


@NoArgsConstructor
@Entity
@Data
@Table(name = "contract_renewals")
public class ContractRenewal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "renewal_id")
    private Long renewalId;

    @Column(name = "renewal_due_date", nullable=false)
    private LocalDate renewalDueDate;

    @Column(name = "renewal_status", length=30, nullable=false)
    private String renewalStatus;

    @Column(name = "renewal_reason", columnDefinition="text")
    private String renewalReason;

    @Column(name = "remarks", columnDefinition="text")
    private String remarks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_contract_id", nullable = true)
    private Contract originalContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewed_contract_id", nullable = false)
    private Contract renewedContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", nullable = false)
    private User reviewedBy;

}

