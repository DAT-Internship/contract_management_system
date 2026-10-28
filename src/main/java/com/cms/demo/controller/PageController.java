package com.cms.demo.controller;

import com.cms.demo.service.UserService; // 🔴 ဒါလေး ထည့်ပါ
import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final UserService userService; // 🔴 Variable ကြေညာပါ

    // 🔴 Constructor မှာ UserService ကို လက်ခံပါ
    public PageController(UserService userService) {
        this.userService = userService;
    }

    // ၁။ ပင်မလမ်းကြောင်း (/) ကို ခေါ်ရင် Login ကို သွားခိုင်းမယ်
    @GetMapping("/")
    public String root() {
        return "redirect:/login"; 
    }

    // ၂။ Login Page ကို ပြဖို့
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // ၃။ Dashboard ကို ပြဖို့
    @GetMapping("/dashboard")
    public String showDashboard() {
        return "dashboard";
    }
    
    // Change Password Form ပြသဖို့
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "change-password"; 
    }

    // Form ကနေ Password အသစ် လက်ခံဖို့
    @PostMapping("/change-password")
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
}