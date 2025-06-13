package com.akmz.springBase.base.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("message", "환영합니다! 여기는 홈 페이지입니다.");
        return "home";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }
}
