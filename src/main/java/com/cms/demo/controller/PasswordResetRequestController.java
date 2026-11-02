package com.cms.demo.controller;

import com.cms.demo.entity.User;
import com.cms.demo.entity.EmailLog;
import java.util.List;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.PasswordResetRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class PasswordResetRequestController {

    private final PasswordResetRequestService requestService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final EmailLogRepository emailLogRepository;

    // User submits Forgot Password
    @PostMapping("/forgot-password")
    public String submitRequest(
            @RequestParam String employeeCode,
            @RequestParam String reason,
            @RequestParam(required = false) String additionalDetails,
            RedirectAttributes redirectAttributes
    ) {
        try {
            requestService.submitRequest(
                    employeeCode,
                    reason,
                    additionalDetails
            );
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Your request has been submitted successfully."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }
        return "redirect:/login";
    }

    // All logged-in users: own notifications only
    @GetMapping("/notifications")
    public String notifications(Principal principal, Model model) {
        User currentUser = getCurrentUser(principal);
        model.addAttribute(
                "notifications",
                notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(
                        currentUser.getUserId()
                )
        );
        model.addAttribute("page", "notifications");
        return "notifications";
    }

    @GetMapping("/admin/email-monitoring")
    public String emailMonitoring(Principal principal, Model model) {
        User currentUser = getCurrentUser(principal);

        String role = currentUser.getRole() == null
                || currentUser.getRole().getRoleCode() == null
                ? ""
                : currentUser.getRole().getRoleCode().trim().toUpperCase();

        List<EmailLog> logs = "ADMIN".equals(role)
                ? emailLogRepository.findAllByOrderByCreatedAtDesc()
                : emailLogRepository.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(
                        currentUser.getEmail());

        // Stats are computed from the SAME list that the table shows -> always consistent
        long failed  = logs.stream().filter(e -> "FAILED".equalsIgnoreCase(e.getStatus())).count();
        long pending = logs.stream().filter(e -> "PENDING".equalsIgnoreCase(e.getStatus())).count();
        long sent    = logs.size() - failed - pending;   // SENT / SENT_UNREAD / SENT_READ

        model.addAttribute("emailLogs", logs);
        model.addAttribute("totalCount", logs.size());
        model.addAttribute("sentCount", sent);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("failedCount", failed);
        model.addAttribute("page", "email-monitoring");
        return "email-monitoring";
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not logged in.");
        }
        return userRepository.findByEmployeeCode(principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );
    }
}