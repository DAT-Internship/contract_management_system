package com.cms.demo.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;


@NoArgsConstructor
@Entity
@Data
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "type", length=40, nullable=false)
    private String type;

    @Column(name = "severity", length=20, nullable=false)
    private String severity;

    @Column(name = "title", length=200, nullable=false)
    private String title;

    @Column(name = "message", columnDefinition="text", nullable=false)
    private String message;

    @Column(name = "entity_type", length=40)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "action_url", length=255)
    private String actionUrl;

    @Column(name = "is_read", nullable=false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

}
