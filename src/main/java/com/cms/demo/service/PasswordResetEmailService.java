
package com.cms.demo.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public String sendRequestEmail(
            String recipient,
            String staffId,
            String fullName,
            String department,
            String role,
            String reason,
            String details
    ) {

        try {
            // 1. Validate recipient
            String toAddress = validateEmail(recipient, "Recipient");

            // 2. Validate sender
            String senderAddress = validateEmail(fromEmail, "Sender");

            // 3. Create email message
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            // 4. Set sender and recipient
            helper.setFrom(new InternetAddress(senderAddress));
            helper.setTo(new InternetAddress(toAddress));

            helper.setSubject("CMS - Forgot Password Request");

            // 5. Prepare email HTML
            String html = """
                    <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2>Forgot Password Request</h2>

                        <p>A user has submitted a password reset request.</p>

                        <table cellpadding="8" cellspacing="0"
                               style="border-collapse: collapse;">

                            <tr>
                                <td><b>Staff ID</b></td>
                                <td>%s</td>
                            </tr>

                            <tr>
                                <td><b>Full Name</b></td>
                                <td>%s</td>
                            </tr>

                            <tr>
                                <td><b>Department</b></td>
                                <td>%s</td>
                            </tr>

                            <tr>
                                <td><b>Role</b></td>
                                <td>%s</td>
                            </tr>

                            <tr>
                                <td><b>Reason</b></td>
                                <td>%s</td>
                            </tr>

                            <tr>
                                <td><b>Additional Details</b></td>
                                <td>%s</td>
                            </tr>

                        </table>

                        <p>Please check the Notifications section in CMS.</p>
                        <p>Contract Management System</p>
                    </body>
                    </html>
                    """.formatted(
                    escapeHtml(staffId),
                    escapeHtml(fullName),
                    escapeHtml(department),
                    escapeHtml(role),
                    escapeHtml(reason),
                    escapeHtml(
                            details == null || details.isBlank()
                                    ? "None"
                                    : details
                    )
            );

            // 6. Set email content
            helper.setText(html, true);

            // 7. Send email
            mailSender.send(message);

            System.out.println(
                    "[Email] Successfully sent to: " + toAddress
            );

            return null;

        } catch (Exception e) {

            String errorMessage =
                    e.getMessage() == null
                            ? e.getClass().getSimpleName()
                            : e.getClass().getSimpleName()
                                    + ": " + e.getMessage();

            System.err.println(
                    "[Email] Sending failed: " + errorMessage
            );

            return errorMessage;
        }
    }

    // Validate and normalize email address
    private String validateEmail(String email, String fieldName)
            throws Exception {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " email address is empty."
            );
        }

        String normalized = email.trim();

        // Remove accidental wrapping quotation marks
        if (normalized.length() >= 2
                && normalized.startsWith("\"")
                && normalized.endsWith("\"")) {

            normalized = normalized.substring(
                    1, normalized.length() - 1
            ).trim();
        }

        InternetAddress address = new InternetAddress(
                normalized, true
        );

        address.validate();

        if (address.getAddress() == null
                || !address.getAddress().equals(normalized)) {

            throw new IllegalArgumentException(
                    "Invalid " + fieldName + " email address."
            );
        }

        return normalized;
    }

    // Escape HTML content
    private String escapeHtml(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
