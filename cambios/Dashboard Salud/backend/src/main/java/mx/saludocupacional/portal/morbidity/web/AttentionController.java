package mx.saludocupacional.portal.morbidity.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.morbidity.service.AttentionService;
import mx.saludocupacional.portal.morbidity.web.dto.AttentionDtos.AttentionItem;
import mx.saludocupacional.portal.morbidity.web.dto.AttentionDtos.AttentionRequest;
import mx.saludocupacional.portal.security.service.PortalUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta y captura de atenciones médicas. */
@RestController
@RequestMapping("/api/atenciones")
@RequiredArgsConstructor
public class AttentionController {

    private static final int TAMANO_MAXIMO = 100;

    private final AttentionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('attention.read')")
    public Page<AttentionItem> buscar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @RequestParam(required = false) Long causaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return service.buscar(anio, mes, predioId, causaId, PageRequest.of(pagina, limite));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('attention.read')")
    public AttentionItem detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @GetMapping("/empleado/{empleadoId}")
    @PreAuthorize("hasAuthority('attention.read')")
    public List<AttentionItem> historial(@PathVariable Long empleadoId) {
        return service.historialDe(empleadoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('attention.create')")
    public AttentionItem crear(@Valid @RequestBody AttentionRequest request,
                               @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('attention.update')")
    public AttentionItem actualizar(@PathVariable Long id,
                                    @Valid @RequestBody AttentionRequest request,
                                    @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.actualizar(id, request, usuario.getUsuarioId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('attention.update')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal PortalUserDetails usuario) {
        service.eliminar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
