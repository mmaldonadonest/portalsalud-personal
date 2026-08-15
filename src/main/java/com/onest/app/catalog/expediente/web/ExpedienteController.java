package com.onest.app.catalog.expediente.web;

import com.onest.app.catalog.expediente.service.ExpedienteService;
import com.onest.app.catalog.file.service.FileStoreService;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    public ExpedienteController(ExpedienteService expedienteService, FileStoreService fileStoreService) {
        this.expedienteService = expedienteService;
        this.fileStoreService = fileStoreService;
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
            model.addAttribute("adjuntos",
                    detalle != null ? fileStoreService.listByRelacion(detalle.consultaRelacionada()) : java.util.List.of());
            return "fragments/consulta-detalle :: detalle";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
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
    public String icd(@RequestParam("icd") String icd, Model model) {
        model.addAttribute("claves", expedienteService.buscarIcd(icd));
        return "fragments/icd-result :: table";
    }
}
