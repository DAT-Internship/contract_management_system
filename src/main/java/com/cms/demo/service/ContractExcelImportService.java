package com.cms.demo.service;

import com.cms.demo.dto.ContractRequest;
import com.cms.demo.entity.*;
import com.cms.demo.repository.*;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ContractExcelImportService {
    private final ContractService contracts;
    private final AgreementForRepository agreementForRepository;
    private final ContractCategoryRepository categoryRepository;
    private final ContractTypeRepository typeRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final ContractStatusRepository statusRepository;
    private final DepartmentRepository departmentRepository;
    private final CustomerSupplierRepository partyRepository;
    private final CurrencyRepository currencyRepository;
    private final ProjectRepository projectRepository;
    private final ContractRepository contractRepository;

    public ContractExcelImportService(ContractService contracts, AgreementForRepository agreementForRepository,
            ContractCategoryRepository categoryRepository, ContractTypeRepository typeRepository,
            PaymentTypeRepository paymentTypeRepository, ContractStatusRepository statusRepository,
            DepartmentRepository departmentRepository, CustomerSupplierRepository partyRepository,
            CurrencyRepository currencyRepository, ProjectRepository projectRepository, ContractRepository contractRepository) {
        this.contracts = contracts;
        this.agreementForRepository = agreementForRepository;
        this.categoryRepository = categoryRepository;
        this.typeRepository = typeRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.statusRepository = statusRepository;
        this.departmentRepository = departmentRepository;
        this.partyRepository = partyRepository;
        this.currencyRepository = currencyRepository;
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
    }

    public ImportResult importWorkbook(InputStream input, String employeeCode) {
        return importWorkbook(input, employeeCode, false);
    }

    public ImportResult importWorkbook(InputStream input, String employeeCode, boolean skipExisting) {
        List<Map<String, String>> rows = XlsxSheetReader.read(input);
        List<RowError> errors = new java.util.ArrayList<>();
        int successful = 0;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                String contractNo = blankToNull(row.get("contract_no"));
                if (skipExisting && contractNo != null && contractRepository.existsByContractNo(contractNo)) {
                    successful++;
                    continue;
                }
                contracts.create(toRequest(row, i + 2), employeeCode);
                successful++;
            } catch (ImportRowException ex) {
                errors.add(new RowError(i + 2, ex.field, ex.getMessage()));
            } catch (RuntimeException ex) {
                String message = ex.getMessage() == null ? "Could not import this row" : ex.getMessage();
                errors.add(new RowError(i + 2, "row", message));
            }
        }
        return new ImportResult(rows.size(), successful, errors);
    }

    private ContractRequest toRequest(Map<String, String> row, int rowNumber) {
        String contractNo = required(row, "contract_no", rowNumber);
        String customer = required(row, "customer", rowNumber);
        String category = required(row, "category", rowNumber);
        String status = required(row, "status", rowNumber);
        String department = required(row, "department", rowNumber);
        String currency = required(row, "currency", rowNumber);
        LocalDate start = date(row.get("start_date"), "start_date", rowNumber);
        LocalDate end = date(row.get("end_date"), "end_date", rowNumber);
        BigDecimal value = decimal(row.get("value"), "value", rowNumber);
        CustomerSupplier party = partyRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()) && same(p.getPartyName(), customer)).findFirst()
                .orElseThrow(() -> fail("customer", "Customer '" + customer + "' was not found in Customer/Supplier master data"));
        MasterContractCategory categoryEntity = categoryRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()) && (same(item.getCode(), category) || same(item.getName(), category))).findFirst()
                .orElseThrow(() -> fail("category", "Category '" + category + "' was not found in Contract Category master data"));
        String statusCode = status.equalsIgnoreCase("DRAFT") ? "WAITING_SIGN" : status.equalsIgnoreCase("CANCELLED") ? "TERMINATED" : status;
        MasterContractStatus statusEntity = statusRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()) && (same(item.getCode(), statusCode) || same(item.getName(), status))).findFirst()
                .orElseThrow(() -> fail("status", "Status '" + status + "' was not found in Contract Status master data"));
        Department departmentEntity = departmentRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()) && (same(item.getDeptCode(), department) || same(item.getDeptName(), department))).findFirst()
                .orElseThrow(() -> fail("department", "Department '" + department + "' was not found in Department master data"));
        MasterCurrency currencyEntity = currencyRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()) && (same(item.getCode(), currency) || same(item.getName(), currency))).findFirst()
                .orElseThrow(() -> fail("currency", "Currency '" + currency + "' was not found in Currency master data"));
        Project project = null;
        String projectCode = blankToNull(row.get("project_id"));
        if (projectCode != null) {
            project = projectRepository.findByProjectCodeIgnoreCase(projectCode)
                    .orElseThrow(() -> fail("project_id", "Project '" + projectCode + "' was not found"));
        }
        MasterAgreementFor agreement = agreementForRepository.findAll().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                .sorted(Comparator.comparing(MasterAgreementFor::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .filter(item -> same(item.getCode(), party.getPartyType()) || same(item.getName(), party.getPartyType()))
                .findFirst().orElseGet(() -> firstActive(agreementForRepository.findAll(), MasterAgreementFor::getIsActive, "Agreement For"));
        MasterContractType contractType = firstActive(typeRepository.findAll(), MasterContractType::getIsActive, "Contract Type");
        MasterPaymentType paymentType = firstActive(paymentTypeRepository.findAll(), MasterPaymentType::getIsActive, "Payment Type");
        if (value.signum() < 0) throw fail("value", "Contract value cannot be negative");
        if (end.isBefore(start)) throw fail("end_date", "End date must be on or after start date");
        String description = blankToNull(row.get("description"));
        if (description == null) description = "Imported contract " + contractNo;
        return new ContractRequest(contractNo, contractNo, null, agreement.getId(), categoryEntity.getId(),
                contractType.getId(), paymentType.getId(), statusEntity.getId(), departmentEntity.getDepartmentId(),
                "FY" + start.getYear(), party.getPartyId(), description, null, LocalDate.now(), start, end,
                (int) Math.max(1, ChronoUnit.MONTHS.between(start, end.plusDays(1))), null, value,
                new BigDecimal("5.00"), currencyEntity.getId(), project == null ? null : project.getProjectId(),
                null, null, null, null);
    }

    private <T> T firstActive(List<T> list, java.util.function.Function<T, Boolean> active, String label) {
        return list.stream().filter(item -> Boolean.TRUE.equals(active.apply(item))).findFirst()
                .orElseThrow(() -> fail("master data", "No active " + label + " is configured"));
    }
    private String required(Map<String, String> row, String field, int rowNumber) {
        String value = blankToNull(row.get(field));
        if (value == null) throw fail(field, "Row " + rowNumber + ": " + field + " is required");
        return value;
    }
    private LocalDate date(String value, String field, int row) {
        if (value == null || value.isBlank()) throw fail(field, "Row " + row + ": " + field + " is required");
        try {
            if (value.matches("\\d+(\\.\\d+)?")) return LocalDate.of(1899, 12, 30).plusDays(new BigDecimal(value).longValue());
            try { return LocalDate.parse(value.trim()); }
            catch (Exception ignored) { return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ROOT)); }
        } catch (Exception ex) { throw fail(field, "Row " + row + ": use YYYY-MM-DD for " + field); }
    }
    private BigDecimal decimal(String value, String field, int row) {
        try { return new BigDecimal(required(Map.of(field, value == null ? "" : value), field, row).replace(",", "")); }
        catch (NumberFormatException ex) { throw fail(field, "Row " + row + ": " + field + " must be a number"); }
    }
    private boolean same(String left, String right) { return left != null && right != null && left.trim().equalsIgnoreCase(right.trim()); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ImportRowException fail(String field, String message) { return new ImportRowException(field, message); }

    public record RowError(int rowNumber, String field, String message) { }
    public record ImportResult(int total, int successful, List<RowError> errors) { }
    private static class ImportRowException extends RuntimeException {
        private final String field;
        private ImportRowException(String field, String message) { super(message); this.field = field; }
    }
}
