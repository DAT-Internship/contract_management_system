package com.cms.demo.service;

import com.cms.demo.dto.NotificationDto;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.Invoice;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.User;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.InvoiceRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractReminderService {
    private static final Logger log = LoggerFactory.getLogger(ContractReminderService.class);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final NotificationWebSocketService webSocketService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    public ContractReminderService(
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            UserRepository userRepository,
            NotificationRepository notificationRepository,
            EmailLogRepository emailLogRepository,
            NotificationWebSocketService webSocketService,
            JavaMailSender mailSender) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.emailLogRepository = emailLogRepository;
        this.webSocketService = webSocketService;
        this.mailSender = mailSender;
    }

    @Transactional
    public int sendScheduledReminders(LocalDate today) {
        int sent = 0;
        for (Invoice invoice : invoiceRepository.findOpenInvoicesDueOn(today.plusDays(7))) {
            sent += notifyPayment(invoice, "PAYMENT_DUE_7D", "Payment due in 7 days",
                    "Payment for invoice " + invoiceLabel(invoice) + " is due on "
                            + invoice.getDueDate().format(DISPLAY_DATE) + ".", today);
        }
        for (Invoice invoice : invoiceRepository.findOpenInvoicesDueOn(today)) {
            sent += notifyPayment(invoice, "PAYMENT_DUE_TODAY", "Payment due today",
                    "Payment for invoice " + invoiceLabel(invoice) + " is due today.", today);
        }
        for (Invoice invoice : invoiceRepository.findOpenOverdueInvoices(today)) {
            long overdueDays = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
            if (overdueDays % 7 == 1) {
                sent += notifyPayment(invoice, "PAYMENT_OVERDUE_WEEKLY", "Payment overdue",
                        "Payment for invoice " + invoiceLabel(invoice) + " was due on "
                                + invoice.getDueDate().format(DISPLAY_DATE)
                                + " and has an outstanding balance.", today);
            }
        }
        for (int days : List.of(90, 60, 30, 7)) {
            LocalDate reminderDate = today.plusDays(days);
            for (Contract contract : contractRepository.findActiveContractsEndingOn(reminderDate)) {
                sent += notifyContract(contract, "CONTRACT_EXPIRY_REMINDER",
                        "CONTRACT_EXPIRY_" + days + "D",
                        "Contract expires in " + days + " days",
                        "Contract " + contractLabel(contract) + " expires on "
                                + contract.getEndDate().format(DISPLAY_DATE) + ".",
                        "/contracts/" + contract.getContractId(), "WARNING", today);
            }
        }
        return sent;
    }

    @Transactional
    public int notifyContractApproved(Contract contract, LocalDate today) {
        return notifyContract(contract, "CONTRACT_APPROVED", "CONTRACT_APPROVED",
                "Contract approved - Finance can add invoices",
                "Contract " + contractLabel(contract)
                        + " was approved. Finance can now add invoices and payment schedules for this contract.",
                "/payments/" + contract.getContractId() + "?addInvoice=true", "INFO", today,
                contract.getContractId(), null, false, false);
    }

    @Transactional
    public int notifyInvoiceActivity(Invoice invoice, String eventType) {
        switch (eventType) {
            case "INVOICE_CREATED", "INVOICE_UPDATED", "PAYMENT_UPDATED" -> {
                return 0;
            }
            case "INVOICE_PARTIALLY_PAID" -> {
                Contract contract = invoice.getContract();
                return notifyContract(contract, "INVOICE_PAYMENT_PARTIAL", eventType,
                        "Invoice partially paid",
                        "Invoice " + invoiceLabel(invoice) + " for contract "
                                + contractLabel(contract) + " is partially paid. Open for amounts.",
                        "/payments/" + contract.getContractId(), "INFO", LocalDate.now(),
                        invoice.getInvoiceId(), invoiceDetails(invoice), false, false, false, false);
            }
            case "INVOICE_FULLY_PAID" -> {
                Contract contract = invoice.getContract();
                return notifyRecipients(contract, "INVOICE_PAYMENT_COMPLETED", eventType,
                        "Invoice fully paid",
                        "Invoice " + invoiceLabel(invoice) + " for contract "
                                + contractLabel(contract) + " is fully paid.",
                        "/payments/" + contract.getContractId(), "INFO", LocalDate.now(),
                        invoice.getInvoiceId(), null, false, true,
                        activeFinanceRecipients());
            }
            case "CONTRACT_FULLY_PAID" -> {
                Contract contract = invoice.getContract();
                return notifyContract(contract, "CONTRACT_PAYMENT_COMPLETED", eventType,
                        "Contract fully paid",
                        "All invoices for contract " + contractLabel(contract)
                                + " are fully paid, and the contract gross amount is covered.",
                        "/payments/" + contract.getContractId(), "INFO", LocalDate.now(),
                        contract.getContractId(), null, true, false, false, false);
            }
            default -> throw new IllegalArgumentException("Unsupported invoice activity: " + eventType);
        }
    }

    @Transactional
    public int notifyContractTerminated(Contract contract, String actorEmployeeCode) {
        String message = "Contract " + contractLabel(contract)
                + " was terminated or cancelled. Review the contract's remaining payment obligations and closure steps."
                + (actorEmployeeCode == null || actorEmployeeCode.isBlank()
                        ? "" : " Updated by " + actorEmployeeCode + ".");
        return notifyContract(contract, "CONTRACT_TERMINATED", "CONTRACT_TERMINATED",
                "Contract terminated", message, "/contracts/" + contract.getContractId(),
                "WARNING", LocalDate.now(), contract.getContractId(), null, false, true);
    }

    private int notifyPayment(Invoice invoice, String eventType, String title, String message, LocalDate today) {
        Contract contract = invoice.getContract();
        return notifyContract(contract, "PAYMENT_REMINDER", eventType, title, message,
                "/payments/" + contract.getContractId(), "PAYMENT_OVERDUE_WEEKLY".equals(eventType)
                        ? "WARNING" : "INFO", today, invoice.getInvoiceId(),
                invoiceDetails(invoice));
    }

    private int notifyContract(
            Contract contract,
            String notificationType,
            String eventType,
            String title,
            String message,
            String actionUrl,
            String severity,
            LocalDate eventDate) {
        return notifyContract(contract, notificationType, eventType, title, message,
                actionUrl, severity, eventDate, contract.getContractId(), null, true, false);
    }

    private int notifyContract(
            Contract contract,
            String notificationType,
            String eventType,
            String title,
            String message,
            String actionUrl,
            String severity,
            LocalDate eventDate,
            Long entityId,
            String detail) {
        return notifyContract(contract, notificationType, eventType, title, message,
                actionUrl, severity, eventDate, entityId, detail, true, false);
    }

    private int notifyContract(
            Contract contract,
            String notificationType,
            String eventType,
            String title,
            String message,
            String actionUrl,
            String severity,
            LocalDate eventDate,
            Long entityId,
            String detail,
            boolean deduplicatePerDay,
            boolean deduplicatePermanently) {
        return notifyContract(contract, notificationType, eventType, title, message,
                actionUrl, severity, eventDate, entityId, detail,
                deduplicatePerDay, deduplicatePermanently, false);
    }

    private int notifyContract(
            Contract contract,
            String notificationType,
            String eventType,
            String title,
            String message,
            String actionUrl,
            String severity,
            LocalDate eventDate,
            Long entityId,
            String detail,
            boolean deduplicatePerDay,
            boolean deduplicatePermanently,
            boolean adminOnly) {
        return notifyContract(contract, notificationType, eventType, title, message, actionUrl,
                severity, eventDate, entityId, detail, deduplicatePerDay,
                deduplicatePermanently, adminOnly, true);
    }

    private int notifyContract(
            Contract contract,
            String notificationType,
            String eventType,
            String title,
            String message,
            String actionUrl,
            String severity,
            LocalDate eventDate,
            Long entityId,
            String detail,
            boolean deduplicatePerDay,
            boolean deduplicatePermanently,
            boolean adminOnly,
            boolean includeProjectIncharge) {
        return notifyRecipients(contract, notificationType, eventType, title, message, actionUrl,
                severity, eventDate, entityId, detail, deduplicatePerDay,
                deduplicatePermanently, recipients(contract, !"CONTRACT_EXPIRY_REMINDER".equals(notificationType),
                        adminOnly, includeProjectIncharge));
    }

    private int notifyRecipients(
            Contract contract,
            String notificationType,
            String eventType,
            String title,
            String message,
            String actionUrl,
            String severity,
            LocalDate eventDate,
            Long entityId,
            String detail,
            boolean deduplicatePerDay,
            boolean deduplicatePermanently,
            List<User> recipients) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayStart = eventDate.atStartOfDay();
        LocalDateTime nextDay = eventDate.plusDays(1).atStartOfDay();
        int created = 0;
        for (User recipient : recipients) {
            boolean duplicate = recipient.getUserId() == null;
            if (!duplicate && deduplicatePermanently) {
                duplicate = notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityId(
                        recipient.getUserId(), notificationType, eventType, entityId);
            } else if (!duplicate && deduplicatePerDay) {
                duplicate = notificationRepository
                        .existsByUser_UserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                recipient.getUserId(), notificationType, eventType, entityId,
                                dayStart, nextDay);
            }
            if (duplicate) {
                continue;
            }

            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setType(notificationType);
            notification.setSeverity(severity);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setEntityType(eventType);
            notification.setEntityId(entityId);
            notification.setActionUrl(actionUrl);
            notification.setIsRead(false);
            notification.setCreatedAt(now);
            notification = notificationRepository.saveAndFlush(notification);

            long unreadCount = notificationRepository
                    .countByUser_UserIdAndIsReadFalse(recipient.getUserId());
            webSocketService.pushToEmployee(recipient.getEmployeeCode(), NotificationDto.builder()
                    .notificationId(notification.getNotificationId())
                    .type(notification.getType())
                    .severity(notification.getSeverity())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .actionUrl(notification.getActionUrl())
                    .isRead(false)
                    .createdAt(notification.getCreatedAt())
                    .unreadCount(unreadCount)
                    .build());

            sendEmail(contract, recipient, notification, detail, now);
            created++;
        }
        return created;
    }

    private List<User> activeFinanceRecipients() {
        Map<Long, User> financeRecipients = new LinkedHashMap<>();
        for (User financeUser : userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")) {
            addRecipient(financeRecipients, financeUser);
        }
        return List.copyOf(financeRecipients.values());
    }

    private List<User> recipients(
            Contract contract, boolean includeFinance, boolean adminOnly, boolean includeProjectIncharge) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        if (!adminOnly) {
            addRecipient(recipients, contract.getCreatedBy());
            if (includeProjectIncharge) {
                addRecipient(recipients, contract.getProjectIncharge());
            }
            addRecipient(recipients, contract.getContractManager());
            if (contract.getProject() != null) {
                addRecipient(recipients, contract.getProject().getProjectManager());
            }
        }
        if (includeFinance && !adminOnly) {
            for (User financeUser : userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")) {
                addRecipient(recipients, financeUser);
            }
        }
        for (User admin : userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN")) {
            addRecipient(recipients, admin);
        }
        return List.copyOf(recipients.values());
    }

    private void addRecipient(Map<Long, User> recipients, User user) {
        if (user != null && user.getUserId() != null && Boolean.TRUE.equals(user.getIsActive())) {
            recipients.putIfAbsent(user.getUserId(), user);
        }
    }

    private void sendEmail(
            Contract contract,
            User recipient,
            Notification notification,
            String detail,
            LocalDateTime now) {
        String recipientEmail = recipient.getEmail() == null ? "" : recipient.getEmail().trim();
        EmailLog emailLog = new EmailLog();
        emailLog.setNotification(notification);
        emailLog.setContract(contract);
        emailLog.setRecipientEmail(recipientEmail.isBlank() ? "unknown" : recipientEmail);
        emailLog.setSubject("CMS - " + notification.getTitle());
        emailLog.setEmailType(notification.getType());
        emailLog.setStatus("PENDING");
        emailLog.setAttemptCount(1);
        emailLog.setLastAttemptAt(now);
        emailLog.setCreatedAt(now);
        emailLog = emailLogRepository.saveAndFlush(emailLog);

        try {
            if (recipientEmail.isBlank()) {
                throw new IllegalArgumentException("Recipient email address is empty");
            }
            InternetAddress to = new InternetAddress(recipientEmail, true);
            to.validate();
            InternetAddress from = new InternetAddress(fromEmail, true);
            from.validate();

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(emailLog.getSubject());
            String actionUrl = absoluteUrl(notification.getActionUrl());
            String safeDetail = detail == null ? "" : "<p>" + escapeHtml(detail) + "</p>";
            String html = "<html><body style=\"font-family:Arial,sans-serif\">"
                    + "<h2>" + escapeHtml(notification.getTitle()) + "</h2>"
                    + "<p>" + escapeHtml(notification.getMessage()) + "</p>"
                    + safeDetail
                    + "<p><a href=\"" + escapeHtml(actionUrl) + "\">View in Contract Management System</a></p>"
                    + "</body></html>";
            helper.setText(notification.getMessage() + (detail == null ? "" : "\n" + detail)
                    + "\n" + actionUrl, html);
            mailSender.send(mimeMessage);

            emailLog.setStatus("SENT_UNREAD");
            emailLog.setSentAt(LocalDateTime.now());
            emailLog.setErrorMessage(null);
        } catch (MessagingException | MailException | IllegalArgumentException ex) {
            String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(error);
            log.error("Failed to send {} email to user {}: {}",
                    notification.getType(), recipient.getEmployeeCode(), error);
        }
        emailLogRepository.save(emailLog);
    }

    private String invoiceDetails(Invoice invoice) {
        String currency = invoice.getCurrency() == null ? "" : invoice.getCurrency().getName() + " ";
        BigDecimal outstanding = invoice.getTotalAmount().subtract(invoice.getPaidAmount() == null
                ? BigDecimal.ZERO : invoice.getPaidAmount()).max(BigDecimal.ZERO);
        return "Outstanding amount: " + currency + outstanding.toPlainString()
                + "; due date: " + invoice.getDueDate().format(DISPLAY_DATE);
    }

    private String invoiceLabel(Invoice invoice) {
        return invoice.getInvoiceNo() == null || invoice.getInvoiceNo().isBlank()
                ? "#" + invoice.getInvoiceId() : invoice.getInvoiceNo();
    }

    private String contractLabel(Contract contract) {
        return contract.getContractNo() == null || contract.getContractNo().isBlank()
                ? "#" + contract.getContractId() : contract.getContractNo();
    }

    private String absoluteUrl(String path) {
        return publicUrl.replaceAll("/+$", "") + path;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
