package com.onest.app.catalog.accidente.web;

import com.onest.app.catalog.accidente.service.AccidenteService;
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
 * Accidentes de trabajo: lista y alta. Backend aplicado y verificado 2026-08-14
 * contra el WS ORDS real (docs/ords-accidentes.sql). Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class AccidenteController {

    private final AccidenteService accidenteService;

    public AccidenteController(AccidenteService accidenteService) {
        this.accidenteService = accidenteService;
    }

    @PostMapping(
            path = "/accidentes",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String accidentes(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("accidentes", accidenteService.byNss(data));
            model.addAttribute("nss", data == null ? "" : data.trim());
            return "fragments/accidentes-list :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Formulario de alta de accidente de trabajo. */
    @PostMapping(
            path = "/accidentes/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/accidentes-form :: form";
    }

    /** Guarda el accidente de trabajo (POST /Servcio/accidente). */
    @PostMapping(
            path = "/accidentes/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String save(@ModelAttribute AccidenteAltaForm form) {
        try {
            String proceso = accidenteService.crearAccidente(form);
            return (proceso == null || proceso.isBlank()) ? "Accidente registrado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
