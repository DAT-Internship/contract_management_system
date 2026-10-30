package com.cms.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** Route the application root to the available landing page. */
    @GetMapping("/")
    public String home() {
        return "redirect:/master-setup";
    }
}
