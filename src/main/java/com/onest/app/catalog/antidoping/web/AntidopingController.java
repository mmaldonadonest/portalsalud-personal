package com.onest.app.catalog.antidoping.web;

import com.onest.app.catalog.antidoping.service.AntidopingService;
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
 * Antidoping / alcoholimetria: lista y alta. Backend aplicado y verificado 2026-08-14
 * contra el WS ORDS real (docs/ords-antidoping.sql). Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class AntidopingController {

    private final AntidopingService antidopingService;

    public AntidopingController(AntidopingService antidopingService) {
        this.antidopingService = antidopingService;
    }

    @PostMapping(
            path = "/antidoping",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String antidoping(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("antidopings", antidopingService.byNss(data));
            model.addAttribute("nss", data == null ? "" : data.trim());
            return "fragments/antidoping-list :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Formulario de alta de antidoping/alcoholimetria. */
    @PostMapping(
            path = "/antidoping/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/antidoping-form :: form";
    }

    /** Guarda el registro de antidoping (POST /Servcio/antidoping). */
    @PostMapping(
            path = "/antidoping/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String save(@ModelAttribute AntidopingAltaForm form) {
        try {
            String proceso = antidopingService.crearAntidoping(form);
            return (proceso == null || proceso.isBlank()) ? "Registro de antidoping guardado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
