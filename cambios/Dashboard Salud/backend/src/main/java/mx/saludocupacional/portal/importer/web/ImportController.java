package mx.saludocupacional.portal.importer.web;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.importer.service.ImportService;
import mx.saludocupacional.portal.importer.web.dto.ImportDtos.PreviewResponse;
import mx.saludocupacional.portal.importer.web.dto.ImportDtos.ResultadoImportacion;
import mx.saludocupacional.portal.security.service.PortalUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importación de información desde Excel.
 *
 * <p>La carga no altera nada de inmediato: primero devuelve una vista previa
 * para revisión y solo escribe al confirmarla.
 */
@RestController
@RequestMapping("/api/importaciones")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService service;

    /** Analiza el archivo y devuelve lo detectado, sin escribir nada todavía. */
    @PostMapping("/analizar")
    @PreAuthorize("hasAuthority('import.execute')")
    public PreviewResponse analizar(@RequestParam("archivo") MultipartFile archivo,
                                    @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.analizar(archivo, usuario.getUsuarioId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('import.execute')")
    public PreviewResponse consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    /** Escribe las filas válidas del lote en las tablas operativas. */
    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('import.execute')")
    public ResultadoImportacion confirmar(@PathVariable Long id,
                                          @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.confirmar(id, usuario.getUsuarioId());
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('import.execute')")
    public ResponseEntity<Void> rechazar(@PathVariable Long id,
                                         @AuthenticationPrincipal PortalUserDetails usuario) {
        service.rechazar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
