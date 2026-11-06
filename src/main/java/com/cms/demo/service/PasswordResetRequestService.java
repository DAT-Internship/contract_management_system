package com.cms.demo.service;

import com.cms.demo.dto.NotificationDto;
import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.PasswordResetRequest;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.PasswordResetRequestRepository;
import com.cms.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
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
    private final PasswordResetRequestRepository requestRepository;
    private final PasswordResetEmailService emailService;
    private final NotificationWebSocketService notificationWebSocketService;

    private static final int COOLDOWN_MINUTES = 5;

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "PM", "DEPT_HEAD", "PMO", "CM", "FINANCE"
    );

    private static final Set<String> ALLOWED_REASONS = Set.of(
            "forgot", "compromised", "expired", "other"
    );

    @Transactional
    public String submitRequest(
            String employeeCode,
            String reason,
            String additionalDetails
    ) {

        // 1. Validate Staff ID
        if (employeeCode == null || employeeCode.isBlank()) {
            throw new IllegalArgumentException("Please enter Staff ID.");
        }

        // 2. Validate reason
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Please select a reason.");
        }

        String normalizedReason =
                reason.trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_REASONS.contains(normalizedReason)) {
            throw new IllegalArgumentException("Invalid reason.");
        }

        String staffId = employeeCode.trim();

        // 3. Find requester
        User requester = userRepository
                .findByEmployeeCode(staffId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Staff ID not found.")
                );

        // 4. Check account status
        if (!Boolean.TRUE.equals(requester.getIsActive())) {
            throw new IllegalArgumentException("Your account is inactive.");
        }

        // 5. Validate role
        String roleCode = requester.getRole() == null
                || requester.getRole().getRoleCode() == null
                ? ""
                : requester.getRole().getRoleCode()
                        .trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_ROLES.contains(roleCode)) {
            throw new IllegalArgumentException(
                    "Your account is not allowed to submit this request."
            );
        }

        // 6. Validate details
        String details = additionalDetails == null
                ? ""
                : additionalDetails.trim();

        if (details.length() > 2000) {
            throw new IllegalArgumentException(
                    "Additional information must not exceed 2000 characters."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // 7. Check 5-minute cooldown
        checkCooldown(staffId, now);

        // 8. Find active administrators
        List<User> admins =
                userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN");

        if (admins.isEmpty()) {
            throw new IllegalStateException(
                    "No active administrator is available."
            );
        }

        // 9. Save request first
        PasswordResetRequest request = new PasswordResetRequest();

        request.setEmployeeCode(requester.getEmployeeCode());
        request.setReason(normalizedReason);
        request.setDetails(details);
        request.setStatus("PENDING");
        request.setRequestedAt(now);

        requestRepository.saveAndFlush(request);

        String departmentName = requester.getDepartment() == null
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

        int sentCount = 0;
        int failedCount = 0;

        // 10. Create Admin Notifications and send emails
        for (User admin : admins) {

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

            // WebSocket notification after DB commit
            long unreadCount =
                    notificationRepository.countByUser_UserIdAndIsReadFalse(
                            admin.getUserId()
                    );

            NotificationDto dto = NotificationDto.builder()
                    .notificationId(notification.getNotificationId())
                    .type(notification.getType())
                    .severity(notification.getSeverity())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .actionUrl(notification.getActionUrl())
                    .isRead(false)
                    .createdAt(notification.getCreatedAt())
                    .unreadCount(unreadCount)
                    .build();

            sendWebSocketAfterCommit(admin.getEmployeeCode(), dto);

            // 11. Validate admin email
            String recipientEmail = admin.getEmail() == null
                    ? ""
                    : admin.getEmail().trim();

            if (recipientEmail.isBlank()) {
                failedCount++;
                System.err.println(
                        "[PasswordReset] Admin email is empty: "
                        + admin.getEmployeeCode()
                );
                continue;
            }

            // 12. Create Email Log
            EmailLog emailLog = new EmailLog();

            emailLog.setNotification(notification);
            emailLog.setContract(null);
            emailLog.setRecipientEmail(recipientEmail);
            emailLog.setSubject("CMS - Forgot Password Request");
            emailLog.setEmailType("PASSWORD_RESET_REQUEST");
            emailLog.setStatus("PENDING");
            emailLog.setAttemptCount(1);
            emailLog.setLastAttemptAt(now);
            emailLog.setCreatedAt(now);

            try {

                emailLog = emailLogRepository.saveAndFlush(emailLog);

                String emailError = emailService.sendRequestEmail(
                        recipientEmail,
                        requester.getEmployeeCode(),
                        requester.getFullName(),
                        departmentName,
                        roleCode,
                        reasonText,
                        details
                );

                if (emailError == null) {

                    emailLog.setStatus("SENT_UNREAD");
                    emailLog.setSentAt(LocalDateTime.now());
                    emailLog.setErrorMessage(null);

                    sentCount++;

                    System.out.println(
                            "[PasswordReset] Email sent: " + recipientEmail
                    );

                } else {

                    emailLog.setStatus("FAILED");
                    emailLog.setErrorMessage(emailError);

                    failedCount++;

                    System.err.println(
                            "[PasswordReset] Email failed: "
                            + recipientEmail + " - " + emailError
                    );
                }

                emailLogRepository.saveAndFlush(emailLog);

            } catch (Exception ex) {

                failedCount++;

                emailLog.setStatus("FAILED");
                emailLog.setErrorMessage(
                        ex.getClass().getSimpleName()
                        + ": " + ex.getMessage()
                );

                try {
                    emailLogRepository.saveAndFlush(emailLog);
                } catch (Exception logEx) {
                    System.err.println(
                            "[PasswordReset] Could not save failed email log: "
                            + logEx.getMessage()
                    );
                }
            }
        }

        // 13. Create requester confirmation notification
        String requesterMessage =
                "Your password reset request has been submitted successfully.\n"
                + "Reason: " + reasonText + "\n"
                + "The administrator will review your request.";

        if (failedCount > 0 && sentCount == 0) {
            requesterMessage +=
                    "\nHowever, administrator email notification could not be sent. "
                    + "Your request is saved successfully.";
        } else if (failedCount > 0) {
            requesterMessage +=
                    "\nSome administrator email notifications could not be sent. "
                    + "Your request is saved successfully.";
        } else {
            requesterMessage +=
                    "\nAdministrator email notification has been sent.";
        }

        Notification selfNotification = new Notification();

        selfNotification.setUser(requester);
        selfNotification.setType("PASSWORD_RESET_REQUEST");
        selfNotification.setSeverity(
                failedCount > 0 ? "WARNING" : "INFO"
        );
        selfNotification.setTitle("Password Reset Request Submitted");
        selfNotification.setMessage(requesterMessage);
        selfNotification.setEntityType("PASSWORD_RESET_REQUEST");
        selfNotification.setEntityId(requester.getUserId());
        selfNotification.setActionUrl(null);
        selfNotification.setIsRead(false);
        selfNotification.setCreatedAt(LocalDateTime.now());

        notificationRepository.saveAndFlush(selfNotification);

        // Return message for login page
        if (failedCount > 0 && sentCount == 0) {
            return "Your request was submitted successfully, but administrator email notification failed. "
                    + "Your request is saved and the administrator can still view it in CMS.";
        }

        if (failedCount > 0) {
            return "Your request was submitted successfully, but some administrator email notifications failed.";
        }

        return "Your request was submitted successfully. Administrator email notification has been sent.";
    }

    // 14. Cooldown validation
    private void checkCooldown(String staffId, LocalDateTime now) {

        requestRepository
                .findFirstByEmployeeCodeIgnoreCaseOrderByRequestedAtDesc(
                        staffId
                )
                .ifPresent(lastRequest -> {

                    LocalDateTime nextAllowedTime =
                            lastRequest.getRequestedAt()
                                    .plusMinutes(COOLDOWN_MINUTES);

                    if (now.isBefore(nextAllowedTime)) {

                        long secondsRemaining =
                                Duration.between(now, nextAllowedTime)
                                        .getSeconds();

                        long minutesRemaining =
                                (secondsRemaining + 59) / 60;

                        throw new IllegalArgumentException(
                                "You have already submitted a request. "
                                + "Please try again in "
                                + minutesRemaining
                                + " minute(s)."
                        );
                    }
                });
    }

    // 15. WebSocket after transaction commit
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
                        "[WebSocket] Notification pushed to: "
                        + employeeCode
                );

            } catch (Exception ex) {
                System.err.println(
                        "[WebSocket] Failed: " + ex.getMessage()
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

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

    private String getReasonText(String reason) {

        return switch (reason) {
            case "forgot" -> "I forgot my password";
            case "compromised" -> "My account was compromised";
            case "expired" -> "My password expired";
            case "other" -> "Other";
            default -> "Unknown";
        };
    }
}