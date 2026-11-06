package com.cms.demo.controller;

import com.cms.demo.config.SecurityConfig;
import com.cms.demo.repository.ApprovalStepRepository;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.security.CustomUserDetailsService;
import com.cms.demo.service.AuditLogService;
import com.cms.demo.service.ApprovalNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApprovalController.class)
@Import(SecurityConfig.class)
class ApprovalSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ApprovalStepRepository approvalStepRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ContractRepository contractRepository;

    @MockitoBean
    private ContractStatusRepository contractStatusRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private ApprovalNotificationService approvalNotificationService;

    @Test
    void unauthenticatedQueueRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/approvals"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(approvalStepRepository, userRepository);
    }

    @Test
    void unauthenticatedDecisionRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/approvals/7/decision").param("decision", "APPROVED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(approvalStepRepository, userRepository, auditLogService);
    }
}
