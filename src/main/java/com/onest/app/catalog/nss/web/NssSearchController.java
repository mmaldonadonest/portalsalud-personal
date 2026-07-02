package com.onest.app.catalog.nss.web;

import com.onest.app.catalog.nss.dto.NssSearchResponse;
import com.onest.app.catalog.nss.service.NssSearchService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Busqueda por NSS. Replica el contrato de php-old/rest/dashboard/searchEmploye.php:
 * XHR POST con el parametro de formulario 'data' (el NSS) y respuesta HTML (fragmento)
 * que el frontend inyecta en el contenedor de resultados.
 */
@Controller
@RequestMapping("/api/nss")
public class NssSearchController {

    private final NssSearchService nssSearchService;

    public NssSearchController(NssSearchService nssSearchService) {
        this.nssSearchService = nssSearchService;
    }

    @PostMapping(
            path = "/search",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String search(@RequestParam("data") String data, Model model) {
        try {
            NssSearchResponse result = nssSearchService.findByNss(data).orElse(null);
            model.addAttribute("result", result);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("result", null);
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/nss-result :: result";
    }
}
