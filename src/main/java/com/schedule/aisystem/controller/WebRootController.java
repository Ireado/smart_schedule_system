package com.schedule.aisystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebRootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/ui.html";
    }
}
