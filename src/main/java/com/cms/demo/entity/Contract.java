package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@NoArgsConstructor
@Entity
@Data
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "contract_no", length=30, nullable=false, unique=true)
    private String contractNo;

    @Column(name = "fiscal_year", length=10, nullable=true)
    private String fiscalYear;

    @Column(name = "contract_po_no", length=40, nullable=true)
    private String contractPoNo;

    @Column(name = "po_no_ref", length=40)
    private String poNoRef;

    @Column(name = "description", columnDefinition="text", nullable=true)
    private String description;

    @Column(name = "signed_date")
    private LocalDate signedDate;

    @Column(name = "register_date", nullable=false)
    private LocalDate registerDate;

    @Column(name = "start_date", nullable=true)
    private LocalDate startDate;

    @Column(name = "end_date", nullable=true)
    private LocalDate endDate;

    @Column(name = "period_months")
    private Integer periodMonths;

    @Column(name = "headcount_per_month", precision=8, scale=2)
    private BigDecimal headcountPerMonth;

    @Column(name = "total_man_months", precision=10, scale=2, nullable=false)
    private BigDecimal totalManMonths;

    @Column(name = "contract_value", precision=18, scale=2, nullable=false)
    private BigDecimal contractValue;

    @Column(name = "commercial_tax_rate", precision=5, scale=2, nullable=false)
    private BigDecimal commercialTaxRate;

    @Column(name = "commercial_tax_amount", precision=18, scale=2)
    private BigDecimal commercialTaxAmount;

    @Column(name = "missing_attachment", length=3, nullable=false)
    private String missingAttachment;

    @Column(name = "payment_risk", length=10, nullable=false)
    private String paymentRisk;

    @Column(name = "renewal_risk", length=10, nullable=false)
    private String renewalRisk;

    @Column(name = "client_incharge", length=120)
    private String clientIncharge;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renewed_from_id")
    private Contract renewedFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_for_id")
    private MasterAgreementFor agreementFor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private MasterContractCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_type_id")
    private MasterContractType contractType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private MasterPaymentType paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private MasterContractStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private CustomerSupplier party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private MasterCurrency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_incharge_id")
    private User projectIncharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_manager_id")
    private User contractManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

}
