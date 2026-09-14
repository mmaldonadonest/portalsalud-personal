package com.onest.app.catalog.expediente.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.causaconsulta.service.CausaConsultaService;
import com.onest.app.catalog.expediente.service.DocumentoImpresoService;
import com.onest.app.catalog.expediente.service.ExpedienteService;
import com.onest.app.catalog.file.service.FileStoreService;
import com.onest.app.catalog.nss.service.NssSearchService;
import java.util.List;
import java.util.UUID;
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

    private final ExpedienteService expedienteService;
    private final FileStoreService fileStoreService;
    private final CausaConsultaService causaConsultaService;
    private final NssSearchService nssSearchService;

    public ExpedienteController(
            ExpedienteService expedienteService, FileStoreService fileStoreService,
            CausaConsultaService causaConsultaService, NssSearchService nssSearchService) {
        this.expedienteService = expedienteService;
        this.fileStoreService = fileStoreService;
        this.causaConsultaService = causaConsultaService;
        this.nssSearchService = nssSearchService;
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
        model.addAttribute("ahora", DocumentoImpresoService.ahora());
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/consulta-documento";
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
            String proceso = expedienteService.crearConsulta(form);
            return (proceso == null || proceso.isBlank()) ? "Consulta registrada." : proceso;
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
