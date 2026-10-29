package com.cms.demo.service;

import com.cms.demo.dto.AttachmentResponse;
import com.cms.demo.dto.ContractRequest;
import com.cms.demo.dto.ContractResponse;
import com.cms.demo.entity.Contract;
import com.cms.demo.entity.ContractAttachment;
import com.cms.demo.entity.CustomerSupplier;
import com.cms.demo.entity.Department;
import com.cms.demo.entity.MasterAgreementFor;
import com.cms.demo.entity.MasterContractCategory;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.MasterContractType;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.MasterPaymentType;
import com.cms.demo.entity.Project;
import com.cms.demo.entity.User;
import com.cms.demo.repository.AgreementForRepository;
import com.cms.demo.repository.ContractAttachmentRepository;
import com.cms.demo.repository.ContractCategoryRepository;
import com.cms.demo.repository.ContractRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.ContractTypeRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.CustomerSupplierRepository;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.PaymentTypeRepository;
import com.cms.demo.repository.ProjectRepository;
import com.cms.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
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
    private final ContractAttachmentRepository contractAttachmentRepository;
    private final FileStorageService fileStorageService;

    public ContractService(
            ContractRepository contractRepository,
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
            ContractAttachmentRepository contractAttachmentRepository,
            FileStorageService fileStorageService) {
        this.contractRepository = contractRepository;
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
        this.contractAttachmentRepository = contractAttachmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public ContractResponse create(ContractRequest request, String employeeCode) {
        return create(request, Collections.emptyList(), employeeCode);
    }

    @Transactional
    public ContractResponse create(ContractRequest request, List<MultipartFile> files, String employeeCode) {
        User actor = getUserByEmployeeCode(employeeCode);
        String contractNo = normalizeContractNo(request);
        if (contractRepository.existsByContractNo(contractNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contract number already exists");
        }

        List<MultipartFile> validFiles = (files == null)
                ? Collections.emptyList()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();

        Contract contract = new Contract();
        applyRequest(contract, request, contractNo);
        contract.setMissingAttachment(validFiles.isEmpty() ? "YES" : "NO");
        contract.setCreatedBy(actor);
        contract.setUpdatedBy(actor);
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());

        Contract savedContract = contractRepository.save(contract);

        if (!validFiles.isEmpty()) {
            for (MultipartFile file : validFiles) {
                FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "contracts/" + savedContract.getContractId());

                ContractAttachment attachment = new ContractAttachment();
                attachment.setContract(savedContract);
                attachment.setFileName(stored.originalFileName());
                attachment.setFilePath("/uploads/" + stored.relativePath());
                attachment.setFileSize(stored.size());
                attachment.setMimeType(stored.mimeType());
                attachment.setUploadedBy(actor);
                attachment.setUploadedAt(LocalDateTime.now());

                contractAttachmentRepository.save(attachment);
            }
        }

        return toResponse(savedContract);
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> findAll() {
        return contractRepository.findAllByOrderByRegisterDateDescContractIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContractResponse findById(Long id) {
        return toResponse(getContract(id));
    }

    @Transactional
    public ContractResponse update(Long id, ContractRequest request, String employeeCode) {
        Contract contract = getContract(id);
        String contractNo = normalizeContractNo(request);
        if (!contract.getContractNo().equals(contractNo) && contractRepository.existsByContractNo(contractNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contract number already exists");
        }

        applyRequest(contract, request, contractNo);

        // Update missing_attachment based on existing attachments in database
        List<ContractAttachment> existingAttachments = contractAttachmentRepository.findByContractContractId(id);
        contract.setMissingAttachment(existingAttachments.isEmpty() ? "YES" : "NO");

        contract.setUpdatedBy(getUserByEmployeeCode(employeeCode));
        contract.setUpdatedAt(LocalDateTime.now());
        return toResponse(contractRepository.save(contract));
    }

    @Transactional(readOnly = true)
    public List<ContractAttachment> getAttachmentsByContractId(Long contractId) {
        // verify contract exists
        getContract(contractId);
        return contractAttachmentRepository.findByContractContractId(contractId);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentResponses(Long contractId) {
        return getAttachmentsByContractId(contractId).stream()
                .map(this::toAttachmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContractAttachment getAttachment(Long attachmentId) {
        return contractAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }

    @Transactional
    public AttachmentResponse addAttachment(Long contractId, MultipartFile file, String employeeCode) {
        Contract contract = getContract(contractId);
        User actor = getUserByEmployeeCode(employeeCode);

        FileStorageService.StoredFile stored = fileStorageService.storeFile(file, "contracts/" + contract.getContractId());

        ContractAttachment attachment = new ContractAttachment();
        attachment.setContract(contract);
        attachment.setFileName(stored.originalFileName());
        attachment.setFilePath("/uploads/" + stored.relativePath());
        attachment.setFileSize(stored.size());
        attachment.setMimeType(stored.mimeType());
        attachment.setUploadedBy(actor);
        attachment.setUploadedAt(LocalDateTime.now());

        ContractAttachment savedAttachment = contractAttachmentRepository.save(attachment);

        contract.setMissingAttachment("NO");
        contract.setUpdatedAt(LocalDateTime.now());
        contract.setUpdatedBy(actor);
        contractRepository.save(contract);

        return toAttachmentResponse(savedAttachment);
    }

    private AttachmentResponse toAttachmentResponse(ContractAttachment att) {
        String uploadedByName = (att.getUploadedBy() != null) ? att.getUploadedBy().getFullName() : "System";
        return new AttachmentResponse(
                att.getAttachmentId(),
                att.getFileName(),
                att.getFilePath(),
                att.getFileSize(),
                att.getMimeType(),
                att.getUploadedAt(),
                uploadedByName);
    }

    private void applyRequest(Contract contract, ContractRequest request, String contractNo) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }

        int periodMonths = request.periodMonths() != null
                ? request.periodMonths()
                : (int) Math.max(1, ChronoUnit.MONTHS.between(request.startDate(), request.endDate().plusDays(1)));
        if (periodMonths < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodMonths must be at least 1");
        }

        BigDecimal headcount = request.headcountPerMonth();
        BigDecimal totalManMonths = headcount == null
                ? BigDecimal.ZERO.setScale(2)
                : headcount.multiply(BigDecimal.valueOf(periodMonths)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = request.commercialTaxRate() == null
                ? new BigDecimal("5.00")
                : request.commercialTaxRate();

        contract.setContractNo(contractNo);
        contract.setContractPoNo(request.contractPoNo().trim());
        contract.setPoNoRef(trimToNull(request.poNoRef()));
        contract.setFiscalYear(request.fiscalYear().trim());
        contract.setAgreementFor(getRequired(agreementForRepository.findById(request.agreementForId()), "Agreement For"));
        contract.setCategory(getRequired(categoryRepository.findById(request.categoryId()), "Contract category"));
        contract.setContractType(getRequired(contractTypeRepository.findById(request.contractTypeId()), "Contract type"));
        contract.setPaymentType(getRequired(paymentTypeRepository.findById(request.paymentTypeId()), "Payment type"));
        contract.setStatus(getRequired(statusRepository.findById(request.statusId()), "Contract status"));
        contract.setDepartment(getRequired(departmentRepository.findById(request.departmentId()), "Department"));
        contract.setParty(getRequired(partyRepository.findById(request.partyId()), "Customer/supplier"));
        contract.setCurrency(getRequired(currencyRepository.findById(request.currencyId()), "Currency"));
        contract.setProject(optionalProject(request.projectId()));
        contract.setProjectIncharge(optionalUser(request.projectInchargeId(), "Project in-charge"));
        contract.setContractManager(optionalUser(request.contractManagerId(), "Contract manager"));
        contract.setRenewedFrom(request.renewedFromId() == null ? null : getContract(request.renewedFromId()));
        contract.setDescription(request.description().trim());
        contract.setSignedDate(request.signedDate());
        contract.setRegisterDate(request.registerDate() == null ? LocalDate.now() : request.registerDate());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setPeriodMonths(periodMonths);
        contract.setHeadcountPerMonth(headcount);
        contract.setTotalManMonths(totalManMonths);
        contract.setContractValue(request.contractValue());
        contract.setCommercialTaxRate(taxRate);
        contract.setCommercialTaxAmount(request.contractValue().multiply(taxRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        contract.setClientIncharge(trimToNull(request.clientIncharge()));
        contract.setPaymentRisk("LOW");
        contract.setRenewalRisk("LOW");
    }

    private ContractResponse toResponse(Contract contract) {
        long remainingDays = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate()));
        return new ContractResponse(
                contract.getContractId(),
                contract.getContractNo(),
                contract.getContractPoNo(),
                contract.getPoNoRef(),
                contract.getFiscalYear(),
                contract.getParty().getPartyName(),
                contract.getDescription(),
                contract.getSignedDate(),
                contract.getRegisterDate(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getPeriodMonths(),
                remainingDays,
                contract.getHeadcountPerMonth(),
                contract.getTotalManMonths(),
                contract.getContractValue(),
                contract.getCommercialTaxRate(),
                contract.getCommercialTaxAmount(),
                contract.getCurrency().getCode(),
                contract.getStatus().getCode(),
                contract.getStatus().getName(),
                contract.getClientIncharge());
    }

    private String normalizeContractNo(ContractRequest request) {
        String contractNo = request.contractNo() == null || request.contractNo().isBlank()
                ? request.contractPoNo().trim()
                : request.contractNo().trim();
        if (contractNo.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "contractNo must be 30 characters or fewer; provide a separate contractNo when the PO number is longer");
        }
        return contractNo;
    }

    public Contract getContract(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found"));
    }

    private User getUserByEmployeeCode(String employeeCode) {
        return userRepository.findByEmployeeCode(employeeCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private Project optionalProject(Long id) {
        return id == null ? null : getRequired(projectRepository.findById(id), "Project");
    }

    private User optionalUser(Long id, String label) {
        return id == null ? null : getRequired(userRepository.findById(id), label);
    }

    private <T> T getRequired(java.util.Optional<T> value, String label) {
        return value.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " not found"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
