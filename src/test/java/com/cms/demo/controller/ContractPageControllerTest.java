package com.cms.demo.controller;

import com.cms.demo.dto.ContractResponse;
import com.cms.demo.entity.MasterContractStatus;
import com.cms.demo.repository.*;
import com.cms.demo.service.ContractService;
import com.cms.demo.service.FileStorageService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ContractPageControllerTest {

    private ContractService contractService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        contractService = mock(ContractService.class);
        MasterContractStatus activeStatus = new MasterContractStatus();
        activeStatus.setId(1);
        activeStatus.setCode("ACTIVE");
        activeStatus.setIsActive(true);

        ContractStatusRepository statuses = repositoryProxy(ContractStatusRepository.class, (method, args) -> {
            if ("findById".equals(method)) return Optional.of(activeStatus);
            return List.of();
        });

        ContractPageController controller = new ContractPageController(
                contractService,
                mock(FileStorageService.class),
                repositoryProxy(AgreementForRepository.class, (m, a) -> List.of()),
                repositoryProxy(ContractCategoryRepository.class, (m, a) -> List.of()),
                repositoryProxy(ContractTypeRepository.class, (m, a) -> List.of()),
                repositoryProxy(PaymentTypeRepository.class, (m, a) -> List.of()),
                statuses,
                repositoryProxy(DepartmentRepository.class, (m, a) -> List.of()),
                repositoryProxy(CustomerSupplierRepository.class, (m, a) -> List.of()),
                repositoryProxy(CurrencyRepository.class, (m, a) -> List.of()),
                repositoryProxy(ProjectRepository.class, (m, a) -> List.of()),
                repositoryProxy(UserRepository.class, (m, a) -> List.of()),
                mock(ApprovalStepRepository.class),
                defaultValidator());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void submitWithAllFieldsPopulatedCreatesContractAndRedirects() throws Exception {
        when(contractService.create(any(), any(), eq("25-00001"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/contracts")
                        .principal(() -> "25-00001")
                        .params(toParams(validForm())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/contracts"));

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(com.cms.demo.dto.ContractRequest.class);
        verify(contractService).create(requestCaptor.capture(), any(), eq("25-00001"));
        var request = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("PO-2026-001", request.contractPoNo());
        org.junit.jupiter.api.Assertions.assertEquals("FY2026", request.fiscalYear());
        org.junit.jupiter.api.Assertions.assertEquals("Full contract description", request.description());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("50000.00"), request.contractValue());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 1, 1), request.registerDate());
        org.junit.jupiter.api.Assertions.assertEquals("REF-001", request.poNoRef());
        org.junit.jupiter.api.Assertions.assertEquals(12, request.periodMonths());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("2.00"), request.headcountPerMonth());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("5.00"), request.commercialTaxRate());
        org.junit.jupiter.api.Assertions.assertEquals(2L, request.projectId());
        org.junit.jupiter.api.Assertions.assertEquals(3L, request.projectInchargeId());
        org.junit.jupiter.api.Assertions.assertEquals(4L, request.contractManagerId());
        org.junit.jupiter.api.Assertions.assertEquals("Jane Doe", request.clientIncharge());
    }

    @ParameterizedTest(name = "missing required field: {0}")
    @MethodSource("requiredFields")
    void missingRequiredFieldReturnsFormErrorWithoutCreatingContract(String field) throws Exception {
        Map<String, String[]> form = validForm();
        form.remove(field);

        mockMvc.perform(post("/contracts")
                        .principal(() -> "25-00001")
                        .params(toParams(form)))
                .andExpect(status().isOk())
                .andExpect(view().name("contracts/form"))
                .andExpect(model().attribute("error",
                        "Complete all required fields and correct invalid values before submitting the contract."));

        verify(contractService, never()).create(any(), any(), any());
    }

    @ParameterizedTest(name = "blank required text field: {0}")
    @MethodSource("requiredTextFields")
    void blankRequiredTextFieldReturnsFormErrorWithoutCreatingContract(String field) throws Exception {
        Map<String, String[]> form = validForm();
        form.put(field, new String[]{"   "});

        mockMvc.perform(post("/contracts")
                        .principal(() -> "25-00001")
                        .params(toParams(form)))
                .andExpect(status().isOk())
                .andExpect(view().name("contracts/form"))
                .andExpect(model().attributeExists("error"));

        verify(contractService, never()).create(any(), any(), any());
    }

    static Stream<String> requiredFields() {
        return Stream.of("contractPoNo", "agreementForId", "categoryId", "contractTypeId",
                "paymentTypeId", "statusId", "departmentId", "fiscalYear", "partyId",
                "description", "startDate", "endDate", "contractValue", "currencyId");
    }

    static Stream<String> requiredTextFields() {
        return Stream.of("contractPoNo", "fiscalYear", "description");
    }

    private static Map<String, String[]> validForm() {
        Map<String, String[]> values = new LinkedHashMap<>();
        values.put("contractPoNo", new String[]{"PO-2026-001"});
        values.put("poNoRef", new String[]{"REF-001"});
        values.put("agreementForId", new String[]{"1"});
        values.put("categoryId", new String[]{"1"});
        values.put("contractTypeId", new String[]{"1"});
        values.put("paymentTypeId", new String[]{"1"});
        values.put("statusId", new String[]{"1"});
        values.put("departmentId", new String[]{"1"});
        values.put("fiscalYear", new String[]{"FY2026"});
        values.put("partyId", new String[]{"1"});
        values.put("description", new String[]{"Full contract description"});
        values.put("signedDate", new String[]{"2025-12-20"});
        values.put("registerDate", new String[]{"2026-01-01"});
        values.put("startDate", new String[]{"2026-01-01"});
        values.put("endDate", new String[]{"2026-12-31"});
        values.put("periodMonths", new String[]{"12"});
        values.put("headcountPerMonth", new String[]{"2.00"});
        values.put("contractValue", new String[]{"50000.00"});
        values.put("commercialTaxRate", new String[]{"5.00"});
        values.put("currencyId", new String[]{"1"});
        values.put("projectId", new String[]{"2"});
        values.put("projectInchargeId", new String[]{"3"});
        values.put("contractManagerId", new String[]{"4"});
        values.put("clientIncharge", new String[]{"Jane Doe"});
        values.put("action", new String[]{"submit"});
        return values;
    }

    private static MultiValueMap<String, String> toParams(Map<String, String[]> values) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        values.forEach((key, entries) -> params.put(key, Arrays.asList(entries)));
        return params;
    }

    private static ContractResponse sampleResponse() {
        return new ContractResponse(100L, "26-00001", "PO-2026-001", "REF-001", "FY2026",
                "Acme Corp", "Full contract description", LocalDate.of(2025, 12, 20),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                12, 364L, new BigDecimal("2.00"), new BigDecimal("24.00"),
                new BigDecimal("50000.00"), new BigDecimal("5.00"), new BigDecimal("2500.00"),
                "USD", "ACTIVE", "Active", "Jane Doe", null, "Services", "Operations");
    }

    private static Validator defaultValidator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @SuppressWarnings("unchecked")
    private static <T> T repositoryProxy(Class<T> type,
            java.util.function.BiFunction<String, Object[], Object> handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if ("toString".equals(method.getName())) return type.getSimpleName() + " test proxy";
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("equals".equals(method.getName())) return proxy == args[0];
            Object[] actualArgs = args == null ? new Object[0] : args;
            Object result = handler.apply(method.getName(), actualArgs);
            if (result != null) return result;
            if (List.class.isAssignableFrom(method.getReturnType())) return List.of();
            if (Optional.class.isAssignableFrom(method.getReturnType())) return Optional.empty();
            return null;
        });
    }
}
