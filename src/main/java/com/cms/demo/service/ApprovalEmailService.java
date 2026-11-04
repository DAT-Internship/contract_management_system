package com.cms.demo.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Generic HTML mail sender used by the approval workflow
 * (approval request / contract approved / contract rejected).
 *
 * send(...) never throws: it returns null on success, or an error text on failure
 * (same convention as PasswordResetEmailService).
 */
@Service
@RequiredArgsConstructor
public class ApprovalEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public String send(String recipient, String subject, String html) {
        try {
            String toAddress = validateEmail(recipient, "Recipient");
            String senderAddress = validateEmail(fromEmail, "Sender");

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(senderAddress));
            helper.setTo(new InternetAddress(toAddress));
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            System.out.println("[ApprovalEmail] Sent to: " + toAddress + " | " + subject);
            return null;

        } catch (Exception e) {
            String error = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getClass().getSimpleName() + ": " + e.getMessage();
            System.err.println("[ApprovalEmail] Failed: " + recipient + " - " + error);
            return error;
        }
    }

    private String validateEmail(String email, String fieldName) throws Exception {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(fieldName + " email address is empty.");
        }
        String normalized = email.trim();
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        InternetAddress address = new InternetAddress(normalized, true);
        address.validate();
        if (address.getAddress() == null || !address.getAddress().equals(normalized)) {
            throw new IllegalArgumentException("Invalid " + fieldName + " email address.");
        }
        return normalized;
    }
}