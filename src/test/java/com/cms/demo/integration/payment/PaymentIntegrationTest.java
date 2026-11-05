package com.cms.demo.integration.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.cms.demo.dto.ContractPaymentDetailResponse;
import com.cms.demo.dto.ContractPaymentSummaryResponse;
import com.cms.demo.dto.InvoiceRequest;
import com.cms.demo.dto.InvoiceResponse;
import com.cms.demo.dto.PaymentHistoryEntry;
import com.cms.demo.dto.PaymentUpdateRequest;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.CustomerSupplier;
import com.cms.demo.entity.Department;
import com.cms.demo.entity.Invoice;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.MasterPaymentStatus;
import com.cms.demo.entity.MasterPaymentType;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.CustomerSupplierRepository;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.InvoiceRepository;
import com.cms.demo.repository.PaymentStatusRepository;
import com.cms.demo.repository.PaymentTypeRepository;
import com.cms.demo.repository.RoleRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.PaymentCalculator;
import com.cms.demo.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

/**
 * Full Spring Boot integration tests for the payment module.
 *
 * <p>These tests boot the real application context (Flyway, JPA, security and
 * Thymeleaf) and exercise {@link PaymentService} against the configured MySQL
 * database, the same way {@code ApplicationTests} does. Every row created by a
 * test carries a unique suffix and is removed again in {@link #cleanUp()}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CustomerSupplierRepository customerSupplierRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private PaymentStatusRepository paymentStatusRepository;

    @Autowired
    private ContractStatusRepository contractStatusRepository;

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    private final List<Long> invoiceIds = new ArrayList<>();
    private final List<Long> contractIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> partyIds = new ArrayList<>();
    private final List<Integer> departmentIds = new ArrayList<>();
    private final List<Integer> roleIds = new ArrayList<>();
    private final List<Integer> paymentStatusIds = new ArrayList<>();
    private final List<Integer> currencyIds = new ArrayList<>();
    private final List<Integer> contractStatusIds = new ArrayList<>();
    private final List<Integer> paymentTypeIds = new ArrayList<>();

    private String suffix;
    private int paymentTypeSequence;
    private LocalDate today;
    private User financeUser;
    private User operatorUser;
    private Contract contract;
    private MasterCurrency currency;
    private CustomerSupplier party;
    private MasterContractStatus contractStatus;
    private MasterPaymentType paymentType;

    @BeforeEach
    void setUp() {
        suffix = Long.toString(System.nanoTime(), 36);
        paymentTypeSequence = 0;
        today = LocalDate.now();

        Role financeRole = ensureRole("FINANCE", "Finance",
                "View all contracts and manage payment records");
        Role operatorRole = ensureRole("OPERATOR", "Operator", "Create and view own contracts");
        Department department = ensureDepartment();
        currency = ensureCurrency();
        contractStatus = ensureContractStatus();
        paymentType = ensurePaymentType();

        ensurePaymentStatus(PaymentCalculator.PENDING, "Pending");
        ensurePaymentStatus(PaymentCalculator.PARTIALLY_PAID, "Partially Paid");
        ensurePaymentStatus(PaymentCalculator.PAID, "Paid");
        ensurePaymentStatus(PaymentCalculator.OVERDUE, "Overdue");

        financeUser = createUser("PF-" + suffix, "Payment Finance " + suffix,
                "pf-" + suffix + "@cms.test", financeRole, department);
        operatorUser = createUser("PO-" + suffix, "Payment Operator " + suffix,
                "po-" + suffix + "@cms.test", operatorRole, department);
        party = createParty();
        contract = createContract(party, contractStatus, paymentType);
    }

    @AfterEach
    void cleanUp() {
        // Audit rows use a REQUIRES_NEW transaction, so they are committed even
        // outside the fixture transaction and have to be removed explicitly.
        deleteInvoiceAuditLogs();
        deleteRows("audit_logs", "actor_id", userIds);
        deleteRows("invoices", "invoice_id", invoiceIds);
        deleteRows("contracts", "contract_id", contractIds);
        deleteRows("users", "user_id", userIds);
        deleteRows("departments", "department_id", departmentIds);
        deleteRows("customers_suppliers", "party_id", partyIds);
        deleteRows("master_payment_status", "id", paymentStatusIds);
        deleteRows("master_currency", "id", currencyIds);
        deleteRows("master_contract_status", "id", contractStatusIds);
        deleteRows("master_payment_type", "id", paymentTypeIds);
        deleteRows("roles", "role_id", roleIds);
    }

    // ------------------------------------------------------------------
    // Invoice creation
    // ------------------------------------------------------------------

    @Test
    void createInvoiceAppliesContractTaxPersistsItAndWritesAuditTrail() {
        InvoiceResponse response = createInvoice(invoiceNo("INV"), "100.00", null,
                today, today.plusDays(30));

        assertNotNull(response.invoiceId());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals(new BigDecimal("5.00"), response.taxAmount());
        assertEquals(new BigDecimal("105.00"), response.totalAmount());
        assertEquals(PaymentCalculator.PENDING, response.statusCode());
        assertEquals("Pending", response.statusName());
        assertEquals(currency.getName(), response.currencyName());

        Invoice persisted = invoiceRepository.findById(response.invoiceId()).orElseThrow();
        assertEquals(new BigDecimal("105.00"), persisted.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), persisted.getPaidAmount());
        assertEquals(contract.getContractId(), persisted.getContract().getContractId());
        assertEquals(financeUser.getUserId(), persisted.getUpdatedBy().getUserId());

        List<PaymentHistoryEntry> history = paymentService.getHistory(
                contract.getContractId(), financeUser.getEmployeeCode());
        assertTrue(history.stream().anyMatch(entry -> "CREATED".equals(entry.action())));
        assertEquals(financeUser.getFullName(), history.get(0).actorName());
    }

    @Test
    void createInvoiceRejectsDuplicateInvoiceNumber() {
        String invoiceNo = invoiceNo("DUP");
        createInvoice(invoiceNo, "100.00", ZERO, today, today.plusDays(30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> createInvoice(invoiceNo, "50.00", ZERO, today, today.plusDays(30)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createInvoiceRejectsDueDateBeforeInvoiceDate() {
        // Only milestone-based contracts keep the user-supplied due date; other
        // payment terms derive it from the invoice date.
        contract.setPaymentType(ensurePaymentType("Milestone-based"));
        contractRepository.save(contract);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> createInvoice(invoiceNo("DATE"), "100.00", ZERO,
                        today, today.minusDays(1)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Due date"));
    }

    // ------------------------------------------------------------------
    // Payment recording
    // ------------------------------------------------------------------

    @Test
    void recordPaymentMovesInvoiceFromPartiallyPaidToPaid() {
        InvoiceResponse invoice = createInvoice(invoiceNo("PAY"), "100.00", ZERO,
                today, today.plusDays(30));

        InvoiceResponse partial = paymentService.recordPayment(
                invoice.invoiceId(),
                new PaymentUpdateRequest(new BigDecimal("40.00"), today, "First installment"),
                financeUser.getEmployeeCode());

        assertEquals(PaymentCalculator.PARTIALLY_PAID, partial.statusCode());
        assertEquals(new BigDecimal("40.00"), partial.paidAmount());
        assertEquals(new BigDecimal("60.00"), partial.outstandingAmount());

        LocalDate finalPaidDate = today.plusDays(1);
        InvoiceResponse paid = paymentService.recordPayment(
                invoice.invoiceId(),
                new PaymentUpdateRequest(new BigDecimal("100.00"), finalPaidDate, "Final settlement"),
                financeUser.getEmployeeCode());

        assertEquals(PaymentCalculator.PAID, paid.statusCode());
        assertEquals("Paid", paid.statusName());
        assertEquals(new BigDecimal("100.00"), paid.paidAmount());
        assertEquals(ZERO, paid.outstandingAmount());
        assertEquals(finalPaidDate, paid.paidDate());

        Invoice persisted = invoiceRepository.findById(invoice.invoiceId()).orElseThrow();
        assertEquals(new BigDecimal("100.00"), persisted.getPaidAmount());

        List<PaymentHistoryEntry> history = paymentService.getHistory(
                contract.getContractId(), financeUser.getEmployeeCode());
        assertEquals(2, history.stream()
                .filter(entry -> "PAYMENT_UPDATED".equals(entry.action()))
                .count());
    }

    @Test
    void nonFinanceUserCannotRecordPayment() {
        InvoiceResponse invoice = createInvoice(invoiceNo("FORBID"), "100.00", ZERO,
                today, today.plusDays(30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.recordPayment(
                        invoice.invoiceId(),
                        new PaymentUpdateRequest(new BigDecimal("10.00"), today, "Unauthorized"),
                        operatorUser.getEmployeeCode()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        Invoice persisted = invoiceRepository.findById(invoice.invoiceId()).orElseThrow();
        assertEquals(ZERO, persisted.getPaidAmount());
    }

    @Test
    void recordPaymentRejectsAmountGreaterThanInvoiceTotal() {
        InvoiceResponse invoice = createInvoice(invoiceNo("OVER"), "100.00", ZERO,
                today, today.plusDays(30));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paymentService.recordPayment(
                        invoice.invoiceId(),
                        new PaymentUpdateRequest(new BigDecimal("100.01"), today, "Too much"),
                        financeUser.getEmployeeCode()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ------------------------------------------------------------------
    // Read side / aggregation
    // ------------------------------------------------------------------

    @Test
    void contractDetailAggregatesInvoicesPaymentsAndHistory() {
        InvoiceResponse invoice = createInvoice(invoiceNo("DETAIL"), "100.00", ZERO,
                today, today.plusDays(30));
        paymentService.recordPayment(invoice.invoiceId(),
                new PaymentUpdateRequest(new BigDecimal("40.00"), today, "First installment"),
                financeUser.getEmployeeCode());

        ContractPaymentDetailResponse detail = paymentService.getContractDetail(
                contract.getContractId(), financeUser.getEmployeeCode());

        assertEquals(contract.getContractId(), detail.contract().contractId());
        assertEquals(1, detail.contract().invoiceCount());
        assertEquals(new BigDecimal("100.00"), detail.contract().totalInvoiced());
        assertEquals(new BigDecimal("40.00"), detail.contract().totalPaid());
        assertEquals(new BigDecimal("60.00"), detail.contract().outstandingAmount());
        assertEquals(PaymentCalculator.PARTIALLY_PAID, detail.contract().paymentStatus());
        assertEquals(1, detail.invoices().size());
        assertEquals(invoice.invoiceId(), detail.invoices().get(0).invoiceId());
        assertTrue(detail.history().stream().anyMatch(entry -> "CREATED".equals(entry.action())));
        assertTrue(detail.history().stream()
                .anyMatch(entry -> "PAYMENT_UPDATED".equals(entry.action())));
    }

    @Test
    void contractSummaryCanBeFilteredByContractNumber() {
        createInvoice(invoiceNo("FILTER"), "100.00", ZERO, today, today.plusDays(30));

        List<ContractPaymentSummaryResponse> summaries = paymentService.findContractSummaries(
                financeUser.getEmployeeCode(), contract.getContractNo(), null, null, null);

        assertEquals(1, summaries.size());
        assertEquals(contract.getContractId(), summaries.get(0).contractId());
        assertEquals(1, summaries.get(0).invoiceCount());
    }

    @Test
    void contractSummariesAreSortedMonotonicallyByContractNumber() {
        String prefix = "ORD-" + suffix + "-";
        createContractWithNo(prefix + "C");
        createContractWithNo(prefix + "A");
        createContractWithNo(prefix + "B");

        List<ContractPaymentSummaryResponse> summaries = paymentService.findContractSummaries(
                financeUser.getEmployeeCode(), prefix, null, null, null);

        assertEquals(List.of(prefix + "A", prefix + "B", prefix + "C"),
                summaries.stream().map(ContractPaymentSummaryResponse::contractNo).toList());
    }

    // ------------------------------------------------------------------
    // Controller smoke tests
    // ------------------------------------------------------------------

    @Test
    void paymentsListPageRendersForAuthenticatedFinanceUser() throws Exception {
        createInvoice(invoiceNo("PAGE"), "100.00", ZERO, today, today.plusDays(30));

        mockMvc.perform(get("/payments")
                        .param("q", contract.getContractNo())
                        .with(user(financeUser.getEmployeeCode()).roles("FINANCE")))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andExpect(model().attributeExists("overview", "contracts", "statusOptions"));
    }

    @Test
    void paymentsListPaginatesInStableMonotoneOrder() throws Exception {
        String prefix = "PAG-" + suffix + "-";
        List<String> orderedNumbers = List.of(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
        orderedNumbers.forEach(letter -> createContractWithNo(prefix + letter));

        MvcResult firstPage = mockMvc.perform(get("/payments")
                        .param("q", prefix)
                        .param("page", "1")
                        .param("pageSize", "10")
                        .with(user(financeUser.getEmployeeCode()).roles("FINANCE")))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/list"))
                .andExpect(model().attribute("total", 12))
                .andExpect(model().attribute("pageNumber", 1))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("showingStart", 1))
                .andExpect(model().attribute("showingEnd", 10))
                .andReturn();

        assertEquals(orderedNumbers.stream().limit(10).map(prefix::concat).toList(),
                contractNumbers(firstPage));

        MvcResult secondPage = mockMvc.perform(get("/payments")
                        .param("q", prefix)
                        .param("page", "2")
                        .param("pageSize", "10")
                        .with(user(financeUser.getEmployeeCode()).roles("FINANCE")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("total", 12))
                .andExpect(model().attribute("pageNumber", 2))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("showingStart", 11))
                .andExpect(model().attribute("showingEnd", 12))
                .andReturn();

        assertEquals(orderedNumbers.stream().skip(10).map(prefix::concat).toList(),
                contractNumbers(secondPage));
    }

    @Test
    void paymentDetailPageRendersForAuthenticatedFinanceUser() throws Exception {
        createInvoice(invoiceNo("DPAGE"), "100.00", ZERO, today, today.plusDays(30));

        mockMvc.perform(get("/payments/{contractId}", contract.getContractId())
                        .with(user(financeUser.getEmployeeCode()).roles("FINANCE")))
                .andExpect(status().isOk())
                .andExpect(view().name("payments/detail"))
                .andExpect(model().attributeExists("detail", "invoices", "history"));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private InvoiceResponse createInvoice(String invoiceNo, String amount, BigDecimal taxAmount,
            LocalDate invoiceDate, LocalDate dueDate) {
        InvoiceResponse response = paymentService.createInvoice(
                contract.getContractId(),
                new InvoiceRequest("Payment integration invoice " + invoiceNo, invoiceNo,
                        invoiceDate, dueDate, new BigDecimal(amount), taxAmount,
                        currency.getId(), null, "Created by PaymentIntegrationTest"),
                financeUser.getEmployeeCode());
        invoiceIds.add(response.invoiceId());
        return response;
    }

    private String invoiceNo(String prefix) {
        return prefix + "-" + suffix;
    }

    private Role ensureRole(String code, String name, String description) {
        return roleRepository.findByRoleCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setRoleCode(code);
            role.setRoleName(name);
            role.setDescription(description);
            Role saved = roleRepository.save(role);
            roleIds.add(saved.getRoleId());
            return saved;
        });
    }

    private Department ensureDepartment() {
        return departmentRepository.findByDeptCodeIgnoreCase("MGMT")
                .orElseGet(() -> {
                    Department root = departmentRepository.findByDeptCodeIgnoreCase("ROOT")
                            .orElseThrow(() -> new IllegalStateException(
                                    "ROOT department is missing; run the Flyway migrations first"));
                    Department department = new Department();
                    department.setDeptCode("PAY-" + suffix);
                    department.setDeptName("Payment Test Department " + suffix);
                    department.setIsActive(true);
                    department.setCreatedAt(LocalDateTime.now());
                    department.setParentDepartment(root);
                    Department saved = departmentRepository.save(department);
                    departmentIds.add(saved.getDepartmentId());
                    return saved;
                });
    }

    private MasterCurrency ensureCurrency() {
        String code = "C" + lastDigits(suffix, 9);
        return currencyRepository.findAll().stream()
                .filter(existing -> code.equalsIgnoreCase(existing.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    MasterCurrency created = new MasterCurrency();
                    created.setCode(code);
                    created.setName(code);
                    created.setIsActive(true);
                    created.setSortOrder(0);
                    MasterCurrency saved = currencyRepository.save(created);
                    currencyIds.add(saved.getId());
                    return saved;
                });
    }

    private MasterContractStatus ensureContractStatus() {
        MasterContractStatus status = new MasterContractStatus();
        status.setCode("CS-" + suffix);
        status.setName("Active");
        status.setIsActive(true);
        status.setSortOrder(0);
        MasterContractStatus saved = contractStatusRepository.save(status);
        contractStatusIds.add(saved.getId());
        return saved;
    }

    private MasterPaymentType ensurePaymentType() {
        return ensurePaymentType("Monthly");
    }

    private MasterPaymentType ensurePaymentType(String name) {
        MasterPaymentType type = new MasterPaymentType();
        type.setCode("PT-" + suffix + "-" + (++paymentTypeSequence));
        type.setName(name);
        type.setIsActive(true);
        type.setSortOrder(0);
        MasterPaymentType saved = paymentTypeRepository.save(type);
        paymentTypeIds.add(saved.getId());
        return saved;
    }

    private MasterPaymentStatus ensurePaymentStatus(String code, String name) {
        return paymentStatusRepository.findByCodeIgnoreCase(code)
                .or(() -> paymentStatusRepository.findFirstByNameIgnoreCase(name))
                .orElseGet(() -> {
                    MasterPaymentStatus created = new MasterPaymentStatus();
                    created.setCode(code);
                    created.setName(name);
                    created.setIsActive(true);
                    created.setSortOrder(0);
                    MasterPaymentStatus saved = paymentStatusRepository.save(created);
                    paymentStatusIds.add(saved.getId());
                    return saved;
                });
    }

    private User createUser(String employeeCode, String fullName, String email,
            Role role, Department department) {
        User created = new User();
        created.setEmployeeCode(employeeCode);
        created.setFullName(fullName);
        created.setEmail(email);
        created.setPasswordHash("integration-test");
        created.setIsActive(true);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        created.setDepartment(department);
        created.setRole(role);
        User saved = userRepository.save(created);
        userIds.add(saved.getUserId());
        return saved;
    }

    private CustomerSupplier createParty() {
        CustomerSupplier party = new CustomerSupplier();
        party.setPartyCode("P-" + suffix);
        party.setPartyName("Payment Test Supplier " + suffix);
        party.setPartyType("SUPPLIER");
        party.setTaxId("TAX-" + suffix);
        party.setAddress("Yangon");
        party.setContactPerson("Payment Tester");
        party.setContactEmail("party-" + suffix + "@cms.test");
        party.setContactPhone("09123456789");
        party.setIsActive(true);
        party.setCreatedAt(LocalDateTime.now());
        CustomerSupplier saved = customerSupplierRepository.save(party);
        partyIds.add(saved.getPartyId());
        return saved;
    }

    private Contract createContract(CustomerSupplier party, MasterContractStatus status,
            MasterPaymentType paymentType) {
        Contract created = new Contract();
        created.setContractNo("IT-" + suffix);
        created.setDescription("Payment integration test contract " + suffix);
        created.setRegisterDate(today);
        created.setStartDate(today.minusDays(1));
        created.setEndDate(today.plusMonths(6));
        created.setPeriodMonths(6);
        created.setTotalManMonths(new BigDecimal("6.00"));
        created.setContractValue(new BigDecimal("1000.00"));
        created.setCommercialTaxRate(new BigDecimal("5.00"));
        created.setMissingAttachment("NO");
        created.setPaymentRisk("LOW");
        created.setRenewalRisk("LOW");
        created.setStatus(status);
        created.setParty(party);
        created.setCurrency(currency);
        created.setPaymentType(paymentType);
        created.setCreatedBy(financeUser);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        Contract saved = contractRepository.save(created);
        contractIds.add(saved.getContractId());
        return saved;
    }

    private Contract createContractWithNo(String contractNo) {
        Contract created = new Contract();
        created.setContractNo(contractNo);
        created.setDescription("Payment integration test contract " + contractNo);
        created.setRegisterDate(today);
        created.setStartDate(today.minusDays(1));
        created.setEndDate(today.plusMonths(6));
        created.setPeriodMonths(6);
        created.setTotalManMonths(new BigDecimal("6.00"));
        created.setContractValue(new BigDecimal("1000.00"));
        created.setCommercialTaxRate(new BigDecimal("5.00"));
        created.setMissingAttachment("NO");
        created.setPaymentRisk("LOW");
        created.setRenewalRisk("LOW");
        created.setStatus(contractStatus);
        created.setParty(party);
        created.setCurrency(currency);
        created.setPaymentType(paymentType);
        created.setCreatedBy(financeUser);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        Contract saved = contractRepository.save(created);
        contractIds.add(saved.getContractId());
        return saved;
    }

    private List<String> contractNumbers(MvcResult result) {
        @SuppressWarnings("unchecked")
        List<ContractPaymentSummaryResponse> contracts = (List<ContractPaymentSummaryResponse>) result
                .getModelAndView().getModel().get("contracts");
        return contracts.stream().map(ContractPaymentSummaryResponse::contractNo).toList();
    }

    private String lastDigits(String value, int count) {
        return value.substring(Math.max(0, value.length() - count));
    }

    private void deleteInvoiceAuditLogs() {
        if (invoiceIds.isEmpty()) {
            return;
        }
        String placeholders = invoiceIds.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> arguments = new ArrayList<>();
        arguments.add("INVOICE");
        arguments.addAll(invoiceIds);
        jdbcTemplate.update("delete from audit_logs where entity_type = ? and entity_id in ("
                + placeholders + ")", arguments.toArray());
    }

    private void deleteRows(String table, String column, Collection<? extends Number> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        jdbcTemplate.update("delete from " + table + " where " + column + " in ("
                + placeholders + ")", ids.toArray());
    }
}
