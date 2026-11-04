package com.cms.demo.controller;

import com.cms.demo.entity.ApprovalStep;
import com.cms.demo.repository.ApprovalStepRepository;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.service.AuditLogService;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/approvals")
public class ApprovalController {
    private static final List<String> QUEUE_STATUSES = List.of("PENDING", "RETURNED");
    private final ApprovalStepRepository approvalRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final ContractStatusRepository contractStatusRepository;
    private final AuditLogService auditLogService;

    public ApprovalController(ApprovalStepRepository approvalRepository, UserRepository userRepository,
            ContractRepository contractRepository, ContractStatusRepository contractStatusRepository,
            AuditLogService auditLogService) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.contractRepository = contractRepository;
        this.contractStatusRepository = contractStatusRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String q, Principal principal, Model model) {
        var user = userRepository.findByEmployeeCodeAndIsActiveTrue(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String roleCode = user.getRole() == null ? "" : user.getRole().getRoleCode();
        boolean privileged = hasRoleCode(roleCode, "PMO", "CM", "ADMIN");
        boolean deptHead = hasRoleCode(roleCode, "HR", "DEPT_HEAD", "FI", "DEPARTMENT_HEAD", "APPROVER");
        boolean canReview = privileged || (deptHead && user.getDepartment() != null
                && Boolean.TRUE.equals(user.getDepartment().getIsActive()));
        Integer departmentId = canReview && !privileged ? user.getDepartment().getDepartmentId() : -1;
        String query = q == null || q.isBlank() ? null : q.trim();
        var queuePage = queuePage(canReview, privileged, departmentId, query, 0);
        var history = canReview ? approvalRepository.findForDepartments(
                        List.of("APPROVED", "REJECTED", "PAYMENT_UPDATED"), privileged, departmentId).stream()
                .sorted((a,b) -> time(b).compareTo(time(a))).toList() : List.<ApprovalStep>of();
        model.addAttribute("page", "approvals");
        model.addAttribute("query", query);
        addQueueModel(model, queuePage);
        model.addAttribute("history", history);
        model.addAttribute("approvedCount", history.stream().filter(step -> "APPROVED".equalsIgnoreCase(step.getStatus())).count());
        model.addAttribute("rejectedCount", history.stream().filter(step -> "REJECTED".equalsIgnoreCase(step.getStatus())).count());
        return "approvals";
    }

    @GetMapping("/queue")
    public String searchQueue(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, Principal principal, Model model) {
        var user = userRepository.findByEmployeeCodeAndIsActiveTrue(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String roleCode = user.getRole() == null ? "" : user.getRole().getRoleCode();
        boolean privileged = hasRoleCode(roleCode, "PMO", "CM", "ADMIN");
        boolean deptHead = hasRoleCode(roleCode, "HR", "DEPT_HEAD", "FI", "DEPARTMENT_HEAD", "APPROVER");
        boolean canReview = privileged || (deptHead && user.getDepartment() != null
                && Boolean.TRUE.equals(user.getDepartment().getIsActive()));
        Integer departmentId = canReview && !privileged ? user.getDepartment().getDepartmentId() : -1;
        String query = q == null || q.isBlank() ? null : q.trim();
        int safePage = Math.max(0, page);
        model.addAttribute("query", query == null ? "" : query);
        addQueueModel(model, queuePage(canReview, privileged, departmentId, query, safePage));
        return "approvals :: queueResults";
    }

    private Page<ApprovalStep> queuePage(boolean canReview, boolean privileged, Integer departmentId,
            String query, int page) {
        if (!canReview) return Page.empty(PageRequest.of(page, 25));
        return approvalRepository.searchQueue(QUEUE_STATUSES, privileged, departmentId, query,
                PageRequest.of(page, 25, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private void addQueueModel(Model model, Page<ApprovalStep> queuePage) {
        model.addAttribute("queue", queuePage.getContent());
        model.addAttribute("queuePage", queuePage);
        model.addAttribute("pendingCount", queuePage.getTotalElements());
    }

    @PostMapping("/{stepId}/decision")
    @Transactional
    public String decide(@PathVariable Long stepId, @RequestParam String decision,
            @RequestParam(required = false) String comment, Principal principal, RedirectAttributes flash) {
        var user = userRepository.findByEmployeeCodeAndIsActiveTrue(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String roleCode = user.getRole() == null ? "" : user.getRole().getRoleCode();
        var step = approvalRepository.findById(stepId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var contract = step.getContract();
        String previousStatus = step.getStatus();
        String approvalStage = step.getCurrentApproval();
        String normalized = decision.toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(normalized) && !"REJECTED".equals(normalized))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid approval decision");
        if (step.getContract() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approval is not linked to a contract");
        if (!QUEUE_STATUSES.contains(step.getStatus() == null ? "" : step.getStatus().toUpperCase(Locale.ROOT)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This approval has already been decided");
        boolean deptHead = hasRoleCode(roleCode, "HR", "DEPT_HEAD", "DEPARTMENT_HEAD", "APPROVER");
        boolean privileged = hasRoleCode(roleCode, "PMO", "CM", "ADMIN");
        if (!privileged && !(deptHead && user.getDepartment() != null && Boolean.TRUE.equals(user.getDepartment().getIsActive()) && contract.getDepartment() != null
                && user.getDepartment().getDepartmentId().equals(contract.getDepartment().getDepartmentId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot decide this contract");
        if ("REJECTED".equals(normalized) && (comment == null || comment.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A comment is required when returning a contract");
        if ("REJECTED".equals(normalized)) {
            step.setStatus("REJECTED");
            step.setComment(comment.trim());
            step.setApprover(user);
            step.setUpdatedAt(LocalDateTime.now());
            approvalRepository.save(step);
            ApprovalStep returned = new ApprovalStep();
            returned.setContract(contract);
            returned.setParty(contract.getParty());
            returned.setSubmittedBy(step.getSubmittedBy());
            String creatorName = contract.getCreatedBy() == null ? null : contract.getCreatedBy().getFullName();
            if (creatorName == null || creatorName.isBlank()) creatorName = "System";
            returned.setCurrentApproval("Return to " + creatorName);
            returned.setStatus("RETURNED");
            returned.setComment(comment.trim());
            returned.setApprover(user);
            returned.setCreatedAt(LocalDateTime.now());
            returned.setUpdatedAt(LocalDateTime.now());
            approvalRepository.save(returned);
        } else {
            step.setStatus(normalized);
            step.setComment(comment == null || comment.isBlank() ? null : comment.trim());
            step.setApprover(user);
            step.setUpdatedAt(LocalDateTime.now());
            approvalRepository.save(step);
            var activeStatus = contractStatusRepository.findAll().stream()
                    .filter(status -> "CS001".equalsIgnoreCase(status.getCode()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Active contract status (CS001) is missing"));
            contract.setStatus(activeStatus);
            contract.setUpdatedAt(LocalDateTime.now());
            contract.setUpdatedBy(user);
            contractRepository.save(contract);
        }
        auditLogService.record(principal.getName(), "APPROVAL", contract.getContractId(), "UPDATE",
                "{\"status\":\"" + auditValue(previousStatus) + "\",\"stage\":\"" + auditValue(approvalStage) + "\"}",
                "{\"status\":\"" + normalized + "\",\"stage\":\"" + auditValue(approvalStage)
                        + "\",\"comment\":\"" + auditValue(comment == null ? "" : comment.trim()) + "\"}");
        if ("APPROVED".equals(normalized)) flash.addFlashAttribute("success", "Contract approved.");
        return "redirect:/approvals";
    }

    private boolean hasRoleCode(String roleCode, String... acceptedCodes) {
        for (String acceptedCode : acceptedCodes) {
            if (acceptedCode.equalsIgnoreCase(roleCode == null ? "" : roleCode.trim())) return true;
        }
        return false;
    }

    private String auditValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private LocalDateTime time(ApprovalStep step) {
        return step.getUpdatedAt() != null ? step.getUpdatedAt() : step.getCreatedAt();
    }
}
