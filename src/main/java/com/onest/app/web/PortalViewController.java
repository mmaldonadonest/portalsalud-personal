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

    /** Home = el dashboard de KPIs (ApexCharts), decision del 18 de agosto. La ruta /dashboard-demo-apex
     * se conserva aparte (mismo template) para no perderla como URL independiente. */
    @GetMapping("/home")
    public String home() {
        return "pages/dashboard-demo-apex";
    }

    /** Demo del Dashboard ligero de KPIs (Resumen General) usando Recharts (igual que salud-ocupacional-v2). */
    @GetMapping("/dashboard-demo")
    public String dashboardDemo() {
        return "pages/dashboard-demo";
    }

    /** Misma demo, usando ApexCharts (ya bundleado en el theme) en vez de Recharts, para comparar. */
    @GetMapping("/dashboard-demo-apex")
    public String dashboardDemoApex() {
        return "pages/dashboard-demo-apex";
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

    /**
     * Consumibles de antidoping (inventario por PREDIO/mes). NO forma parte del flujo de
     * busqueda por NSS - pagina standalone, accesible por URL directa (sin id_menu registrado
     * todavia, mismo patron que Antidoping/Accidentes al lanzarse).
     */
    @GetMapping("/consumibles")
    public String consumibles() {
        return "pages/consumibles";
    }
}
