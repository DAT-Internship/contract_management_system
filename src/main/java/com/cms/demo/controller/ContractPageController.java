package com.cms.demo.controller;

import com.cms.demo.dto.ContractForm;
import com.cms.demo.entity.ContractAttachment;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.service.ContractService;
import com.cms.demo.service.FileStorageService;
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
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import java.util.List;
import java.util.Optional;

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
            UserRepository userRepository) {
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
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("page", "contracts");
        model.addAttribute("contracts", contractService.findAll());
        return "contracts/list";
    }

    @GetMapping("/new")
    public String newContract(Model model) {
        ContractForm form = new ContractForm();
        form.setRegisterDate(LocalDate.now());
        form.setFiscalYear("FY" + LocalDate.now().getYear());
        form.setCommercialTaxRate(new java.math.BigDecimal("5.00"));
        model.addAttribute("page", "contracts");
        model.addAttribute("contractForm", form);
        addFormOptions(model);
        return "contracts/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("contractForm") ContractForm form,
            BindingResult bindingResult,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "action", required = false) String action,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Auto-assign status based on action if not explicitly chosen
        if (form.getStatusId() == null) {
            if ("draft".equalsIgnoreCase(action)) {
                resolveStatusByCandidateCodes(List.of("DRAFT", "WAITING_SIGN"))
                        .ifPresent(s -> form.setStatusId(s.getId()));
            } else {
                resolveStatusByCandidateCodes(List.of("WAITING_SIGN", "ACTIVE"))
                        .ifPresent(s -> form.setStatusId(s.getId()));
            }
        }

        if (bindingResult.hasErrors() || hasMissingRequiredFields(form)) {
            if (!bindingResult.hasErrors()) {
                model.addAttribute("error", "Complete all required fields before saving the contract.");
            }
            model.addAttribute("page", "contracts");
            addFormOptions(model);
            return "contracts/form";
        }

        try {
            contractService.create(form.toRequest(), files, principal.getName());
            String successMessage = "draft".equalsIgnoreCase(action)
                    ? "Contract draft saved successfully."
                    : "Contract registered successfully.";
            redirectAttributes.addFlashAttribute("success", successMessage);
            return "redirect:/contracts";
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

    @GetMapping("/{contractId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId) {
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (!attachment.getContract().getContractId().equals(contractId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Attachment mismatch");
        }

        Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{contractId}/attachments/{attachmentId}/view")
    public ResponseEntity<Resource> viewAttachment(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId) {
        ContractAttachment attachment = contractService.getAttachment(attachmentId);
        if (!attachment.getContract().getContractId().equals(contractId)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Attachment mismatch");
        }

        Resource resource = fileStorageService.loadFileAsResource(attachment.getFilePath());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getMimeType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    private Optional<MasterContractStatus> resolveStatusByCandidateCodes(List<String> codes) {
        List<MasterContractStatus> all = statusRepository.findAll();
        for (String code : codes) {
            for (MasterContractStatus s : all) {
                if (code.equalsIgnoreCase(s.getCode()) && Boolean.TRUE.equals(s.getIsActive())) {
                    return Optional.of(s);
                }
            }
        }
        return all.stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).findFirst();
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
