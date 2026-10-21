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
    private final com.onest.app.catalog.formato.service.FormatosImpresosService formatos;
    private final com.onest.app.catalog.examen.service.CertificacionAptitudService certificacion;
    private final com.onest.app.catalog.predio.service.PredioService predios;

    /**
     * FT-SO-31 Dictamen de certificacion de aptitud medica laboral (20-oct-2026, formato B "opcional"):
     * pagina con la captura (arriba, solo en pantalla), la lista de dictamenes anteriores (historial que
     * pidio SM) y el formato del dictamen elegido o del ultimo (abajo, lo que se imprime). Bajo
     * /api/nss/examen para heredar el permiso del modulo.
     */
    @GetMapping(path = "/examen/certificacion", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Examenes", accion = "export", entidad = "Dictamen de certificacion FT-SO-31", registro = "nss")
    public String certificacion(@RequestParam("nss") String nss,
                                @RequestParam(name = "exp", required = false) String exp,
                                @RequestParam(name = "auto", required = false) String auto,
                                @RequestParam(name = "error", required = false) String error, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        if (nssLimpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss es obligatorio");
        }
        ExamenDocumentoService.Documento d = examenDocumentoService.armar(nssLimpio);
        Map<String, String> c = certificacion.leer(nssLimpio, exp);
        Map<String, String> ultimo = exp == null || exp.isBlank() ? c : certificacion.leer(nssLimpio, null);
        model.addAttribute("d", d);
        model.addAttribute("c", c);
        model.addAttribute("ultimo", ultimo);
        model.addAttribute("expedido", !c.isEmpty());
        model.addAttribute("anteriores", certificacion.expediciones(nssLimpio));
        model.addAttribute("trabajos", com.onest.app.catalog.examen.service.CertificacionAptitudService.TRABAJOS);
        // centro de trabajo = CEDIS / cuenta (respuesta de SM 20-oct-2026, ej. "Charcon / Diageo"): el del
        // ultimo dictamen o, la primera vez, el predio asignado a la cuenta + la cuenta (editable)
        model.addAttribute("centro", ultimo.containsKey("CENTRO") ? ultimo.get("CENTRO") : centroDeTrabajo(d.v("cuenta")));
        model.addAttribute("nss", nssLimpio);
        model.addAttribute("error", error);
        model.addAttribute("enc", formatos.encabezado("FT-SO-31"));
        if (c.isEmpty()) {
            // sin expedir no hay documento que firmar: solo nombre y cedula
            model.addAttribute("medico", formatos.medicoResponsable());
        } else {
            formatos.aplicarFirmas(model, "FT-SO-31", registroCertificacion(nssLimpio, c.get("ID")), nssLimpio);
        }
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto) && !c.isEmpty());
        return "pages/formato-ft-so-31";
    }

    /** "CEDIS / cuenta" con el predio asignado a la cuenta en /admin/predios; solo la cuenta si no tiene. */
    private String centroDeTrabajo(String cuenta) {
        String cta = cuenta == null ? "" : cuenta.trim();
        try {
            return predios.predioFinoPorCuenta(cta).map(p -> p.predioNombre() + " / " + cta).orElse(cta);
        } catch (RuntimeException ex) {
            return cta;
        }
    }

    /** Expide el FT-SO-31 (guarda) y vuelve a la pagina imprimiendo; con faltantes vuelve con el motivo. */
    @PostMapping(path = "/examen/certificacion")
    @Auditado(modulo = "Examenes", accion = "update", entidad = "Dictamen de certificacion FT-SO-31", registro = "nss")
    public String expedirCertificacion(@RequestParam MultiValueMap<String, String> form) {
        String nss = form.getFirst("nss") == null ? "" : form.getFirst("nss").trim();
        if (nss.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss es obligatorio");
        }
        Map<String, String> aptitud = new LinkedHashMap<>();
        Map<String, String> especificar = new LinkedHashMap<>();
        for (int i = 0; i < com.onest.app.catalog.examen.service.CertificacionAptitudService.TRABAJOS.size(); i++) {
            aptitud.put(String.valueOf(i), form.getFirst("aptitud" + i));
            especificar.put(String.valueOf(i), form.getFirst("especificar" + i));
        }
        String base = "redirect:/api/nss/examen/certificacion?nss=" + java.net.URLEncoder.encode(nss, java.nio.charset.StandardCharsets.UTF_8);
        try {
            // cada expedicion se guarda aparte: los dictamenes anteriores quedan en el historial
            String id = certificacion.expedir(nss, aptitud, especificar, form.getFirst("centro"));
            // quien expide es quien elaboro este dictamen (segunda firma)
            formatos.registrarAutor(registroCertificacion(nss, id), nss);
            return base + "&exp=" + id + "&auto=1";
        } catch (IllegalArgumentException ex) {
            return base + "&error=" + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /** Documento para las firmas del FT-SO-31: NSS + id de la expedicion (fecha y hora). */
    private static String registroCertificacion(String nss, String id) {
        return "FTSO31:" + nss + ":" + (id == null ? "" : id);
    }

    /** Documento para las firmas de los impresos del examen: NSS + fecha del examen guardado. */
    private static String registroExamen(String nss, ExamenDocumentoService.Documento d) {
        return "EXAMEN:" + nss + ":" + d.v("FECHA_EXAMEN");
    }

    public ExamenController(
            ExamenService examenService,
            ContactoEmergenciaService contactoEmergenciaService,
            DiagnosticoSecundarioService diagnosticoSecundarioService,
            DatosGeneralesExamenService datosGeneralesExamenService,
            RestriccionService restriccionService,
            NssSearchService nssSearchService,
            ExamenDocumentoService examenDocumentoService,
            org.thymeleaf.spring6.SpringTemplateEngine templateEngine,
            com.onest.app.catalog.examen.service.PrecargaFormato precarga,
            com.onest.app.catalog.formato.service.FormatosImpresosService formatos,
            com.onest.app.catalog.examen.service.CertificacionAptitudService certificacion,
            com.onest.app.catalog.predio.service.PredioService predios) {
        this.certificacion = certificacion;
        this.predios = predios;
        this.examenService = examenService;
        this.contactoEmergenciaService = contactoEmergenciaService;
        this.diagnosticoSecundarioService = diagnosticoSecundarioService;
        this.datosGeneralesExamenService = datosGeneralesExamenService;
        this.restriccionService = restriccionService;
        this.nssSearchService = nssSearchService;
        this.examenDocumentoService = examenDocumentoService;
        this.templateEngine = templateEngine;
        this.precarga = precarga;
        this.formatos = formatos;
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
        ExamenDocumentoService.Documento d = examenDocumentoService.armar(nssLimpio,
                tipo != null && datosGeneralesExamenService.tipos().containsKey(tipo.trim()) ? tipo.trim() : null);
        model.addAttribute("d", d);
        // firma del medico y de quien elaboro (modulo de firmas, 20-oct-2026); antes era la imagen publica
        // static/img/examen/firmaDoc.png heredada del PHP, que se quito del proyecto
        formatos.aplicarFirmas(model, "EXAMEN", registroExamen(nssLimpio, d), nssLimpio);
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/examen-documento";
    }

    /**
     * FT-SO-25 Carta compromiso (20-oct-2026, formato B "por condicion"): solo si el examen guardado
     * tiene dictamen Apto condicionado (45) o Apto restringido (60), como pidio Servicio Medico. Bajo
     * /api/nss/examen para heredar el permiso del modulo (ClinicalAccessFilter).
     */
    @GetMapping(path = "/examen/carta-compromiso", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Examenes", accion = "export", entidad = "Carta compromiso FT-SO-25", registro = "nss")
    public String cartaCompromiso(@RequestParam("nss") String nss,
                                  @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        if (nssLimpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss es obligatorio");
        }
        ExamenDocumentoService.Documento d = examenDocumentoService.armar(nssLimpio);
        String error = errorCarta(d);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        model.addAttribute("d", d);
        model.addAttribute("enc", formatos.encabezado("FT-SO-25"));
        formatos.aplicarFirmas(model, "FT-SO-25", registroExamen(nssLimpio, d), nssLimpio);
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/formato-ft-so-25";
    }

    /**
     * Validacion previa de la carta, en texto, para que la pantalla muestre el motivo junto al boton
     * (y no en una pestana nueva con la pagina de error).
     */
    @GetMapping(path = "/examen/carta-compromiso/validar", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> validarCartaCompromiso(@RequestParam("nss") String nss) {
        String nssLimpio = nss == null ? "" : nss.trim();
        if (nssLimpio.isEmpty()) {
            return org.springframework.http.ResponseEntity.badRequest().body("NSS obligatorio.");
        }
        String error = errorCarta(examenDocumentoService.armar(nssLimpio));
        return error == null ? org.springframework.http.ResponseEntity.ok("ok")
                : org.springframework.http.ResponseEntity.badRequest().body(error);
    }

    /** null si el examen guardado permite la carta; si no, el motivo para el usuario. */
    private static String errorCarta(ExamenDocumentoService.Documento d) {
        String dictamen = d.v("DICTAMEN");
        if ("Apto condicionado".equals(dictamen) || "Apto restringido".equals(dictamen)) {
            return null;
        }
        return "La carta compromiso solo aplica con dictamen Apto condicionado o Apto restringido. Dictamen guardado: "
                + (dictamen.isEmpty() ? "ninguno" : dictamen) + ". Si lo acabas de cambiar, guarda el examen primero.";
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
            // Trabajos previos (20-oct-2026): precarga desde SERV_MED_TAG. Si no se pueden leer, la lista
            // llega vacia y SIN la marca de precarga, asi el guardado no los borra (ver ExamenService).
            java.util.List<Map<String, String>> trabajos = null;
            try {
                trabajos = examenService.trabajosPrevios(nss);
            } catch (RuntimeException ex) {
                // queda null: sin precarga ni marca
            }
            model.addAttribute("trabajosPrevios", trabajos == null ? java.util.List.of() : trabajos);
            model.addAttribute("trabajosPrecargados", trabajos != null);
            model.addAttribute("camposTrabajo", ExamenService.CAMPOS_TRABAJO);
            model.addAttribute("maxTrabajos", ExamenService.MAX_TRABAJOS);
            model.addAttribute("marcaTrabajos", ExamenService.MARCA_TRABAJOS);
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
