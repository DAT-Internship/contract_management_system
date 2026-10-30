package com.cms.demo.controller;

import com.cms.demo.service.AuditLogService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/audit")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            Model model) {
        pageSize = List.of(10, 25, 50, 100).contains(pageSize) ? pageSize : 25;
        page = Math.max(1, page);
        var auditPage = auditLogService.search(q, action, entityType, from, to,
                PageRequest.of(page - 1, pageSize,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("logId"))));
        int totalPages = Math.max(1, auditPage.getTotalPages());
        if (page > totalPages) {
            page = totalPages;
            auditPage = auditLogService.search(q, action, entityType, from, to,
                    PageRequest.of(page - 1, pageSize,
                            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("logId"))));
        }

        model.addAttribute("page", "audit");
        model.addAttribute("auditLogs", auditPage.getContent());
        model.addAttribute("total", auditPage.getTotalElements());
        model.addAttribute("pageNumber", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("showingStart", auditPage.getTotalElements() == 0 ? 0 : (page - 1) * pageSize + 1);
        model.addAttribute("showingEnd", Math.min((long) page * pageSize, auditPage.getTotalElements()));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSizes", List.of(10, 25, 50, 100));
        model.addAttribute("q", q);
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("actions", List.of("CREATE", "UPDATE", "DELETE"));
        model.addAttribute("entityTypes", List.of("CONTRACT", "IMPORT_BATCH", "USER", "MASTER_SETUP", "PAYMENT", "APPROVAL", "DASHBOARD", "SYSTEM"));
        return "audit";
    }
}
