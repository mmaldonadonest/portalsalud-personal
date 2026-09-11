package mx.saludocupacional.portal.disability.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.disability.service.DisabilityService;
import mx.saludocupacional.portal.disability.web.dto.DisabilityDtos.DisabilityItem;
import mx.saludocupacional.portal.disability.web.dto.DisabilityDtos.DisabilityRequest;
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

/** Consulta y captura de incapacidades. */
@RestController
@RequestMapping("/api/incapacidades")
@RequiredArgsConstructor
public class DisabilityController {

    private static final int TAMANO_MAXIMO = 100;

    private final DisabilityService service;

    @GetMapping
    @PreAuthorize("hasAuthority('disability.read')")
    public Page<DisabilityItem> buscar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return service.buscar(anio, mes, predioId, tipoId, PageRequest.of(pagina, limite));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('disability.read')")
    public DisabilityItem detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @GetMapping("/empleado/{empleadoId}")
    @PreAuthorize("hasAuthority('disability.read')")
    public List<DisabilityItem> historial(@PathVariable Long empleadoId) {
        return service.historialDe(empleadoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('disability.create')")
    public DisabilityItem crear(@Valid @RequestBody DisabilityRequest request,
                                @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('disability.update')")
    public DisabilityItem actualizar(@PathVariable Long id,
                                     @Valid @RequestBody DisabilityRequest request,
                                     @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.actualizar(id, request, usuario.getUsuarioId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('disability.update')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal PortalUserDetails usuario) {
        service.eliminar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
