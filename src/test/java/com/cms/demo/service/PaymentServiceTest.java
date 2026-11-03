package com.cms.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cms.demo.dto.InvoiceRequest;
import com.cms.demo.dto.PaymentUpdateRequest;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.CustomerSupplier;
import com.cms.demo.entity.Invoice;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.MasterPaymentStatus;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.AuditLogRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.InvoiceRepository;
import com.cms.demo.repository.PaymentStatusRepository;
import com.cms.demo.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private ContractService contractService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentStatusRepository paymentStatusRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private User financeUser;

    @BeforeEach
    void setUp() {
        financeUser = user("U001", "Finance User", "FINANCE");
    }

    @Test
    void userWithoutWriteRoleCannotRecordPayment() {
        User operator = user("U002", "Operator User", "OPERATOR");
        when(userRepository.findByEmployeeCode("U002")).thenReturn(Optional.of(operator));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.recordPayment(1L,
                        new PaymentUpdateRequest(new BigDecimal("10.00"), LocalDate.now(), "Payment"),
                        "U002"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(invoiceRepository, never()).findById(anyLong());
    }

    @Test
    void adminCannotRecordPayment() {
        User admin = user("U003", "Admin User", "ADMIN");
        when(userRepository.findByEmployeeCode("U003")).thenReturn(Optional.of(admin));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.recordPayment(1L,
                        new PaymentUpdateRequest(new BigDecimal("10.00"), LocalDate.now(), "Admin payment"),
                        "U003"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(invoiceRepository, never()).findById(anyLong());
    }

    @Test
    void recordingPaymentDerivesPartialStatusAndWritesAudit() {
        Invoice invoice = invoice(1L, "100.00", "0.00", LocalDate.now().plusDays(10));
        when(userRepository.findByEmployeeCode("U001")).thenReturn(Optional.of(financeUser));
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(contractService.getContractForUser(1L, "U001")).thenReturn(invoice.getContract());
        when(paymentStatusRepository.findByCodeIgnoreCase(PaymentCalculator.PARTIALLY_PAID))
                .thenReturn(Optional.of(status(PaymentCalculator.PARTIALLY_PAID, "Partially Paid")));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        paymentService.recordPayment(1L,
                new PaymentUpdateRequest(new BigDecimal("40.00"), LocalDate.now(), "First tranche"),
                "U001");

        assertEquals(new BigDecimal("40.00"), invoice.getPaidAmount());
        assertEquals(PaymentCalculator.PARTIALLY_PAID, invoice.getStatus().getCode());
        verify(auditLogService).recordEvent(eq(financeUser), eq("INVOICE"), eq(1L),
                eq("PAYMENT_UPDATED"), any(Map.class), any(Map.class));
    }

    @Test
    void creatingInvoiceAppliesContractTaxAndPendingStatus() {
        Contract contract = contract(1L, "ACTIVE");
        contract.setCommercialTaxRate(new BigDecimal("5.00"));
        contract.setCurrency(currency("MMK"));
        contract.setParty(party());

        when(userRepository.findByEmployeeCode("U001")).thenReturn(Optional.of(financeUser));
        when(contractService.getContractForUser(1L, "U001")).thenReturn(contract);
        when(invoiceRepository.existsByInvoiceNo("INV-1")).thenReturn(false);
        when(paymentStatusRepository.findByCodeIgnoreCase(PaymentCalculator.PENDING))
                .thenReturn(Optional.of(status(PaymentCalculator.PENDING, "Pending")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createInvoice(1L,
                new InvoiceRequest("Phase 1", "INV-1", LocalDate.now(), LocalDate.now().plusDays(30),
                        new BigDecimal("100.00"), null, null, null, "Created"),
                "U001");

        assertEquals(new BigDecimal("5.00"), response.taxAmount());
        assertEquals(new BigDecimal("105.00"), response.totalAmount());
        assertEquals(PaymentCalculator.PENDING, response.statusCode());
        assertEquals("INV-1", response.invoiceNo());
        verify(auditLogService).recordEvent(eq(financeUser), eq("INVOICE"), any(),
                eq("CREATED"), eq(null), any(Map.class));
    }

    @Test
    void creatingInvoiceDefaultsDueDateFromPaymentTerm() {
        Contract contract = contract(1L, "ACTIVE");
        contract.setCommercialTaxRate(new BigDecimal("5.00"));
        contract.setCurrency(currency("MMK"));
        contract.setParty(party());

        LocalDate invoiceDate = LocalDate.now().plusDays(1);

        when(userRepository.findByEmployeeCode("U001")).thenReturn(Optional.of(financeUser));
        when(contractService.getContractForUser(1L, "U001")).thenReturn(contract);
        when(paymentStatusRepository.findByCodeIgnoreCase(PaymentCalculator.PENDING))
                .thenReturn(Optional.of(status(PaymentCalculator.PENDING, "Pending")));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createInvoice(1L,
                new InvoiceRequest("Phase 1", null, invoiceDate, null,
                        new BigDecimal("100.00"), null, null, null, "Created"),
                "U001");

        assertEquals(invoiceDate.plusDays(PaymentCalculator.DEFAULT_PAYMENT_TERM_DAYS),
                response.dueDate());
    }

    @Test
    void creatingInvoiceUsesSelectedPaymentStatus() {
        Contract contract = contract(1L, "ACTIVE");
        contract.setCommercialTaxRate(new BigDecimal("5.00"));
        contract.setCurrency(currency("MMK"));
        contract.setParty(party());

        MasterPaymentStatus paidStatus = status(PaymentCalculator.PAID, "Paid");
        paidStatus.setId(7);

        when(userRepository.findByEmployeeCode("U001")).thenReturn(Optional.of(financeUser));
        when(contractService.getContractForUser(1L, "U001")).thenReturn(contract);
        when(paymentStatusRepository.findById(7)).thenReturn(Optional.of(paidStatus));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.createInvoice(1L,
                new InvoiceRequest("Phase 1", null, LocalDate.now(), LocalDate.now().plusDays(30),
                        new BigDecimal("100.00"), null, null, 7, "Manual status"),
                "U001");

        assertEquals(PaymentCalculator.PAID, response.statusCode());
        assertEquals("Paid", response.statusName());
        assertEquals(7, response.paymentStatusId());
    }

    @Test
    void overdueSweepUpdatesStatusAndAuditsAsSystem() {
        Invoice invoice = invoice(1L, "100.00", "0.00", LocalDate.now().minusDays(1));
        invoice.setStatus(status(PaymentCalculator.PENDING, "Pending"));
        when(invoiceRepository.findUnpaidPastDue(any(LocalDate.class))).thenReturn(List.of(invoice));
        when(paymentStatusRepository.findByCodeIgnoreCase(PaymentCalculator.OVERDUE))
                .thenReturn(Optional.of(status(PaymentCalculator.OVERDUE, "Overdue")));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        int updated = paymentService.markOverdueInvoices();

        assertEquals(1, updated);
        assertEquals(PaymentCalculator.OVERDUE, invoice.getStatus().getCode());
        verify(auditLogService).recordEvent(eq(null), eq("INVOICE"), eq(1L),
                eq("STATUS_CHANGED"), any(Map.class), any(Map.class));
    }

    @Test
    void overdueSweepDoesNotRepeatForCustomOverdueStatusCode() {
        Invoice invoice = invoice(1L, "100.00", "0.00", LocalDate.now().minusDays(1));
        invoice.setStatus(status("PS003", "Overdue"));
        when(invoiceRepository.findUnpaidPastDue(any(LocalDate.class))).thenReturn(List.of(invoice));

        int updated = paymentService.markOverdueInvoices();

        assertEquals(0, updated);
        verify(invoiceRepository, never()).save(any(Invoice.class));
        verifyNoInteractions(auditLogService);
    }

    private Invoice invoice(Long id, String total, String paid, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setContract(contract(1L, "ACTIVE"));
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setAmount(new BigDecimal(total));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(new BigDecimal(paid));
        invoice.setDueDate(dueDate);
        return invoice;
    }

    private Contract contract(Long id, String statusCode) {
        Contract contract = new Contract();
        contract.setContractId(id);
        MasterContractStatus status = new MasterContractStatus();
        status.setCode(statusCode);
        contract.setStatus(status);
        contract.setContractValue(new BigDecimal("100.00"));
        return contract;
    }

    private User user(String code, String name, String roleCode) {
        User user = new User();
        user.setEmployeeCode(code);
        user.setFullName(name);
        Role role = new Role();
        role.setRoleCode(roleCode);
        user.setRole(role);
        return user;
    }

    private MasterPaymentStatus status(String code, String name) {
        MasterPaymentStatus status = new MasterPaymentStatus();
        status.setCode(code);
        status.setName(name);
        return status;
    }

    private MasterCurrency currency(String code) {
        MasterCurrency currency = new MasterCurrency();
        currency.setCode(code);
        return currency;
    }

    private CustomerSupplier party() {
        CustomerSupplier party = new CustomerSupplier();
        party.setPartyId(1L);
        party.setPartyName("ABC Bank");
        return party;
    }
}
