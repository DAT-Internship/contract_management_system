
package com.cms.demo.service;

import com.cms.demo.dto.NotificationDto;
import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PasswordResetRequestService {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final PasswordResetEmailService emailService;
    private final NotificationWebSocketService notificationWebSocketService;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "PM",
            "DEPT_HEAD",
            "PMO",
            "CM",
            "FINANCE"
    );

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "forgot",
            "compromised",
            "expired",
            "other"
    );

    @Transactional
    public void submitRequest(
            String employeeCode,
            String reason,
            String additionalDetails
    ) {

        // 1. Validate employee code
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter Staff ID."
            );
        }

        // 2. Validate reason
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Please select a reason."
            );
        }

        String normalizedReason =
                reason.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_REASONS.contains(normalizedReason)) {
            throw new IllegalArgumentException(
                    "Invalid reason."
            );
        }

        String staffId = employeeCode.trim();

        // 3. Find requester
        User requester = userRepository
                .findByEmployeeCode(staffId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Staff ID not found."
                        )
                );

        // 4. Check active account
        if (!Boolean.TRUE.equals(requester.getIsActive())) {
            throw new IllegalArgumentException(
                    "Your account is inactive."
            );
        }

        // 5. Validate role
        String roleCode =
                requester.getRole() == null
                || requester.getRole().getRoleCode() == null
                        ? ""
                        : requester.getRole().getRoleCode()
                                .trim()
                                .toUpperCase(Locale.ROOT);

        if (!ALLOWED_ROLES.contains(roleCode)) {
            throw new IllegalArgumentException(
                    "Your account is not allowed to submit this request."
            );
        }

        // 6. Validate additional details
        String details = additionalDetails == null
                ? ""
                : additionalDetails.trim();

        if (details.length() > 2000) {
            throw new IllegalArgumentException(
                    "Additional information must not exceed 2000 characters."
            );
        }

        // 7. Find active admins
        List<User> admins =
                userRepository.findByIsActiveTrueAndRole_RoleCode(
                        "ADMIN"
                );

        if (admins.isEmpty()) {
            throw new IllegalStateException(
                    "No active administrator is available."
            );
        }

        // 8. Prepare request information
        String departmentName =
                requester.getDepartment() == null
                        ? "N/A"
                        : requester.getDepartment().getDeptName();

        String reasonText = getReasonText(normalizedReason);

        String notificationMessage =
                "Staff ID: " + requester.getEmployeeCode()
                + "\nName: " + requester.getFullName()
                + "\nDepartment: " + departmentName
                + "\nRole: " + roleCode
                + "\nReason: " + reasonText
                + "\nAdditional Details: "
                + (details.isBlank() ? "None" : details);

        LocalDateTime now = LocalDateTime.now();

        // 9. Create notification and email for admins
        for (User admin : admins) {

            // A. Admin notification
            Notification notification = new Notification();

            notification.setUser(admin);
            notification.setType("PASSWORD_RESET_REQUEST");
            notification.setSeverity("WARNING");
            notification.setTitle("New Forgot Password Request");
            notification.setMessage(notificationMessage);
            notification.setEntityType("PASSWORD_RESET_REQUEST");
            notification.setEntityId(requester.getUserId());

            notification.setActionUrl("/admin/password-resets");
            
            notification.setIsRead(false);
            notification.setCreatedAt(now);

            notification =
                    notificationRepository.saveAndFlush(notification);

            // B. Admin WebSocket notification
            long adminUnreadCount =
                    notificationRepository
                            .countByUser_UserIdAndIsReadFalse(
                                    admin.getUserId()
                            );

            NotificationDto adminDto = NotificationDto.builder()
                    .notificationId(notification.getNotificationId())
                    .type(notification.getType())
                    .severity(notification.getSeverity())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .actionUrl(notification.getActionUrl())
                    .isRead(false)
                    .createdAt(notification.getCreatedAt())
                    .unreadCount(adminUnreadCount)
                    .build();

            sendWebSocketAfterCommit(
                    admin.getEmployeeCode(),
                    adminDto
            );

            // C. Validate admin email
            String recipientEmail = admin.getEmail() == null
                    ? ""
                    : admin.getEmail().trim();

            if (recipientEmail.isBlank()) {
                System.err.println(
                        "[ForgotPassword] Admin email is empty: "
                        + admin.getEmployeeCode()
                );
                continue;
            }

            // D. Create email log
            EmailLog emailLog = new EmailLog();

            emailLog.setNotification(notification);
            emailLog.setContract(null);
            emailLog.setRecipientEmail(recipientEmail);
            emailLog.setSubject(
                    "CMS - Forgot Password Request"
            );
            emailLog.setEmailType("PASSWORD_RESET_REQUEST");
            emailLog.setStatus("PENDING");
            emailLog.setAttemptCount(1);
            emailLog.setLastAttemptAt(now);
            emailLog.setCreatedAt(now);

            try {

                emailLog =
                        emailLogRepository.saveAndFlush(emailLog);

                // E. Send email
                String error = emailService.sendRequestEmail(
                        recipientEmail,
                        requester.getEmployeeCode(),
                        requester.getFullName(),
                        departmentName,
                        roleCode,
                        reasonText,
                        details
                );

                // F. Update email status
                if (error == null) {

                    emailLog.setStatus("SENT_UNREAD");
                    emailLog.setSentAt(LocalDateTime.now());
                    emailLog.setErrorMessage(null);

                    System.out.println(
                            "[ForgotPassword] Email sent to: "
                            + recipientEmail
                    );

                } else {

                    emailLog.setStatus("FAILED");
                    emailLog.setErrorMessage(error);

                    System.err.println(
                            "[ForgotPassword] Email failed for "
                            + recipientEmail + ": " + error
                    );
                }

                emailLogRepository.save(emailLog);

            } catch (Exception ex) {

                System.err.println(
                        "[ForgotPassword] Email processing failed for "
                        + recipientEmail + ": "
                        + ex.getClass().getSimpleName()
                        + ": " + ex.getMessage()
                );

                // Try to retain the failure in the email log
                try {
                    emailLog.setStatus("FAILED");
                    emailLog.setErrorMessage(
                            ex.getClass().getSimpleName()
                                    + ": " + ex.getMessage()
                    );

                    emailLogRepository.saveAndFlush(emailLog);

                } catch (Exception logException) {

                    System.err.println(
                            "[ForgotPassword] Could not update email log: "
                            + logException.getMessage()
                    );
                }
            }
        }

        // 10. Requester confirmation notification
        Notification selfNotification = new Notification();

        selfNotification.setUser(requester);
        selfNotification.setType("PASSWORD_RESET_REQUEST");
        selfNotification.setSeverity("INFO");
        selfNotification.setTitle(
                "Password Reset Request Submitted"
        );

        selfNotification.setMessage(
                "Your password reset request has been submitted successfully.\n"
                + "Reason: " + reasonText + "\n"
                + "The administrator will review your request."
        );

        selfNotification.setEntityType("PASSWORD_RESET_REQUEST");
        selfNotification.setEntityId(requester.getUserId());
        selfNotification.setActionUrl(null);
        selfNotification.setIsRead(false);
        selfNotification.setCreatedAt(now);

        selfNotification =
                notificationRepository.saveAndFlush(selfNotification);

        // 11. Requester WebSocket notification
        long requesterUnreadCount =
                notificationRepository
                        .countByUser_UserIdAndIsReadFalse(
                                requester.getUserId()
                        );

        NotificationDto requesterDto = NotificationDto.builder()
                .notificationId(selfNotification.getNotificationId())
                .type(selfNotification.getType())
                .severity(selfNotification.getSeverity())
                .title(selfNotification.getTitle())
                .message(selfNotification.getMessage())
                .actionUrl(null)
                .isRead(false)
                .createdAt(selfNotification.getCreatedAt())
                .unreadCount(requesterUnreadCount)
                .build();

        sendWebSocketAfterCommit(
                requester.getEmployeeCode(),
                requesterDto
        );
    }

    // 12. Send WebSocket after database commit
    private void sendWebSocketAfterCommit(
            String employeeCode,
            NotificationDto dto
    ) {

        Runnable sendMessage = () -> {

            try {

                notificationWebSocketService.pushToEmployee(
                        employeeCode,
                        dto
                );

                System.out.println(
                        "[ForgotPassword] WebSocket sent to: "
                        + employeeCode
                );

            } catch (Exception ex) {

                System.err.println(
                        "[ForgotPassword] WebSocket failed for "
                        + employeeCode
                        + ": " + ex.getMessage()
                );
            }
        };

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            sendMessage.run();
                        }
                    }
            );

        } else {

            sendMessage.run();
        }
    }

    // 13. Convert reason code
    private String getReasonText(String reason) {

        return switch (reason) {

            case "forgot" ->
                    "I forgot my password";

            case "compromised" ->
                    "My account was compromised";

            case "expired" ->
                    "My password expired";

            case "other" ->
                    "Other";

            default ->
                    "Unknown";
        };
    }
}
