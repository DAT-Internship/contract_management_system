package com.cms.demo.controller;

import com.cms.demo.entity.CustomerSupplier;
import com.cms.demo.entity.Department;
import com.cms.demo.entity.MasterAgreementFor;
import com.cms.demo.entity.MasterContractCategory;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.entity.MasterContractType;
import com.cms.demo.entity.MasterCurrency;
import com.cms.demo.entity.MasterPaymentStatus;
import com.cms.demo.entity.MasterPaymentType;
import com.cms.demo.entity.Project;
import com.cms.demo.entity.User;
import com.cms.demo.repository.ProjectRepository;
import com.cms.demo.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

import com.cms.demo.repository.AgreementForRepository;
import com.cms.demo.repository.ContractCategoryRepository;
import com.cms.demo.repository.ContractStatusRepository;
import com.cms.demo.repository.ContractTypeRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.CustomerSupplierRepository;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.PaymentStatusRepository;
import com.cms.demo.repository.PaymentTypeRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/master-setup")
public class MasterSetupController {

    private final CustomerSupplierRepository customerSupplierRepository;
    private final AgreementForRepository agreementForRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final ContractCategoryRepository contractCategoryRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final ContractStatusRepository contractStatusRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final CurrencyRepository currencyRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public MasterSetupController(
            CustomerSupplierRepository customerSupplierRepository,
            AgreementForRepository agreementForRepository,
            ContractTypeRepository contractTypeRepository,
            ContractCategoryRepository contractCategoryRepository,
            PaymentTypeRepository paymentTypeRepository,
            ContractStatusRepository contractStatusRepository,
            PaymentStatusRepository paymentStatusRepository,
            CurrencyRepository currencyRepository,
            DepartmentRepository departmentRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository) {

        this.customerSupplierRepository = customerSupplierRepository;
        this.agreementForRepository = agreementForRepository;
        this.contractTypeRepository = contractTypeRepository;
        this.contractCategoryRepository = contractCategoryRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.contractStatusRepository = contractStatusRepository;
        this.paymentStatusRepository = paymentStatusRepository;
        this.currencyRepository = currencyRepository;
        this.departmentRepository = departmentRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }


    // =========================================================
    // SHOW MASTER SETUP PAGE
    // =========================================================

    @GetMapping
    public String page(Model model) {

        model.addAttribute("page", "master-setup");

        model.addAttribute(
                "customerSuppliers",
                customerSupplierRepository.findAll(Sort.by(Sort.Direction.ASC, "partyName")).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "agreementFors",
                agreementForRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "contractTypes",
                contractTypeRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "contractCategories",
                contractCategoryRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "paymentTypes",
                paymentTypeRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "contractStatuses",
                contractStatusRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "paymentStatuses",
                paymentStatusRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "currencies",
                currencyRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );

        model.addAttribute(
                "departments",
                departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "deptName")).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );
        model.addAttribute(
                "projects",
                projectRepository.findAll(Sort.by(Sort.Direction.ASC, "projectName")).stream()
                        .filter(item -> item.getStatus() == null || !"INACTIVE".equalsIgnoreCase(item.getStatus())).toList()
        );

        model.addAttribute(
                "allUsers",
                userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName")).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList()
        );


        return "master-setup";
    }


    // =========================================================
    // ADD
    // =========================================================

    @PostMapping("/add")
    public String add(
            @RequestParam("type") String type,
            @RequestParam("code") String code,
            @RequestParam("name") String name,

            @RequestParam(
                    value = "partyType",
                    required = false
            )
            String partyType,

            @RequestParam(value = "taxId", required = false) String taxId,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "contactPerson", required = false) String contactPerson,
            @RequestParam(value = "contactEmail", required = false) String contactEmail,
            @RequestParam(value = "contactPhone", required = false) String contactPhone,
            @RequestParam(value = "projectRank", required = false) String projectRank,
            @RequestParam(value = "departmentId", required = false) Integer departmentId,
            @RequestParam(value = "projectManagerId", required = false) Long projectManagerId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false) String status,

            RedirectAttributes redirectAttributes) {

        code = code == null ? "" : code.trim();
        name = name == null ? "" : name.trim();

        if (code.isEmpty() || name.isEmpty()) {

            return showError(
                    "Code and Name are required.",
                    redirectAttributes
            );
        }

        try {

            switch (type) {

                // =================================================
                // CUSTOMER / SUPPLIER
                // =================================================

                case "customerSupplier":

                    if (!hasSupplierContactDetails(taxId, address, contactPerson, contactEmail, contactPhone)) {
                        return showError("Tax ID, address, contact person, contact email and contact phone are required.", redirectAttributes);
                    }
                    if (!isValidEmail(contactEmail)) {
                        return showError("Enter a valid contact email.", redirectAttributes);
                    }
                    if (!isValidPhone(contactPhone)) {
                        return showError("Contact phone must contain 7–11 characters and at least 7 digits.", redirectAttributes);
                    }

                    if (customerSupplierRepository
                            .existsByPartyCodeIgnoreCase(code)) {

                        return showError(
                                "Party code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    CustomerSupplier customerSupplier =
                            new CustomerSupplier();

                    customerSupplier.setPartyCode(code);
                    customerSupplier.setPartyName(name);

                    customerSupplier.setPartyType(
                            normalizePartyType(partyType)
                    );
                    setSupplierContactDetails(customerSupplier, taxId, address, contactPerson, contactEmail, contactPhone);

                    customerSupplier.setIsActive(true);

                    customerSupplier.setCreatedAt(
                            LocalDateTime.now()
                    );

                    customerSupplierRepository.save(
                            customerSupplier
                    );

                    break;


                // =================================================
                // AGREEMENT FOR
                // =================================================

                case "agreementFor":

                    if (agreementForRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterAgreementFor agreementFor =
                            new MasterAgreementFor();

                    agreementFor.setCode(code);
                    agreementFor.setName(name);
                    agreementFor.setIsActive(true);
                    agreementFor.setSortOrder(nextSortOrder(agreementForRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterAgreementFor::getSortOrder).toList()));

                    agreementForRepository.save(
                            agreementFor
                    );

                    break;


                // =================================================
                // CONTRACT TYPE
                // =================================================

                case "contractType":

                    if (contractTypeRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterContractType contractType =
                            new MasterContractType();

                    contractType.setCode(code);
                    contractType.setName(name);
                    contractType.setIsActive(true);
                    contractType.setSortOrder(nextSortOrder(contractTypeRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterContractType::getSortOrder).toList()));

                    contractTypeRepository.save(
                            contractType
                    );

                    break;


                // =================================================
                // CONTRACT CATEGORY
                // =================================================

                case "contractCategory":

                    if (contractCategoryRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterContractCategory category =
                            new MasterContractCategory();

                    category.setCode(code);
                    category.setName(name);
                    category.setIsActive(true);
                    category.setSortOrder(nextSortOrder(contractCategoryRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterContractCategory::getSortOrder).toList()));

                    contractCategoryRepository.save(category);

                    break;


                // =================================================
                // PAYMENT TYPE
                // =================================================

                case "paymentType":

                    if (paymentTypeRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterPaymentType paymentType =
                            new MasterPaymentType();

                    paymentType.setCode(code);
                    paymentType.setName(name);
                    paymentType.setIsActive(true);
                    paymentType.setSortOrder(nextSortOrder(paymentTypeRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterPaymentType::getSortOrder).toList()));

                    paymentTypeRepository.save(paymentType);

                    break;


                // =================================================
                // CONTRACT STATUS
                // =================================================

                case "contractStatus":

                    if (contractStatusRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterContractStatus contractStatus =
                            new MasterContractStatus();

                    contractStatus.setCode(code);
                    contractStatus.setName(name);
                    contractStatus.setIsActive(true);
                    contractStatus.setSortOrder(nextSortOrder(contractStatusRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterContractStatus::getSortOrder).toList()));

                    contractStatusRepository.save(contractStatus);

                    break;


                // =================================================
                // PAYMENT STATUS
                // =================================================

                case "paymentStatus":

                    if (paymentStatusRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterPaymentStatus paymentStatus =
                            new MasterPaymentStatus();

                    paymentStatus.setCode(code);
                    paymentStatus.setName(name);
                    paymentStatus.setIsActive(true);
                    paymentStatus.setSortOrder(nextSortOrder(paymentStatusRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterPaymentStatus::getSortOrder).toList()));

                    paymentStatusRepository.save(paymentStatus);

                    break;


                // =================================================
                // CURRENCY
                // =================================================

                case "currency":

                    if (currencyRepository
                            .existsByCodeIgnoreCase(code)) {

                        return showError(
                                "Code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    MasterCurrency currency =
                            new MasterCurrency();

                    currency.setCode(code);
                    currency.setName(name);
                    currency.setIsActive(true);
                    currency.setSortOrder(nextSortOrder(currencyRepository.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).map(MasterCurrency::getSortOrder).toList()));

                    currencyRepository.save(currency);

                    break;


                // =================================================
                // DEPARTMENT
                // =================================================

                case "department":

                    if (departmentRepository
                            .existsByDeptCodeIgnoreCase(code)) {

                        return showError(
                                "Department code already exists: " + code,
                                redirectAttributes
                        );
                    }

                    Department department =
                            new Department();

                    department.setDeptCode(code);
                    department.setDeptName(name);
                    department.setIsActive(true);
                    department.setCreatedAt(LocalDateTime.now());

                    Department parentDepartment = departmentRepository.findByDeptCodeIgnoreCase("ROOT")
                            .orElseGet(() -> departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "deptName"))
                                    .stream().findFirst().orElse(null));
                    if (parentDepartment == null) {
                        return showError("A root department is missing. Apply the latest database migration.", redirectAttributes);
                    }
                    department.setParentDepartment(parentDepartment);
                    departmentRepository.save(department);

                    break;

                // =================================================
                // PROJECT
                // =================================================

                case "project":

                    if (projectRepository.existsByProjectCodeIgnoreCase(code)) {
                        return showError("Project code already exists: " + code, redirectAttributes);
                    }

                    String rank = projectRank == null ? "" : projectRank.trim().toUpperCase();
                    if (!rank.equals("A") && !rank.equals("B") && !rank.equals("C")) {
                        return showError("Project rank must be A, B, or C.", redirectAttributes);
                    }

                    Project project = new Project();
                    project.setProjectCode(code);
                    project.setProjectName(name);
                    project.setProjectRank(rank);
                    if (departmentId != null) {
                        project.setDepartment(departmentRepository.findById(departmentId).orElse(null));
                    }
                    if (projectManagerId != null) {
                        project.setProjectManager(userRepository.findById(projectManagerId).orElse(null));
                    }
                    project.setStartDate(startDate);
                    project.setEndDate(endDate);
                    project.setStatus(status != null && !status.isBlank() ? status.trim().toUpperCase() : "ACTIVE");
                    project.setCreatedAt(LocalDateTime.now());

                    projectRepository.save(project);
                    break;



                default:

                    return showError(
                            "Unknown master type: " + type,
                            redirectAttributes
                    );
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    name + " added successfully."
            );

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "This value already exists or is being used."
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Could not add the item."
            );
        }

        return "redirect:/master-setup";
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @PostMapping("/update")
    public String update(
            @RequestParam("type") String type,
            @RequestParam("id") String id,
            @RequestParam("code") String code,
            @RequestParam("name") String name,

            @RequestParam(
                    value = "partyType",
                    required = false
            )
            String partyType,

            @RequestParam(value = "taxId", required = false) String taxId,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "contactPerson", required = false) String contactPerson,
            @RequestParam(value = "contactEmail", required = false) String contactEmail,
            @RequestParam(value = "contactPhone", required = false) String contactPhone,
            @RequestParam(value = "projectRank", required = false) String projectRank,
            @RequestParam(value = "departmentId", required = false) Integer departmentId,
            @RequestParam(value = "projectManagerId", required = false) Long projectManagerId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "status", required = false) String status,

            RedirectAttributes redirectAttributes) {

        code = code == null ? "" : code.trim();
        name = name == null ? "" : name.trim();

        if (code.isEmpty() || name.isEmpty()) {

            return showError(
                    "Code and Name are required.",
                    redirectAttributes
            );
        }

        try {

            switch (type) {

                // =================================================
                // CUSTOMER / SUPPLIER
                // =================================================

                case "customerSupplier":

                    if (!hasSupplierContactDetails(taxId, address, contactPerson, contactEmail, contactPhone)) {
                        return showError("Tax ID, address, contact person, contact email and contact phone are required.", redirectAttributes);
                    }
                    if (!isValidEmail(contactEmail)) {
                        return showError("Enter a valid contact email.", redirectAttributes);
                    }
                    if (!isValidPhone(contactPhone)) {
                        return showError("Contact phone must contain 7–11 characters and at least 7 digits.", redirectAttributes);
                    }

                    CustomerSupplier customerSupplier =
                            customerSupplierRepository
                                    .findById(Long.parseLong(id))
                                    .orElseThrow();

                    customerSupplier.setPartyCode(code);
                    customerSupplier.setPartyName(name);
                    setSupplierContactDetails(customerSupplier, taxId, address, contactPerson, contactEmail, contactPhone);

                    if (partyType != null &&
                            !partyType.isBlank()) {

                        customerSupplier.setPartyType(
                                normalizePartyType(partyType)
                        );
                    }

                    customerSupplierRepository.save(
                            customerSupplier
                    );

                    break;


                // =================================================
                // AGREEMENT FOR
                // =================================================

                case "agreementFor":

                    MasterAgreementFor agreementFor =
                            agreementForRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    agreementFor.setCode(code);
                    agreementFor.setName(name);

                    agreementForRepository.save(agreementFor);

                    break;


                // =================================================
                // CONTRACT TYPE
                // =================================================

                case "contractType":

                    MasterContractType contractType =
                            contractTypeRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    contractType.setCode(code);
                    contractType.setName(name);

                    contractTypeRepository.save(contractType);

                    break;


                // =================================================
                // CONTRACT CATEGORY
                // =================================================

                case "contractCategory":

                    MasterContractCategory category =
                            contractCategoryRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    category.setCode(code);
                    category.setName(name);

                    contractCategoryRepository.save(category);

                    break;


                // =================================================
                // PAYMENT TYPE
                // =================================================

                case "paymentType":

                    MasterPaymentType paymentType =
                            paymentTypeRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    paymentType.setCode(code);
                    paymentType.setName(name);

                    paymentTypeRepository.save(paymentType);

                    break;


                // =================================================
                // CONTRACT STATUS
                // =================================================

                case "contractStatus":

                    MasterContractStatus contractStatus =
                            contractStatusRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    contractStatus.setCode(code);
                    contractStatus.setName(name);

                    contractStatusRepository.save(contractStatus);

                    break;


                // =================================================
                // PAYMENT STATUS
                // =================================================

                case "paymentStatus":

                    MasterPaymentStatus paymentStatus =
                            paymentStatusRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    paymentStatus.setCode(code);
                    paymentStatus.setName(name);

                    paymentStatusRepository.save(paymentStatus);

                    break;


                // =================================================
                // CURRENCY
                // =================================================

                case "currency":

                    MasterCurrency currency =
                            currencyRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    currency.setCode(code);
                    currency.setName(name);

                    currencyRepository.save(currency);

                    break;


                // =================================================
                // DEPARTMENT
                // =================================================

                case "department":

                    Department department =
                            departmentRepository
                                    .findById(Integer.parseInt(id))
                                    .orElseThrow();

                    department.setDeptCode(code);
                    department.setDeptName(name);

                    departmentRepository.save(department);

                    break;

                // =================================================
                // PROJECT
                // =================================================

                case "project":

                    Project projectToUpdate = projectRepository.findById(Long.parseLong(id)).orElseThrow();
                    String updatedRank = projectRank == null ? "" : projectRank.trim().toUpperCase();
                    if (!updatedRank.equals("A") && !updatedRank.equals("B") && !updatedRank.equals("C")) {
                        return showError("Project rank must be A, B, or C.", redirectAttributes);
                    }

                    if (!projectToUpdate.getProjectCode().equalsIgnoreCase(code)
                            && projectRepository.existsByProjectCodeIgnoreCase(code)) {
                        return showError("Project code already exists: " + code, redirectAttributes);
                    }

                    projectToUpdate.setProjectCode(code);
                    projectToUpdate.setProjectName(name);
                    projectToUpdate.setProjectRank(updatedRank);
                    if (departmentId != null) {
                        projectToUpdate.setDepartment(departmentRepository.findById(departmentId).orElse(null));
                    } else {
                        projectToUpdate.setDepartment(null);
                    }
                    if (projectManagerId != null) {
                        projectToUpdate.setProjectManager(userRepository.findById(projectManagerId).orElse(null));
                    } else {
                        projectToUpdate.setProjectManager(null);
                    }
                    projectToUpdate.setStartDate(startDate);
                    projectToUpdate.setEndDate(endDate);
                    if (status != null && !status.isBlank()) {
                        projectToUpdate.setStatus(status.trim().toUpperCase());
                    }

                    projectRepository.save(projectToUpdate);
                    break;



                default:

                    return showError(
                            "Unknown master type: " + type,
                            redirectAttributes
                    );
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    name + " updated successfully."
            );

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "This item is already used or the code already exists."
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Could not update the item."
            );
        }

        return "redirect:/master-setup";
    }


    // =========================================================
    // DELETE
    // =========================================================

    @PostMapping("/delete")
    public String delete(
            @RequestParam("type") String type,
            @RequestParam("id") String id,
            RedirectAttributes redirectAttributes) {

        try {

            switch (type) {

                case "customerSupplier":
                    CustomerSupplier party = customerSupplierRepository.findById(Long.parseLong(id)).orElseThrow();
                    party.setIsActive(false);
                    customerSupplierRepository.save(party);

                    break;


                case "agreementFor":
                    MasterAgreementFor agreementFor = agreementForRepository.findById(Integer.parseInt(id)).orElseThrow();
                    agreementFor.setIsActive(false);
                    agreementForRepository.save(agreementFor);

                    break;


                case "contractType":
                    MasterContractType contractType = contractTypeRepository.findById(Integer.parseInt(id)).orElseThrow();
                    contractType.setIsActive(false);
                    contractTypeRepository.save(contractType);

                    break;


                case "contractCategory":
                    MasterContractCategory category = contractCategoryRepository.findById(Integer.parseInt(id)).orElseThrow();
                    category.setIsActive(false);
                    contractCategoryRepository.save(category);

                    break;


                case "paymentType":
                    MasterPaymentType paymentType = paymentTypeRepository.findById(Integer.parseInt(id)).orElseThrow();
                    paymentType.setIsActive(false);
                    paymentTypeRepository.save(paymentType);

                    break;


                case "contractStatus":
                    MasterContractStatus contractStatus = contractStatusRepository.findById(Integer.parseInt(id)).orElseThrow();
                    contractStatus.setIsActive(false);
                    contractStatusRepository.save(contractStatus);

                    break;


                case "paymentStatus":
                    MasterPaymentStatus paymentStatus = paymentStatusRepository.findById(Integer.parseInt(id)).orElseThrow();
                    paymentStatus.setIsActive(false);
                    paymentStatusRepository.save(paymentStatus);

                    break;


                case "currency":
                    MasterCurrency currency = currencyRepository.findById(Integer.parseInt(id)).orElseThrow();
                    currency.setIsActive(false);
                    currencyRepository.save(currency);

                    break;


                case "department":
                    Department department = departmentRepository.findById(Integer.parseInt(id)).orElseThrow();
                    department.setIsActive(false);
                    departmentRepository.save(department);

                    break;

                case "project":
                    Project projectToDelete = projectRepository.findById(Long.parseLong(id)).orElseThrow();
                    projectToDelete.setStatus("INACTIVE");
                    projectRepository.save(projectToDelete);
                    break;



                default:

                    return showError(
                            "Unknown master type: " + type,
                            redirectAttributes
                    );
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Item removed from the list."
            );

        } catch (DataIntegrityViolationException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "This item cannot be deleted because it is already used."
            );

        } catch (Exception ex) {

            ex.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Could not delete the item."
            );
        }

        return "redirect:/master-setup";
    }


    // =========================================================
    // NORMALIZE PARTY TYPE
    // =========================================================

    private boolean hasSupplierContactDetails(String taxId, String address, String contactPerson,
            String contactEmail, String contactPhone) {
        return isPresent(taxId) && isPresent(address) && isPresent(contactPerson)
                && isPresent(contactEmail) && isPresent(contactPhone);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean isValidPhone(String phone) {
        return phone != null
                && phone.length() >= 7
                && phone.length() <= 11
                && phone.matches("[0-9+() -]+")
                && phone.chars().filter(Character::isDigit).count() >= 7;
    }

    private void setSupplierContactDetails(CustomerSupplier party, String taxId, String address,
            String contactPerson, String contactEmail, String contactPhone) {
        party.setTaxId(taxId.trim());
        party.setAddress(address.trim());
        party.setContactPerson(contactPerson.trim());
        party.setContactEmail(contactEmail.trim());
        party.setContactPhone(contactPhone.trim());
    }

    private int nextSortOrder(List<Integer> existingOrders) {
        return existingOrders.stream()
                .filter(order -> order != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private String normalizePartyType(String value) {

        if (value == null || value.isBlank()) {
            return "SUPPLIER";
        }

        return value.trim().toUpperCase();
    }


    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private String showError(
            String message,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "error",
                message
        );

        return "redirect:/master-setup";
    }
}
