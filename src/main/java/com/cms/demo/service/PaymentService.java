package com.cms.demo.service;

import com.cms.demo.dto.ContractPaymentDetailResponse;
import com.cms.demo.dto.ContractPaymentSummaryResponse;
import com.cms.demo.dto.InvoiceRequest;
import com.cms.demo.dto.InvoiceResponse;
import com.cms.demo.dto.PaymentHistoryEntry;
import com.cms.demo.dto.PaymentOverviewResponse;
import com.cms.demo.dto.PaymentUpdateRequest;
import com.cms.demo.entity.AuditLog;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.Invoice;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.MasterPaymentStatus;
import com.cms.demo.entity.User;
import com.cms.demo.repository.AuditLogRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.InvoiceRepository;
import com.cms.demo.repository.PaymentStatusRepository;
import com.cms.demo.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentService {

    private static final String ENTITY_TYPE = "INVOICE";
    private static final String ROLE_FINANCE = "FINANCE";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ContractService contractService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final CurrencyRepository currencyRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public PaymentService(
            ContractService contractService,
            InvoiceRepository invoiceRepository,
            PaymentStatusRepository paymentStatusRepository,
            CurrencyRepository currencyRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.contractService = contractService;
        this.invoiceRepository = invoiceRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.currencyRepository = currencyRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------
    // Read side
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ContractPaymentSummaryResponse> findContractSummaries(String employeeCode, String query,
            String status, Integer paymentTypeId, Integer categoryId) {
        List<Contract> contracts = contractService.findVisibleContracts(employeeCode).stream()
                .filter(contract -> !isDraft(contract))
                .toList();
        if (contracts.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Invoice>> invoicesByContract = invoicesByContract(contracts);
        String normalizedStatus = normalizeStatusFilter(status);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return contracts.stream()
                .filter(contract -> paymentTypeId == null
                        || (contract.getPaymentType() != null
                                && paymentTypeId.equals(contract.getPaymentType().getId())))
                .filter(contract -> categoryId == null
                        || (contract.getCategory() != null
                                && categoryId.equals(contract.getCategory().getId())))
                .map(contract -> toSummary(contract,
                        invoicesByContract.getOrDefault(contract.getContractId(), List.of())))
                .filter(summary -> normalizedStatus == null
                        || normalizedStatus.equals(summary.paymentStatus()))
                .filter(summary -> normalizedQuery.isEmpty() || matchesQuery(summary, normalizedQuery))
                .sorted(Comparator.comparing(
                                ContractPaymentSummaryResponse::contractNo,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ContractPaymentSummaryResponse::contractId))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentOverviewResponse getOverview(String employeeCode) {
        List<Contract> contracts = contractService.findVisibleContracts(employeeCode).stream()
                .filter(contract -> !isDraft(contract))
                .toList();
        Map<Long, List<Invoice>> invoicesByContract = invoicesByContract(contracts);

        Map<String, BigDecimal> invoicedByCurrency = new LinkedHashMap<>();
        Map<String, BigDecimal> paidByCurrency = new LinkedHashMap<>();
        Map<String, BigDecimal> outstandingByCurrency = new LinkedHashMap<>();
        int totalInvoices = 0;
        int overdueInvoices = 0;
        int overdueContracts = 0;
        int fullySettledContracts = 0;
        LocalDate today = LocalDate.now();

        for (Contract contract : contracts) {
            List<Invoice> invoices = invoicesByContract.getOrDefault(contract.getContractId(), List.of());
            ContractPaymentSummaryResponse summary = toSummary(contract, invoices);
            if (PaymentCalculator.OVERDUE.equals(summary.paymentStatus())) {
                overdueContracts++;
            }
            if (PaymentCalculator.PAID.equals(summary.paymentStatus())) {
                fullySettledContracts++;
            }
            for (Invoice invoice : invoices) {
                totalInvoices++;
                BigDecimal total = PaymentCalculator.money(invoice.getTotalAmount());
                BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
                BigDecimal outstanding = PaymentCalculator.outstanding(total, paid);
                String currency = currencyName(invoice);
                invoicedByCurrency.merge(currency, total, BigDecimal::add);
                paidByCurrency.merge(currency, paid, BigDecimal::add);
                outstandingByCurrency.merge(currency, outstanding, BigDecimal::add);

                boolean fullyPaid = PaymentCalculator.isFullyPaid(total, paid);
                if (!fullyPaid && invoice.getDueDate() != null && invoice.getDueDate().isBefore(today)) {
                    overdueInvoices++;
                }
            }
        }

        return new PaymentOverviewResponse(
                contracts.size(),
                totalInvoices,
                overdueInvoices,
                overdueContracts,
                fullySettledContracts,
                invoicedByCurrency,
                paidByCurrency,
                outstandingByCurrency);
    }

    @Transactional(readOnly = true)
    public ContractPaymentDetailResponse getContractDetail(Long contractId, String employeeCode) {
        Contract contract = contractService.getContractForUser(contractId, employeeCode);
        ensureNotDraft(contract);
        List<Invoice> invoices = invoiceRepository
                .findByContractId(contractId);
        return new ContractPaymentDetailResponse(
                toSummary(contract, invoices),
                invoices.stream().map(this::toInvoiceResponse).toList(),
                toHistory(invoices));
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryEntry> getHistory(Long contractId, String employeeCode) {
        Contract contract = contractService.getContractForUser(contractId, employeeCode);
        ensureNotDraft(contract);
        List<Invoice> invoices = invoiceRepository.findByContractId(contractId);
        return toHistory(invoices);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findInvoice(Long invoiceId, String employeeCode) {
        Invoice invoice = getInvoice(invoiceId);
        Contract contract = contractService.getContractForUser(
                invoice.getContract().getContractId(), employeeCode);
        ensureNotDraft(contract);
        return toInvoiceResponse(invoice);
    }

    // ------------------------------------------------------------------
    // Write side
    // ------------------------------------------------------------------

    @Transactional
    public InvoiceResponse createInvoice(Long contractId, InvoiceRequest request, String employeeCode) {
        User actor = requireWriteAccess(employeeCode);
        Contract contract = contractService.getContractForUser(contractId, employeeCode);
        ensureNotDraft(contract);
        if (contract.getParty() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contract has no customer/supplier; set one before adding an invoice");
        }

        LocalDate today = LocalDate.now();
        LocalDate invoiceDate = request.invoiceDate() == null ? today : request.invoiceDate();
        LocalDate dueDate = resolveDueDate(contract, invoiceDate, request.dueDate());
        validateDates(invoiceDate, dueDate);

        MasterCurrency currency = resolveCurrency(request.currencyId(), contract.getCurrency());
        if (currency == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice currency is required");
        }

        BigDecimal amount = PaymentCalculator.money(request.amount());
        BigDecimal tax = request.taxAmount() == null
                ? calculateTax(amount, contract.getCommercialTaxRate())
                : PaymentCalculator.money(request.taxAmount());
        BigDecimal total = PaymentCalculator.money(amount.add(tax));
        validateContractGrossLimit(contract, total, null);

        String invoiceNo = trimToNull(request.invoiceNo());
        if (invoiceNo != null && invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice number already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = new Invoice();
        invoice.setInvoiceNo(invoiceNo);
        invoice.setDescription(request.description().trim());
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setAmount(amount);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(PaymentCalculator.money(BigDecimal.ZERO));
        invoice.setPaidDate(null);
        invoice.setContract(contract);
        invoice.setParty(contract.getParty());
        invoice.setCurrency(currency);
        String derivedStatus = PaymentCalculator.deriveInvoiceStatus(
                total, BigDecimal.ZERO, dueDate, today);
        invoice.setStatus(resolveSelectedStatus(request.paymentStatusId(), derivedStatus));
        invoice.setUpdatedBy(actor);
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);

        Invoice saved = invoiceRepository.save(invoice);
        Map<String, Object> newValue = invoiceSnapshot(saved);
        newValue.put("note", noteOrDefault(request.note(), "Invoice/schedule created."));
        auditLogService.recordEvent(actor, ENTITY_TYPE, saved.getInvoiceId(), "CREATED", null, newValue);
        return toInvoiceResponse(saved);
    }

    @Transactional
    public InvoiceResponse updateInvoice(Long invoiceId, InvoiceRequest request, String employeeCode) {
        User actor = requireWriteAccess(employeeCode);
        Invoice invoice = getInvoice(invoiceId);
        Contract contract = contractService.getContractForUser(
                invoice.getContract().getContractId(), employeeCode);
        ensureNotDraft(contract);

        LocalDate today = LocalDate.now();
        LocalDate invoiceDate = request.invoiceDate() == null
                ? (invoice.getInvoiceDate() == null ? today : invoice.getInvoiceDate())
                : request.invoiceDate();
        LocalDate dueDate = resolveDueDate(contract, invoiceDate,
                request.dueDate() != null ? request.dueDate() : invoice.getDueDate());
        validateDates(invoiceDate, dueDate);

        BigDecimal amount = PaymentCalculator.money(request.amount());
        BigDecimal tax = request.taxAmount() == null
                ? calculateTax(amount, contract.getCommercialTaxRate())
                : PaymentCalculator.money(request.taxAmount());
        BigDecimal total = PaymentCalculator.money(amount.add(tax));
        validateContractGrossLimit(contract, total, invoice.getInvoiceId());
        BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
        if (paid.compareTo(total) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Total amount cannot be less than the amount already paid");
        }

        String invoiceNo = trimToNull(request.invoiceNo());
        if (invoiceNo != null && invoiceRepository.existsByInvoiceNoAndIdNot(invoiceNo, invoiceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice number already exists");
        }

        MasterCurrency currency = resolveCurrency(request.currencyId(),
                invoice.getCurrency() != null ? invoice.getCurrency() : contract.getCurrency());
        if (currency == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice currency is required");
        }

        Map<String, Object> oldValue = invoiceSnapshot(invoice);

        invoice.setInvoiceNo(invoiceNo);
        invoice.setDescription(request.description().trim());
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDueDate(dueDate);
        invoice.setAmount(amount);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(total);
        invoice.setCurrency(currency);
        String derivedStatus = PaymentCalculator.deriveInvoiceStatus(
                total, paid, dueDate, today);
        invoice.setStatus(resolveSelectedStatus(request.paymentStatusId(), derivedStatus));
        invoice.setUpdatedBy(actor);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        Map<String, Object> newValue = invoiceSnapshot(saved);
        newValue.put("note", noteOrDefault(request.note(), "Invoice details updated."));
        auditLogService.recordEvent(actor, ENTITY_TYPE, saved.getInvoiceId(),
                "INVOICE_UPDATED", oldValue, newValue);
        return toInvoiceResponse(saved);
    }

    @Transactional
    public InvoiceResponse recordPayment(Long invoiceId, PaymentUpdateRequest request, String employeeCode) {
        User actor = requireWriteAccess(employeeCode);
        Invoice invoice = getInvoice(invoiceId);
        Contract contract = contractService.getContractForUser(
                invoice.getContract().getContractId(), employeeCode);
        ensureNotDraft(contract);

        BigDecimal total = PaymentCalculator.money(invoice.getTotalAmount());
        BigDecimal paid = PaymentCalculator.money(request.paidAmount());
        if (paid.compareTo(total) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paid amount cannot exceed the total invoice amount");
        }

        LocalDate today = LocalDate.now();
        LocalDate paidDate = paid.signum() > 0
                ? (request.paidDate() == null ? today : request.paidDate())
                : null;

        Map<String, Object> oldValue = paymentSnapshot(invoice);

        invoice.setPaidAmount(paid);
        invoice.setPaidDate(paidDate);
        invoice.setStatus(resolveStatus(PaymentCalculator.deriveInvoiceStatus(
                total, paid, invoice.getDueDate(), today)));
        invoice.setUpdatedBy(actor);
        invoice.setUpdatedAt(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        Map<String, Object> newValue = paymentSnapshot(saved);
        newValue.put("note", request.note().trim());
        auditLogService.recordEvent(actor, ENTITY_TYPE, saved.getInvoiceId(),
                "PAYMENT_UPDATED", oldValue, newValue);
        return toInvoiceResponse(saved);
    }

    /**
     * Daily sweep: persist OVERDUE for unpaid invoices whose due date passed.
     * Returns the number of invoices changed.
     */
    @Transactional
    public int markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> candidates = invoiceRepository.findUnpaidPastDue(today);
        int updated = 0;

        for (Invoice invoice : candidates) {
            BigDecimal total = PaymentCalculator.money(invoice.getTotalAmount());
            BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
            if (PaymentCalculator.isFullyPaid(total, paid)) {
                continue;
            }
            String oldStatus = storedInvoiceStatus(invoice.getStatus());
            if (PaymentCalculator.OVERDUE.equals(oldStatus)) {
                continue;
            }

            Map<String, Object> oldValue = paymentSnapshot(invoice);
            oldValue.put("status", oldStatus);
            oldValue.put("status_name", oldStatus == null ? null : paymentStatusName(oldStatus));
            invoice.setStatus(resolveStatus(PaymentCalculator.OVERDUE));
            invoice.setUpdatedAt(LocalDateTime.now());
            Invoice saved = invoiceRepository.save(invoice);

            Map<String, Object> newValue = paymentSnapshot(saved);
            newValue.put("note", "Automatically flagged Overdue after passing due date.");
            auditLogService.recordEvent(null, ENTITY_TYPE, saved.getInvoiceId(),
                    "STATUS_CHANGED", oldValue, newValue);
            updated++;
        }
        return updated;
    }

    // ------------------------------------------------------------------
    // Mapping and helpers
    // ------------------------------------------------------------------

    private Map<Long, List<Invoice>> invoicesByContract(Collection<Contract> contracts) {
        if (contracts.isEmpty()) {
            return Map.of();
        }
        List<Long> contractIds = contracts.stream().map(Contract::getContractId).toList();
        return invoiceRepository.findByContractIdIn(contractIds)
                .stream()
                .collect(Collectors.groupingBy(invoice -> invoice.getContract().getContractId()));
    }

    private ContractPaymentSummaryResponse toSummary(Contract contract, List<Invoice> invoices) {
        LocalDate today = LocalDate.now();
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        LocalDate nextDueDate = null;

        for (Invoice invoice : invoices) {
            BigDecimal total = PaymentCalculator.money(invoice.getTotalAmount());
            BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
            totalInvoiced = totalInvoiced.add(total);
            totalPaid = totalPaid.add(paid);
            outstanding = outstanding.add(PaymentCalculator.outstanding(total, paid));

            boolean fullyPaid = PaymentCalculator.isFullyPaid(total, paid);
            if (!fullyPaid && invoice.getDueDate() != null
                    && (nextDueDate == null || invoice.getDueDate().isBefore(nextDueDate))) {
                nextDueDate = invoice.getDueDate();
            }
        }

        BigDecimal contractGross = PaymentCalculator.grossWithTax(
                contract.getContractValue(), contract.getCommercialTaxRate());
        BigDecimal remainingInvoiceable = PaymentCalculator.remaining(contractGross, totalInvoiced);
        String statusCode = PaymentCalculator.deriveContractStatus(
                invoices, contractGross, today);
        long remainingDays = contract.getEndDate() == null
                ? 0
                : Math.max(0, ChronoUnit.DAYS.between(today, contract.getEndDate()));

        return new ContractPaymentSummaryResponse(
                contract.getContractId(),
                contract.getContractNo(),
                contract.getContractPoNo(),
                contract.getPoNoRef(),
                contract.getDescription(),
                contract.getParty() == null ? null : contract.getParty().getPartyId(),
                contract.getParty() == null ? null : contract.getParty().getPartyName(),
                contract.getCategory() == null ? null : contract.getCategory().getId(),
                contract.getCategory() == null ? null : contract.getCategory().getName(),
                contract.getAgreementFor() == null ? null : contract.getAgreementFor().getName(),
                contract.getDepartment() == null ? null : contract.getDepartment().getDeptName(),
                contract.getProject() == null ? null : contract.getProject().getProjectCode(),
                contract.getPaymentType() == null ? null : contract.getPaymentType().getId(),
                contract.getPaymentType() == null ? null : contract.getPaymentType().getName(),
                contract.getCurrency() == null ? null : contract.getCurrency().getName(),
                PaymentCalculator.money(contract.getContractValue()),
                contract.getCommercialTaxRate(),
                contractGross,
                remainingInvoiceable,
                PaymentCalculator.money(totalInvoiced),
                PaymentCalculator.money(totalPaid),
                PaymentCalculator.money(outstanding),
                invoices.size(),
                nextDueDate,
                PaymentCalculator.OVERDUE.equals(statusCode),
                statusCode,
                paymentStatusName(statusCode),
                PaymentCalculator.progressPercent(totalPaid, contractGross),
                contract.getStatus() == null ? null : contract.getStatus().getCode(),
                contract.getStartDate(),
                contract.getEndDate(),
                remainingDays);
    }

    private String effectiveStatusCode(Invoice invoice) {
        String stored = storedInvoiceStatus(invoice.getStatus());
        if (stored != null) {
            return stored;
        }
        return PaymentCalculator.deriveInvoiceStatus(
                PaymentCalculator.money(invoice.getTotalAmount()),
                PaymentCalculator.money(invoice.getPaidAmount()),
                invoice.getDueDate(),
                LocalDate.now());
    }

    private String effectiveStatusName(Invoice invoice, String statusCode) {
        String storedName = invoice.getStatus() == null ? null : invoice.getStatus().getName();
        return storedName == null || storedName.isBlank()
                ? paymentStatusName(statusCode)
                : storedName;
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        BigDecimal total = PaymentCalculator.money(invoice.getTotalAmount());
        BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
        String statusCode = effectiveStatusCode(invoice);
        String statusName = effectiveStatusName(invoice, statusCode);
        boolean overdue = PaymentCalculator.OVERDUE.equals(statusCode);
        long remainingDays = invoice.getDueDate() == null
                ? 0
                : ChronoUnit.DAYS.between(LocalDate.now(), invoice.getDueDate());

        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getContract().getContractId(),
                invoice.getContract().getContractNo(),
                invoice.getInvoiceNo(),
                invoice.getDescription(),
                invoice.getParty() == null ? null : invoice.getParty().getPartyName(),
                currencyName(invoice),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                PaymentCalculator.money(invoice.getAmount()),
                PaymentCalculator.money(invoice.getTaxAmount()),
                total,
                paid,
                PaymentCalculator.outstanding(total, paid),
                invoice.getPaidDate(),
                statusCode,
                statusName,
                overdue,
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                invoice.getUpdatedBy() == null ? null : invoice.getUpdatedBy().getFullName(),
                invoice.getStatus() == null ? null : invoice.getStatus().getId(),
                remainingDays);
    }

    private List<PaymentHistoryEntry> toHistory(List<Invoice> invoices) {
        if (invoices.isEmpty()) {
            return List.of();
        }
        List<Long> invoiceIds = invoices.stream()
                .map(Invoice::getInvoiceId)
                .filter(Objects::nonNull)
                .toList();
        if (invoiceIds.isEmpty()) {
            return List.of();
        }
        return auditLogRepository
                .findByEntityTypeAndEntityIdInOrderByCreatedAtDesc(ENTITY_TYPE, invoiceIds)
                .stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    private PaymentHistoryEntry toHistoryEntry(AuditLog log) {
        return new PaymentHistoryEntry(
                log.getLogId(),
                log.getAction(),
                log.getEntityId(),
                log.getActor() == null ? "System" : log.getActor().getFullName(),
                log.getActor() == null || log.getActor().getRole() == null
                        ? null : log.getActor().getRole().getRoleName(),
                log.getActor() == null ? null : log.getActor().getEmployeeCode(),
                log.getIpAddress(),
                log.getCreatedAt(),
                readJson(log.getOldValue()),
                readJson(log.getNewValue()));
    }

    private JsonNode readJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Map<String, Object> invoiceSnapshot(Invoice invoice) {
        BigDecimal total = PaymentCalculator.money(invoice.getTotalAmount());
        BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
        String status = effectiveStatusCode(invoice);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("invoice_no", invoice.getInvoiceNo() == null ? "Unbilled" : invoice.getInvoiceNo());
        snapshot.put("description", invoice.getDescription());
        snapshot.put("invoice_date", dateString(invoice.getInvoiceDate()));
        snapshot.put("due_date", dateString(invoice.getDueDate()));
        snapshot.put("amount", PaymentCalculator.money(invoice.getAmount()));
        snapshot.put("tax_amount", PaymentCalculator.money(invoice.getTaxAmount()));
        snapshot.put("total_amount", total);
        snapshot.put("paid_amount", paid);
        snapshot.put("paid_date", dateString(invoice.getPaidDate()));
        snapshot.put("status", status);
        snapshot.put("status_name", effectiveStatusName(invoice, status));
        snapshot.put("status_code", invoice.getStatus() == null ? null : invoice.getStatus().getCode());
        return snapshot;
    }

    private Map<String, Object> paymentSnapshot(Invoice invoice) {
        BigDecimal paid = PaymentCalculator.money(invoice.getPaidAmount());
        String status = effectiveStatusCode(invoice);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("invoice_no", invoice.getInvoiceNo() == null ? "Unbilled" : invoice.getInvoiceNo());
        snapshot.put("paid_amount", paid);
        snapshot.put("paid_date", dateString(invoice.getPaidDate()));
        snapshot.put("status", status);
        snapshot.put("status_name", effectiveStatusName(invoice, status));
        snapshot.put("status_code", invoice.getStatus() == null ? null : invoice.getStatus().getCode());
        return snapshot;
    }

    private String dateString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private String currencyName(Invoice invoice) {
        if (invoice.getCurrency() != null) {
            return invoice.getCurrency().getName();
        }
        if (invoice.getContract() != null && invoice.getContract().getCurrency() != null) {
            return invoice.getContract().getCurrency().getName();
        }
        return "UNKNOWN";
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return switch (normalized) {
            case "FULLY_PAID", "FULLY_SETTLED" -> PaymentCalculator.PAID;
            case "HAS_OVERDUE" -> PaymentCalculator.OVERDUE;
            case "NO_INVOICE" -> PaymentCalculator.NO_INVOICES;
            default -> normalized;
        };
    }

    private boolean matchesQuery(ContractPaymentSummaryResponse summary, String query) {
        return Stream.of(
                summary.contractId() == null ? null : summary.contractId().toString(),
                summary.contractNo(),
                summary.contractPoNo(),
                summary.poNoRef(),
                summary.description(),
                summary.partyName(),
                summary.categoryName(),
                summary.departmentName(),
                summary.projectCode(),
                summary.paymentTypeName(),
                summary.contractStatus())
                .filter(Objects::nonNull)
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(query));
    }

    /**
     * Semantic status represented by the currently stored master-data row.
     * Custom status codes such as PS003 still map through their display name.
     */
    private String storedInvoiceStatus(MasterPaymentStatus status) {
        if (status == null) {
            return null;
        }
        String name = status.getName();
        if (name != null) {
            String normalized = name.trim().toUpperCase(Locale.ROOT)
                    .replace(' ', '_')
                    .replace('-', '_');
            return switch (normalized) {
                case PaymentCalculator.PAID -> PaymentCalculator.PAID;
                case PaymentCalculator.PARTIALLY_PAID -> PaymentCalculator.PARTIALLY_PAID;
                case PaymentCalculator.OVERDUE -> PaymentCalculator.OVERDUE;
                case PaymentCalculator.PENDING -> PaymentCalculator.PENDING;
                default -> status.getCode();
            };
        }
        return status.getCode();
    }

    private String paymentStatusName(String code) {
        return switch (code) {
            case PaymentCalculator.PAID -> "Paid";
            case PaymentCalculator.PARTIALLY_PAID -> "Partially Paid";
            case PaymentCalculator.OVERDUE -> "Overdue";
            case PaymentCalculator.NO_INVOICES -> "No Invoices";
            default -> "Pending";
        };
    }

    private BigDecimal calculateTax(BigDecimal amount, BigDecimal rate) {
        BigDecimal taxRate = rate == null ? BigDecimal.ZERO : rate;
        return PaymentCalculator.money(amount.multiply(taxRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP));
    }

    private LocalDate resolveDueDate(Contract contract, LocalDate invoiceDate, LocalDate dueDate) {
        String paymentType = contract.getPaymentType() == null || contract.getPaymentType().getName() == null
                ? "" : contract.getPaymentType().getName().trim().toLowerCase(java.util.Locale.ROOT);
        return switch (paymentType) {
            case "monthly" -> invoiceDate.plusMonths(1);
            case "quarterly" -> invoiceDate.plusMonths(3);
            case "semi-annual" -> invoiceDate.plusMonths(6);
            case "annual" -> invoiceDate.plusMonths(12);
            case "lump sum" -> dueDate != null ? dueDate : invoiceDate;
            case "milestone-based" -> dueDate;
            default -> dueDate != null ? dueDate
                    : invoiceDate.plusDays(PaymentCalculator.DEFAULT_PAYMENT_TERM_DAYS);
        };
    }

    private void validateDates(LocalDate invoiceDate, LocalDate dueDate) {
        if (dueDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Due date is required for milestone-based payments");
        }
        if (dueDate.isBefore(invoiceDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Due date must be on or after the invoice date");
        }
    }

    private void validateContractGrossLimit(Contract contract, BigDecimal newInvoiceTotal, Long excludedInvoiceId) {
        BigDecimal invoiced = invoiceRepository.findByContractId(contract.getContractId()).stream()
                .filter(invoice -> excludedInvoiceId == null
                        || !excludedInvoiceId.equals(invoice.getInvoiceId()))
                .map(invoice -> PaymentCalculator.money(invoice.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = PaymentCalculator.grossWithTax(
                contract.getContractValue(), contract.getCommercialTaxRate());
        if (invoiced.add(newInvoiceTotal).compareTo(gross) > 0) {
            BigDecimal remaining = PaymentCalculator.remaining(gross, invoiced);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invoice total including tax (" + formatAmount(newInvoiceTotal)
                            + ") exceeds the remaining invoiceable amount of " + formatAmount(remaining)
                            + " (contract gross " + formatAmount(gross)
                            + ", already invoiced " + formatAmount(invoiced) + ").");
        }
    }

    private String formatAmount(BigDecimal value) {
        return PaymentCalculator.money(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private MasterCurrency resolveCurrency(Integer currencyId, MasterCurrency fallback) {
        if (currencyId == null) {
            return fallback;
        }
        return currencyRepository.findById(currencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency not found"));
    }

    private MasterPaymentStatus resolveSelectedStatus(Integer paymentStatusId, String fallbackCode) {
        if (paymentStatusId == null) {
            return resolveStatus(fallbackCode);
        }
        return paymentStatusRepository.findById(paymentStatusId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected payment status was not found"));
    }

    private MasterPaymentStatus resolveStatus(String code) {
        return paymentStatusRepository.findByCodeIgnoreCase(code)
                .or(() -> paymentStatusRepository.findFirstByNameIgnoreCase(paymentStatusName(code)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment status " + code + " is missing. Add it to the master payment statuses."));
    }

    private User requireWriteAccess(String employeeCode) {
        User actor = getUser(employeeCode);
        String roleCode = actor.getRole() == null ? "" : actor.getRole().getRoleCode();
        if (!ROLE_FINANCE.equalsIgnoreCase(roleCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only Finance users can modify payment records");
        }
        return actor;
    }

    private User getUser(String employeeCode) {
        return userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
    }

    private Invoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private void ensureNotDraft(Contract contract) {
        if (isDraft(contract)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Draft contracts do not have payment records");
        }
    }

    private boolean isDraft(Contract contract) {
        return contract.getStatus() != null && "DRAFT".equalsIgnoreCase(contract.getStatus().getCode());
    }

    private String noteOrDefault(String note, String fallback) {
        return note == null || note.isBlank() ? fallback : note.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
