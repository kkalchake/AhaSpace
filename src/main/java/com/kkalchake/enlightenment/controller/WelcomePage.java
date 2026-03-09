package com.kkalchake.enlightenment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WelcomePage {
    @GetMapping ("/welcome")
    public String welcomePage(Model model) {
        model.addAttribute("message", "Welcome To Your Learning Platform!");

        return "welcome";
    }
}
