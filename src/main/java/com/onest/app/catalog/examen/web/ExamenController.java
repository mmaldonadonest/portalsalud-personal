package com.onest.app.catalog.examen.web;

import com.onest.app.catalog.examen.service.ContactoEmergenciaService;
import com.onest.app.catalog.examen.service.DiagnosticoSecundarioService;
import com.onest.app.catalog.examen.service.ExamenService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Examen medico (U07) - Slice 1. Shell con navegacion in-page (irAContenido) y
 * secciones heredofamiliares. Equivale a viewExpHeroFam.php / viewTypeExped.php.
 */
@Controller
@RequestMapping("/api/nss")
public class ExamenController {

    private final ExamenService examenService;
    private final ContactoEmergenciaService contactoEmergenciaService;
    private final DiagnosticoSecundarioService diagnosticoSecundarioService;

    public ExamenController(
            ExamenService examenService,
            ContactoEmergenciaService contactoEmergenciaService,
            DiagnosticoSecundarioService diagnosticoSecundarioService) {
        this.examenService = examenService;
        this.contactoEmergenciaService = contactoEmergenciaService;
        this.diagnosticoSecundarioService = diagnosticoSecundarioService;
    }

    /** Shell del examen: navegacion de secciones + contenedor in-page. */
    @PostMapping(
            path = "/examen",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String examen(@RequestParam("data") String data, Model model) {
        try {
            String nss = data == null ? "" : data.trim();
            model.addAttribute("nss", nss);
            model.addAttribute("grupos", examenService.grupos());
            model.addAttribute("contactos", contactoEmergenciaService.cargar(nss));
            model.addAttribute("diagnosticosSecundarios", diagnosticoSecundarioService.cargar(nss));
            return "fragments/examen-shell :: shell";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Una seccion, pre-cargada (viewTypeExped). Se agrega al contenedor sin recargar. */
    @PostMapping(
            path = "/examen/seccion",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String seccion(
            @RequestParam("data") String seccion,
            @RequestParam("nss") String nss,
            Model model) {
        try {
            model.addAttribute("seccion", seccion);
            model.addAttribute("items", examenService.itemsDeSeccion(seccion, nss));
            return "fragments/examen-seccion :: seccion";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Guarda el examen completo (sendHeredoFamDats CAMBIO -> /Servcio/Medico).
     * Recibe los campos de todas las secciones cargadas (name=clave punteada) + firma.
     */
    @PostMapping(
            path = "/examen/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String save(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            String firma = params.getFirst("firma");
            Map<String, String> campos = new LinkedHashMap<>();
            params.forEach((key, values) -> {
                if (!"nss".equals(key) && !"firma".equals(key) && !"_csrf".equals(key)) {
                    campos.put(key, (values == null || values.isEmpty()) ? "" : values.get(0));
                }
            });
            String proceso = examenService.guardar(nss, campos, firma);
            return (proceso == null || proceso.isBlank()) ? "Examen guardado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Guarda los contactos de emergencia (EAV via MED_TAG, independiente del examen -> WS).
     * Recibe los 3x5 campos (contactoEmer{campo}{indice}).
     */
    @PostMapping(
            path = "/examen/contactos-emergencia/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String guardarContactosEmergencia(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            Map<String, String> campos = new LinkedHashMap<>();
            params.forEach((key, values) -> {
                if (!"nss".equals(key) && !"_csrf".equals(key)) {
                    campos.put(key, (values == null || values.isEmpty()) ? "" : values.get(0));
                }
            });
            contactoEmergenciaService.guardar(nss, campos);
            return "Contactos de emergencia guardados.";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Guarda los diagnosticos secundarios (EAV via MED_TAG, independiente del examen -> WS).
     * Recibe los 3 campos (diagnosticoSecundario{indice}).
     */
    @PostMapping(
            path = "/examen/diagnosticos-secundarios/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String guardarDiagnosticosSecundarios(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            Map<String, String> campos = new LinkedHashMap<>();
            params.forEach((key, values) -> {
                if (!"nss".equals(key) && !"_csrf".equals(key)) {
                    campos.put(key, (values == null || values.isEmpty()) ? "" : values.get(0));
                }
            });
            diagnosticoSecundarioService.guardar(nss, campos);
            return "Diagnósticos secundarios guardados.";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
