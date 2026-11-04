package com.cms.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private Long notificationId;
    private String type;
    private String severity;
    private String title;
    private String message;
    private String actionUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long unreadCount;
}