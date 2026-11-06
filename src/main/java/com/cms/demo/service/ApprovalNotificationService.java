package com.cms.demo.service;

import com.cms.demo.dto.NotificationDto;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;

import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Approval workflow notifications (in-app notification + WebSocket push + e-mail + email_logs row).
 *
 *  1. Contract submitted / resubmitted -> relevant approvers
 *  2. Contract approved                -> contract creator (and submitter)
 *  3. Contract rejected                -> contract creator (and submitter),
 *                                         plus the other relevant approvers (FYI)
 *
 * The e-mail itself is sent AFTER the DB transaction commits, on a background thread,
 * so a slow / failing SMTP server can never block or roll back the approval decision.
 * The result is written back to email_logs (SENT_UNREAD or FAILED).
 */
@Service
@RequiredArgsConstructor
public class ApprovalNotificationService {

    /** Same role codes that ApprovalController.decide() accepts for a department head. */
    private static final List<String> DEPT_HEAD_ROLES = List.of("DEPT_HEAD", "DEPARTMENT_HEAD", "HR", "APPROVER");
    /** Privileged reviewers (stage name is "Department / PMO review"). */
    private static final List<String> PRIVILEGED_ROLES = List.of("PMO", "CM");
    /** Used only when no approver can be found. */
    private static final String FALLBACK_ROLE = "ADMIN";
    /** Set to false if rejection should go to the creator only. */
    private static final boolean NOTIFY_OTHER_APPROVERS_ON_REJECT = true;
    /** Set to false if approval should go to the creator only. */
    private static final boolean NOTIFY_OTHER_APPROVERS_ON_APPROVE = true;

    private static final String TYPE_REQUEST = "APPROVAL_REQ";
    private static final String TYPE_APPROVED = "APPROVAL_APPROVED";
    private static final String TYPE_REJECTED = "APPROVAL_REJECTED";
    private static final String EMAIL_TYPE = "APPROVAL";   // matches the "Approval" filter in Email Monitoring

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final ApprovalEmailService emailService;
    private final NotificationWebSocketService webSocketService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "approval-mail");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    void shutdown() {
        mailExecutor.shutdown();
    }

    // =====================================================================
    // 1. Submitted -> approvers
    // =====================================================================
    @Transactional
    public void notifyApproversSubmitted(Contract contract, User submitter, boolean resubmitted) {
        try {
            doNotifyApproversSubmitted(contract, submitter, resubmitted);
        } catch (RuntimeException ex) {
            logFailure("submitted", contract, ex);
        }
    }

    private void doNotifyApproversSubmitted(Contract contract, User submitter, boolean resubmitted) {
        String submitterName = nameOf(submitter);
        String no = contract.getContractNo();
        String title = (resubmitted ? "Approval required (resubmitted): " : "Approval required: ") + no;
        String message = submitterName + (resubmitted ? " resubmitted" : " submitted")
                + " contract " + no + " (" + partyOf(contract) + ") for approval."
                + "\nDepartment: " + departmentOf(contract)
                + "\nValue: " + valueOf(contract);
        String actionUrl = "/approvals?q=" + URLEncoder.encode(no, StandardCharsets.UTF_8);

        List<String[]> rows = baseRows(contract);
        rows.add(new String[] {"Submitted by", submitterName});

        for (User approver : findApprovers(contract, submitter)) {
            deliver(approver, contract, TYPE_REQUEST, "INFO", title, message, actionUrl,
                    "CMS - Approval required: " + no,
                    html("Approval required",
                            "Hello " + nameOf(approver) + ", a contract is waiting for your review.",
                            rows, actionUrl, "Review in CMS", "#0d6efd"));
        }
    }

    // =====================================================================
    // 2. Approved -> creator
    // =====================================================================
    @Transactional
    public void notifyApproved(Contract contract, User approver, User submitter, String comment) {
        try {
            doNotifyApproved(contract, approver, submitter, comment);
        } catch (RuntimeException ex) {
            logFailure("approved", contract, ex);
        }
    }

    private void doNotifyApproved(Contract contract, User approver, User submitter, String comment) {
        String no = contract.getContractNo();
        String approverName = nameOf(approver);
        boolean hasComment = comment != null && !comment.isBlank();
        String title = "Contract approved: " + no;
        String message = "Your contract " + no + " (" + partyOf(contract) + ") was approved by " + approverName + "."
                + (hasComment ? "\nComment: " + comment.trim() : "");
        String actionUrl = "/contracts/" + contract.getContractId();

        List<String[]> rows = baseRows(contract);
        rows.add(new String[] {"Approved by", approverName});
        if (hasComment) rows.add(new String[] {"Comment", comment.trim()});

        for (User receiver : creatorAndSubmitter(contract, submitter, approver)) {
            deliver(receiver, contract, TYPE_APPROVED, "INFO", title, message, actionUrl,
                    "CMS - Contract approved: " + no,
                    html("Contract approved",
                            "Hello " + nameOf(receiver) + ", your contract has been approved.",
                            rows, actionUrl, "Open contract", "#198754"));
        }

        // --- other relevant approvers (information only): who approved this contract ---
        if (!NOTIFY_OTHER_APPROVERS_ON_APPROVE) return;

        Map<Long, User> skip = new LinkedHashMap<>();
        for (User u : creatorAndSubmitter(contract, submitter, approver)) skip.put(u.getUserId(), u);

        String roleName = approver != null && approver.getRole() != null && approver.getRole().getRoleName() != null
                ? " (" + approver.getRole().getRoleName() + ")" : "";
        String fyiTitle = "Contract approved by " + approverName + ": " + no;
        String fyiMessage = "Contract " + no + " (" + partyOf(contract) + ") was approved by "
                + approverName + roleName + "."
                + (hasComment ? "\nComment: " + comment.trim() : "")
                + "\nNo further action is needed from you.";
        String fyiUrl = "/approvals?q=" + URLEncoder.encode(no, StandardCharsets.UTF_8);

        List<String[]> fyiRows = baseRows(contract);
        fyiRows.add(new String[] {"Approved by", approverName + roleName});
        if (hasComment) fyiRows.add(new String[] {"Comment", comment.trim()});

        for (User other : findApprovers(contract, approver)) {
            if (skip.containsKey(other.getUserId())) continue;
            deliver(other, contract, TYPE_APPROVED, "INFO", fyiTitle, fyiMessage, fyiUrl,
                    "CMS - Contract approved by " + approverName + ": " + no,
                    html("Contract approved",
                            "Hello " + nameOf(other) + ", this contract has been approved by " + approverName + roleName
                                    + ". No further action is needed.",
                            fyiRows, fyiUrl, "View approvals", "#198754"));
        }
    }

    // =====================================================================
    // 3. Rejected -> creator (+ other approvers FYI)
    // =====================================================================
    @Transactional
    public void notifyRejected(Contract contract, User rejector, User submitter, String comment) {
        try {
            doNotifyRejected(contract, rejector, submitter, comment);
        } catch (RuntimeException ex) {
            logFailure("rejected", contract, ex);
        }
    }

    private void doNotifyRejected(Contract contract, User rejector, User submitter, String comment) {
        String no = contract.getContractNo();
        String rejectorName = nameOf(rejector);
        String reason = comment == null || comment.isBlank() ? "-" : comment.trim();

        // --- creator / submitter ---
        String title = "Contract rejected: " + no;
        String message = "Your contract " + no + " (" + partyOf(contract) + ") was rejected by " + rejectorName + "."
                + "\nReason: " + reason
                + "\nPlease revise the contract and submit it again.";
        String actionUrl = "/contracts/" + contract.getContractId();

        List<String[]> rows = baseRows(contract);
        rows.add(new String[] {"Rejected by", rejectorName});
        rows.add(new String[] {"Reason", reason});

        List<User> receivers = creatorAndSubmitter(contract, submitter, rejector);
        for (User receiver : receivers) {
            deliver(receiver, contract, TYPE_REJECTED, "WARNING", title, message, actionUrl,
                    "CMS - Contract rejected: " + no,
                    html("Contract rejected",
                            "Hello " + nameOf(receiver) + ", your contract was rejected. Please revise and resubmit.",
                            rows, actionUrl, "Open contract", "#dc3545"));
        }

        // --- other relevant approvers (information only) ---
        if (!NOTIFY_OTHER_APPROVERS_ON_REJECT) return;

        Map<Long, User> skip = new LinkedHashMap<>();
        for (User u : receivers) skip.put(u.getUserId(), u);

        String fyiTitle = "Contract rejected by " + rejectorName + ": " + no;
        String fyiMessage = "Contract " + no + " (" + partyOf(contract) + ") was rejected by " + rejectorName + "."
                + "\nReason: " + reason;
        String fyiUrl = "/approvals?q=" + URLEncoder.encode(no, StandardCharsets.UTF_8);

        for (User approver : findApprovers(contract, rejector)) {
            if (skip.containsKey(approver.getUserId())) continue;
            deliver(approver, contract, TYPE_REJECTED, "INFO", fyiTitle, fyiMessage, fyiUrl,
                    "CMS - Contract rejected: " + no,
                    html("Contract rejected",
                            "Hello " + nameOf(approver) + ", this contract was rejected by " + rejectorName + ".",
                            rows, fyiUrl, "View approvals", "#dc3545"));
        }
    }

    // =====================================================================
    // Recipients
    // =====================================================================

    /** Department heads of the contract's department + PMO + CM; ADMIN only as fallback. */
    private List<User> findApprovers(Contract contract, User exclude) {
        Map<Long, User> found = new LinkedHashMap<>();

        if (contract.getDepartment() != null) {
            Integer deptId = contract.getDepartment().getDepartmentId();
            for (String role : DEPT_HEAD_ROLES) {
                userRepository.findByDepartmentDepartmentIdAndRoleRoleCodeIgnoreCaseAndIsActiveTrue(deptId, role)
                        .forEach(u -> found.putIfAbsent(u.getUserId(), u));
            }
        }
        for (String role : PRIVILEGED_ROLES) {
            userRepository.findByIsActiveTrueAndRole_RoleCode(role)
                    .forEach(u -> found.putIfAbsent(u.getUserId(), u));
        }
        if (exclude != null) found.remove(exclude.getUserId());

        if (found.isEmpty()) {
            userRepository.findByIsActiveTrueAndRole_RoleCode(FALLBACK_ROLE)
                    .forEach(u -> found.putIfAbsent(u.getUserId(), u));
            if (exclude != null) found.remove(exclude.getUserId());
        }
        if (found.isEmpty()) {
            System.err.println("[ApprovalNotify] No approver found for contract " + contract.getContractNo());
        }
        return new ArrayList<>(found.values());
    }

    /** Contract creator (+ the person who submitted it, if different). The actor never notifies himself. */
    private List<User> creatorAndSubmitter(Contract contract, User submitter, User actor) {
        Map<Long, User> found = new LinkedHashMap<>();
        if (contract.getCreatedBy() != null) found.put(contract.getCreatedBy().getUserId(), contract.getCreatedBy());
        if (submitter != null) found.putIfAbsent(submitter.getUserId(), submitter);
        if (actor != null) found.remove(actor.getUserId());
        return new ArrayList<>(found.values());
    }

    // =====================================================================
    // Delivery: notification row + websocket + email_logs + email
    // =====================================================================
    private void deliver(User recipient, Contract contract, String type, String severity,
                         String title, String message, String actionUrl,
                         String emailSubject, String emailHtml) {

        LocalDateTime now = LocalDateTime.now();

        // 1) in-app notification
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEntityType("CONTRACT");
        notification.setEntityId(contract.getContractId());
        notification.setActionUrl(actionUrl);
        notification.setIsRead(false);
        notification.setCreatedAt(now);
        notification = notificationRepository.saveAndFlush(notification);

        // 2) real-time push (after commit)
        long unread = notificationRepository.countByUser_UserIdAndIsReadFalse(recipient.getUserId());
        NotificationDto dto = NotificationDto.builder()
                .notificationId(notification.getNotificationId())
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .isRead(false)
                .createdAt(now)
                .unreadCount(unread)
                .build();
        String employeeCode = recipient.getEmployeeCode();
        afterCommit(() -> webSocketService.pushToEmployee(employeeCode, dto));

        // 3) e-mail
        String to = recipient.getEmail() == null ? "" : recipient.getEmail().trim();
        if (to.isBlank()) {
            System.err.println("[ApprovalNotify] No e-mail address for user " + recipient.getEmployeeCode());
            return;
        }

        EmailLog emailLog = new EmailLog();
        emailLog.setNotification(notification);
        emailLog.setContract(contract);
        emailLog.setRecipientEmail(to);
        emailLog.setSubject(emailSubject);
        emailLog.setEmailType(EMAIL_TYPE);
        emailLog.setStatus("PENDING");
        emailLog.setAttemptCount(0);
        emailLog.setCreatedAt(now);
        final Long emailLogId = emailLogRepository.saveAndFlush(emailLog).getEmailLogId();

        afterCommit(() -> mailExecutor.submit(() -> sendAndRecord(emailLogId, to, emailSubject, emailHtml)));
    }

    /** Runs on the background thread (no surrounding transaction). */
    private void sendAndRecord(Long emailLogId, String to, String subject, String html) {
        String error = emailService.send(to, subject, html);
        try {
            emailLogRepository.findById(emailLogId).ifPresent(log -> {
                LocalDateTime now = LocalDateTime.now();
                log.setAttemptCount((log.getAttemptCount() == null ? 0 : log.getAttemptCount()) + 1);
                log.setLastAttemptAt(now);
                if (error == null) {
                    log.setStatus("SENT_UNREAD");
                    log.setSentAt(now);
                    log.setErrorMessage(null);
                } else {
                    log.setStatus("FAILED");
                    log.setErrorMessage(error);
                }
                emailLogRepository.save(log);
            });
        } catch (Exception ex) {
            System.err.println("[ApprovalNotify] Could not update email log " + emailLogId + ": " + ex.getMessage());
        }
    }

    /** A notification problem must never break the approval itself, so it is logged and swallowed. */
    private void logFailure(String event, Contract contract, RuntimeException ex) {
        System.err.println("[ApprovalNotify] " + event + " notification failed for contract "
                + (contract == null ? "?" : contract.getContractNo()) + ": " + ex);
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception ex) {
                        System.err.println("[ApprovalNotify] after-commit action failed: " + ex.getMessage());
                    }
                }
            });
        } else {
            action.run();
        }
    }

    // =====================================================================
    // Text / HTML helpers
    // =====================================================================
    private List<String[]> baseRows(Contract c) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Contract No", c.getContractNo()});
        rows.add(new String[] {"Customer / Supplier", partyOf(c)});
        rows.add(new String[] {"Department", departmentOf(c)});
        rows.add(new String[] {"Contract Value", valueOf(c)});
        return rows;
    }

    private String html(String heading, String intro, List<String[]> rows,
                        String actionUrl, String buttonLabel, String color) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style=\"font-family:Arial,sans-serif;color:#1f2937;\">")
          .append("<h2 style=\"color:").append(color).append(";\">").append(esc(heading)).append("</h2>")
          .append("<p>").append(esc(intro)).append("</p>")
          .append("<table cellpadding=\"8\" cellspacing=\"0\" style=\"border-collapse:collapse;border:1px solid #e5e7eb;\">");
        for (String[] r : rows) {
            sb.append("<tr><td style=\"border:1px solid #e5e7eb;background:#f9fafb;\"><b>").append(esc(r[0]))
              .append("</b></td><td style=\"border:1px solid #e5e7eb;\">").append(esc(r[1])).append("</td></tr>");
        }
        sb.append("</table>");
        if (actionUrl != null) {
            String base = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
            sb.append("<p style=\"margin-top:20px;\"><a href=\"").append(esc(base + actionUrl))
              .append("\" style=\"background:").append(color)
              .append(";color:#fff;padding:10px 18px;text-decoration:none;border-radius:6px;\">")
              .append(esc(buttonLabel)).append("</a></p>");
        }
        sb.append("<p style=\"color:#6b7280;font-size:12px;\">Contract Management System</p></body></html>");
        return sb.toString();
    }

    private String nameOf(User u) {
        if (u == null) return "System";
        return u.getFullName() == null || u.getFullName().isBlank() ? u.getEmployeeCode() : u.getFullName();
    }

    private String partyOf(Contract c) {
        return c.getParty() == null || c.getParty().getPartyName() == null ? "-" : c.getParty().getPartyName();
    }

    private String departmentOf(Contract c) {
        return c.getDepartment() == null || c.getDepartment().getDeptName() == null ? "-" : c.getDepartment().getDeptName();
    }

    private String valueOf(Contract c) {
        if (c.getContractValue() == null) return "-";
        String code = c.getCurrency() == null ? "" : " " + c.getCurrency().getCode();
        return String.format("%,.2f", c.getContractValue()) + code;
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;").replace("\n", "<br>");
    }
}