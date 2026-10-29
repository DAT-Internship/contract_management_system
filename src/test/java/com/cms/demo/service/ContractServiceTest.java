package com.cms.demo.service;

import com.cms.demo.dto.ContractRequest;
import com.cms.demo.dto.ContractResponse;
import com.cms.demo.entity.*;
import com.cms.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ContractServiceTest {

    private ContractRepository contractRepository;
    private AgreementForRepository agreementForRepository;
    private ContractCategoryRepository categoryRepository;
    private ContractTypeRepository contractTypeRepository;
    private PaymentTypeRepository paymentTypeRepository;
    private ContractStatusRepository statusRepository;
    private DepartmentRepository departmentRepository;
    private CustomerSupplierRepository partyRepository;
    private CurrencyRepository currencyRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private ContractAttachmentRepository contractAttachmentRepository;
    private FileStorageService fileStorageService;

    private ContractService contractService;

    private User testUser;
    private MasterAgreementFor agreementFor;
    private MasterContractCategory category;
    private MasterContractType contractType;
    private MasterPaymentType paymentType;
    private MasterContractStatus status;
    private Department department;
    private CustomerSupplier party;
    private MasterCurrency currency;

    private final AtomicReference<Contract> savedContractHolder = new AtomicReference<>();
    private final List<ContractAttachment> savedAttachments = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Class<T> iface, java.util.function.BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> handler.apply(method.getName(), args)
        );
    }

    @BeforeEach
    void setUp() {
        savedContractHolder.set(null);
        savedAttachments.clear();

        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmployeeCode("25-00001");
        testUser.setFullName("Test User");

        agreementFor = new MasterAgreementFor();
        agreementFor.setId(1);

        category = new MasterContractCategory();
        category.setId(1);

        contractType = new MasterContractType();
        contractType.setId(1);

        paymentType = new MasterPaymentType();
        paymentType.setId(1);

        status = new MasterContractStatus();
        status.setId(1);
        status.setCode("ACTIVE");
        status.setName("Active");

        department = new Department();
        department.setDepartmentId(1);

        party = new CustomerSupplier();
        party.setPartyId(1L);
        party.setPartyName("Acme Corp");

        currency = new MasterCurrency();
        currency.setId(1);
        currency.setCode("USD");

        contractRepository = createProxy(ContractRepository.class, (name, args) -> {
            if ("existsByContractNo".equals(name)) return false;
            if ("save".equals(name)) {
                Contract c = (Contract) args[0];
                if (c.getContractId() == null) c.setContractId(100L);
                savedContractHolder.set(c);
                return c;
            }
            if ("findById".equals(name)) return Optional.ofNullable(savedContractHolder.get());
            return null;
        });

        agreementForRepository = createProxy(AgreementForRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(agreementFor);
            return null;
        });

        categoryRepository = createProxy(ContractCategoryRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(category);
            return null;
        });

        contractTypeRepository = createProxy(ContractTypeRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(contractType);
            return null;
        });

        paymentTypeRepository = createProxy(PaymentTypeRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(paymentType);
            return null;
        });

        statusRepository = createProxy(ContractStatusRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(status);
            return null;
        });

        departmentRepository = createProxy(DepartmentRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(department);
            return null;
        });

        partyRepository = createProxy(CustomerSupplierRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(party);
            return null;
        });

        currencyRepository = createProxy(CurrencyRepository.class, (name, args) -> {
            if ("findById".equals(name)) return Optional.of(currency);
            return null;
        });

        projectRepository = createProxy(ProjectRepository.class, (name, args) -> Optional.empty());

        userRepository = createProxy(UserRepository.class, (name, args) -> {
            if ("findByEmployeeCode".equals(name)) return Optional.of(testUser);
            return null;
        });

        contractAttachmentRepository = createProxy(ContractAttachmentRepository.class, (name, args) -> {
            if ("save".equals(name)) {
                ContractAttachment a = (ContractAttachment) args[0];
                a.setAttachmentId((long) (savedAttachments.size() + 1));
                savedAttachments.add(a);
                return a;
            }
            if ("findByContractContractId".equals(name)) return savedAttachments;
            return null;
        });

        fileStorageService = new FileStorageService("target/test-uploads") {
            @Override
            public StoredFile storeFile(MultipartFile file, String subFolder) {
                return new StoredFile(
                        file.getOriginalFilename(),
                        "uuid_" + file.getOriginalFilename(),
                        subFolder + "/uuid_" + file.getOriginalFilename(),
                        Path.of(subFolder),
                        file.getSize(),
                        file.getContentType()
                );
            }
        };

        contractService = new ContractService(
                contractRepository,
                agreementForRepository,
                categoryRepository,
                contractTypeRepository,
                paymentTypeRepository,
                statusRepository,
                departmentRepository,
                partyRepository,
                currencyRepository,
                projectRepository,
                userRepository,
                contractAttachmentRepository,
                fileStorageService
        );
    }

    private ContractRequest createSampleRequest() {
        return new ContractRequest(
                "PO-2026-001",
                "PO-2026-001",
                null,
                1, 1, 1, 1, 1, 1,
                "FY2026",
                1L,
                "Master Service Agreement",
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                6,
                new BigDecimal("2.00"),
                new BigDecimal("50000.00"),
                new BigDecimal("5.00"),
                1,
                null, null, null,
                "Jane Doe",
                null
        );
    }

    @Test
    void testCreateContractWithAttachments() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "contract.pdf", "application/pdf", "dummy content".getBytes());

        ContractResponse response = contractService.create(createSampleRequest(), List.of(file), "25-00001");

        assertNotNull(response);
        assertEquals(100L, response.contractId());
        assertEquals("PO-2026-001", response.contractPoNo());

        // Verify attachment saved
        assertEquals(1, savedAttachments.size());
        assertEquals("contract.pdf", savedAttachments.get(0).getFileName());
        assertEquals("NO", savedContractHolder.get().getMissingAttachment());
    }

    @Test
    void testCreateContractWithoutAttachmentsSetsMissingAttachmentYes() {
        ContractResponse response = contractService.create(createSampleRequest(), List.of(), "25-00001");

        assertNotNull(response);
        assertEquals(0, savedAttachments.size());
        assertEquals("YES", savedContractHolder.get().getMissingAttachment());
    }

    @Test
    void testCreateContractInvalidDatesThrowsBadRequest() {
        ContractRequest invalidRequest = new ContractRequest(
                "PO-2026-001", "PO-2026-001", null,
                1, 1, 1, 1, 1, 1, "FY2026", 1L, "Description",
                LocalDate.now(), LocalDate.now(),
                LocalDate.now().plusMonths(6), // startDate is after endDate
                LocalDate.now(),
                6, null, new BigDecimal("1000.00"), new BigDecimal("5.00"), 1,
                null, null, null, null, null
        );

        assertThrows(ResponseStatusException.class, () ->
                contractService.create(invalidRequest, List.of(), "25-00001"));
    }
}
