package mx.saludocupacional.portal.audit.web;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditLog;
import mx.saludocupacional.portal.shared.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Consulta de la bitácora.
 *
 * <p>Solo los perfiles con permiso de auditoría acceden a este registro, que
 * reúne los cambios de todos los módulos del portal.
 */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditController {

    private static final int TAMANO_MAXIMO = 200;

    private final AuditLogRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('audit.read')")
    public Page<AuditLog> buscar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) AuditAction accion,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        OffsetDateTime inicio = desde == null ? null : desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hasta == null ? null : hasta.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        return repository.buscar(usuarioId, modulo, accion, inicio, fin, PageRequest.of(pagina, limite));
    }
}
