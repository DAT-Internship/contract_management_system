package com.cms.demo.controller;

import com.cms.demo.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal; // 🔴 Principal ကို Import လုပ်ပါ

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PasswordResetService passwordResetService;

    public AdminController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // ၁။ Request စာရင်း ကြည့်ဖို့ Page
    @GetMapping("/password-resets")
    public String viewPasswordRequests(Model model) {
        model.addAttribute("requests", passwordResetService.getPendingRequests());
        return "auth/password-requests"; 
    }

    // ၂။ Reset ခလုတ် နှိပ်ရင် အလုပ်လုပ်မယ့် Method
    @PostMapping("/password-resets/reset/{id}")
    public String resetPassword(@PathVariable("id") Long id, 
                                Principal principal, // 🔴 Principal ကို လက်ခံပါ
                                RedirectAttributes redirectAttributes) {
        try {
            // 🔴 principal.getName() (Admin ရဲ့ Staff ID) ကို Service ဆီ လှမ်းပို့ပါ
            passwordResetService.resetUserPassword(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Password reset to default successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/password-resets";
    }
}