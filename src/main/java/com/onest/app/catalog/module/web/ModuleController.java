package com.onest.app.catalog.module.web;

import com.onest.app.catalog.module.service.ModulePermissionService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Menu dinamico de modulos por permisos. Replica el contrato de
 * php-old/rest/validateModules.php: XHR POST que devuelve los modulos
 * permitidos para el rol del usuario. Aqui se devuelve un fragmento HTML
 * (botones de modulo) que el frontend inyecta bajo la ficha del empleado.
 */
@Controller
@RequestMapping("/api/nss")
public class ModuleController {

    private final ModulePermissionService modulePermissionService;

    public ModuleController(ModulePermissionService modulePermissionService) {
        this.modulePermissionService = modulePermissionService;
    }

    @PostMapping(
            path = "/modules",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String modules(@RequestParam(name = "data", required = false) String data, Model model) {
        model.addAttribute("modulos", modulePermissionService.allowedModulesForCurrentUser());
        // El NSS no lo usa el WS (en php-old data va vacio), pero lo embebemos en los
        // botones para la futura navegacion a cada modulo (U03-U07).
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/nss-modules :: modules";
    }
}
