package com.cms.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.PasswordResetRequestService;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class EmailMonitoringControllerTest {

    @Test
    void emailMonitoringCountsSentPendingAndFailedStatuses() {
        PasswordResetRequestService requestService = mock(PasswordResetRequestService.class);
        UserRepository userRepository = mock(UserRepository.class);
        NotificationRepository notificationRepository = mock(NotificationRepository.class);
        EmailLogRepository emailLogRepository = mock(EmailLogRepository.class);
        PasswordResetRequestController controller = new PasswordResetRequestController(
                requestService, userRepository, notificationRepository, emailLogRepository);

        User admin = new User();
        admin.setEmployeeCode("ADMIN-1");
        admin.setEmail("admin@example.com");
        Role role = new Role();
        role.setRoleCode("ADMIN");
        admin.setRole(role);
        when(userRepository.findByEmployeeCode("ADMIN-1")).thenReturn(java.util.Optional.of(admin));
        when(emailLogRepository.countUnreadForRecipient("admin@example.com")).thenReturn(3L);
        when(emailLogRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                email("SENT_UNREAD"),
                email("SENT_READ"),
                email("SENT"),
                email("PENDING"),
                email("FAILED")));

        ExtendedModelMap model = new ExtendedModelMap();
        Principal principal = () -> "ADMIN-1";

        assertEquals("email-monitoring", controller.emailMonitoring(principal, model));
        assertEquals(5, model.get("totalCount"));
        assertEquals(3L, model.get("sentCount"));
        assertEquals(1L, model.get("pendingCount"));
        assertEquals(1L, model.get("failedCount"));
        assertEquals(3L, model.get("emailUnreadCount"));
    }

    private EmailLog email(String status) {
        EmailLog email = new EmailLog();
        email.setStatus(status);
        return email;
    }
}
