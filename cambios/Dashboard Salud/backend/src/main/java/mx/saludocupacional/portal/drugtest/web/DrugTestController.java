package mx.saludocupacional.portal.drugtest.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.drugtest.service.DrugTestService;
import mx.saludocupacional.portal.drugtest.service.InventoryService;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.AlertaInventario;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.BatchItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.BatchRequest;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.DrugTestItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.DrugTestRequest;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.MovementItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.MovementRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta y captura de pruebas de antidoping, y control de su inventario. */
@RestController
@RequestMapping("/api/antidoping")
@RequiredArgsConstructor
public class DrugTestController {

    private static final int TAMANO_MAXIMO = 100;

    private final DrugTestService pruebas;
    private final InventoryService inventario;

    // ---------- Pruebas ----------

    @GetMapping("/pruebas")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public Page<DrugTestItem> buscar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return pruebas.buscar(anio, mes, predioId, tipoId, PageRequest.of(pagina, limite));
    }

    @GetMapping("/pruebas/{id}")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public DrugTestItem detalle(@PathVariable Long id) {
        return pruebas.detalle(id);
    }

    @GetMapping("/pruebas/empleado/{empleadoId}")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public List<DrugTestItem> historial(@PathVariable Long empleadoId) {
        return pruebas.historialDe(empleadoId);
    }

    @PostMapping("/pruebas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('drugtest.create')")
    public DrugTestItem crearPrueba(@Valid @RequestBody DrugTestRequest request,
                                    @AuthenticationPrincipal PortalUserDetails usuario) {
        return pruebas.crear(request, usuario.getUsuarioId());
    }

    @DeleteMapping("/pruebas/{id}")
    @PreAuthorize("hasAuthority('drugtest.create')")
    public ResponseEntity<Void> eliminarPrueba(@PathVariable Long id,
                                               @AuthenticationPrincipal PortalUserDetails usuario) {
        pruebas.eliminar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    // ---------- Inventario ----------

    @GetMapping("/inventario")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public List<BatchItem> lotes(@RequestParam(required = false) Long predioId) {
        return inventario.listarLotes(predioId);
    }

    @GetMapping("/inventario/{id}")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public BatchItem detalleLote(@PathVariable Long id) {
        return inventario.detalleLote(id);
    }

    @GetMapping("/inventario/{id}/movimientos")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public List<MovementItem> movimientos(@PathVariable Long id) {
        return inventario.movimientosDe(id);
    }

    @GetMapping("/inventario/alertas")
    @PreAuthorize("hasAuthority('drugtest.read')")
    public List<AlertaInventario> alertas() {
        return inventario.alertas();
    }

    @PostMapping("/inventario")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.manage')")
    public BatchItem crearLote(@Valid @RequestBody BatchRequest request,
                               @AuthenticationPrincipal PortalUserDetails usuario) {
        return inventario.crearLote(request, usuario.getUsuarioId());
    }

    @PostMapping("/inventario/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('inventory.manage')")
    public MovementItem registrarMovimiento(@Valid @RequestBody MovementRequest request,
                                            @AuthenticationPrincipal PortalUserDetails usuario) {
        return inventario.registrarMovimiento(request, usuario.getUsuarioId());
    }
}
