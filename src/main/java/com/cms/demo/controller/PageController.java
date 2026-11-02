package com.cms.demo.controller;

import com.cms.demo.entity.PasswordResetRequest; // 🔴 Import လုပ်ပါ
import com.cms.demo.service.PasswordResetService; // 🔴 Import လုပ်ပါ
import com.cms.demo.service.UserService; 
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute; // 🔴 Import လုပ်ပါ
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final UserService userService;
    private final PasswordResetService passwordResetService; // 🔴 Variable ကြေညာပါ

    // 🔴 Constructor မှာ နှစ်ခုလုံးကို လက်ခံပါ
    public PageController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    // Login Page ကို ပြဖို့
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // Dashboard ကို ပြဖို့
    @GetMapping("/dashboard")
    public String showDashboard() {
        return "dashboard";
    }

    @GetMapping("/master-setup/import-batches")
    public String showImportBatches(@RequestParam(required = false) String type, Model model) {
        model.addAttribute("page", "import-batches");
        model.addAttribute("importType", type == null ? "" : type.toLowerCase(java.util.Locale.ROOT));
        return "import-batches";
    }
    
    // Change Password Form ပြသဖို့
    @GetMapping({"/change-password", "/profile/password"})
    public String showChangePasswordForm() {
        return "auth/change-password"; 
    }

    // Form ကနေ Password အသစ် လက်ခံဖို့
    @PostMapping({"/change-password", "/profile/password"})
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Principal principal, 
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("New passwords do not match!");
            }

            userService.changePassword(principal.getName(), currentPassword, newPassword);
            
            redirectAttributes.addFlashAttribute("success", "Password changed successfully! Please login again.");
            return "redirect:/logout"; 

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/change-password";
        }
    }
    
    // 🔴 Login Page က Forgot Password Form လက်ခံဖို့
    @PostMapping("/forgot-password")
    public String submitForgotPassword(@ModelAttribute PasswordResetRequest request, RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.submitRequest(request);
            redirectAttributes.addFlashAttribute("success", "Request submitted! Admin will review it.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/login";
    }
}