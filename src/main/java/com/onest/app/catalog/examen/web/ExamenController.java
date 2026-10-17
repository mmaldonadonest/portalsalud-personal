package com.onest.app.catalog.examen.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.examen.service.ContactoEmergenciaService;
import com.onest.app.catalog.examen.service.DatosGeneralesExamenService;
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
    private final DatosGeneralesExamenService datosGeneralesExamenService;
    private final RestriccionService restriccionService;
    private final NssSearchService nssSearchService;
    private final ExamenDocumentoService examenDocumentoService;
    private final org.thymeleaf.spring6.SpringTemplateEngine templateEngine;
    private final com.onest.app.catalog.examen.service.PrecargaFormato precarga;

    public ExamenController(
            ExamenService examenService,
            ContactoEmergenciaService contactoEmergenciaService,
            DiagnosticoSecundarioService diagnosticoSecundarioService,
            DatosGeneralesExamenService datosGeneralesExamenService,
            RestriccionService restriccionService,
            NssSearchService nssSearchService,
            ExamenDocumentoService examenDocumentoService,
            org.thymeleaf.spring6.SpringTemplateEngine templateEngine,
            com.onest.app.catalog.examen.service.PrecargaFormato precarga) {
        this.examenService = examenService;
        this.contactoEmergenciaService = contactoEmergenciaService;
        this.diagnosticoSecundarioService = diagnosticoSecundarioService;
        this.datosGeneralesExamenService = datosGeneralesExamenService;
        this.restriccionService = restriccionService;
        this.nssSearchService = nssSearchService;
        this.examenDocumentoService = examenDocumentoService;
        this.templateEngine = templateEngine;
        this.precarga = precarga;
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
                           @RequestParam(name = "auto", required = false) String auto,
                           @RequestParam(name = "tipo", required = false) String tipo, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        if (nssLimpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss es obligatorio");
        }
        // ?tipo=N: vista previa del impreso como si fuera de ese tipo de examen (solo lectura)
        model.addAttribute("d", examenDocumentoService.armar(nssLimpio,
                tipo != null && datosGeneralesExamenService.tipos().containsKey(tipo.trim()) ? tipo.trim() : null));
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
            model.addAttribute("datosGenerales", datosGeneralesExamenService.cargar(nss));
            model.addAttribute("tiposExamen", datosGeneralesExamenService.tipos());
            // formato EMLE de cada tipo, para mostrarlo junto al tipo (y cambiarlo en vivo al guardar otro tipo)
            Map<String, String> formatos = new LinkedHashMap<>();
            datosGeneralesExamenService.tipos().keySet().forEach(t -> formatos.put(t,
                    com.onest.app.catalog.examen.service.ExamenDocumentoService.claveFormato(t)));
            model.addAttribute("formatosExamen", formatos);
            // AGENCIA de los formatos EMLE (16-oct-2026): igual que el examen inicial, la de la base es
            // de solo lectura; sin ella, GLI predeterminada u "Otro" especificando (tag AGENCIAEXAMEN)
            model.addAttribute("agenciaBase", examenDocumentoService.agenciaDeBase(nss));
            model.addAttribute("agenciaElegida", examenDocumentoService.agenciaCapturada(nss));
            model.addAttribute("agenciaPredeterminada", ExamenDocumentoService.AGENCIA_PREDETERMINADA);
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
            java.util.List<com.onest.app.catalog.examen.dto.ExamItem> items =
                    precarga.aplicar(seccion, examenService.itemsDeSeccion(seccion, nss), nss);
            model.addAttribute("items", items);
            model.addAttribute("valores", valores(items));
            // ayudas del formato EMLE (FT-SO-17 / FT-SO-04) para la exploracion
            model.addAttribute("ayudas", com.onest.app.catalog.examen.service.AyudasFormato.AYUDAS);
            model.addAttribute("conAyuda", com.onest.app.catalog.examen.service.AyudasFormato.alguno(items));
            return "fragments/examen-seccion :: seccion";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Nombre de campo -> valor, para el odontograma compartido con el examen inicial (16-oct-2026). */
    private static Map<String, String> valores(java.util.List<com.onest.app.catalog.examen.dto.ExamItem> items) {
        Map<String, String> m = new LinkedHashMap<>();
        items.forEach(it -> m.put(it.fieldName(), it.value() == null ? "" : it.value()));
        return m;
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
            java.util.List<com.onest.app.catalog.examen.dto.ExamItem> items = precarga.aplicar(e.getKey(), e.getValue(), nss);
            ctx.setVariable("seccion", e.getKey());
            ctx.setVariable("items", items);
            ctx.setVariable("valores", valores(items));
            ctx.setVariable("ayudas", com.onest.app.catalog.examen.service.AyudasFormato.AYUDAS);
            ctx.setVariable("conAyuda", com.onest.app.catalog.examen.service.AyudasFormato.alguno(items));
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
            // Examen inicial (tipo 1 Admision): el dictamen se definio al finalizarlo y OCR ya lo recibio;
            // desde aqui es de solo lectura (16-oct-2026). Si lo piden, se cambia esta regla.
            String dictamenNuevo = campos.get("SERV_MED_RESULTADO_EXAMEN.DICTAMEN");
            if (dictamenNuevo != null && "1".equals(datosGeneralesExamenService.cargar(nss).tipoExamen())) {
                String vigente = examenService.dictamenVigente(nss);
                if (!dictamenNuevo.trim().equalsIgnoreCase(vigente == null ? "" : vigente.trim())) {
                    throw new IllegalArgumentException("El dictamen del examen inicial es de solo lectura: se definió al finalizar el examen inicial.");
                }
            }
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
     * Guarda tipo de examen, SpO2, cintura y cadera (EAV via SERV_MED_TAG, independiente del
     * examen -> WS). Recibe tipoExamen, spo2, cintura, cadera.
     */
    @PostMapping(
            path = "/examen/datos-generales/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Examenes", accion = "update", entidad = "Datos generales del examen", registro = "nss")
    public String guardarDatosGenerales(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            Map<String, String> campos = new LinkedHashMap<>();
            params.forEach((key, values) -> {
                if (!"nss".equals(key) && !"_csrf".equals(key)) {
                    campos.put(key, (values == null || values.isEmpty()) ? "" : values.get(0));
                }
            });
            datosGeneralesExamenService.guardar(nss, campos);
            return "Datos generales guardados.";
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
