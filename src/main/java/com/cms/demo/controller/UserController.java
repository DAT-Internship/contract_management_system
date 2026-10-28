package com.cms.demo.controller;

import com.cms.demo.entity.Department;
import com.cms.demo.entity.User;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.RoleRepository;
import com.cms.demo.service.UserService;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    public UserController(UserService userService, RoleRepository roleRepository, DepartmentRepository departmentRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
    }

    // Read (Page ပြသခြင်း)
    @GetMapping
    public String showUserList(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        
        // 🔴 Form အတွက် Role နဲ့ Department စာရင်းတွေ ပို့ပေးမယ်
        model.addAttribute("roles", roleRepository.findAll()); 
        model.addAttribute("departments", departmentRepository.findAll()); 
        
        return "users";
    }

    // Create (Form မှ လက်ခံခြင်း)
    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, 
                             @RequestParam("roleId") Integer roleId, 
                             @RequestParam("departmentId") Integer departmentId, // 🔴 ဒါလေး ထည့်လိုက်တယ်
                             RedirectAttributes redirectAttributes) {
        try {
            // Department ကို ရှာပြီး User ထဲ ထည့်မယ်
            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            user.setDepartment(dept);

            userService.createUser(user, roleId);
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    // Delete (Deactivate)
    @GetMapping("/deactivate/{id}")
    public String deactivateUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateUser(id);
            redirectAttributes.addFlashAttribute("success", "User deactivated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
    
    // ၁။ Edit Form ကို ပြသဖို့
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id); // Service မှာ ဒီ Method လေး ထပ်ထည့်ဖို့ လိုမယ်
        model.addAttribute("user", user);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        return "edit-user"; // edit-user.html ကို ခေါ်မယ်
    }

    // ၂။ Update လုပ်ပြီး Save ဖို့
    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable("id") Long id,
                             @ModelAttribute User user,
                             @RequestParam("roleId") Integer roleId,
                             @RequestParam("departmentId") Integer departmentId,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, user, roleId, departmentId);
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
    
   
}