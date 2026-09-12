package com.onest.app.importer.web;

import com.onest.app.audit.web.Auditado;
import com.onest.app.importer.dto.ImportacionDto.FilaPreview;
import com.onest.app.importer.dto.ImportacionDto.Lote;
import com.onest.app.importer.dto.ImportacionDto.Resultado;
import com.onest.app.importer.service.ImportacionService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** API del importador Excel (Analisis > Importar Excel). Rol MEDICO_ANALISTA por /api/analisis/**. */
@RestController
@RequestMapping("/api/analisis/importar")
public class ImportacionController {

    private final ImportacionService service;

    public ImportacionController(ImportacionService service) {
        this.service = service;
    }

    /** Pasos 1-4: sube, lee, valida y deja el lote en VALIDADO. Multipart: CSRF por header. */
    @PostMapping
    @Auditado(modulo = "Importar Excel", accion = "create", entidad = "Lote validado", registro = "nombre")
    public Resultado cargar(@RequestParam("file") MultipartFile file,
                            @RequestParam(name = "nombre", required = false) String nombre) {
        try {
            return service.cargar(file);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            // tablas de staging sin aplicar: es configuracion, no un error del archivo
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    @GetMapping
    public List<Lote> historial() {
        return service.historial();
    }

    @GetMapping("/{id}")
    public Resultado detalle(@PathVariable long id) {
        return service.detalle(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no existe"));
    }

    @GetMapping("/{id}/filas")
    public List<FilaPreview> filas(@PathVariable long id, @RequestParam("hoja") String hoja,
                                   @RequestParam(name = "pagina", defaultValue = "0") int pagina,
                                   @RequestParam(name = "porPagina", defaultValue = "100") int porPagina) {
        return service.filas(id, hoja, pagina, porPagina);
    }

    /** Paso 5 (+6 via @Auditado). */
    @PostMapping("/{id}/confirmar")
    @Auditado(modulo = "Importar Excel", accion = "update", entidad = "Lote confirmado", registro = "id")
    public Lote confirmar(@PathVariable long id) {
        try {
            return service.confirmar(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/{id}/descartar")
    @Auditado(modulo = "Importar Excel", accion = "delete", entidad = "Lote descartado", registro = "id")
    public Lote descartar(@PathVariable long id) {
        try {
            return service.descartar(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
