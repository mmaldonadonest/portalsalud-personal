package com.onest.app.catalog.incapacidad.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.expediente.service.DocumentoImpresoService;
import com.onest.app.catalog.file.service.FileStoreService;
import com.onest.app.catalog.incapacidad.service.IncapacidadService;
import com.onest.app.catalog.nss.service.NssSearchService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

/**
 * Incapacidades (U05): lista (viewArchivoInc -> expedienteShowConsults?type=expedienteIncapacidad)
 * y detalle (showDetailsIncap -> viewIncapDetails.php). Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class IncapacidadController {

    private final IncapacidadService incapacidadService;
    private final FileStoreService fileStoreService;
    private final NssSearchService nssSearchService;

    public IncapacidadController(IncapacidadService incapacidadService, FileStoreService fileStoreService,
                                 NssSearchService nssSearchService) {
        this.incapacidadService = incapacidadService;
        this.fileStoreService = fileStoreService;
        this.nssSearchService = nssSearchService;
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
            model.addAttribute("nss", data == null ? "" : data.trim());
            model.addAttribute("serieId", serieId);
            model.addAttribute("adjuntos",
                    detalle != null ? fileStoreService.listByRelacion(detalle.urlArchivos()) : List.of());
            return "fragments/incapacidad-detalle :: detalle";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Constancia interna imprimible de una incapacidad (no es el documento oficial del IMSS).
     * El PDF lo genera el navegador; {@code auto=1} abre el dialogo al cargar.
     */
    @GetMapping(path = "/incapacidad/imprimir", produces = MediaType.TEXT_HTML_VALUE)
    @Auditado(modulo = "Incapacidades", accion = "export", entidad = "Constancia impresa", registro = "nss")
    public String imprimir(
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
        // id = incapacidad conocida (boton Imprimir del detalle); rel = idArchivoRel del alta
        // recien guardada (el WS no devuelve el ID_CONSULTA que asigno).
        var detalle = (conId ? incapacidadService.detalle(nssLimpio, id.trim())
                : DocumentoImpresoService.porRelacion(
                        incapacidadService.byNss(nssLimpio).stream().map(c -> c.idConsulta()).toList(), rel,
                        i -> incapacidadService.detalle(nssLimpio, i), d -> d.urlArchivos()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la incapacidad " + (conId ? id : rel)));
        var empleado = DocumentoImpresoService.empleado(nssSearchService, nssLimpio);
        model.addAttribute("nss", nssLimpio);
        model.addAttribute("detalle", DocumentoImpresoService.limpio(detalle));
        model.addAttribute("goceSueldo", DocumentoImpresoService.siNo(detalle.goceSueldo()));
        model.addAttribute("adjuntos", detalle.urlArchivos() == null || detalle.urlArchivos().isBlank()
                ? List.of() : fileStoreService.listByRelacion(detalle.urlArchivos()));
        model.addAttribute("empleado", empleado);
        model.addAttribute("nombreTrabajador", DocumentoImpresoService.nombreCompleto(empleado, nssLimpio));
        model.addAttribute("firmaTrabajador", DocumentoImpresoService.firma(detalle.firmaDigital()));
        model.addAttribute("capturo", DocumentoImpresoService.usuarioActual());
        model.addAttribute("ahora", DocumentoImpresoService.ahora());
        model.addAttribute("auto", auto != null && !auto.isBlank() && !"0".equals(auto));
        return "pages/incapacidad-documento";
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
    @Auditado(modulo = "Incapacidades", accion = "create", entidad = "Incapacidad", registro = "nss", detalle = {"ramo", "diasAutorizados"})
    public String save(@ModelAttribute IncapacidadAltaForm form) {
        try {
            String proceso = incapacidadService.crearIncapacidad(form);
            return (proceso == null || proceso.isBlank()) ? "Incapacidad registrada." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Reporte administrativo de incapacidades por rango de fechas (todas las NSS, no solo la
     * actual). Equivale a generarExcelIncapa.php -> traerDatosGeneralIncap. No depende del NSS
     * en pantalla; vive bajo /api/nss por consistencia con el resto de los endpoints del modulo.
     */
    @PostMapping(
            path = "/incapacidades/reporte",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String reporte(
            @RequestParam("fechaInicial") String fechaInicial,
            @RequestParam("fechaFinal") String fechaFinal,
            Model model) {
        try {
            model.addAttribute("reporte", incapacidadService.reportePorFecha(fechaInicial, fechaFinal));
            model.addAttribute("fechaInicial", fechaInicial);
            model.addAttribute("fechaFinal", fechaFinal);
            return "fragments/incapacidades-reporte :: tabla";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
