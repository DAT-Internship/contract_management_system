package com.cms.demo.controller;

import com.cms.demo.entity.Department;
import com.cms.demo.entity.User;
import com.cms.demo.repository.DepartmentRepository;
import com.cms.demo.repository.RoleRepository;
import com.cms.demo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal; // 🔴 Principal ကို Import လုပ်ပါ

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

    @GetMapping
    public String showUserList(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("roles", roleRepository.findAll()); 
        model.addAttribute("departments", departmentRepository.findAll()); 
        return "users";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, 
                             @RequestParam("roleId") Integer roleId, 
                             @RequestParam("departmentId") Integer departmentId,
                             Principal principal, // 🔴 Principal ကို လက်ခံပါ
                             RedirectAttributes redirectAttributes) {
        try {
            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            user.setDepartment(dept);

            // 🔴 principal.getName() ကို Service ဆီ လှမ်းပို့ပါ
            userService.createUser(user, roleId, principal.getName());
            
            redirectAttributes.addFlashAttribute("success", "User created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }

    // Activate (ပြန်ဖွင့်ခြင်း)
    @GetMapping("/activate/{id}")
    public String activateUser(@PathVariable("id") Long id, 
                               Principal principal, 
                               RedirectAttributes redirectAttributes) {
        try {
            userService.activateUser(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "User activated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
    
    @GetMapping("/deactivate/{id}")
    public String deactivateUser(@PathVariable("id") Long id, 
                                 Principal principal, // 🔴 Principal ကို လက်ခံပါ
                                 RedirectAttributes redirectAttributes) {
        try {
            // 🔴 principal.getName() ကို Service ဆီ လှမ်းပို့ပါ
            userService.deactivateUser(id, principal.getName());
            redirectAttributes.addFlashAttribute("success", "User deactivated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);
        model.addAttribute("user", user);
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll());
        return "edit-user"; 
    }

    @PostMapping("/update/{id}")
    public String updateUser(@PathVariable("id") Long id,
                             @ModelAttribute User user,
                             @RequestParam("roleId") Integer roleId,
                             @RequestParam("departmentId") Integer departmentId,
                             Principal principal, // 🔴 Principal ကို လက်ခံပါ
                             RedirectAttributes redirectAttributes) {
        try {
            // 🔴 principal.getName() ကို Service ဆီ လှမ်းပို့ပါ
            userService.updateUser(id, user, roleId, departmentId, principal.getName());
            redirectAttributes.addFlashAttribute("success", "User updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/users";
    }
}