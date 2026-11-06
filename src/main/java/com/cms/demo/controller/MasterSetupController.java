package com.cms.demo.controller;

import com.cms.demo.service.MasterSetupService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/master-setup")
public class MasterSetupController {

    private final MasterSetupService masterSetupService;

    public MasterSetupController(MasterSetupService masterSetupService) {
        this.masterSetupService = masterSetupService;
    }

    // ၁။ Page ပြသဖို့
    @GetMapping
    public String page(Model model) {
        Map<String, Object> data = masterSetupService.getAllMasterData();
        model.addAllAttributes(data);
        model.addAttribute("page", "master-setup");
        return "master-setup";
    }

    // ၂။ အသစ်ထည့်ဖို့ (Add)
    @PostMapping("/add")
    public String add(@RequestParam("type") String type, @RequestParam("code") String code, @RequestParam("name") String name,
                      @RequestParam(value = "partyType", required = false) String partyType,
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
        try {
            // 🔴 UI မှာ ပိတ်ထားပေမယ့် URL ကနေ လှမ်းခေါ်ရင် ပိတ်ဖို့
            if ("role".equals(type)) {
                throw new Exception("Adding new roles is not permitted.");
            }
            masterSetupService.addMasterData(type, code, name, partyType, taxId, address, contactPerson, contactEmail, contactPhone, projectRank, departmentId, projectManagerId, startDate, endDate, status);
            redirectAttributes.addFlashAttribute("success", name + " added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master-setup";
    }

    // ၃။ ပြင်ဆင်ဖို့ (Update)
    @PostMapping("/update")
    public String update(@RequestParam("type") String type, @RequestParam("id") String id, 
                         @RequestParam("code") String code, @RequestParam("name") String name,
                         @RequestParam(value = "partyType", required = false) String partyType,
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
        try {
            // 🔴 Role ကို ပြင်ခွင့်မပြု
            if ("role".equals(type)) {
                throw new Exception("Updating roles is not permitted.");
            }
            masterSetupService.updateMasterData(type, id, code, name, partyType, taxId, address, contactPerson, contactEmail, contactPhone, projectRank, departmentId, projectManagerId, startDate, endDate, status);
            redirectAttributes.addFlashAttribute("success", name + " updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master-setup";
    }

    // ၄။ ဖျက်ဖို့ (Delete)
    @PostMapping("/delete")
    public String delete(@RequestParam("type") String type, @RequestParam("id") String id, RedirectAttributes redirectAttributes) {
        try {
            // 🔴 Role ကို ဖျက်ခွင့်မပြု
            if ("role".equals(type)) {
                throw new Exception("Deleting roles is not permitted.");
            }
            masterSetupService.deleteMasterData(type, id);
            redirectAttributes.addFlashAttribute("success", "Item removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/master-setup";
    }
}