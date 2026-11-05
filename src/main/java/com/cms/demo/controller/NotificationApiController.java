
package com.cms.demo.controller;

import com.cms.demo.dto.NotificationDto;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.InvoiceRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.PaymentCalculator;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

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

    // ================= PAYMENT DETAIL (live figures) =================

    /**
     * Live invoice figures for a payment notification ("View details" shows how much is still unpaid).
     * Computed at click time, so the remaining amount is correct even if a payment arrived after the alert.
     * 204 when the notification is not about an invoice.
     */
    @GetMapping("/notifications/{id}/payment-detail")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> paymentDetail(@PathVariable Long id, Authentication auth) {
        User user = me(auth);
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n == null || n.getUser() == null || !user.getUserId().equals(n.getUser().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        boolean invoiceNotice = List.of("PAYMENT_REMINDER", "INVOICE_PAYMENT_PARTIAL",
                "INVOICE_PAYMENT_COMPLETED", "INVOICE_ACTIVITY").contains(n.getType());
        if (!invoiceNotice || n.getEntityId() == null) {
            return ResponseEntity.noContent().build();
        }
        var invoice = invoiceRepository.findNotificationDetailsByInvoiceId(n.getEntityId()).orElse(null);
        if (invoice == null) {
            return ResponseEntity.noContent().build();
        }
        var contract = invoice.getContract();
        BigDecimal total = zero(invoice.getTotalAmount());
        BigDecimal paid = zero(invoice.getPaidAmount());
        BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);
        LocalDate today = LocalDate.now();
        long days = invoice.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(today, invoice.getDueDate());

        BigDecimal contractInvoiced = BigDecimal.ZERO;
        BigDecimal contractPaid = BigDecimal.ZERO;
        for (var i : invoiceRepository.findByContractId(contract.getContractId())) {
            contractInvoiced = contractInvoiced.add(zero(i.getTotalAmount()));
            contractPaid = contractPaid.add(zero(i.getPaidAmount()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contractId", contract.getContractId());
        body.put("contractNo", contract.getContractNo());
        body.put("invoiceId", invoice.getInvoiceId());
        body.put("invoiceNo", invoice.getInvoiceNo());
        body.put("description", invoice.getDescription());
        body.put("currency", invoice.getCurrency() == null ? ""
                : (invoice.getCurrency().getCode() != null && !invoice.getCurrency().getCode().isBlank()
                        ? invoice.getCurrency().getCode() : invoice.getCurrency().getName()));
        body.put("dueDate", invoice.getDueDate() == null ? "" : invoice.getDueDate().toString());
        body.put("daysToDue", days);                       // negative = overdue
        body.put("status", invoice.getStatus() == null ? "" : invoice.getStatus().getName());
        body.put("invoiceTotal", total);
        body.put("invoicePaid", paid);
        body.put("invoiceRemaining", remaining);
        // Contract gross (tax-inclusive) - same figure the Payments page shows as "Contract Gross"
        BigDecimal contractGross = PaymentCalculator.grossWithTax(
                contract.getContractValue(), contract.getCommercialTaxRate());
        body.put("contractGross", contractGross);
        body.put("contractInvoiced", contractInvoiced);
        body.put("contractPaid", contractPaid);
        body.put("contractRemaining", contractGross.subtract(contractPaid).max(BigDecimal.ZERO));
        body.put("paymentUrl", "/payments/" + contract.getContractId());
        return ResponseEntity.ok(body);
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
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
