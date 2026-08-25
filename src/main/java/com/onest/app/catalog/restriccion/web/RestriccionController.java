package com.onest.app.catalog.restriccion.web;

import com.onest.app.catalog.restriccion.service.RestriccionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Alta de restricciones medicas (embebida en el dictamen de Examen, ver examen-shell.html
 * "Restricciones médicas"). Backend en docs/ords-restriccion.sql, aplicado 2026-08-24.
 * La lista se carga junto con el shell del examen (ExamenController#examen), no aqui.
 */
@Controller
@RequestMapping("/api/nss")
public class RestriccionController {

    private final RestriccionService restriccionService;

    public RestriccionController(RestriccionService restriccionService) {
        this.restriccionService = restriccionService;
    }

    /** Guarda una restriccion asignada a un NSS (POST /Servcio/restriccion). */
    @PostMapping(
            path = "/restricciones/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String save(@ModelAttribute RestriccionAltaForm form) {
        try {
            String proceso = restriccionService.crear(form);
            return (proceso == null || proceso.isBlank()) ? "Restricción registrada." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
