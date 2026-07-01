package dev.antoniogrillo.testldap.controller;

import dev.antoniogrillo.testldap.model.Utente;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal Utente utente, Model model) {
        model.addAttribute("utente", utente);
        return "index";
    }

    @GetMapping("/accesso-negato")
    public String accessoNegato() {
        return "accesso-negato";
    }
}
