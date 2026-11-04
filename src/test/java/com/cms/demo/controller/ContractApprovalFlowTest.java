package com.cms.demo.controller;

import com.cms.demo.dto.ContractResponse;
import com.cms.demo.entity.ApprovalStep;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.MasterPaymentType;
import com.cms.demo.entity.User;
import com.cms.demo.repository.AgreementForRepository;
import com.cms.demo.repository.ApprovalStepRepository;
import com.cms.demo.repository.ContractCategoryRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.ContractTypeRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.CustomerSupplierRepository;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.PaymentTypeRepository;
import com.cms.demo.repository.ProjectRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.AuditLogService;
import com.cms.demo.service.ContractService;
import com.cms.demo.service.FileStorageService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContractApprovalFlowTest {
    private ContractService contractService;
    private UserRepository userRepository;
    private ApprovalStepRepository approvalRepository;
    private AuditLogService auditLogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        contractService = mock(ContractService.class);
        userRepository = mock(UserRepository.class);
        approvalRepository = mock(ApprovalStepRepository.class);
        auditLogService = mock(AuditLogService.class);
        ContractPageController controller = new ContractPageController(contractService, mock(FileStorageService.class),
                mock(AgreementForRepository.class), mock(ContractCategoryRepository.class),
                mock(ContractTypeRepository.class), mock(PaymentTypeRepository.class),
                mock(ContractStatusRepository.class), mock(DepartmentRepository.class),
                mock(CustomerSupplierRepository.class), mock(CurrencyRepository.class), mock(ProjectRepository.class),
                userRepository, approvalRepository, validator(), auditLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(testViewResolver())
                .build();
    }

    private static ViewResolver testViewResolver() {
        return (viewName, locale) -> {
            if (viewName.startsWith("redirect:")) {
                return new RedirectView(viewName.substring("redirect:".length()));
            }
            return new AbstractView() {
                @Override
                protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                        HttpServletResponse response) {
                    // These tests assert controller results; template rendering is outside their scope.
                }

                @Override
                public String toString() {
                    return viewName;
                }
            };
        };
    }

    @Test
    void submittingNewContractCreatesPendingApprovalAndAuditEvent() throws Exception {
        User submitter = user("25-00001", "Contract Creator");
        Contract contract = new Contract();
        contract.setContractId(100L);
        when(contractService.create(any(), any(), eq("25-00001"))).thenReturn(contractResponse());
        when(contractService.getContract(100L)).thenReturn(contract);
        when(userRepository.findByEmployeeCode("25-00001")).thenReturn(Optional.of(submitter));

        mockMvc.perform(post("/contracts").principal(() -> "25-00001").params(params("submit")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contracts"))
                .andExpect(flash().attribute("success", "Contract submitted for approval."));

        var saved = org.mockito.ArgumentCaptor.forClass(ApprovalStep.class);
        verify(approvalRepository).save(saved.capture());
        ApprovalStep step = saved.getValue();
        assertEquals("PENDING", step.getStatus());
        assertEquals("Department / PMO review", step.getCurrentApproval());
        assertEquals(contract, step.getContract());
        assertEquals(submitter, step.getSubmittedBy());
        assertEquals(contract.getParty(), step.getParty());
        org.junit.jupiter.api.Assertions.assertNotNull(step.getCreatedAt());
        org.junit.jupiter.api.Assertions.assertNotNull(step.getUpdatedAt());
        verify(auditLogService).record(eq("25-00001"), eq("APPROVAL"), eq(100L), eq("UPDATE"),
                contains("\"status\":\"\""), contains("\"status\":\"PENDING\""));
    }

    @Test
    void registeringContractWithoutSubmitActionDoesNotCreateApproval() throws Exception {
        when(contractService.create(any(), any(), eq("25-00001"))).thenReturn(contractResponse());

        mockMvc.perform(post("/contracts").principal(() -> "25-00001").params(params("save")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contracts"));

        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void resubmittingReturnedContractReusesStepAndPreservesApprovalHistory() throws Exception {
        User reviser = user("25-00001", "Contract Creator");
        Contract contract = editableContract();
        ApprovalStep returned = new ApprovalStep();
        returned.setStepId(77L);
        returned.setStatus("RETURNED");
        returned.setComment("Please revise");
        returned.setContract(contract);
        returned.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        when(contractService.getContractForUser(100L, "25-00001")).thenReturn(contract);
        when(approvalRepository.findByContractContractIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(returned));
        when(userRepository.findByEmployeeCode("25-00001")).thenReturn(Optional.of(reviser));

        mockMvc.perform(post("/contracts/100").principal(() -> "25-00001").params(params("submit")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contracts/100"));

        assertEquals("PENDING", returned.getStatus());
        assertEquals("Resubmitted by Contract Creator", returned.getCurrentApproval());
        assertNull(returned.getComment());
        assertEquals(reviser, returned.getSubmittedBy());
        org.junit.jupiter.api.Assertions.assertNotNull(returned.getUpdatedAt());
        verify(approvalRepository).save(returned);
        verify(auditLogService).record(eq("25-00001"), eq("APPROVAL"), eq(100L), eq("UPDATE"),
                contains("RETURNED"), contains("PENDING"));
    }

    @Test
    void resubmittingAfterApprovalCreatesNewPendingStepAndKeepsApprovedRecord() throws Exception {
        User reviser = user("25-00001", "Contract Creator");
        Contract contract = editableContract();
        ApprovalStep approved = new ApprovalStep();
        approved.setStatus("APPROVED");
        approved.setContract(contract);
        when(contractService.getContractForUser(100L, "25-00001")).thenReturn(contract);
        when(contractService.getContract(100L)).thenReturn(contract);
        when(approvalRepository.findByContractContractIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(approved));
        when(userRepository.findByEmployeeCode("25-00001")).thenReturn(Optional.of(reviser));

        mockMvc.perform(post("/contracts/100").principal(() -> "25-00001").params(params("submit")))
                .andExpect(status().is3xxRedirection());

        assertEquals("APPROVED", approved.getStatus());
        var saved = org.mockito.ArgumentCaptor.forClass(ApprovalStep.class);
        verify(approvalRepository).save(saved.capture());
        assertEquals("PENDING", saved.getValue().getStatus());
        assertEquals("Department / PMO review", saved.getValue().getCurrentApproval());
        org.junit.jupiter.api.Assertions.assertNotSame(approved, saved.getValue());
    }

    @Test
    void resubmissionSupersedesOlderDuplicatePendingSteps() throws Exception {
        Contract contract = editableContract();
        ApprovalStep newest = new ApprovalStep();
        newest.setStatus("PENDING");
        newest.setCreatedAt(java.time.LocalDateTime.of(2026, 2, 2, 0, 0));
        ApprovalStep older = new ApprovalStep();
        older.setStatus("PENDING");
        older.setCreatedAt(java.time.LocalDateTime.of(2026, 2, 1, 0, 0));
        when(contractService.getContractForUser(100L, "25-00001")).thenReturn(contract);
        when(approvalRepository.findByContractContractIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(newest, older));

        mockMvc.perform(post("/contracts/100").principal(() -> "25-00001").params(params("submit")))
                .andExpect(status().is3xxRedirection());

        assertEquals("PENDING", newest.getStatus());
        assertEquals("SUPERSEDED", older.getStatus());
        verify(approvalRepository).saveAll(List.of(newest, older));
    }

    private static Contract editableContract() {
        Contract contract = new Contract();
        contract.setContractId(100L);
        contract.setContractNo("26-00001");
        MasterContractStatus status = new MasterContractStatus();
        status.setCode("ACTIVE");
        contract.setStatus(status);
        MasterPaymentType paymentType = new MasterPaymentType();
        paymentType.setId(1);
        contract.setPaymentType(paymentType);
        MasterCurrency currency = new MasterCurrency();
        currency.setId(1);
        contract.setCurrency(currency);
        contract.setContractValue(new BigDecimal("50000.00"));
        contract.setCommercialTaxRate(new BigDecimal("5.00"));
        return contract;
    }

    private static User user(String employeeCode, String name) {
        User user = new User();
        user.setEmployeeCode(employeeCode);
        user.setFullName(name);
        return user;
    }

    private static Validator validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static MultiValueMap<String, String> params(String action) {
        Map<String, String[]> values = new LinkedHashMap<>();
        values.put("contractPoNo", new String[]{"PO-2026-001"});
        values.put("agreementForId", new String[]{"1"});
        values.put("categoryId", new String[]{"1"});
        values.put("contractTypeId", new String[]{"1"});
        values.put("paymentTypeId", new String[]{"1"});
        values.put("statusId", new String[]{"1"});
        values.put("departmentId", new String[]{"1"});
        values.put("fiscalYear", new String[]{"FY2026"});
        values.put("partyId", new String[]{"1"});
        values.put("description", new String[]{"Full contract description"});
        values.put("startDate", new String[]{"2026-01-01"});
        values.put("endDate", new String[]{"2026-12-31"});
        values.put("contractValue", new String[]{"50000.00"});
        values.put("commercialTaxRate", new String[]{"5.00"});
        values.put("currencyId", new String[]{"1"});
        values.put("action", new String[]{action});
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        values.forEach((key, entries) -> params.put(key, Arrays.asList(entries)));
        return params;
    }

    private static ContractResponse contractResponse() {
        return new ContractResponse(100L, "26-00001", "PO-2026-001", null, "FY2026", "Acme Corp",
                "Full contract description", null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), 12, 364L, new BigDecimal("2.00"), new BigDecimal("24.00"),
                new BigDecimal("50000.00"), new BigDecimal("5.00"), new BigDecimal("2500.00"), "USD", "ACTIVE",
                "Active", null, null, "Services", "Operations");
    }
}
