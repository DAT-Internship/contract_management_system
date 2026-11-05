package com.cms.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.demo.entity.Contract;
import com.cms.demo.entity.EmailLog;
import com.cms.demo.entity.Invoice;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.Notification;
import com.cms.demo.entity.Project;
import com.cms.demo.entity.User;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.EmailLogRepository;
import com.cms.demo.repository.InvoiceRepository;
import com.cms.demo.repository.NotificationRepository;
import com.cms.demo.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class ContractReminderServiceTest {
    private ContractRepository contractRepository;
    private InvoiceRepository invoiceRepository;
    private UserRepository userRepository;
    private NotificationRepository notificationRepository;
    private EmailLogRepository emailLogRepository;
    private JavaMailSender mailSender;
    private ContractReminderService service;
    private final List<Notification> savedNotifications = new ArrayList<>();

    @BeforeEach
    void setUp() {
        contractRepository = org.mockito.Mockito.mock(ContractRepository.class);
        invoiceRepository = org.mockito.Mockito.mock(InvoiceRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        notificationRepository = org.mockito.Mockito.mock(NotificationRepository.class);
        emailLogRepository = org.mockito.Mockito.mock(EmailLogRepository.class);
        mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
        service = new ContractReminderService(contractRepository, invoiceRepository, userRepository,
                notificationRepository, emailLogRepository,
                org.mockito.Mockito.mock(NotificationWebSocketService.class), mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "no-reply@example.com");
        ReflectionTestUtils.setField(service, "publicUrl", "https://cms.example.com");

        when(notificationRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId((long) savedNotifications.size() + 1);
            savedNotifications.add(notification);
            return notification;
        });
        when(notificationRepository.countByUser_UserIdAndIsReadFalse(anyLong())).thenReturn(1L);
        when(emailLogRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsSevenDayAndExpiryNotificationsToContractOwnerAndFinanceOncePerDay() {
        LocalDate today = LocalDate.of(2026, 3, 1);
        User owner = user(1L, "owner@example.com");
        User finance = user(2L, "finance@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setEndDate(today.plusDays(90));
        contract.setCreatedBy(owner);
        contract.setContractManager(owner);
        contract.setProjectIncharge(owner);

        MasterCurrency currency = new MasterCurrency();
        currency.setName("USD");
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(89L);
        invoice.setInvoiceNo("INV-89");
        invoice.setDueDate(today.plusDays(7));
        invoice.setTotalAmount(new BigDecimal("1250.00"));
        invoice.setCurrency(currency);
        invoice.setContract(contract);

        Invoice dueToday = invoice(90L, "INV-90", today, currency, contract);
        Invoice overdue = invoice(91L, "INV-91", today.minusDays(1), currency, contract);
        when(invoiceRepository.findOpenInvoicesDueOn(today.plusDays(7))).thenReturn(List.of(invoice));
        when(invoiceRepository.findOpenInvoicesDueOn(today)).thenReturn(List.of(dueToday));
        when(invoiceRepository.findOpenOverdueInvoices(today)).thenReturn(List.of(overdue));
        when(contractRepository.findActiveContractsEndingOn(today.plusDays(90))).thenReturn(List.of(contract));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                anyLong(), anyString(), anyString(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        assertEquals(7, service.sendScheduledReminders(today));
        assertEquals(7, savedNotifications.size());

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(7)).saveAndFlush(notificationCaptor.capture());
        assertEquals(6, notificationCaptor.getAllValues().stream()
                .filter(notification -> "PAYMENT_REMINDER".equals(notification.getType())).count());
        assertEquals(1, notificationCaptor.getAllValues().stream()
                .filter(notification -> "CONTRACT_EXPIRY_REMINDER".equals(notification.getType())).count());
        assertEquals(2, notificationCaptor.getAllValues().stream()
                .filter(notification -> "PAYMENT_DUE_7D".equals(notification.getEntityType())).count());
        verify(invoiceRepository, org.mockito.Mockito.never())
                .findOpenInvoicesDueOn(today.plusDays(3));
        assertEquals(2, notificationCaptor.getAllValues().stream()
                .filter(notification -> "PAYMENT_DUE_TODAY".equals(notification.getEntityType())).count());
        assertEquals(2, notificationCaptor.getAllValues().stream()
                .filter(notification -> "PAYMENT_OVERDUE_WEEKLY".equals(notification.getEntityType())).count());
        verify(contractRepository).findActiveContractsEndingOn(today.plusDays(60));
        verify(contractRepository).findActiveContractsEndingOn(today.plusDays(30));
        verify(contractRepository).findActiveContractsEndingOn(today.plusDays(7));
        verify(mailSender, org.mockito.Mockito.times(7)).send(any(MimeMessage.class));

        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                anyLong(), anyString(), anyString(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);
        assertEquals(0, service.sendScheduledReminders(today));
        verify(mailSender, org.mockito.Mockito.times(7)).send(any(MimeMessage.class));
    }

    @Test
    void overduePaymentReminderRepeatsWeeklyStartingOneDayAfterDueDate() {
        LocalDate dueDate = LocalDate.of(2026, 3, 1);
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        Invoice overdue = invoice(91L, "INV-91", dueDate, null, contract);
        User finance = user(2L, "finance@example.com");
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(invoiceRepository.findOpenOverdueInvoices(any(LocalDate.class))).thenReturn(List.of(overdue));
        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                anyLong(), anyString(), anyString(), anyLong(),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        assertEquals(0, service.sendScheduledReminders(dueDate.plusDays(2)));
        assertEquals(1, service.sendScheduledReminders(dueDate.plusDays(1)));
        assertEquals(1, service.sendScheduledReminders(dueDate.plusDays(8)));

        assertEquals(2, savedNotifications.size());
        assertEquals(2, savedNotifications.stream()
                .filter(notification -> "PAYMENT_OVERDUE_WEEKLY".equals(notification.getEntityType()))
                .count());
    }

    @Test
    void approvalNoticeTellsFinanceToAddInvoicesAndLinksToInvoiceForm() {
        User owner = user(1L, "owner@example.com");
        User finance = user(2L, "finance@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setCreatedBy(owner);
        contract.setProjectIncharge(owner);
        contract.setContractManager(owner);
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                anyLong(), anyString(), anyString(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        assertEquals(2, service.notifyContractApproved(contract, LocalDate.of(2026, 11, 4)));
        assertEquals(2, savedNotifications.size());
        assertEquals(1, savedNotifications.stream()
                .filter(notification -> notification.getUser().equals(finance))
                .filter(notification -> notification.getMessage().contains("Finance can now add invoices"))
                .filter(notification -> notification.getActionUrl().equals("/payments/23?addInvoice=true"))
                .count());
    }

    @Test
    void fullyPaidContractNotifiesFinanceContractManagerCreatorProjectManagerAndAdmin() {
        User creator = user(1L, "creator@example.com");
        User projectIncharge = user(2L, "incharge@example.com");
        User manager = user(3L, "manager@example.com");
        User projectManager = user(4L, "pm@example.com");
        User finance = user(5L, "finance@example.com");
        User admin = user(6L, "admin@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setCreatedBy(creator);
        contract.setProjectIncharge(projectIncharge);
        contract.setContractManager(manager);
        Project project = new Project();
        project.setProjectManager(projectManager);
        contract.setProject(project);
        Invoice invoice = invoice(89L, "INV-89", LocalDate.of(2026, 11, 4), null, contract);
        invoice.setPaidAmount(new BigDecimal("1250.00"));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN")).thenReturn(List.of(admin));
        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                anyLong(), anyString(), anyString(), any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);

        assertEquals(5, service.notifyInvoiceActivity(invoice, "CONTRACT_FULLY_PAID"));

        assertEquals(5, savedNotifications.size());
        assertEquals(5, savedNotifications.stream()
                .map(notification -> notification.getUser().getUserId()).distinct().count());
        assertTrue(savedNotifications.stream()
                .anyMatch(notification -> notification.getUser().equals(manager)));
        assertEquals(5, savedNotifications.stream()
                .filter(notification -> notification.getTitle().equals("Contract fully paid"))
                .count());
        assertEquals(0, savedNotifications.stream()
                .filter(notification -> notification.getUser().equals(projectIncharge)).count());
        assertEquals(5, savedNotifications.stream()
                .filter(notification -> Long.valueOf(23L).equals(notification.getEntityId())).count());
        ArgumentCaptor<EmailLog> emailCaptor = ArgumentCaptor.forClass(EmailLog.class);
        verify(emailLogRepository, org.mockito.Mockito.times(5)).saveAndFlush(emailCaptor.capture());
        assertTrue(emailCaptor.getAllValues().stream()
                .anyMatch(email -> "manager@example.com".equals(email.getRecipientEmail())));
    }

    @Test
    void routineInvoiceActivityDoesNotCreateNoisyNotificationsOrEmails() {
        User owner = user(1L, "owner@example.com");
        User finance = user(2L, "finance@example.com");
        User admin = user(3L, "admin@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setCreatedBy(owner);
        Invoice invoice = invoice(89L, "INV-89", LocalDate.of(2026, 11, 4), null, contract);
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN")).thenReturn(List.of(admin));

        for (String eventType : List.of("INVOICE_CREATED", "INVOICE_UPDATED", "PAYMENT_UPDATED")) {
            assertEquals(0, service.notifyInvoiceActivity(invoice, eventType));
        }

        assertEquals(0, savedNotifications.size());
        verify(mailSender, org.mockito.Mockito.never()).send(any(MimeMessage.class));
        verify(userRepository, org.mockito.Mockito.never())
                .findByIsActiveTrueAndRole_RoleCode("FINANCE");
    }

    @Test
    void terminationNoticeReachesContractStakeholdersFinanceAndAdministrators() {
        User creator = user(1L, "creator@example.com");
        User manager = user(2L, "manager@example.com");
        User finance = user(3L, "finance@example.com");
        User admin = user(4L, "admin@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setCreatedBy(creator);
        contract.setContractManager(manager);
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN")).thenReturn(List.of(admin));
        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityId(
                anyLong(), anyString(), anyString(), anyLong())).thenReturn(false);

        assertEquals(4, service.notifyContractTerminated(contract, "EDITOR-1"));

        assertEquals(4, savedNotifications.size());
        assertEquals(4, savedNotifications.stream()
                .map(notification -> notification.getUser().getUserId()).distinct().count());
        assertEquals(4, savedNotifications.stream()
                .filter(notification -> notification.getMessage().contains("terminated or cancelled"))
                .count());
    }

    @Test
    void fullyPaidInvoiceNotifiesAndEmailsOnlyFinanceUsers() {
        User finance = user(5L, "finance@example.com");
        User admin = user(6L, "admin@example.com");
        User creator = user(1L, "creator@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setCreatedBy(creator);
        Invoice invoice = invoice(89L, "INV-89", LocalDate.of(2026, 11, 4), null, contract);
        invoice.setPaidAmount(new BigDecimal("1250.00"));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN")).thenReturn(List.of(admin));
        when(notificationRepository.existsByUser_UserIdAndTypeAndEntityTypeAndEntityId(
                anyLong(), anyString(), anyString(), anyLong())).thenReturn(false);

        assertEquals(1, service.notifyInvoiceActivity(invoice, "INVOICE_FULLY_PAID"));

        assertEquals(1, savedNotifications.size());
        assertEquals(finance.getUserId(), savedNotifications.get(0).getUser().getUserId());
        assertEquals("INVOICE_PAYMENT_COMPLETED", savedNotifications.get(0).getType());
        assertEquals("INVOICE_FULLY_PAID", savedNotifications.get(0).getEntityType());
        assertEquals(invoice.getInvoiceId(), savedNotifications.get(0).getEntityId());
        assertEquals("Invoice fully paid", savedNotifications.get(0).getTitle());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void partiallyPaidInvoiceNotifiesFinanceAdminAndContractStakeholdersWithBalance() {
        User creator = user(1L, "creator@example.com");
        User projectIncharge = user(2L, "incharge@example.com");
        User manager = user(3L, "manager@example.com");
        User projectManager = user(4L, "pm@example.com");
        User finance = user(5L, "finance@example.com");
        User admin = user(6L, "admin@example.com");
        Contract contract = new Contract();
        contract.setContractId(23L);
        contract.setContractNo("CMS-23");
        contract.setCreatedBy(creator);
        contract.setContractManager(manager);
        contract.setProjectIncharge(projectIncharge);
        Project project = new Project();
        project.setProjectManager(projectManager);
        contract.setProject(project);
        MasterCurrency currency = new MasterCurrency();
        currency.setCode("USD");
        Invoice invoice = invoice(89L, "INV-89", LocalDate.of(2026, 11, 4), currency, contract);
        invoice.setPaidAmount(new BigDecimal("500.00"));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("FINANCE")).thenReturn(List.of(finance));
        when(userRepository.findByIsActiveTrueAndRole_RoleCode("ADMIN")).thenReturn(List.of(admin));

        assertEquals(5, service.notifyInvoiceActivity(invoice, "INVOICE_PARTIALLY_PAID"));

        assertEquals(5, savedNotifications.size());
        assertEquals(5, savedNotifications.stream()
                .map(notification -> notification.getUser().getUserId()).distinct().count());
        assertEquals(0, savedNotifications.stream()
                .filter(notification -> notification.getUser().equals(projectIncharge)).count());
        Notification financeNotification = savedNotifications.stream()
                .filter(notification -> notification.getUser().equals(finance))
                .findFirst().orElseThrow();
        assertEquals("INVOICE_PAYMENT_PARTIAL", financeNotification.getType());
        // the list text stays short; the amounts are shown in the detail window and in the email
        assertTrue(!financeNotification.getMessage().contains("USD 500.00"));
        verify(mailSender, org.mockito.Mockito.times(5)).send(any(MimeMessage.class));
    }

    private Invoice invoice(Long id, String number, LocalDate dueDate, MasterCurrency currency, Contract contract) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setInvoiceNo(number);
        invoice.setDueDate(dueDate);
        invoice.setTotalAmount(new BigDecimal("1250.00"));
        invoice.setCurrency(currency);
        invoice.setContract(contract);
        return invoice;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmployeeCode("USER-" + id);
        user.setEmail(email);
        user.setIsActive(true);
        return user;
    }
}
