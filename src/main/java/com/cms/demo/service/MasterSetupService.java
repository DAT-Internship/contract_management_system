package com.cms.demo.service;

import com.cms.demo.entity.*;
import com.cms.demo.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MasterSetupService {

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
    private final RoleRepository roleRepository;

    public MasterSetupService(
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
            UserRepository userRepository,
            RoleRepository roleRepository) {
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
        this.roleRepository = roleRepository;
    }

    // =========================================================
    // GET ALL (Controller ကို Data တွေ ပြန်ပေးမယ်)
    // =========================================================
    public Map<String, Object> getAllMasterData() {
        Map<String, Object> data = new HashMap<>();
        
        data.put("customerSuppliers", customerSupplierRepository.findAll(Sort.by(Sort.Direction.ASC, "partyName")).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("agreementFors", agreementForRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("contractTypes", contractTypeRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("contractCategories", contractCategoryRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("paymentTypes", paymentTypeRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("contractStatuses", contractStatusRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("paymentStatuses", paymentStatusRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("currencies", currencyRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("departments", departmentRepository.findAll(Sort.by(Sort.Direction.ASC, "deptName")).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        data.put("projects", projectRepository.findAll(Sort.by(Sort.Direction.ASC, "projectName")).stream().filter(item -> item.getStatus() == null || !"INACTIVE".equalsIgnoreCase(item.getStatus())).toList());
        data.put("allUsers", userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName")).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())).toList());
        
        // Role တွေကို ကြည့်ရုံပဲပြဖို့ Data ဆွဲထုတ်မယ်
        data.put("roles", roleRepository.findAll(Sort.by(Sort.Direction.ASC, "roleName")));

        return data;
    }

    // =========================================================
    // ADD (အသစ်ထည့်ခြင်း)
    // =========================================================
    @Transactional
    public void addMasterData(String type, String code, String name, String partyType,
                              String taxId, String address, String contactPerson, String contactEmail, String contactPhone,
                              String projectRank, Integer departmentId, Long projectManagerId,
                              LocalDate startDate, LocalDate endDate, String status) throws Exception {

        switch (type) {
            case "customerSupplier":
                if (!isValidEmail(contactEmail)) throw new Exception("Enter a valid contact email.");
                if (!isValidPhone(contactPhone)) throw new Exception("Contact phone must contain 7–11 characters and at least 7 digits.");
                if (customerSupplierRepository.existsByPartyCodeIgnoreCase(code)) throw new Exception("Party code already exists: " + code);

                CustomerSupplier customerSupplier = new CustomerSupplier();
                customerSupplier.setPartyCode(code);
                customerSupplier.setPartyName(name);
                customerSupplier.setPartyType(partyType == null || partyType.isBlank() ? "SUPPLIER" : partyType.trim().toUpperCase());
                customerSupplier.setTaxId(taxId.trim());
                customerSupplier.setAddress(address.trim());
                customerSupplier.setContactPerson(contactPerson.trim());
                customerSupplier.setContactEmail(contactEmail.trim());
                customerSupplier.setContactPhone(contactPhone.trim());
                customerSupplier.setIsActive(true);
                customerSupplier.setCreatedAt(LocalDateTime.now());
                customerSupplierRepository.save(customerSupplier);
                break;

            case "agreementFor":
                if (agreementForRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterAgreementFor agreementFor = new MasterAgreementFor();
                agreementFor.setCode(code);
                agreementFor.setName(name);
                agreementFor.setIsActive(true);
                agreementFor.setSortOrder(nextSortOrder(agreementForRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterAgreementFor::getSortOrder).toList()));
                agreementForRepository.save(agreementFor);
                break;

            case "contractType":
                if (contractTypeRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterContractType contractType = new MasterContractType();
                contractType.setCode(code);
                contractType.setName(name);
                contractType.setIsActive(true);
                contractType.setSortOrder(nextSortOrder(contractTypeRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterContractType::getSortOrder).toList()));
                contractTypeRepository.save(contractType);
                break;

            case "contractCategory":
                if (contractCategoryRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterContractCategory category = new MasterContractCategory();
                category.setCode(code);
                category.setName(name);
                category.setIsActive(true);
                category.setSortOrder(nextSortOrder(contractCategoryRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterContractCategory::getSortOrder).toList()));
                contractCategoryRepository.save(category);
                break;

            case "paymentType":
                if (paymentTypeRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterPaymentType paymentType = new MasterPaymentType();
                paymentType.setCode(code);
                paymentType.setName(name);
                paymentType.setIsActive(true);
                paymentType.setSortOrder(nextSortOrder(paymentTypeRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterPaymentType::getSortOrder).toList()));
                paymentTypeRepository.save(paymentType);
                break;

            case "contractStatus":
                if (contractStatusRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterContractStatus contractStatus = new MasterContractStatus();
                contractStatus.setCode(code);
                contractStatus.setName(name);
                contractStatus.setIsActive(true);
                contractStatus.setSortOrder(nextSortOrder(contractStatusRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterContractStatus::getSortOrder).toList()));
                contractStatusRepository.save(contractStatus);
                break;

            case "paymentStatus":
                if (paymentStatusRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterPaymentStatus paymentStatus = new MasterPaymentStatus();
                paymentStatus.setCode(code);
                paymentStatus.setName(name);
                paymentStatus.setIsActive(true);
                paymentStatus.setSortOrder(nextSortOrder(paymentStatusRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterPaymentStatus::getSortOrder).toList()));
                paymentStatusRepository.save(paymentStatus);
                break;

            case "currency":
                if (currencyRepository.existsByCodeIgnoreCase(code)) throw new Exception("Code already exists: " + code);
                MasterCurrency currency = new MasterCurrency();
                currency.setCode(code);
                currency.setName(name);
                currency.setIsActive(true);
                currency.setSortOrder(nextSortOrder(currencyRepository.findAll().stream().filter(i -> Boolean.TRUE.equals(i.getIsActive())).map(MasterCurrency::getSortOrder).toList()));
                currencyRepository.save(currency);
                break;

            case "department":
                if (departmentRepository.existsByDeptCodeIgnoreCase(code)) throw new Exception("Department code already exists: " + code);
                Department department = new Department();
                department.setDeptCode(code);
                department.setDeptName(name);
                department.setIsActive(true);
                department.setCreatedAt(LocalDateTime.now());
                Department parentDept = departmentRepository.findByDeptCodeIgnoreCase("ROOT").orElse(null);
                if (parentDept == null) throw new Exception("A root department is missing.");
                department.setParentDepartment(parentDept);
                departmentRepository.save(department);
                break;

            case "project":
                if (projectRepository.existsByProjectCodeIgnoreCase(code)) throw new Exception("Project code already exists: " + code);
                Project project = new Project();
                project.setProjectCode(code);
                project.setProjectName(name);
                project.setProjectRank(projectRank);
                project.setDepartment(departmentId != null ? departmentRepository.findById(departmentId).orElse(null) : null);
                project.setProjectManager(projectManagerId != null ? userRepository.findById(projectManagerId).orElse(null) : null);
                project.setStartDate(startDate);
                project.setEndDate(endDate);
                project.setStatus(status != null && !status.isBlank() ? status.trim().toUpperCase() : "ACTIVE");
                project.setCreatedAt(LocalDateTime.now());
                projectRepository.save(project);
                break;

            case "role":
                // 🔴 Role အသစ် ထည့်ခွင့်မပြုတော့ပါဘူး
                throw new Exception("Adding new roles is not permitted.");

            default:
                throw new Exception("Unknown master type: " + type);
        }
    }

    // =========================================================
    // UPDATE (ပြင်ဆင်ခြင်း - အားလုံးအပြည့်အစုံ)
    // =========================================================
    @Transactional
    public void updateMasterData(String type, String id, String code, String name, String partyType,
                                 String taxId, String address, String contactPerson, String contactEmail, String contactPhone,
                                 String projectRank, Integer departmentId, Long projectManagerId,
                                 LocalDate startDate, LocalDate endDate, String status) throws Exception {

        switch (type) {
            case "customerSupplier":
                CustomerSupplier customerSupplier = customerSupplierRepository.findById(Long.parseLong(id)).orElseThrow();
                if (!isValidEmail(contactEmail)) throw new Exception("Enter a valid contact email.");
                if (!isValidPhone(contactPhone)) throw new Exception("Contact phone must contain 7–11 characters and at least 7 digits.");
                
                customerSupplier.setPartyCode(code);
                customerSupplier.setPartyName(name);
                if (partyType != null && !partyType.isBlank()) customerSupplier.setPartyType(partyType.trim().toUpperCase());
                customerSupplier.setTaxId(taxId.trim());
                customerSupplier.setAddress(address.trim());
                customerSupplier.setContactPerson(contactPerson.trim());
                customerSupplier.setContactEmail(contactEmail.trim());
                customerSupplier.setContactPhone(contactPhone.trim());
                customerSupplierRepository.save(customerSupplier);
                break;

            case "agreementFor":
                MasterAgreementFor agreementFor = agreementForRepository.findById(Integer.parseInt(id)).orElseThrow();
                agreementFor.setCode(code);
                agreementFor.setName(name);
                agreementForRepository.save(agreementFor);
                break;

            case "contractType":
                MasterContractType contractType = contractTypeRepository.findById(Integer.parseInt(id)).orElseThrow();
                contractType.setCode(code);
                contractType.setName(name);
                contractTypeRepository.save(contractType);
                break;

            case "contractCategory":
                MasterContractCategory category = contractCategoryRepository.findById(Integer.parseInt(id)).orElseThrow();
                category.setCode(code);
                category.setName(name);
                contractCategoryRepository.save(category);
                break;

            case "paymentType":
                MasterPaymentType paymentType = paymentTypeRepository.findById(Integer.parseInt(id)).orElseThrow();
                paymentType.setCode(code);
                paymentType.setName(name);
                paymentTypeRepository.save(paymentType);
                break;

            case "contractStatus":
                MasterContractStatus contractStatus = contractStatusRepository.findById(Integer.parseInt(id)).orElseThrow();
                contractStatus.setCode(code);
                contractStatus.setName(name);
                contractStatusRepository.save(contractStatus);
                break;

            case "paymentStatus":
                MasterPaymentStatus paymentStatus = paymentStatusRepository.findById(Integer.parseInt(id)).orElseThrow();
                paymentStatus.setCode(code);
                paymentStatus.setName(name);
                paymentStatusRepository.save(paymentStatus);
                break;

            case "currency":
                MasterCurrency currency = currencyRepository.findById(Integer.parseInt(id)).orElseThrow();
                currency.setCode(code);
                currency.setName(name);
                currencyRepository.save(currency);
                break;

            case "department":
                Department department = departmentRepository.findById(Integer.parseInt(id)).orElseThrow();
                department.setDeptCode(code);
                department.setDeptName(name);
                departmentRepository.save(department);
                break;

            case "project":
                Project project = projectRepository.findById(Long.parseLong(id)).orElseThrow();
                if (!project.getProjectCode().equalsIgnoreCase(code) && projectRepository.existsByProjectCodeIgnoreCase(code)) {
                    throw new Exception("Project code already exists: " + code);
                }
                project.setProjectCode(code);
                project.setProjectName(name);
                project.setProjectRank(projectRank);
                project.setDepartment(departmentId != null ? departmentRepository.findById(departmentId).orElse(null) : null);
                project.setProjectManager(projectManagerId != null ? userRepository.findById(projectManagerId).orElse(null) : null);
                project.setStartDate(startDate);
                project.setEndDate(endDate);
                if (status != null && !status.isBlank()) project.setStatus(status.trim().toUpperCase());
                projectRepository.save(project);
                break;

            case "role":
                // 🔴 Role ကို ပြင်ခွင့်မပြုတော့ပါဘူး
                throw new Exception("Updating roles is not permitted.");

            default:
                throw new Exception("Unknown master type: " + type);
        }
    }

    // =========================================================
    // DELETE (ဖျက်ခြင်း - အားလုံးအပြည့်အစုံ)
    // =========================================================
    @Transactional
    public void deleteMasterData(String type, String id) throws Exception {
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
                Project project = projectRepository.findById(Long.parseLong(id)).orElseThrow();
                project.setStatus("INACTIVE");
                projectRepository.save(project);
                break;

            case "role":
                // 🔴 Role ကို ဖျက်ခွင့်မပြုတော့ပါဘူး
                throw new Exception("Deleting roles is not permitted.");

            default:
                throw new Exception("Unknown master type: " + type);
        }
    }

    // =========================================================
    // Helper Methods
    // =========================================================
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.length() >= 7 && phone.length() <= 11 && phone.matches("[0-9+() -]+") && phone.chars().filter(Character::isDigit).count() >= 7;
    }

    private int nextSortOrder(List<Integer> existingOrders) {
        return existingOrders.stream().filter(order -> order != null).mapToInt(Integer::intValue).max().orElse(0) + 1;
    }
}