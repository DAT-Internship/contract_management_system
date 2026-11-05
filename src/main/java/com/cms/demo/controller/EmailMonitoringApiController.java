package com.cms.demo.controller;

import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailMonitoringApiController {

    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;

    private User me(Authentication auth) {
        return userRepository.findByEmployeeCode(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

    private boolean isAdmin(User u) {
        return u.getRole() != null
                && u.getRole().getRoleCode() != null
                && "ADMIN".equalsIgnoreCase(u.getRole().getRoleCode().trim());
    }

    private boolean isRecipient(User u, EmailLog e) {
        return u.getEmail() != null && e.getRecipientEmail() != null
                && u.getEmail().equalsIgnoreCase(e.getRecipientEmail());
    }

    private long unreadCount(User u) {
        return u.getEmail() == null ? 0 : emailLogRepository.countUnreadForRecipient(u.getEmail());
    }

    // ===== Sidebar badge =====
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", unreadCount(me(auth)));
    }

    // ===== Details =====
    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id, Authentication auth) {
        User user = me(auth);
        EmailLog e = emailLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        if (!isAdmin(user) && !isRecipient(user, e)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("emailLogId", e.getEmailLogId());
        body.put("recipientEmail", e.getRecipientEmail() != null ? e.getRecipientEmail() : "");
        body.put("subject", e.getSubject() != null ? e.getSubject() : "");
        body.put("emailType", e.getEmailType() != null ? e.getEmailType() : "");
        body.put("status", e.getStatus() != null ? e.getStatus() : "");
        body.put("attemptCount", e.getAttemptCount() != null ? e.getAttemptCount() : 0);
        body.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : "");
        body.put("sentAt", e.getSentAt() != null ? e.getSentAt().toString() : "");
        body.put("errorMessage", e.getErrorMessage() != null ? e.getErrorMessage() : "");
        body.put("readAt", e.getReadAt() != null ? e.getReadAt().toString() : "");
        body.put("canMarkRead", e.getReadAt() == null && isRecipient(user, e));
        return ResponseEntity.ok(body);
    }

    // ===== Mark as read (called automatically when the recipient opens the email) =====
    @PostMapping("/{id}/mark-read")
    @Transactional
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        User user = me(auth);
        EmailLog e = emailLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        if (!isAdmin(user) && !isRecipient(user, e)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Only the real recipient can change unread -> read
        if (user.getEmail() != null) {
            emailLogRepository.markAsRead(id, user.getEmail());
        }

        String status = emailLogRepository.findById(id)
                .map(EmailLog::getStatus).orElse("");

        Map<String, Object> body = new HashMap<>();
        body.put("emailLogId", id);
        body.put("status", status);
        body.put("count", unreadCount(user));   // new badge number
        return ResponseEntity.ok(body);
    }
}