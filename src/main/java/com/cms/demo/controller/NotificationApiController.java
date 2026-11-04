
package com.cms.demo.controller;

import com.cms.demo.dto.NotificationDto;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;

    private User me(Authentication auth) {
        return userRepository.findByEmployeeCode(auth.getName())
                .orElseThrow(() ->
                        new IllegalStateException("User not found")
                );
    }

    // ================= NOTIFICATION APIs =================

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {

        User user = me(auth);

        long count =
                notificationRepository.countByUser_UserIdAndIsReadFalse(
                        user.getUserId()
                );

        return Map.of("count", count);
    }

    @GetMapping("/notifications")
    public List<NotificationDto> list(Authentication auth) {

        User user = me(auth);

        long unread =
                notificationRepository.countByUser_UserIdAndIsReadFalse(
                        user.getUserId()
                );

        return notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(user.getUserId())
                .stream()
                .map(n -> toDto(n, unread))
                .toList();
    }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    public ResponseEntity<Map<String, Long>> markRead(
            @PathVariable Long id,
            Authentication auth
    ) {

        User user = me(auth);

        notificationRepository.markRead(id, user.getUserId());

        long count =
                notificationRepository.countByUser_UserIdAndIsReadFalse(
                        user.getUserId()
                );

        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/notifications/read-all")
    @Transactional
    public ResponseEntity<Map<String, Long>> markAll(
            Authentication auth
    ) {

        User user = me(auth);

        notificationRepository.markAllRead(user.getUserId());

        return ResponseEntity.ok(Map.of("count", 0L));
    }

    // ================= DTO =================

    private NotificationDto toDto(
            Notification n,
            long unread
    ) {

        return NotificationDto.builder()
                .notificationId(n.getNotificationId())
                .type(n.getType())
                .severity(n.getSeverity())
                .title(n.getTitle())
                .message(n.getMessage())
                .actionUrl(n.getActionUrl())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .unreadCount(unread)
                .build();
    }
}
