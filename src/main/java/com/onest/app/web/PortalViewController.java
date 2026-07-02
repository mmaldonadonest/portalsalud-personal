package com.onest.app.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PortalViewController {

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/home")
    public String home() {
        return "pages/home";
    }

    /**
     * Pagina dedicada de busqueda por NSS. Acepta un NSS opcional por query param
     * (usado como shortcut desde el buscador global del header) para pre-cargar y buscar.
     */
    @GetMapping("/nss")
    public String nssSearch(@RequestParam(name = "nss", required = false) String nss, Model model) {
        model.addAttribute("nssQuery", nss == null ? "" : nss.trim());
        return "pages/nss-search";
    }
}
