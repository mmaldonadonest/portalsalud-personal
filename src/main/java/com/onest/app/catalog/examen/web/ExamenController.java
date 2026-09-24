package com.onest.app.catalog.examen.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.examen.service.ContactoEmergenciaService;
import com.onest.app.catalog.examen.service.DiagnosticoSecundarioService;
import com.onest.app.catalog.examen.service.ExamenDocumentoService;
import com.onest.app.catalog.examen.service.ExamenService;
import com.onest.app.catalog.nss.service.NssSearchService;
import com.onest.app.catalog.restriccion.service.RestriccionService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final RestriccionService restriccionService;
    private final NssSearchService nssSearchService;
    private final ExamenDocumentoService examenDocumentoService;
    private final org.thymeleaf.spring6.SpringTemplateEngine templateEngine;

    public ExamenController(
            ExamenService examenService,
            ContactoEmergenciaService contactoEmergenciaService,
            DiagnosticoSecundarioService diagnosticoSecundarioService,
            RestriccionService restriccionService,
            NssSearchService nssSearchService,
            ExamenDocumentoService examenDocumentoService,
            org.thymeleaf.spring6.SpringTemplateEngine templateEngine) {
        this.examenService = examenService;
        this.contactoEmergenciaService = contactoEmergenciaService;
        this.diagnosticoSecundarioService = diagnosticoSecundarioService;
        this.restriccionService = restriccionService;
        this.nssSearchService = nssSearchService;
        this.examenDocumentoService = examenDocumentoService;
        this.templateEngine = templateEngine;
    }

    /**
     * "Imprimir expediente" (menu Examen medico del PHP -> pdf/pdfGenerator.php?nss=): el mismo
     * documento de 15 paginas del legacy (consentimiento, aviso de privacidad, examen FT-SO-04),
     * armado por ExamenDocumentoService con la logica del PHP. El PDF lo genera el navegador
     * (window.print), igual que el legacy; {@code auto=1} lanza el dialogo al cargar.
     */
    @GetMapping(path = "/examen/imprimir", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Examenes", accion = "export", entidad = "Expediente impreso", registro = "nss")
    public String imprimir(@RequestParam("nss") String nss,
                           @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        if (nssLimpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss es obligatorio");
        }
        model.addAttribute("d", examenDocumentoService.armar(nssLimpio));
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/examen-documento";
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
            model.addAttribute("catalogoRestricciones", restriccionService.catalogo());
            model.addAttribute("restricciones", restriccionService.byNss(nss));
            model.addAttribute("firmaGuardada", examenService.firmaGuardada(nss));
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
     * Todas las secciones en UNA peticion, para "Abrir todo": antes eran 46 POST a
     * /examen/seccion (y 46 lecturas del WS). Devuelve {seccion: html del fragmento}.
     */
    @PostMapping(
            path = "/examen/secciones",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, String> secciones(@RequestParam("nss") String nss) {
        Map<String, String> html = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.List<com.onest.app.catalog.examen.dto.ExamItem>> e : examenService.itemsDeTodas(nss).entrySet()) {
            org.thymeleaf.context.Context ctx = new org.thymeleaf.context.Context(new java.util.Locale("es", "MX"));
            ctx.setVariable("seccion", e.getKey());
            ctx.setVariable("items", e.getValue());
            html.put(e.getKey(), templateEngine.process("fragments/examen-seccion", java.util.Set.of("seccion"), ctx));
        }
        return html;
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
    @Auditado(modulo = "Examenes", accion = "create", entidad = "Examen medico", registro = "nss")
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
     * Guarda los contactos de emergencia (EAV via SERV_MED_TAG, independiente del examen -> WS).
     * Recibe los 3x5 campos (contactoEmer{campo}{indice}).
     */
    @PostMapping(
            path = "/examen/contactos-emergencia/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Examenes", accion = "update", entidad = "Contactos de emergencia", registro = "nss")
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
     * Guarda los diagnosticos secundarios (EAV via SERV_MED_TAG, independiente del examen -> WS).
     * Recibe los 3 campos (diagnosticoSecundario{indice}).
     */
    @PostMapping(
            path = "/examen/diagnosticos-secundarios/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Examenes", accion = "update", entidad = "Diagnosticos secundarios", registro = "nss")
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
