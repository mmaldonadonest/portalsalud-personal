package com.onest.app.audit.web;

import com.onest.app.audit.dto.AuditoriaDto;
import com.onest.app.audit.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.format.DateTimeParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** API del modulo Auditoria (/analisis/auditoria). Lectura de la bitacora + registro de exportaciones. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public AuditoriaDto buscar(
            @RequestParam(name = "usuario", required = false) String usuario,
            @RequestParam(name = "modulo", required = false) String modulo,
            @RequestParam(name = "accion", required = false) String accion,
            @RequestParam(name = "desde", required = false) String desde,
            @RequestParam(name = "hasta", required = false) String hasta,
            @RequestParam(name = "texto", required = false) String texto,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "porPagina", defaultValue = "50") int porPagina) {
        try {
            return auditoriaService.buscar(usuario, modulo, accion, desde, hasta, texto, pagina, porPagina);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha invalida, use yyyy-MM-dd", ex);
        }
    }

    @GetMapping("/filtros")
    public AuditoriaDto.Filtros filtros() {
        return auditoriaService.filtros();
    }

    /**
     * Las exportaciones CSV del dashboard analitico se arman en el navegador (no pasan por el
     * servidor), asi que cada pantalla avisa aqui al exportar. Es el unico evento que se
     * registra a peticion del cliente; el resto lo captura {@link AuditoriaInterceptor}.
     */
    @PostMapping("/exportacion")
    public void exportacion(
            @RequestParam("modulo") String modulo,
            @RequestParam(name = "detalle", required = false) String detalle,
            HttpServletRequest request) {
        auditoriaService.registrar("Reportes", "export", "CSV", modulo, detalle, request);
    }
}
