package com.onest.app.catalog.maternidad.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.file.service.FileStoreService;
import com.onest.app.catalog.maternidad.service.MaternidadService;
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
 * Seguimiento de maternidad: historial y alta de un chequeo. Backend en
 * docs/ords-maternidad.sql, aplicado 2026-08-21. Respuesta HTML (fragmentos).
 */
@Controller
@RequestMapping("/api/nss")
public class MaternidadController {

    private final MaternidadService maternidadService;

    private final FileStoreService fileStoreService;

    public MaternidadController(MaternidadService maternidadService, FileStoreService fileStoreService) {
        this.maternidadService = maternidadService;
        this.fileStoreService = fileStoreService;
    }

    /** C-040: fotografias del NSS; si la base no responde la pantalla sigue sin ellas. */
    private java.util.List<com.onest.app.catalog.file.dto.MedFileMeta> fotos(String nss) {
        try {
            return nss.isEmpty() ? java.util.List.of()
                    : fileStoreService.listByNssAndType(nss, FileStoreService.TIPO_FOTO_MATERNIDAD);
        } catch (org.springframework.dao.DataAccessException ex) {
            return java.util.List.of();
        }
    }

    @PostMapping(
            path = "/maternidad",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String maternidad(@RequestParam("data") String data, Model model) {
        try {
            String nss = data == null ? "" : data.trim();
            model.addAttribute("seguimientos", maternidadService.byNss(nss));
            model.addAttribute("nss", nss);
            model.addAttribute("sexoHombre", maternidadService.esSexoHombre(nss));
            model.addAttribute("calculo", maternidadService.calculo(nss));
            model.addAttribute("fotos", fotos(nss));
            return "fragments/maternidad-list :: table";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Formulario de alta de un chequeo de seguimiento. */
    @PostMapping(
            path = "/maternidad/form",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String form(@RequestParam("data") String data, Model model) {
        String nss = data == null ? "" : data.trim();
        model.addAttribute("nss", nss);
        model.addAttribute("sexoHombre", maternidadService.esSexoHombre(nss));
        // C-037: la FUM y el inicio del embarazo en curso vienen llenos para no recapturarlos
        model.addAttribute("calculo", nss.isEmpty() ? null : maternidadService.calculo(nss));
        model.addAttribute("tipoFoto", FileStoreService.TIPO_FOTO_MATERNIDAD);
        return "fragments/maternidad-form :: form";
    }

    /** Guarda el chequeo de seguimiento (POST /Servcio/maternidad). */
    @PostMapping(
            path = "/maternidad/save",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Maternidad", accion = "create", entidad = "Seguimiento de maternidad", registro = "nss", detalle = {"estatus", "fum", "inicioIncapacidad"})
    public String save(@ModelAttribute MaternidadAltaForm form) {
        try {
            String proceso = maternidadService.crearSeguimiento(form);
            return (proceso == null || proceso.isBlank()) ? "Seguimiento de maternidad registrado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
