package com.cms.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cms.demo.entity.ApprovalStep;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.Role;
import com.cms.demo.entity.User;
import com.cms.demo.event.ContractApprovedEvent;
import com.cms.demo.repository.ApprovalStepRepository;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.AuditLogService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ApprovalControllerTest {

    @Test
    void publishesApprovedEventForNotificationAfterApproval() {
        ApprovalStepRepository approvals = org.mockito.Mockito.mock(ApprovalStepRepository.class);
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        ContractRepository contracts = org.mockito.Mockito.mock(ContractRepository.class);
        ContractStatusRepository statuses = org.mockito.Mockito.mock(ContractStatusRepository.class);
        AuditLogService auditLog = org.mockito.Mockito.mock(AuditLogService.class);
        ApplicationEventPublisher eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);

        User approver = new User();
        approver.setEmployeeCode("ADMIN-1");
        Role role = new Role();
        role.setRoleCode("ADMIN");
        approver.setRole(role);
        when(users.findByEmployeeCodeAndIsActiveTrue("ADMIN-1")).thenReturn(Optional.of(approver));

        Contract contract = new Contract();
        contract.setContractId(17L);
        ApprovalStep step = new ApprovalStep();
        step.setContract(contract);
        step.setStatus("PENDING");
        step.setCurrentApproval("Department / PMO review");
        when(approvals.findById(3L)).thenReturn(Optional.of(step));

        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setCode("CS001");
        when(statuses.findAll()).thenReturn(List.of(activeStatus));

        ApprovalController controller = new ApprovalController(
                approvals, users, contracts, statuses, auditLog, eventPublisher);
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();
        String result = controller.decide(3L, "APPROVED", null, () -> "ADMIN-1", flash);

        assertEquals("redirect:/approvals", result);
        verify(eventPublisher).publishEvent(new ContractApprovedEvent(17L));
    }
}
