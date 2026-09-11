package mx.saludocupacional.portal.employee.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.employee.service.EmployeeService;
import mx.saludocupacional.portal.employee.web.dto.EmployeeDtos.EmployeeItem;
import mx.saludocupacional.portal.employee.web.dto.EmployeeDtos.EmployeeRequest;
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

/** Consulta y administración de colaboradores. */
@RestController
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
public class EmployeeController {

    private static final int TAMANO_MAXIMO = 100;

    private final EmployeeService service;

    @GetMapping
    @PreAuthorize("hasAuthority('employee.read')")
    public Page<EmployeeItem> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long predioId,
            @RequestParam(required = false) Long cuentaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return service.buscar(texto, predioId, cuentaId, activo, PageRequest.of(pagina, limite));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.read')")
    public EmployeeItem detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('employee.write')")
    public EmployeeItem crear(@Valid @RequestBody EmployeeRequest request,
                              @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.write')")
    public EmployeeItem actualizar(@PathVariable Long id,
                                   @Valid @RequestBody EmployeeRequest request,
                                   @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.actualizar(id, request, usuario.getUsuarioId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('employee.write')")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id,
                                          @AuthenticationPrincipal PortalUserDetails usuario) {
        service.darDeBaja(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
