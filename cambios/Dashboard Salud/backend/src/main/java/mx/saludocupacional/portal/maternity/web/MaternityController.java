package mx.saludocupacional.portal.maternity.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.maternity.service.MaternityService;
import mx.saludocupacional.portal.maternity.web.dto.MaternityDtos.MaternityItem;
import mx.saludocupacional.portal.maternity.web.dto.MaternityDtos.MaternityRequest;
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

/** Consulta y captura de casos de maternidad. */
@RestController
@RequestMapping("/api/maternidad")
@RequiredArgsConstructor
public class MaternityController {

    private static final int TAMANO_MAXIMO = 100;

    private final MaternityService service;

    @GetMapping
    @PreAuthorize("hasAuthority('maternity.read')")
    public Page<MaternityItem> buscar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return service.buscar(anio, mes, predioId, PageRequest.of(pagina, limite));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('maternity.read')")
    public MaternityItem detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('maternity.create')")
    public MaternityItem crear(@Valid @RequestBody MaternityRequest request,
                               @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('maternity.create')")
    public MaternityItem actualizar(@PathVariable Long id,
                                    @Valid @RequestBody MaternityRequest request,
                                    @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.actualizar(id, request, usuario.getUsuarioId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('maternity.create')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal PortalUserDetails usuario) {
        service.eliminar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
