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
    private final com.onest.app.catalog.exameninicial.repository.ExamenBorradorRepository borradores;

    public ExamenInicialBandejaController(SolicitudExamenService solicitudes,
                                          com.onest.app.catalog.exameninicial.repository.ExamenBorradorRepository borradores) {
        this.solicitudes = solicitudes;
        this.borradores = borradores;
    }

    @GetMapping("/examen-inicial/bandeja")
    public String bandeja(@RequestParam(name = "estatus", required = false) String estatus,
                          @RequestParam(name = "nss", required = false) String nss,
                          @RequestParam(name = "nombre", required = false) String nombre,
                          @RequestParam(name = "desde", required = false) String desde,
                          @RequestParam(name = "hasta", required = false) String hasta,
                          @RequestParam(name = "origen", required = false) String origen,
                          Model model) {
        List<SolicitudExamen> lista = new java.util.ArrayList<>();
        boolean disponible = true;
        String org = origen == null ? "" : origen.trim();
        // 9-nov-2026: ademas de las solicitudes de OCR, los examenes iniciales capturados directo (borradores)
        if (!"DIRECTO".equals(org)) {
            try {
                lista.addAll(solicitudes.bandeja(estatus, nss, nombre, desde, hasta));
            } catch (org.springframework.dao.DataAccessException ex) {
                // Falta integracion-ocr.sql o examen-inicial-bandeja.sql: se avisa en pantalla, no se cae
                disponible = false;
            }
        }
        if (!"OCR".equals(org)) {
            try {
                lista.addAll(borradores.directos(estatus, nss, nombre, fecha(desde), fecha(hasta), 500));
            } catch (org.springframework.dao.DataAccessException ex) {
                // sin SERV_MED_EXAMEN_SOLICITUD el NOT EXISTS falla: la bandeja sigue con lo que haya
            }
        }
        // mismo orden que la de OCR: pendientes, en captura, finalizados, cancelados; dentro, lo mas reciente arriba
        java.util.Map<String, Integer> orden = java.util.Map.of(SolicitudExamen.PENDIENTE, 0, SolicitudExamen.EN_CAPTURA, 1,
                SolicitudExamen.FINALIZADO, 2, SolicitudExamen.CANCELADO, 3);
        lista.sort(java.util.Comparator.<SolicitudExamen>comparingInt(s -> orden.getOrDefault(s.estatus(), 4))
                .thenComparing(s -> s.createdAt() == null ? java.time.LocalDateTime.MIN : s.createdAt(),
                        java.util.Comparator.reverseOrder()));
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
        model.addAttribute("origen", org);
        model.addAttribute("usuarioActual", usuarioActual());
        return "pages/examen-inicial-bandeja";
    }

    /** Fecha del filtro (yyyy-MM-dd del input date); vacia o invalida = sin filtro. */
    private static java.time.LocalDate fecha(String s) {
        try {
            return s == null || s.isBlank() ? null : java.time.LocalDate.parse(s.trim());
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
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
