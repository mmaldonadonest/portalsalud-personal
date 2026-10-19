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
    private final com.onest.app.catalog.examen.service.ExamenDocumentoService documentoService;
    private final com.onest.app.catalog.examen.repository.FichaBiometricoRepository fichaBiometrico;

    public NssSearchController(NssSearchService nssSearchService,
                               com.onest.app.catalog.examen.service.ExamenDocumentoService documentoService,
                               com.onest.app.catalog.examen.repository.FichaBiometricoRepository fichaBiometrico) {
        this.nssSearchService = nssSearchService;
        this.documentoService = documentoService;
        this.fichaBiometrico = fichaBiometrico;
    }

    @PostMapping(
            path = "/search",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String search(@RequestParam("data") String data, Model model) {
        try {
            NssSearchResponse result = nssSearchService.findByNss(data).orElse(null);
            model.addAttribute("result", result);
            // Agencia (15-oct-2026) en lugar de la "empresa" del WS, que es "Onest Mexico" para
            // todos: misma regla que el examen inicial y el impreso (OCR > SOCIEDAD > vacio)
            if (result != null && result.empleado() != null) {
                String agenciaOcr = result.solicitudOcr() == null ? null : result.solicitudOcr().agencia();
                model.addAttribute("agencia", documentoService.agenciaConCaptura(result.empleado().nss(), agenciaOcr));
                // Ficha del FT-SO-17 (15-oct-2026): escolaridad y profesion del candidato vienen de
                // la solicitud de OCR; del empleado, escolaridad y departamento del biometrico.
                // Profesion u oficio no existe en el biometrico.
                var sol = result.solicitudOcr();
                var extra = fichaBiometrico.extra(result.empleado().nss());
                String escolaridadOcr = sol == null || sol.escolaridad() == null ? "" : sol.escolaridad().trim();
                model.addAttribute("escolaridad", escolaridadOcr.isEmpty() ? extra.escolaridad() : escolaridadOcr);
                model.addAttribute("profesion", sol == null || sol.profesionOficio() == null ? "" : sol.profesionOficio().trim());
                model.addAttribute("departamento", extra.departamento());
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("result", null);
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/nss-result :: result";
    }
}
