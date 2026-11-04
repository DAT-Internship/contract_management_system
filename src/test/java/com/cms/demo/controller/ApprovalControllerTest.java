package com.cms.demo.controller;

import com.cms.demo.entity.ApprovalStep;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.CustomerSupplier;
import com.cms.demo.entity.Department;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.repository.ApprovalStepRepository;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApprovalControllerTest {
    private ApprovalStepRepository approvalRepository;
    private UserRepository userRepository;
    private ContractRepository contractRepository;
    private ContractStatusRepository contractStatusRepository;
    private AuditLogService auditLogService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        approvalRepository = mock(ApprovalStepRepository.class);
        userRepository = mock(UserRepository.class);
        contractRepository = mock(ContractRepository.class);
        contractStatusRepository = mock(ContractStatusRepository.class);
        auditLogService = mock(AuditLogService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ApprovalController(approvalRepository, userRepository,
                contractRepository, contractStatusRepository, auditLogService))
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
                    // Controller tests assert the logical view and model; template rendering is out of scope.
                }

                @Override
                public String toString() {
                    return viewName;
                }
            };
        };
    }

    @Test
    void approvingPendingContractUpdatesStepContractAndAudit() throws Exception {
        User reviewer = user("reviewer", "PMO", null);
        ApprovalStep step = pendingStep();
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        when(contractStatusRepository.findAll()).thenReturn(List.of(activeStatus));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer")
                        .param("decision", "approved").param("comment", "  Reviewed  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/approvals"))
                .andExpect(flash().attribute("success", "Contract approved."));

        assertEquals("APPROVED", step.getStatus());
        assertEquals("Reviewed", step.getComment());
        assertEquals(reviewer, step.getApprover());
        assertEquals(activeStatus, step.getContract().getStatus());
        assertEquals(reviewer, step.getContract().getUpdatedBy());
        verify(approvalRepository).save(step);
        verify(contractRepository).save(step.getContract());
        verify(auditLogService).record(eq("reviewer"), eq("APPROVAL"), eq(42L), eq("UPDATE"),
                contains("PENDING"), contains("APPROVED"));
    }

    @Test
    void rejectingWithoutCommentIsRejectedAndDoesNotChangeStep() throws Exception {
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        ApprovalStep step = pendingStep();
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer")
                        .param("decision", "REJECTED").param("comment", "   "))
                .andExpect(status().isBadRequest());

        assertEquals("PENDING", step.getStatus());
        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void rejectingWithCommentCreatesReturnedQueueStepAndAudit() throws Exception {
        User reviewer = user("reviewer", "PMO", null);
        ApprovalStep step = pendingStep();
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer")
                        .param("decision", "REJECTED").param("comment", "  Fix clause  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/approvals"));

        assertEquals("REJECTED", step.getStatus());
        assertEquals("Fix clause", step.getComment());
        var returned = org.mockito.ArgumentCaptor.forClass(ApprovalStep.class);
        verify(approvalRepository, times(2)).save(returned.capture());
        ApprovalStep returnedStep = returned.getAllValues().get(1);
        assertEquals("RETURNED", returnedStep.getStatus());
        assertEquals("Return to Contract Creator", returnedStep.getCurrentApproval());
        assertEquals("Fix clause", returnedStep.getComment());
        assertEquals(step.getContract(), returnedStep.getContract());
        verify(auditLogService).record(eq("reviewer"), eq("APPROVAL"), eq(42L), eq("UPDATE"),
                contains("PENDING"), contains("REJECTED"));
    }

    @Test
    void departmentReviewerCannotDecideAnotherDepartmentsContract() throws Exception {
        Department reviewerDepartment = department(1);
        Department contractDepartment = department(2);
        User reviewer = user("reviewer", "APPROVER", reviewerDepartment);
        ApprovalStep step = pendingStep();
        step.getContract().setDepartment(contractDepartment);
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer")
                        .param("decision", "APPROVED"))
                .andExpect(status().isForbidden());

        assertEquals("PENDING", step.getStatus());
        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void queuePageShowsFilteredQueueAndNewestHistoryWithCounts() throws Exception {
        User reviewer = user("reviewer", "PMO", null);
        ApprovalStep pending = step("PENDING", "26-100", "Acme", null, null);
        ApprovalStep returned = step("RETURNED", "26-101", "Beta", null, null);
        ApprovalStep approved = step("APPROVED", "26-100", "Acme", LocalDateTime.of(2026, 1, 1, 0, 0), null);
        ApprovalStep rejected = step("REJECTED", "26-100", "Acme", null, LocalDateTime.of(2026, 1, 2, 0, 0));
        ApprovalStep paymentUpdated = step("PAYMENT_UPDATED", "26-100", "Acme", null, LocalDateTime.of(2026, 1, 3, 0, 0));
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findForDepartments(eq(List.of("PENDING", "RETURNED")), eq(true), eq(-1)))
                .thenReturn(List.of(pending, returned));
        when(approvalRepository.findForDepartments(
                eq(List.of("APPROVED", "REJECTED", "PAYMENT_UPDATED")), eq(true), eq(-1)))
                .thenReturn(List.of(approved, rejected, paymentUpdated));

        var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reviewer").param("q", " acme "))
                .andExpect(status().isOk())
                .andExpect(view().name("approvals"))
                .andExpect(model().attribute("page", "approvals"))
                .andExpect(model().attribute("query", " acme "))
                .andExpect(model().attribute("pendingCount", 1))
                .andExpect(model().attribute("approvedCount", 1L))
                .andExpect(model().attribute("rejectedCount", 1L))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ApprovalStep> history = (List<ApprovalStep>) result.getModelAndView().getModel().get("history");
        assertEquals(List.of(paymentUpdated, rejected, approved), history);
        verify(approvalRepository).findForDepartments(List.of("PENDING", "RETURNED"), true, -1);
        verify(approvalRepository).findForDepartments(List.of("APPROVED", "REJECTED", "PAYMENT_UPDATED"), true, -1);
    }

    @Test
    void queuePageWithNoSearchMatchesReturnsEmptyQueueAndHistory() throws Exception {
        User reviewer = user("reviewer", "PMO", null);
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findForDepartments(anyList(), eq(true), eq(-1)))
                .thenReturn(List.of(pendingStep()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reviewer").param("q", "no-such-contract"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()))
                .andExpect(model().attribute("history", List.of()))
                .andExpect(model().attribute("pendingCount", 0));
    }

    @Test
    void privilegedReviewerQueueUsesAllDepartmentScope() throws Exception {
        User reviewer = user("reviewer", "PMO", department(8));
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findForDepartments(anyList(), eq(true), eq(-1))).thenReturn(List.of());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reviewer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()))
                .andExpect(model().attribute("history", List.of()));

        verify(approvalRepository).findForDepartments(List.of("PENDING", "RETURNED"), true, -1);
        verify(approvalRepository).findForDepartments(List.of("APPROVED", "REJECTED", "PAYMENT_UPDATED"), true, -1);
    }

    @Test
    void queuePageLimitsDepartmentReviewerToOwnActiveDepartment() throws Exception {
        Department department = department(8);
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "APPROVER", department)));
        when(approvalRepository.findForDepartments(anyList(), eq(false), eq(8))).thenReturn(List.of());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reviewer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()))
                .andExpect(model().attribute("history", List.of()));

        verify(approvalRepository).findForDepartments(List.of("PENDING", "RETURNED"), false, 8);
        verify(approvalRepository).findForDepartments(List.of("APPROVED", "REJECTED", "PAYMENT_UPDATED"), false, 8);
    }

    @Test
    void queuePageReturnsEmptyForRoleWithoutReviewPermission() throws Exception {
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reader"))
                .thenReturn(Optional.of(user("reader", "FINANCE", department(8))));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reader"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()))
                .andExpect(model().attribute("history", List.of()))
                .andExpect(model().attribute("pendingCount", 0));

        verifyNoInteractions(approvalRepository);
    }

    @Test
    void queuePageReturnsEmptyForReviewerWithInactiveDepartment() throws Exception {
        Department department = department(8);
        department.setIsActive(false);
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "DEPT_HEAD", department)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reviewer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("queue", List.of()));

        verifyNoInteractions(approvalRepository);
    }

    @Test
    void missingActiveUserCannotOpenQueue() throws Exception {
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("inactive")).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "inactive"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingActiveUserCannotDecideApproval() throws Exception {
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("inactive")).thenReturn(Optional.empty());

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "inactive").param("decision", "APPROVED"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(approvalRepository, contractRepository, auditLogService);
    }

    @Test
    void decisionRejectsUnknownStepAndInvalidDecision() throws Exception {
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        when(approvalRepository.findById(404L)).thenReturn(Optional.empty());
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(pendingStep()));

        mockMvc.perform(post("/approvals/404/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "PENDING"))
                .andExpect(status().isBadRequest());

        verify(approvalRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void decisionRejectsMissingDecisionAndUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/approvals/not-a-number/decision").principal(() -> "reviewer")
                        .param("decision", "APPROVED"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals/7/decision"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void decisionRejectsApprovalStepWithoutContract() throws Exception {
        ApprovalStep step = new ApprovalStep();
        step.setStatus("PENDING");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().isConflict());

        verify(approvalRepository, never()).save(any());
    }

    @Test
    void decisionRejectsStepAlreadyDecided() throws Exception {
        ApprovalStep step = pendingStep();
        step.setStatus("APPROVED");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "REJECTED")
                        .param("comment", "Changed mind"))
                .andExpect(status().isConflict());

        verify(approvalRepository, never()).save(any());
    }

    @Test
    void sameDepartmentReviewerCanApproveAndAliasesAreCaseInsensitive() throws Exception {
        Department department = department(1);
        User reviewer = user("reviewer", "department_head", department);
        ApprovalStep step = pendingStep();
        step.getContract().setDepartment(department(1));
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        when(contractStatusRepository.findAll()).thenReturn(List.of(activeStatus));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().is3xxRedirection());

        assertEquals("APPROVED", step.getStatus());
        assertNull(step.getComment());
        verify(contractRepository).save(step.getContract());
    }

    @Test
    void returnedApprovalStepCanBeApproved() throws Exception {
        ApprovalStep step = pendingStep();
        step.setStatus("RETURNED");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(contractStatusRepository.findAll()).thenReturn(List.of(activeStatus));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/approvals"));

        assertEquals("APPROVED", step.getStatus());
        verify(approvalRepository).save(step);
        verify(contractRepository).save(step.getContract());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PMO", "CM", "ADMIN"})
    void privilegedReviewerRolesCanApproveAcrossDepartments(String roleCode) throws Exception {
        User reviewer = user("reviewer", roleCode, department(10));
        ApprovalStep step = pendingStep();
        step.getContract().setDepartment(department(20));
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        when(contractStatusRepository.findAll()).thenReturn(List.of(activeStatus));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().is3xxRedirection());

        assertEquals("APPROVED", step.getStatus());
        verify(contractRepository).save(step.getContract());
    }

    @ParameterizedTest
    @ValueSource(strings = {"HR", "DEPT_HEAD", "DEPARTMENT_HEAD", "APPROVER"})
    void departmentReviewerRolesCanApproveSameDepartment(String roleCode) throws Exception {
        Department reviewerDepartment = department(5);
        User reviewer = user("reviewer", roleCode, reviewerDepartment);
        ApprovalStep step = pendingStep();
        step.getContract().setDepartment(department(5));
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        when(contractStatusRepository.findAll()).thenReturn(List.of(activeStatus));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().is3xxRedirection());

        assertEquals("APPROVED", step.getStatus());
        verify(contractRepository).save(step.getContract());
    }

    @Test
    void approvalFailsWithServerErrorWhenActiveContractStatusIsMissing() throws Exception {
        ApprovalStep step = pendingStep();
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        when(contractStatusRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().isInternalServerError());

        // The controller saves the step before resolving CS001; a transaction proxy is needed to test DB rollback.
        verify(approvalRepository).save(step);
        verify(contractRepository, never()).save(any());
    }

    @Test
    void returnedStepUsesSystemLabelWhenCreatorIsMissing() throws Exception {
        User reviewer = user("reviewer", "ADMIN", null);
        ApprovalStep step = pendingStep();
        step.getContract().setCreatedBy(null);
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer")
                        .param("decision", "REJECTED").param("comment", "return"))
                .andExpect(status().is3xxRedirection());

        var returned = org.mockito.ArgumentCaptor.forClass(ApprovalStep.class);
        verify(approvalRepository, times(2)).save(returned.capture());
        assertEquals("Return to System", returned.getAllValues().get(1).getCurrentApproval());
    }

    @Test
    void queueRecognizesFiRoleButDecisionDoesNotAuthorizeIt() throws Exception {
        Department department = department(1);
        User reviewer = user("reviewer", "FI", department);
        ApprovalStep step = pendingStep();
        step.getContract().setDepartment(department);
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer")).thenReturn(Optional.of(reviewer));
        when(approvalRepository.findForDepartments(anyList(), eq(false), eq(1))).thenReturn(List.of(step));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/approvals")
                        .principal(() -> "reviewer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pendingCount", 1));
        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer").param("decision", "APPROVED"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvalAuditEscapesSpecialCharactersInComment() throws Exception {
        ApprovalStep step = pendingStep();
        when(userRepository.findByEmployeeCodeAndIsActiveTrue("reviewer"))
                .thenReturn(Optional.of(user("reviewer", "PMO", null)));
        when(approvalRepository.findById(7L)).thenReturn(Optional.of(step));
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(contractStatusRepository.findAll()).thenReturn(List.of(activeStatus));
        String comment = "quote \" slash \\ line\nnext";

        mockMvc.perform(post("/approvals/7/decision").principal(() -> "reviewer")
                        .param("decision", "APPROVED").param("comment", comment))
                .andExpect(status().is3xxRedirection());

        verify(auditLogService).record(eq("reviewer"), eq("APPROVAL"), eq(42L), eq("UPDATE"), anyString(),
                org.mockito.ArgumentMatchers.argThat(value -> value.contains("quote \\\" slash \\\\ line\\nnext")));
    }

    private static ApprovalStep step(String status, String contractNo, String partyName,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        Contract contract = new Contract();
        contract.setContractNo(contractNo);
        CustomerSupplier party = new CustomerSupplier();
        party.setPartyName(partyName);
        contract.setParty(party);
        ApprovalStep step = new ApprovalStep();
        step.setStatus(status);
        step.setContract(contract);
        step.setCreatedAt(createdAt);
        step.setUpdatedAt(updatedAt);
        return step;
    }

    private static ApprovalStep pendingStep() {
        Contract contract = new Contract();
        contract.setContractId(42L);
        contract.setContractNo("26-00042");
        User creator = new User();
        creator.setFullName("Contract Creator");
        contract.setCreatedBy(creator);
        ApprovalStep step = new ApprovalStep();
        step.setStepId(7L);
        step.setStatus("PENDING");
        step.setCurrentApproval("Department / PMO review");
        step.setContract(contract);
        step.setSubmittedBy(creator);
        return step;
    }

    private static User user(String employeeCode, String roleCode, Department department) {
        Role role = new Role();
        role.setRoleCode(roleCode);
        User user = new User();
        user.setEmployeeCode(employeeCode);
        user.setFullName("Test Reviewer");
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }

    private static Department department(int id) {
        Department department = new Department();
        department.setDepartmentId(id);
        department.setIsActive(true);
        return department;
    }
}
