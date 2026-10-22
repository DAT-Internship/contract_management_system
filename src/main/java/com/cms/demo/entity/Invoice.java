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
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_no", length=40, unique=true)
    private String invoiceNo;

    @Column(name = "description", columnDefinition="text")
    private String description;

    @Column(name = "invoice_date", nullable=false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable=false)
    private LocalDate dueDate;

    @Column(name = "amount", precision=18, scale=2, nullable=false)
    private BigDecimal amount;

    @Column(name = "tax_amount", precision=18, scale=2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision=18, scale=2, nullable=false)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision=18, scale=2)
    private BigDecimal paidAmount;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private CustomerSupplier party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = true)
    private MasterCurrency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private MasterPaymentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

}
