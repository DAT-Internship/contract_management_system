package com.cms.demo.controller;

import com.cms.demo.dto.ContractPaymentDetailResponse;
import com.cms.demo.dto.ContractPaymentSummaryResponse;
import com.cms.demo.dto.InvoiceForm;
import com.cms.demo.dto.InvoiceResponse;
import com.cms.demo.dto.PaymentForm;
import com.cms.demo.dto.PaymentOverviewResponse;
import com.cms.demo.repository.ContractCategoryRepository;
import com.cms.demo.repository.CurrencyRepository;
import com.cms.demo.repository.PaymentStatusRepository;
import com.cms.demo.repository.PaymentTypeRepository;
import com.cms.demo.service.PaymentCalculator;
import com.cms.demo.service.PaymentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentTypeRepository paymentTypeRepository;
    private final ContractCategoryRepository categoryRepository;
    private final CurrencyRepository currencyRepository;
    private final PaymentStatusRepository paymentStatusRepository;

    public PaymentController(
            PaymentService paymentService,
            PaymentTypeRepository paymentTypeRepository,
            ContractCategoryRepository categoryRepository,
            CurrencyRepository currencyRepository,
            PaymentStatusRepository paymentStatusRepository) {
        this.paymentService = paymentService;
        this.paymentTypeRepository = paymentTypeRepository;
        this.categoryRepository = categoryRepository;
        this.currencyRepository = currencyRepository;
        this.paymentStatusRepository = paymentStatusRepository;
    }

    @GetMapping
    public String list(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer paymentTypeId,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Long contractId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Model model,
            Principal principal) {
        if (contractId != null) {
            return "redirect:/payments/" + contractId;
        }

        PaymentOverviewResponse overview = paymentService.getOverview(principal.getName());
        List<ContractPaymentSummaryResponse> filteredContracts = paymentService.findContractSummaries(
                principal.getName(), query, status, paymentTypeId, categoryId);

        pageSize = List.of(10, 25, 50, 100).contains(pageSize) ? pageSize : 25;
        int total = filteredContracts.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        page = Math.max(1, Math.min(page, totalPages));
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(page * pageSize, total);

        model.addAttribute("page", "payments");
        model.addAttribute("overview", overview);
        model.addAttribute("contracts", filteredContracts.subList(fromIndex, toIndex));
        model.addAttribute("total", total);
        model.addAttribute("pageNumber", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("showingStart", total == 0 ? 0 : fromIndex + 1);
        model.addAttribute("showingEnd", toIndex);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSizes", List.of(10, 25, 50, 100));
        model.addAttribute("statusOptions", statusOptions());
        model.addAttribute("paymentTypes",
                paymentTypeRepository.findAll(Sort.by("sortOrder", "id")));
        model.addAttribute("categories",
                categoryRepository.findAll(Sort.by("sortOrder", "id")));
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        model.addAttribute("paymentTypeId", paymentTypeId);
        model.addAttribute("categoryId", categoryId);
        return "payments/list";
    }

    @GetMapping("/{contractId}")
    public String detail(
            @PathVariable Long contractId,
            @RequestParam(name = "addInvoice", defaultValue = "false") boolean addInvoice,
            Model model,
            Principal principal) {
        addDetailModel(model, contractId, principal.getName(), newInvoiceForm(), newPaymentForm());
        if (addInvoice) {
            model.addAttribute("openInvoiceModal", true);
        }
        return "payments/detail";
    }

    @PostMapping("/{contractId}/invoices/save")
    public String saveInvoice(
            @PathVariable Long contractId,
            @Valid @ModelAttribute("invoiceForm") InvoiceForm invoiceForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("openInvoiceModal", true);
            addDetailModel(model, contractId, principal.getName(), invoiceForm, newPaymentForm());
            return "payments/detail";
        }

        try {
            if (invoiceForm.getInvoiceId() == null) {
                paymentService.createInvoice(contractId, invoiceForm.toRequest(), principal.getName());
                redirectAttributes.addFlashAttribute("success", "Invoice created successfully.");
            } else {
                ensureInvoiceBelongsToContract(invoiceForm.getInvoiceId(), contractId, principal.getName());
                paymentService.updateInvoice(invoiceForm.getInvoiceId(), invoiceForm.toRequest(), principal.getName());
                redirectAttributes.addFlashAttribute("success", "Invoice updated successfully.");
            }
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("error", errorMessage(ex));
        }
        return "redirect:/payments/" + contractId;
    }

    @PostMapping("/{contractId}/payment")
    public String recordPayment(
            @PathVariable Long contractId,
            @Valid @ModelAttribute("paymentForm") PaymentForm paymentForm,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("openPaymentModal", true);
            addDetailModel(model, contractId, principal.getName(), newInvoiceForm(), paymentForm);
            return "payments/detail";
        }

        try {
            ensureInvoiceBelongsToContract(paymentForm.getInvoiceId(), contractId, principal.getName());
            paymentService.recordPayment(
                    paymentForm.getInvoiceId(), paymentForm.toRequest(), principal.getName());
            redirectAttributes.addFlashAttribute("success", "Payment recorded successfully.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("error", errorMessage(ex));
        }
        return "redirect:/payments/" + contractId;
    }

    private void addDetailModel(Model model, Long contractId, String employeeCode,
            InvoiceForm invoiceForm, PaymentForm paymentForm) {
        ContractPaymentDetailResponse detail = paymentService.getContractDetail(contractId, employeeCode);
        model.addAttribute("page", "payments");
        model.addAttribute("detail", detail);
        model.addAttribute("contract", detail.contract());
        model.addAttribute("invoices", detail.invoices());
        model.addAttribute("history", detail.history());
        model.addAttribute("invoiceForm", invoiceForm);
        model.addAttribute("paymentForm", paymentForm);
        model.addAttribute("currencies", currencyRepository.findAll(Sort.by("sortOrder", "code")));
        model.addAttribute("paymentStatuses",
                paymentStatusRepository.findAll(Sort.by("sortOrder", "id")));
        model.addAttribute("defaultDueDays", PaymentCalculator.DEFAULT_PAYMENT_TERM_DAYS);
    }

    private void ensureInvoiceBelongsToContract(Long invoiceId, Long contractId, String employeeCode) {
        InvoiceResponse invoice = paymentService.findInvoice(invoiceId, employeeCode);
        if (!contractId.equals(invoice.contractId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }
    }

    private InvoiceForm newInvoiceForm() {
        InvoiceForm form = new InvoiceForm();
        form.setInvoiceDate(LocalDate.now());
        form.setDueDate(LocalDate.now().plusDays(PaymentCalculator.DEFAULT_PAYMENT_TERM_DAYS));
        return form;
    }

    private PaymentForm newPaymentForm() {
        PaymentForm form = new PaymentForm();
        form.setPaidDate(LocalDate.now());
        return form;
    }

    private List<Map<String, String>> statusOptions() {
        return List.of(
                Map.of("code", PaymentCalculator.PENDING, "name", "Pending"),
                Map.of("code", PaymentCalculator.PARTIALLY_PAID, "name", "Partially Paid"),
                Map.of("code", PaymentCalculator.PAID, "name", "Paid"),
                Map.of("code", PaymentCalculator.OVERDUE, "name", "Overdue"),
                Map.of("code", PaymentCalculator.NO_INVOICES, "name", "No Invoices"));
    }

    private String errorMessage(ResponseStatusException ex) {
        return ex.getReason() == null ? "Unable to complete the payment operation." : ex.getReason();
    }
}
