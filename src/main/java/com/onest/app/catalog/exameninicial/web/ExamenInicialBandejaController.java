package com.onest.app.catalog.exameninicial.web;

import com.onest.app.integration.ocr.SolicitudExamen;
import com.onest.app.integration.ocr.SolicitudExamenService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Bandeja de examenes iniciales: todas las solicitudes (hoy solo llegan de OCR) con filtros por
 * estatus, NSS, nombre y fechas, y quien las esta atendiendo. Pagina standalone del grupo
 * "Examenes" del sidebar; se asigna por rol con el CODE EXAMEN_INICIAL_BANDEJA
 * (db/sql/app_domain/examen-inicial-bandeja.sql) y la ruta la bloquea ClinicalAccessFilter.
 */
@Controller
public class ExamenInicialBandejaController {

    private final SolicitudExamenService solicitudes;

    public ExamenInicialBandejaController(SolicitudExamenService solicitudes) {
        this.solicitudes = solicitudes;
    }

    @GetMapping("/examen-inicial/bandeja")
    public String bandeja(@RequestParam(name = "estatus", required = false) String estatus,
                          @RequestParam(name = "nss", required = false) String nss,
                          @RequestParam(name = "nombre", required = false) String nombre,
                          @RequestParam(name = "desde", required = false) String desde,
                          @RequestParam(name = "hasta", required = false) String hasta,
                          Model model) {
        List<SolicitudExamen> lista;
        boolean disponible = true;
        try {
            lista = solicitudes.bandeja(estatus, nss, nombre, desde, hasta);
        } catch (org.springframework.dao.DataAccessException ex) {
            // Falta integracion-ocr.sql o examen-inicial-bandeja.sql: se avisa en pantalla, no se cae
            lista = List.of();
            disponible = false;
        }
        long pendientes = lista.stream().filter(s -> SolicitudExamen.PENDIENTE.equals(s.estatus())).count();
        long enCaptura = lista.stream().filter(s -> SolicitudExamen.EN_CAPTURA.equals(s.estatus())).count();
        model.addAttribute("solicitudes", lista);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("enCaptura", enCaptura);
        model.addAttribute("disponible", disponible);
        model.addAttribute("estatus", estatus == null ? "" : estatus);
        model.addAttribute("nss", nss == null ? "" : nss);
        model.addAttribute("nombre", nombre == null ? "" : nombre);
        model.addAttribute("desde", desde == null ? "" : desde);
        model.addAttribute("hasta", hasta == null ? "" : hasta);
        model.addAttribute("usuarioActual", usuarioActual());
        return "pages/examen-inicial-bandeja";
    }

    /** "Tomar examen" desde la bandeja o desde el shell: se lo asigna al usuario en sesion. */
    @PostMapping(path = "/api/examen-inicial/bandeja/{id}/tomar", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String tomar(@org.springframework.web.bind.annotation.PathVariable long id, HttpServletRequest request) {
        try {
            solicitudes.tomar(id, usuarioActual(), request);
            return "Examen asignado a " + usuarioActual() + ".";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    private static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth.getName() == null ? "anonimo" : auth.getName();
    }
}
