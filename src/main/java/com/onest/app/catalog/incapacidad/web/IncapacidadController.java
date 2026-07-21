package com.onest.app.catalog.incapacidad.web;

import com.onest.app.catalog.file.service.FileStoreService;
import com.onest.app.catalog.incapacidad.service.IncapacidadService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Incapacidades (U05): lista (viewArchivoInc -> expedienteShowConsults?type=expedienteIncapacidad)
 * y detalle (showDetailsIncap -> viewIncapDetails.php). Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class IncapacidadController {

    private final IncapacidadService incapacidadService;
    private final FileStoreService fileStoreService;

    public IncapacidadController(IncapacidadService incapacidadService, FileStoreService fileStoreService) {
        this.incapacidadService = incapacidadService;
        this.fileStoreService = fileStoreService;
    }

    @PostMapping(
            path = "/incapacidades",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String incapacidades(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("incapacidades", incapacidadService.byNss(data));
            model.addAttribute("nss", data == null ? "" : data.trim());
            return "fragments/incapacidades-list :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping(
            path = "/incapacidades/detalle",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String detalle(
            @RequestParam("data") String data,
            @RequestParam("serieId") String serieId,
            Model model) {
        try {
            var detalle = incapacidadService.detalle(data, serieId).orElse(null);
            model.addAttribute("detalle", detalle);
            model.addAttribute("adjuntos",
                    detalle != null ? fileStoreService.listByRelacion(detalle.urlArchivos()) : List.of());
            return "fragments/incapacidad-detalle :: detalle";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Formulario de alta de incapacidad (ViewIncap -> incapacidades.php). */
    @PostMapping(
            path = "/incapacidades/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        model.addAttribute("nss", data == null ? "" : data.trim());
        model.addAttribute("idArchivoRel", UUID.randomUUID().toString().replace("-", ""));
        return "fragments/incapacidad-form :: form";
    }

    /** Guarda la incapacidad (incap.php -> addIncapacidad -> /Servcio/incapacidades). */
    @PostMapping(
            path = "/incapacidades/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String save(@ModelAttribute IncapacidadAltaForm form) {
        try {
            String proceso = incapacidadService.crearIncapacidad(form);
            return (proceso == null || proceso.isBlank()) ? "Incapacidad registrada." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
