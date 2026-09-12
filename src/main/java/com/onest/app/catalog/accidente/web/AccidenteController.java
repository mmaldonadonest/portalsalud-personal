package com.onest.app.catalog.accidente.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.accidente.dto.AccidenteDto;
import com.onest.app.catalog.accidente.dto.AccidenteSeguimientoDto;
import com.onest.app.catalog.accidente.service.AccidenteService;
import com.onest.app.catalog.file.service.FileStoreService;
import java.util.List;
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
 * Accidentes de trabajo: lista y alta. Backend aplicado y verificado 2026-08-14
 * contra el WS ORDS real (docs/ords-accidentes.sql). Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class AccidenteController {

    private final AccidenteService accidenteService;
    private final FileStoreService fileStoreService;

    public AccidenteController(AccidenteService accidenteService, FileStoreService fileStoreService) {
        this.accidenteService = accidenteService;
        this.fileStoreService = fileStoreService;
    }

    @PostMapping(
            path = "/accidentes",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String accidentes(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("accidentes", accidenteService.byNssConEstado(data));
            model.addAttribute("nss", data == null ? "" : data.trim());
            return "fragments/accidentes-list :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /**
     * Detalle + seguimiento + evidencias de un caso (data=nss, serieId=id del accidente).
     * Mismo patron que ExpedienteController#consultaDetalle.
     */
    @PostMapping(
            path = "/accidentes/detalle",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String detalle(
            @RequestParam("data") String data,
            @RequestParam("serieId") String serieId,
            Model model) {
        try {
            String nss = data == null ? "" : data.trim();
            List<AccidenteDto> seguimientos = accidenteService.byNss(nss);
            AccidenteDto accidente = seguimientos.stream()
                    .filter(a -> serieId != null && serieId.equals(a.idRegistro()))
                    .findFirst()
                    .orElse(null);
            List<AccidenteSeguimientoDto> historial =
                    accidente != null ? accidenteService.seguimientos(serieId) : List.of();
            String relacionEvidencias = "accidente-" + serieId;
            model.addAttribute("nss", nss);
            model.addAttribute("accidenteRegId", serieId);
            model.addAttribute("accidente", accidente);
            model.addAttribute("historial", historial);
            model.addAttribute("estado", accidenteService.estadoDe(historial));
            model.addAttribute("relacionEvidencias", relacionEvidencias);
            model.addAttribute("evidencias", fileStoreService.listByRelacion(relacionEvidencias));
            return "fragments/accidente-detalle :: detalle";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Guarda una entrada de seguimiento (o el cierre) de un caso ya registrado. */
    @PostMapping(
            path = "/accidentes/seguimiento/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Accidentes", accion = "update", entidad = "Seguimiento de accidente", registro = "accidenteRegId", detalle = {"tipo"})
    public String guardarSeguimiento(
            @RequestParam("accidenteRegId") String accidenteRegId,
            @RequestParam("tipo") String tipo,
            @RequestParam("observaciones") String observaciones) {
        try {
            String proceso = accidenteService.crearSeguimiento(accidenteRegId, tipo, observaciones);
            return (proceso == null || proceso.isBlank()) ? "Seguimiento registrado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Formulario de alta de accidente de trabajo. */
    @PostMapping(
            path = "/accidentes/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        model.addAttribute("nss", data == null ? "" : data.trim());
        return "fragments/accidentes-form :: form";
    }

    /** Guarda el accidente de trabajo (POST /Servcio/accidente). */
    @PostMapping(
            path = "/accidentes/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Accidentes", accion = "create", entidad = "Accidente", registro = "nss", detalle = {"tipoRiesgo"})
    public String save(@ModelAttribute AccidenteAltaForm form) {
        try {
            String proceso = accidenteService.crearAccidente(form);
            return (proceso == null || proceso.isBlank()) ? "Accidente registrado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
