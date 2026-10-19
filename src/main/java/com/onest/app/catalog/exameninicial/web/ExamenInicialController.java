package com.onest.app.catalog.exameninicial.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.catalog.exameninicial.service.ExamenInicialService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

/**
 * Examen medico inicial (FT-SO-04): asistente por hojas con borrador que se guarda solo.
 * Misma convencion que el resto de los modulos del expediente: POST form-urlencoded con
 * {@code data}=NSS para el shell; el autoguardado manda JSON.
 */
@Controller
@RequestMapping("/api/nss")
public class ExamenInicialController {

    private final ExamenInicialService service;

    public ExamenInicialController(ExamenInicialService service) {
        this.service = service;
    }

    @PostMapping(
            path = "/examen-inicial",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_HTML_VALUE)
    public String shell(@RequestParam("data") String data, Model model) {
        try {
            model.addAttribute("v", service.cargar(data));
            model.addAttribute("camposTrabajo", ExamenInicialService.CAMPOS_TRABAJO);
            return "fragments/examen-inicial-shell :: shell";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Cuerpo JSON {@code {nss, hoja, datos:{clave:valor}}}. Devuelve la hora de guardado. */
    public record BorradorRequest(String nss, Integer hoja, Map<String, String> datos) {
    }

    @PostMapping(
            path = "/examen-inicial/borrador",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String borrador(@RequestBody BorradorRequest req) {
        try {
            service.guardarBorrador(req.nss(), req.hoja() == null ? 1 : req.hoja(), req.datos());
            return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    /** Manda el examen completo al WS y a SERV_MED_TAG (tipo Admision) y cierra el borrador. */
    @PostMapping(
            path = "/examen-inicial/finalizar",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    @Auditado(modulo = "Examenes", accion = "create", entidad = "Examen medico inicial", registro = "nss")
    public String finalizar(@RequestParam MultiValueMap<String, String> params) {
        try {
            String nss = params.getFirst("nss");
            String firma = params.getFirst("firma");
            // Lo que venga en el form (ultima hoja sin autoguardar aun) se funde al borrador antes de cerrar
            Map<String, String> datos = new LinkedHashMap<>();
            params.forEach((k, v) -> {
                if (!"nss".equals(k) && !"firma".equals(k) && !"_csrf".equals(k) && !"hoja".equals(k)) {
                    datos.put(k, v == null || v.isEmpty() ? "" : v.get(0));
                }
            });
            if (!datos.isEmpty()) {
                String hoja = params.getFirst("hoja");
                service.guardarBorrador(nss, hoja == null ? 10 : Integer.parseInt(hoja), datos);
            }
            String proceso = service.finalizar(nss, firma);
            return proceso == null || proceso.isBlank() ? "Examen inicial finalizado." : proceso;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
