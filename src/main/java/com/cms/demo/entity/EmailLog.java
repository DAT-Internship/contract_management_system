package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@NoArgsConstructor
@Entity
@Data
@Table(name = "email_logs")
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_log_id")
    private Long emailLogId;

    @Column(name = "recipient_email", length=255, nullable=false)
    private String recipientEmail;

    @Column(name = "subject", length=255, nullable=false)
    private String subject;

    @Column(name = "email_type", length=40, nullable=false)
    private String emailType;

    @Column(name = "status", length=20, nullable=false)
    private String status;

    @Column(name = "attempt_count", nullable=false)
    private Integer attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition="text")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

}
