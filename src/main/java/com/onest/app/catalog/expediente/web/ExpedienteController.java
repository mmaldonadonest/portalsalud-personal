package com.onest.app.catalog.expediente.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.causaconsulta.service.CausaConsultaService;
import com.onest.app.catalog.dashboard.repository.VistaAtencionRepository;
import com.onest.app.catalog.expediente.service.DocumentoImpresoService;
import com.onest.app.catalog.expediente.service.ExpedienteService;
import com.onest.app.catalog.file.service.FileStoreService;
import com.onest.app.catalog.nss.service.NssSearchService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Expediente general (U04). Replica viewExpediente + expedienteShowConsults?type=expedienteConsulta:
 * XHR POST con el NSS ('data') y respuesta HTML (tabla de consultas) que el frontend inyecta
 * en el area de contenido del modulo.
 */
@Controller
@RequestMapping("/api/nss")
public class ExpedienteController {

    private static final Logger log = LoggerFactory.getLogger(ExpedienteController.class);

    private final ExpedienteService expedienteService;
    private final FileStoreService fileStoreService;
    private final CausaConsultaService causaConsultaService;
    private final NssSearchService nssSearchService;
    // Solo existe con portal.historico.fuente=VISTA: entonces el expediente muestra tambien lo del Excel
    private final ObjectProvider<VistaAtencionRepository> vistaAtenciones;
    private final com.onest.app.catalog.expediente.service.PaseSalidaService paseSalida;
    private final com.onest.app.catalog.formato.service.FormatosImpresosService formatos;
    private final com.onest.app.catalog.examen.service.ExamenDocumentoService examenDocumento;
    private final com.onest.app.catalog.expediente.service.FormatosConsultaService formatosConsulta;

    public ExpedienteController(
            ExpedienteService expedienteService, FileStoreService fileStoreService,
            CausaConsultaService causaConsultaService, NssSearchService nssSearchService,
            ObjectProvider<VistaAtencionRepository> vistaAtenciones,
            com.onest.app.catalog.expediente.service.PaseSalidaService paseSalida,
            com.onest.app.catalog.formato.service.FormatosImpresosService formatos,
            com.onest.app.catalog.examen.service.ExamenDocumentoService examenDocumento,
            com.onest.app.catalog.expediente.service.FormatosConsultaService formatosConsulta) {
        this.formatosConsulta = formatosConsulta;
        this.expedienteService = expedienteService;
        this.fileStoreService = fileStoreService;
        this.causaConsultaService = causaConsultaService;
        this.nssSearchService = nssSearchService;
        this.vistaAtenciones = vistaAtenciones;
        this.paseSalida = paseSalida;
        this.formatos = formatos;
        this.examenDocumento = examenDocumento;
    }

    /**
     * Histórico del Excel (layout 1 ATENCIONES por NSS) para la tarjeta "Histórico Excel" del
     * expediente. Sin el interruptor VISTA no se agrega nada. Si la vista falla, el modulo sigue con
     * lo del portal y la tarjeta avisa: el historico es un complemento, no debe tumbar el expediente.
     */
    private void agregarHistorico(String nss, Model model) {
        VistaAtencionRepository vista = vistaAtenciones.getIfAvailable();
        if (vista == null || nss.isEmpty()) {
            return;
        }
        model.addAttribute("conHistorico", true);
        try {
            model.addAttribute("historico", vista.historicoPorNss(nss));
        } catch (DataAccessException ex) {
            log.warn("[expediente] historico de atenciones no disponible para el NSS: {}", ex.getMessage());
            model.addAttribute("historico", List.of());
            model.addAttribute("historicoError", true);
        }
    }

    @PostMapping(
            path = "/expediente",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String expediente(@RequestParam("data") String data, Model model) {
        try {
            String nss = data == null ? "" : data.trim();
            model.addAttribute("consultas", expedienteService.consultasByNss(data));
            model.addAttribute("nss", nss);
            model.addAttribute("cuenta", expedienteService.cuentaDe(nss));
            agregarHistorico(nss, model);
            return "fragments/expediente-consultas :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Detalle de una consulta. Replica viewConsultaDetails.php (data=nss, serieId=id_consulta).
     */
    @PostMapping(
            path = "/expediente/detalle",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String consultaDetalle(
            @RequestParam("data") String data,
            @RequestParam("serieId") String serieId,
            Model model) {
        try {
            var detalle = expedienteService.consultaDetalle(data, serieId).orElse(null);
            model.addAttribute("detalle", detalle);
            model.addAttribute("esPase", detalle != null
                    && com.onest.app.catalog.expediente.service.PaseSalidaService.esPase(detalle.tipoConsulta()));
            // FT-SO-29 y FT-SO-23: el boton sale solo si al dar de alta se genero ese formato
            String nssDet = data == null ? "" : data.trim();
            String relDet = detalle == null ? null : detalle.consultaRelacionada();
            var medDet = detalle == null ? java.util.Map.<String, String>of() : camposFormato(
                    com.onest.app.catalog.expediente.service.FormatosConsultaService.MEDICAMENTO, nssDet, relDet);
            var pruebaDet = detalle == null ? java.util.Map.<String, String>of() : camposFormato(
                    com.onest.app.catalog.expediente.service.FormatosConsultaService.PRUEBAS, nssDet, relDet);
            model.addAttribute("tieneMedicamento", !medDet.isEmpty());
            model.addAttribute("tienePruebas", !pruebaDet.isEmpty());
            // para verlos en el detalle sin imprimir (SM: el resultado de la prueba se guarda "para visualizacion")
            model.addAttribute("medDet", medDet);
            model.addAttribute("medicamentosDet", com.onest.app.catalog.expediente.service.FormatosConsultaService.medicamentosGuardados(medDet));
            model.addAttribute("pruebaDet", pruebaDet);
            model.addAttribute("nss", data == null ? "" : data.trim());
            model.addAttribute("serieId", serieId);
            model.addAttribute("adjuntos",
                    detalle != null ? fileStoreService.listByRelacion(detalle.consultaRelacionada()) : java.util.List.of());
            return "fragments/consulta-detalle :: detalle";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Nota medica imprimible de una consulta. El PDF lo genera el navegador; {@code auto=1}
     * abre el dialogo al cargar. Ver docs/plan-impresion-pretest-incapacidad-consulta.md.
     */
    @GetMapping(path = "/consulta/imprimir", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Consultas", accion = "export", entidad = "Nota medica impresa", registro = "nss")
    public String consultaImprimir(
            @RequestParam("nss") String nss,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "rel", required = false) String rel,
            @RequestParam(name = "auto", required = false) String auto,
            Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        boolean conId = id != null && !id.isBlank();
        if (nssLimpio.isEmpty() || (!conId && (rel == null || rel.isBlank()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss y (id o rel) son obligatorios");
        }
        // id = consulta conocida (boton Imprimir del detalle); rel = idArchivoRel del alta recien
        // guardada (el WS no devuelve el ID_CONSULTA que asigno).
        var detalle = (conId ? expedienteService.consultaDetalle(nssLimpio, id.trim())
                : DocumentoImpresoService.porRelacion(
                        expedienteService.consultasByNss(nssLimpio).stream().map(c -> c.idConsulta()).toList(), rel,
                        i -> expedienteService.consultaDetalle(nssLimpio, i), d -> d.consultaRelacionada()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la consulta " + (conId ? id : rel)));
        var empleado = DocumentoImpresoService.empleado(nssSearchService, nssLimpio);
        model.addAttribute("nss", nssLimpio);
        model.addAttribute("detalle", DocumentoImpresoService.limpio(detalle));
        model.addAttribute("adjuntos", detalle.consultaRelacionada() == null || detalle.consultaRelacionada().isBlank()
                ? List.of() : fileStoreService.listByRelacion(detalle.consultaRelacionada()));
        model.addAttribute("empleado", empleado);
        model.addAttribute("nombreTrabajador", DocumentoImpresoService.nombreCompleto(empleado, nssLimpio));
        model.addAttribute("firmaTrabajador", DocumentoImpresoService.firma(detalle.firmaDigital()));
        model.addAttribute("capturo", DocumentoImpresoService.usuarioActual());
        // firmas del medico y de quien elaboro (modulo de firmas, 20-oct-2026)
        formatos.aplicarFirmas(model, "NOTA-CONSULTA", "CONSULTA:" + detalle.consultaRelacionada(), nssLimpio);
        model.addAttribute("ahora", DocumentoImpresoService.ahora());
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/consulta-documento";
    }

    /**
     * FT-SO-02 Pase de salida (rev. 05, 20-oct-2026, formato B "opcional"): solo para consultas de tipo
     * pase de salida. Igual que la nota medica, {@code id} = consulta conocida (detalle) y {@code rel} =
     * alta recien guardada. Lo propio del pase sale de PaseSalidaService; en consultas anteriores al
     * 20-oct esos campos salen en blanco para llenarse a mano.
     */
    @GetMapping(path = "/consulta/pase-salida", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Consultas", accion = "export", entidad = "Pase de salida FT-SO-02", registro = "nss")
    public String paseSalida(
            @RequestParam("nss") String nss,
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "rel", required = false) String rel,
            @RequestParam(name = "auto", required = false) String auto,
            Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        boolean conId = id != null && !id.isBlank();
        if (nssLimpio.isEmpty() || (!conId && (rel == null || rel.isBlank()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss y (id o rel) son obligatorios");
        }
        var detalle = (conId ? expedienteService.consultaDetalle(nssLimpio, id.trim())
                : DocumentoImpresoService.porRelacion(
                        expedienteService.consultasByNss(nssLimpio).stream().map(c -> c.idConsulta()).toList(), rel,
                        i -> expedienteService.consultaDetalle(nssLimpio, i), d -> d.consultaRelacionada()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la consulta " + (conId ? id : rel)));
        if (!com.onest.app.catalog.expediente.service.PaseSalidaService.esPase(detalle.tipoConsulta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pase de salida solo aplica a consultas de tipo Pase de salida.");
        }
        var empleado = DocumentoImpresoService.empleado(nssSearchService, nssLimpio);
        // agencia como en los formatos del examen: base (OCR > biometrico) > elegida en el examen > GLI
        String agencia = examenDocumento.agenciaDeBase(nssLimpio);
        if (agencia.isEmpty()) {
            agencia = examenDocumento.agenciaCapturada(nssLimpio);
        }
        model.addAttribute("nss", nssLimpio);
        model.addAttribute("detalle", DocumentoImpresoService.limpio(detalle));
        // fechas capturadas en ISO (input date) -> dd/MM/yyyy como el resto del impreso
        java.util.Map<String, String> pase = new java.util.LinkedHashMap<>();
        paseSalida.leer(nssLimpio, detalle.consultaRelacionada()).forEach((k, v) -> pase.put(k, DocumentoImpresoService.fecha(v)));
        model.addAttribute("pase", pase);
        model.addAttribute("empleado", empleado);
        model.addAttribute("nombreTrabajador", DocumentoImpresoService.nombreCompleto(empleado, nssLimpio));
        model.addAttribute("agencia", agencia.isEmpty() ? com.onest.app.catalog.examen.service.ExamenDocumentoService.AGENCIA_PREDETERMINADA : agencia);
        model.addAttribute("firmaTrabajador", DocumentoImpresoService.firma(detalle.firmaDigital()));
        model.addAttribute("enc", formatos.encabezado("FT-SO-02"));
        formatos.aplicarFirmas(model, "FT-SO-02", "CONSULTA:" + detalle.consultaRelacionada(), nssLimpio);
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/formato-ft-so-02";
    }

    /** Consulta por id (detalle) o por idArchivoRel (alta recien guardada); 400/404 si no hay. */
    private com.onest.app.catalog.expediente.dto.ConsultaDetalleDto consultaPorIdORel(String nss, String id, String rel) {
        boolean conId = id != null && !id.isBlank();
        if (nss.isEmpty() || (!conId && (rel == null || rel.isBlank()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nss y (id o rel) son obligatorios");
        }
        return (conId ? expedienteService.consultaDetalle(nss, id.trim())
                : DocumentoImpresoService.porRelacion(
                        expedienteService.consultasByNss(nss).stream().map(c -> c.idConsulta()).toList(), rel,
                        i -> expedienteService.consultaDetalle(nss, i), d -> d.consultaRelacionada()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la consulta " + (conId ? id : rel)));
    }

    /** Lo comun de los formatos sueltos de la consulta (ficha, agencia, encabezado, medico). */
    private void modeloFormatoConsulta(Model model, String nss, com.onest.app.catalog.expediente.dto.ConsultaDetalleDto detalle,
                                       String clave, String auto) {
        var empleado = DocumentoImpresoService.empleado(nssSearchService, nss);
        String agencia = examenDocumento.agenciaDeBase(nss);
        if (agencia.isEmpty()) {
            agencia = examenDocumento.agenciaCapturada(nss);
        }
        model.addAttribute("nss", nss);
        model.addAttribute("detalle", DocumentoImpresoService.limpio(detalle));
        model.addAttribute("empleado", empleado);
        model.addAttribute("nombreTrabajador", DocumentoImpresoService.nombreCompleto(empleado, nss));
        model.addAttribute("agencia", agencia.isEmpty() ? com.onest.app.catalog.examen.service.ExamenDocumentoService.AGENCIA_PREDETERMINADA : agencia);
        model.addAttribute("firmaTrabajador", DocumentoImpresoService.firma(detalle.firmaDigital()));
        model.addAttribute("enc", formatos.encabezado(clave));
        formatos.aplicarFirmas(model, clave, "CONSULTA:" + detalle.consultaRelacionada(), nss);
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
    }

    /** Campos del formato con las fechas ISO capturadas (input date) en dd/MM/yyyy. */
    private java.util.Map<String, String> camposFormato(String formato, String nss, String rel) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        formatosConsulta.leer(formato, nss, rel).forEach((k, v) -> m.put(k, DocumentoImpresoService.fecha(v)));
        return m;
    }

    /**
     * FT-SO-29 Pase de medicamento (rev. 04, 20-oct-2026, formato B "opcional"): sale de una consulta en
     * la que se marco "Generar pase de medicamento". Padecimiento = diagnostico de la consulta.
     */
    @GetMapping(path = "/consulta/pase-medicamento", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Consultas", accion = "export", entidad = "Pase de medicamento FT-SO-29", registro = "nss")
    public String paseMedicamento(@RequestParam("nss") String nss,
                                  @RequestParam(name = "id", required = false) String id,
                                  @RequestParam(name = "rel", required = false) String rel,
                                  @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        var detalle = consultaPorIdORel(nssLimpio, id, rel);
        var med = camposFormato(com.onest.app.catalog.expediente.service.FormatosConsultaService.MEDICAMENTO, nssLimpio, detalle.consultaRelacionada());
        if (med.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta consulta no tiene pase de medicamento.");
        }
        modeloFormatoConsulta(model, nssLimpio, detalle, "FT-SO-29", auto);
        model.addAttribute("med", med);
        model.addAttribute("medicamentos", com.onest.app.catalog.expediente.service.FormatosConsultaService.medicamentosGuardados(med));
        return "pages/formato-ft-so-29";
    }

    /**
     * FT-SO-23 Carta de consentimiento para pruebas especiales (rev. 05, 20-oct-2026, formato B
     * "opcional"): sale de una consulta en la que se marco "Generar carta de pruebas especiales".
     */
    @GetMapping(path = "/consulta/pruebas-especiales", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Consultas", accion = "export", entidad = "Carta pruebas especiales FT-SO-23", registro = "nss")
    public String pruebasEspeciales(@RequestParam("nss") String nss,
                                    @RequestParam(name = "id", required = false) String id,
                                    @RequestParam(name = "rel", required = false) String rel,
                                    @RequestParam(name = "auto", required = false) String auto, Model model) {
        String nssLimpio = nss == null ? "" : nss.trim();
        var detalle = consultaPorIdORel(nssLimpio, id, rel);
        var prueba = camposFormato(com.onest.app.catalog.expediente.service.FormatosConsultaService.PRUEBAS, nssLimpio, detalle.consultaRelacionada());
        if (prueba.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta consulta no tiene carta de pruebas especiales.");
        }
        modeloFormatoConsulta(model, nssLimpio, detalle, "FT-SO-23", auto);
        model.addAttribute("prueba", prueba);
        return "pages/formato-ft-so-23";
    }

    /**
     * Formulario de alta de consulta medica (viewConsulMedic -> consultaMedica.php).
     * Genera el id de relacion consulta<->adjuntos (como el md5 del legacy).
     */
    @PostMapping(
            path = "/consulta/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String consultaForm(@RequestParam("data") String data, Model model) {
        model.addAttribute("nss", data == null ? "" : data.trim());
        model.addAttribute("idArchivoRel", UUID.randomUUID().toString().replace("-", ""));
        model.addAttribute("causas", causaConsultaService.listar(true));
        return "fragments/consulta-form :: form";
    }

    /**
     * Guarda la consulta (consultMedic.php -> addConsultM -> /Servcio/consulta).
     * Devuelve el mensaje Proceso del WS como texto plano.
     */
    @PostMapping(
            path = "/consulta",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Consultas", accion = "create", entidad = "Consulta", registro = "nss", detalle = {"tipoConsulta", "causa"})
    public String consultaAlta(@ModelAttribute ConsultaAltaForm form) {
        try {
            String faltante = com.onest.app.catalog.expediente.service.PaseSalidaService.errorObligatorios(form);
            if (faltante == null) {
                faltante = com.onest.app.catalog.expediente.service.FormatosConsultaService.errorObligatorios(form);
            }
            if (faltante != null) {
                throw new IllegalArgumentException(faltante);
            }
            String proceso = expedienteService.crearConsulta(form);
            String mensaje = (proceso == null || proceso.isBlank()) ? "Consulta registrada." : proceso;
            // quien captura la consulta es quien elaboro sus documentos (segunda firma de nota, pases y cartas)
            if (form.getIdArchivoRel() != null && !form.getIdArchivoRel().isBlank()) {
                formatos.registrarAutor("CONSULTA:" + form.getIdArchivoRel().trim(), form.getNss() == null ? null : form.getNss().trim());
            }
            // el pase va despues del WS: si el WS falla no queda pase huerfano; si falla el pase, la
            // consulta ya quedo y solo se avisa (el FT-SO-02 saldria con esos campos en blanco)
            try {
                paseSalida.guardar(form);
            } catch (RuntimeException ex) {
                log.error("[pase-salida] NSS {}", form.getNss(), ex);
                mensaje += " Aviso: no se guardaron los datos del pase de salida (FT-SO-02).";
            }
            try {
                formatosConsulta.guardar(form);
            } catch (RuntimeException ex) {
                log.error("[formatos-consulta] NSS {}", form.getNss(), ex);
                mensaje += " Aviso: no se guardaron los datos del pase de medicamento o de pruebas especiales.";
            }
            return mensaje;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Busqueda de claves ICD (showIcd.php -> app::showICD -> /Servcio/indice).
     * Devuelve un fragmento con las claves que se agregan al diagnostico.
     */
    @PostMapping(
            path = "/icd",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String icd(@RequestParam(name = "icd", required = false, defaultValue = "") String icd, Model model) {
        model.addAttribute("claves", expedienteService.buscarIcd(icd));
        return "fragments/icd-result :: table";
    }
}
