package com.cms.demo.controller;

import com.cms.demo.dto.ContractForm;
import com.cms.demo.entity.ContractAttachment;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.service.ContractService;
import com.cms.demo.service.FileStorageService;
import com.cms.demo.service.AuditLogService;
import com.cms.demo.service.ApprovalNotificationService;
import com.cms.demo.repository.AgreementForRepository;
import com.cms.demo.repository.ContractCategoryRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.ContractTypeRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.CustomerSupplierRepository;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.PaymentTypeRepository;
import com.cms.demo.repository.ProjectRepository;
import com.cms.demo.repository.UserRepository;
import com.cms.demo.repository.ApprovalStepRepository;
import jakarta.validation.Validator;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.Locale;
import com.cms.demo.entity.ApprovalStep;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.net.URI;

@Controller
@RequestMapping("/contracts")
public class ContractPageController {

    private final ContractService contractService;
    private final FileStorageService fileStorageService;
    private final AgreementForRepository agreementForRepository;
    private final ContractCategoryRepository categoryRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final ContractStatusRepository statusRepository;
    private final DepartmentRepository departmentRepository;
    private final CustomerSupplierRepository partyRepository;
    private final CurrencyRepository currencyRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final Validator validator;
    private final AuditLogService auditLogService;
    private final ApprovalNotificationService approvalNotificationService;

    public ContractPageController(
            ContractService contractService,
            FileStorageService fileStorageService,
            AgreementForRepository agreementForRepository,
            ContractCategoryRepository categoryRepository,
            ContractTypeRepository contractTypeRepository,
            PaymentTypeRepository paymentTypeRepository,
            ContractStatusRepository statusRepository,
            DepartmentRepository departmentRepository,
            CustomerSupplierRepository partyRepository,
            CurrencyRepository currencyRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            ApprovalStepRepository approvalStepRepository,
            Validator validator,
            AuditLogService auditLogService,
            ApprovalNotificationService approvalNotificationService) {
        this.contractService = contractService;
        this.fileStorageService = fileStorageService;
        this.agreementForRepository = agreementForRepository;
        this.categoryRepository = categoryRepository;
        this.contractTypeRepository = contractTypeRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.statusRepository = statusRepository;
        this.departmentRepository = departmentRepository;
        this.partyRepository = partyRepository;
        this.currencyRepository = currencyRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.validator = validator;
        this.auditLogService = auditLogService;
        this.approvalNotificationService = approvalNotificationService;
    }

    @GetMapping
    public String list(Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Principal principal) {
        model.addAttribute("page", "contracts");
        var all = contractService.findAll(principal.getName()).stream()
                .filter(c -> containsAny(search, c.contractNo(), c.partyName(), c.projectCode()))
                .filter(c -> matches(c.categoryName(), category))
                .filter(c -> matches(c.statusName(), status))
                .filter(c -> matches(c.departmentName(), department)).toList();
        pageSize = List.of(10, 25, 50, 100).contains(pageSize) ? pageSize : 25;
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        page = Math.max(1, Math.min(page, totalPages));
        model.addAttribute("contracts", all.subList(Math.min((page - 1) * pageSize, total), Math.min(page * pageSize, total)));
        model.addAttribute("total", total);
        model.addAttribute("pageNumber", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("showingStart", total == 0 ? 0 : (page - 1) * pageSize + 1);
        model.addAttribute("showingEnd", Math.min(page * pageSize, total));
        model.addAttribute("pageSizes", List.of(10, 25, 50, 100));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("status", status);
        model.addAttribute("department", department);
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("sortOrder", "id")));
        model.addAttribute("statuses", statusRepository.findAll(Sort.by("sortOrder", "id")));
        model.addAttribute("departments", departmentRepository.findAll(Sort.by("deptName")));
        return "contracts/list";
    }

    @GetMapping("/{contractId}")
    public String detail(@PathVariable Long contractId, Model model, Principal principal,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String q) {
        var contract = contractService.getContractForUser(contractId, principal.getName());
        if (contract.getStatus() != null && "DRAFT".equalsIgnoreCase(contract.getStatus().getCode())) {
            return "redirect:/contracts/" + contractId + "/edit";
        }
        model.addAttribute("page", "contracts");
        boolean returnFromApprovals = "approvals".equalsIgnoreCase(source);
        model.addAttribute("returnFromApprovals", returnFromApprovals);
        model.addAttribute("approvalQuery", q);
        model.addAttribute("contract", contract);
        model.addAttribute("attachments", contractService.getAttachmentResponses(contractId, principal.getName()));
        // Temporary customer-wide payment-summary sample data; replace with party-level aggregates when payment reporting is connected.
        java.math.BigDecimal customerContractTotal = new java.math.BigDecimal("28800000.00");
        java.math.BigDecimal customerInvoicedTotal = new java.math.BigDecimal("19200000.00");
        java.math.BigDecimal customerPaidTotal = new java.math.BigDecimal("16800000.00");
        model.addAttribute("customerContractTotal", customerContractTotal);
        model.addAttribute("customerInvoicedTotal", customerInvoicedTotal);
        model.addAttribute("customerPaidTotal", customerPaidTotal);
        model.addAttribute("customerOutstandingTotal", customerInvoicedTotal.subtract(customerPaidTotal));
        String currencyCode = contract.getCurrency() == null ? "" : contract.getCurrency().getCode();
        String currencySymbol = switch (currencyCode.toUpperCase(Locale.ROOT)) {
            case "USD" -> "$";
            case "MMK" -> "Ks";
            case "JPY", "CNY" -> "¥";
            default -> currencyCode;
        };
        model.addAttribute("paymentCurrencySymbol", currencySymbol);
        model.addAttribute("approvalSteps", approvalStepRepository.findByContractContractIdOrderByCreatedAtDesc(contractId)
                .stream()
                .sorted(Comparator.comparing((ApprovalStep step) -> step.getUpdatedAt() != null
                        ? step.getUpdatedAt() : step.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        return "contracts/detail";
    }

    @GetMapping("/import-batches")
    public String importBatches(@RequestParam(required = false) String type, Model model) {
        model.addAttribute("page", "import-batches");
        model.addAttribute("importType", type == null ? "" : type.toLowerCase(Locale.ROOT));
        return "import-batches";
    }

    private boolean contains(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT)));
    }

    private boolean containsAny(String filter, String... values) {
        return filter == null || filter.isBlank() || java.util.Arrays.stream(values).anyMatch(value -> contains(value, filter));
    }

    private boolean matches(String value, String filter) {
        return filter == null || filter.isBlank() || (value != null && value.equalsIgnoreCase(filter));
    }

    @GetMapping("/new")
    public String newContract(
            @RequestParam(required = false) Long renewedFromId,
            @RequestParam(required = false) Long duplicate,
            Model model,
            Principal principal) {
        ContractForm form = new ContractForm();
        Long copyFromId = renewedFromId != null ? renewedFromId : duplicate;
        if (copyFromId != null) {
            form = toForm(contractService.getContractForUser(copyFromId, principal.getName()));
            form.setContractId(null);
            form.setContractNo(null);
            form.setRegisterDate(LocalDate.now());
            if (renewedFromId != null) form.setRenewedFromId(renewedFromId);
        }
        form.setRegisterDate(LocalDate.now());
        form.setFiscalYear("FY" + LocalDate.now().getYear());
        form.setCommercialTaxRate(new java.math.BigDecimal("5.00"));
        model.addAttribute("page", "contracts");
        model.addAttribute("contractForm", form);
        model.addAttribute("isDraft", false);
        model.addAttribute("submitForApproval", false);
        addFormOptions(model);
        return "contracts/form";
    }

    @GetMapping("/{contractId}/edit")
    public String editContract(@PathVariable Long contractId, Model model, Principal principal) {
        model.addAttribute("page", "contracts");
        var contract = contractService.getContractForUser(contractId, principal.getName());
        model.addAttribute("isDraft", contract.getStatus() != null && "DRAFT".equalsIgnoreCase(contract.getStatus().getCode()));
        // Waiting Sign contracts are edited with a "Submit for Approval" button; Active (and other) contracts with "Save Changes".
        model.addAttribute("submitForApproval", isWaitingSignStatus(contract.getStatus()));
        model.addAttribute("contractForm", toForm(contract));
        addFormOptions(model);
        return "contracts/form";
    }

    @PostMapping({"", "/{contractId}"})
    public String create(
            @PathVariable(value = "contractId", required = false) Long contractId,
            @ModelAttribute("contractForm") ContractForm form,
            BindingResult bindingResult,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "action", required = false) String action,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean isDraftContract = false;
        boolean waitingSignContract = false;
        if (contractId != null) {
            var existing = contractService.getContractForUser(contractId, principal.getName());
            isDraftContract = existing.getStatus() != null && "DRAFT".equalsIgnoreCase(existing.getStatus().getCode());
            waitingSignContract = isWaitingSignStatus(existing.getStatus());
            // A Waiting Sign contract keeps its status until it is approved (the status select is locked in the form).
            if (waitingSignContract) form.setStatusId(existing.getStatus().getId());
        }
        model.addAttribute("isDraft", isDraftContract);
        model.addAttribute("submitForApproval", waitingSignContract);
        boolean sentToApproval = false;

        boolean saveAsDraft = "draft".equalsIgnoreCase(action);
        if (saveAsDraft) {
            form.setStatusId(draftStatusId());
        } else if (form.getStatusId() == null || isDraftStatus(form.getStatusId())) {
            resolveStatusByCandidateCodes(List.of("WAITING_SIGN", "ACTIVE"))
                    .ifPresent(s -> form.setStatusId(s.getId()));
        }

        boolean validationFailed = bindingResult.hasErrors();
        if (!saveAsDraft) {
            validationFailed |= !validator.validate(form).isEmpty() || hasMissingRequiredFields(form);
        }
        if (validationFailed) {
            model.addAttribute("error", saveAsDraft
                    ? "Check the date and number fields, then save the draft again."
                    : "Complete all required fields and correct invalid values before submitting the contract.");
            model.addAttribute("page", "contracts");
            addFormOptions(model);
            return "contracts/form";
        }

        try {
            if (saveAsDraft) {
                var draft = contractId == null
                        ? contractService.saveDraft(form.toRequest(), files, principal.getName())
                        : contractService.updateDraft(contractId, form.toRequest(), files, principal.getName());
                redirectAttributes.addFlashAttribute("success", "Draft saved. You can continue editing it at any time.");
                return "redirect:/contracts/" + draft.contractId() + "/edit";
            } else if (contractId == null) {
                var created = contractService.create(form.toRequest(), files, principal.getName());
                if ("submit".equalsIgnoreCase(action)) {
                    createApprovalStep(created.contractId(), principal.getName());
                    sentToApproval = true;
                }
            } else {
                var contractBeforeUpdate = contractService.getContractForUser(contractId, principal.getName());
                boolean paymentDetailsChanged = !java.util.Objects.equals(
                        contractBeforeUpdate.getPaymentType() == null ? null : contractBeforeUpdate.getPaymentType().getId(), form.getPaymentTypeId())
                        || !java.util.Objects.equals(contractBeforeUpdate.getContractValue(), form.getContractValue())
                        || !java.util.Objects.equals(contractBeforeUpdate.getCommercialTaxRate(), form.getCommercialTaxRate())
                        || !java.util.Objects.equals(contractBeforeUpdate.getCurrency() == null ? null : contractBeforeUpdate.getCurrency().getId(), form.getCurrencyId());
                contractService.update(contractId, form.toRequest(), principal.getName());
                // Only Draft / Waiting Sign contracts go to the approval stage.
                // Editing an Active (or any other status) contract just saves the changes.
                sentToApproval = isDraftContract || waitingSignContract;
                if (sentToApproval) ensureSinglePendingApproval(contractId, principal.getName());
                if (paymentDetailsChanged) createPaymentUpdatedHistory(contractId, principal.getName());
                if (files != null) files.stream().filter(file -> file != null && !file.isEmpty())
                        .forEach(file -> contractService.addAttachment(contractId, file, principal.getName()));
            }
            String successMessage = sentToApproval ? "Contract submitted for approval."
                    : contractId != null ? "Contract updated successfully."
                    : "draft".equalsIgnoreCase(action) ? "Contract draft saved successfully." : "Contract registered successfully.";
            redirectAttributes.addFlashAttribute("success", successMessage);
            return contractId == null ? "redirect:/contracts" : "redirect:/contracts/" + contractId;
        } catch (ResponseStatusException rse) {
            model.addAttribute("page", "contracts");
            model.addAttribute("error", rse.getReason() != null ? rse.getReason() : rse.getMessage());
            addFormOptions(model);
            return "contracts/form";
        } catch (RuntimeException ex) {
            model.addAttribute("page", "contracts");
            model.addAttribute("error", ex.getMessage());
            addFormOptions(model);
            return "contracts/form";
        }
    }

    private void createApprovalStep(Long contractId, String employeeCode) {
        var contract = contractService.getContract(contractId);
        var submitter = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
        ApprovalStep step = new ApprovalStep();
        step.setContract(contract);
        step.setParty(contract.getParty());
        step.setSubmittedBy(submitter);
        step.setCurrentApproval("Department / PMO review");
        step.setStatus("PENDING");
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());
        approvalStepRepository.save(step);
        recordApprovalChange(employeeCode, contractId, "", "PENDING", step.getCurrentApproval(), "");
        // NEW: notification + e-mail to the relevant approvers
        approvalNotificationService.notifyApproversSubmitted(contract, submitter, false);
    }

    private void createPaymentUpdatedHistory(Long contractId, String employeeCode) {
        var contract = contractService.getContract(contractId);
        var editor = userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
        ApprovalStep event = new ApprovalStep();
        event.setContract(contract);
        event.setParty(contract.getParty());
        event.setSubmittedBy(editor);
        event.setApprover(editor);
        event.setCurrentApproval("Payment Updated");
        event.setStatus("PAYMENT_UPDATED");
        event.setComment("Payment details updated");
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        approvalStepRepository.save(event);
        recordApprovalChange(employeeCode, contractId, "", "PAYMENT_UPDATED", event.getCurrentApproval(), event.getComment());
    }

    private void ensureSinglePendingApproval(Long contractId, String employeeCode) {
        var steps = approvalStepRepository.findByContractContractIdOrderByCreatedAtDesc(contractId);
        var pendingSteps = steps.stream()
                .filter(step -> "PENDING".equalsIgnoreCase(step.getStatus()))
                .toList();
        if (!pendingSteps.isEmpty()) {
            // Keep the newest pending row visible in the queue and close any duplicate pending rows.
            for (int i = 1; i < pendingSteps.size(); i++) {
                pendingSteps.get(i).setStatus("SUPERSEDED");
                pendingSteps.get(i).setUpdatedAt(LocalDateTime.now());
            }
            approvalStepRepository.saveAll(pendingSteps);
            return;
        }

        // Reuse a returned row only when it is the latest approval record. If the
        // latest decision was APPROVED, preserve it in history and create a new review row.
        ApprovalStep latest = steps.isEmpty() ? null : steps.get(0);
        if (latest != null && "RETURNED".equalsIgnoreCase(latest.getStatus())) {
            ApprovalStep step = latest;
            step.setStatus("PENDING");
            step.setComment(null);
            var reviser = userRepository.findByEmployeeCode(employeeCode).orElse(null);
            step.setSubmittedBy(reviser);
            String reviserName = reviser == null ? null : reviser.getFullName();
            if (reviserName == null || reviserName.isBlank()) reviserName = "System";
            step.setCurrentApproval("Resubmitted by " + reviserName);
            step.setUpdatedAt(LocalDateTime.now());
            approvalStepRepository.save(step);
            recordApprovalChange(employeeCode, contractId, "RETURNED", "PENDING", step.getCurrentApproval(), "");
            // NEW: resubmitted after a rejection -> notify the approvers again
            approvalNotificationService.notifyApproversSubmitted(contractService.getContract(contractId), reviser, true);
        } else {
            createApprovalStep(contractId, employeeCode);
        }
    }

    private void recordApprovalChange(String employeeCode, Long contractId, String oldStatus,
            String newStatus, String stage, String comment) {
        auditLogService.record(employeeCode, "APPROVAL", contractId, "UPDATE",
                "{\"status\":\"" + auditJsonValue(oldStatus) + "\",\"stage\":\"" + auditJsonValue(stage) + "\"}",
                "{\"status\":\"" + auditJsonValue(newStatus) + "\",\"stage\":\"" + auditJsonValue(stage)
                        + "\",\"comment\":\"" + auditJsonValue(comment) + "\"}");
    }

    private String auditJsonValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    @GetMapping("/{contractId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId,
            Principal principal) {
        contractService.getContractForUser(contractId, principal.getName());
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (!attachment.getContract().getContractId().equals(contractId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Attachment mismatch");
        }

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 404) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .location(URI.create("/contracts/" + contractId + "/attachments/" + attachmentId + "/view"))
                        .build();
            }
            throw ex;
        }
        MediaType mediaType = resolveAttachmentMediaType(attachment.getMimeType(), attachment.getFileName());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    @GetMapping("/{contractId}/attachments/{attachmentId}/view")
    public String viewAttachment(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId,
            Model model,
            Principal principal) {
        contractService.getContractForUser(contractId, principal.getName());
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (!attachment.getContract().getContractId().equals(contractId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Attachment mismatch");
        }

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 404) {
                model.addAttribute("contractId", contractId);
                model.addAttribute("fileName", attachment.getFileName());
                model.addAttribute("page", "contracts");
                return "contracts/attachment-missing";
            }
            throw ex;
        }
        if (attachment.getFileName() != null && attachment.getFileName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            try (var input = resource.getInputStream()) {
                List<Map<String, String>> previewRows = com.cms.demo.service.XlsxSheetReader.read(input);
                model.addAttribute("contractId", contractId);
                model.addAttribute("attachmentId", attachmentId);
                model.addAttribute("fileName", attachment.getFileName());
                model.addAttribute("columns", previewRows.isEmpty() ? List.of() : new ArrayList<>(previewRows.get(0).keySet()));
                model.addAttribute("previewRows", previewRows.stream().limit(200).toList());
                model.addAttribute("totalPreviewRows", previewRows.size());
                model.addAttribute("page", "contracts");
                return "contracts/attachment-preview";
            } catch (java.io.IOException ex) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not read the uploaded workbook", ex);
            }
        }
        return "redirect:/contracts/" + contractId + "/attachments/" + attachmentId + "/inline";
    }

    @GetMapping("/{contractId}/attachments/{attachmentId}/inline")
    public ResponseEntity<Resource> inlineAttachment(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId,
            Principal principal) {
        contractService.getContractForUser(contractId, principal.getName());
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (!attachment.getContract().getContractId().equals(contractId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Attachment mismatch");
        }
        Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
        MediaType mediaType = resolveAttachmentMediaType(attachment.getMimeType(), attachment.getFileName());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(attachment.getFileName(), StandardCharsets.UTF_8).build().toString())
                .body(resource);
    }

    private MediaType resolveAttachmentMediaType(String storedMimeType, String fileName) {
        if (storedMimeType != null && !storedMimeType.isBlank()
                && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(storedMimeType)) {
            try {
                return MediaType.parseMediaType(storedMimeType);
            } catch (IllegalArgumentException ignored) {
                // Fall through and infer the type from the uploaded file name.
            }
        }
        return MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    /** "WAITING_SIGN", "Waiting Sign", "waiting-sign" ... all become WAITINGSIGN. */
    private String normalizeStatusKey(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    /** Statuses are maintained in Master Setup, so match on the code OR the displayed name. */
    private boolean isWaitingSignStatus(MasterContractStatus status) {
        return status != null
                && ("WAITINGSIGN".equals(normalizeStatusKey(status.getCode()))
                || "WAITINGSIGN".equals(normalizeStatusKey(status.getName())));
    }

    private Optional<MasterContractStatus> resolveStatusByCandidateCodes(List<String> codes) {
        List<MasterContractStatus> all = statusRepository.findAll();
        for (String code : codes) {
            String key = normalizeStatusKey(code);
            for (MasterContractStatus s : all) {
                if ((key.equals(normalizeStatusKey(s.getCode())) || key.equals(normalizeStatusKey(s.getName())))
                        && Boolean.TRUE.equals(s.getIsActive())) {
                    return Optional.of(s);
                }
            }
        }
        return all.stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).findFirst();
    }

    private Integer draftStatusId() {
        return statusRepository.findAll().stream()
                .filter(status -> "DRAFT".equalsIgnoreCase(status.getCode()))
                .map(MasterContractStatus::getId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Draft status is missing. Apply the latest database migration."));
    }

    private boolean isDraftStatus(Integer statusId) {
        return statusRepository.findById(statusId)
                .map(status -> "DRAFT".equalsIgnoreCase(status.getCode()))
                .orElse(false);
    }

    private boolean hasMissingRequiredFields(ContractForm form) {
        return blank(form.getContractPoNo())
                || blank(form.getFiscalYear())
                || blank(form.getDescription())
                || form.getAgreementForId() == null
                || form.getCategoryId() == null
                || form.getContractTypeId() == null
                || form.getPaymentTypeId() == null
                || form.getStatusId() == null
                || form.getDepartmentId() == null
                || form.getPartyId() == null
                || form.getCurrencyId() == null
                || form.getStartDate() == null
                || form.getEndDate() == null
                || form.getContractValue() == null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ContractForm toForm(com.cms.demo.entity.Contract contract) {
        ContractForm form = new ContractForm();
        form.setContractId(contract.getContractId());
        form.setContractNo(contract.getContractNo());
        form.setContractPoNo(contract.getContractPoNo());
        form.setPoNoRef(contract.getPoNoRef());
        form.setFiscalYear(contract.getFiscalYear());
        form.setAgreementForId(contract.getAgreementFor() == null ? null : contract.getAgreementFor().getId());
        form.setCategoryId(contract.getCategory() == null ? null : contract.getCategory().getId());
        form.setContractTypeId(contract.getContractType() == null ? null : contract.getContractType().getId());
        form.setPaymentTypeId(contract.getPaymentType() == null ? null : contract.getPaymentType().getId());
        form.setStatusId(contract.getStatus() == null ? null : contract.getStatus().getId());
        form.setDepartmentId(contract.getDepartment() == null ? null : contract.getDepartment().getDepartmentId());
        form.setPartyId(contract.getParty() == null ? null : contract.getParty().getPartyId());
        form.setDescription(contract.getDescription());
        form.setSignedDate(contract.getSignedDate());
        form.setRegisterDate(contract.getRegisterDate());
        form.setStartDate(contract.getStartDate());
        form.setEndDate(contract.getEndDate());
        form.setPeriodMonths(contract.getPeriodMonths());
        form.setHeadcountPerMonth(contract.getHeadcountPerMonth());
        form.setContractValue(contract.getContractValue());
        form.setCommercialTaxRate(contract.getCommercialTaxRate());
        form.setCurrencyId(contract.getCurrency() == null ? null : contract.getCurrency().getId());
        form.setProjectId(contract.getProject() == null ? null : contract.getProject().getProjectId());
        form.setProjectInchargeId(contract.getProjectIncharge() == null ? null : contract.getProjectIncharge().getUserId());
        form.setContractManagerId(contract.getContractManager() == null ? null : contract.getContractManager().getUserId());
        form.setClientIncharge(contract.getClientIncharge());
        form.setRenewedFromId(contract.getRenewedFrom() == null ? null : contract.getRenewedFrom().getContractId());
        return form;
    }

    private void addFormOptions(Model model) {
        model.addAttribute("agreementFors", agreementForRepository.findAll(Sort.by("sortOrder", "id")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("categories", categoryRepository.findAll(Sort.by("sortOrder", "id")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("contractTypes", contractTypeRepository.findAll(Sort.by("sortOrder", "id")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("paymentTypes", paymentTypeRepository.findAll(Sort.by("sortOrder", "id")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("statuses", statusRepository.findAll(Sort.by("sortOrder", "id")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("departments", departmentRepository.findAll(Sort.by("deptName")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("parties", partyRepository.findAll(Sort.by("partyName")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("currencies", currencyRepository.findAll(Sort.by("sortOrder", "id")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        model.addAttribute("projects", projectRepository.findAll(Sort.by("projectName")));
        model.addAttribute("users", userRepository.findAll(Sort.by("fullName")).stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
    }
}
