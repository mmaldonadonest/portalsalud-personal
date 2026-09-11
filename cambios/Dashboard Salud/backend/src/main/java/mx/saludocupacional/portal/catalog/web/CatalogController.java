package mx.saludocupacional.portal.catalog.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.service.AccidentCauseService;
import mx.saludocupacional.portal.catalog.service.AccidentStatusService;
import mx.saludocupacional.portal.catalog.service.AccidentTypeService;
import mx.saludocupacional.portal.catalog.service.AgenciaService;
import mx.saludocupacional.portal.catalog.service.AreaService;
import mx.saludocupacional.portal.catalog.service.AttentionCauseService;
import mx.saludocupacional.portal.catalog.service.CuentaService;
import mx.saludocupacional.portal.catalog.service.DisabilityTypeService;
import mx.saludocupacional.portal.catalog.service.DrugTestResultService;
import mx.saludocupacional.portal.catalog.service.DrugTestStatusService;
import mx.saludocupacional.portal.catalog.service.DrugTestTypeService;
import mx.saludocupacional.portal.catalog.service.InjuryTypeService;
import mx.saludocupacional.portal.catalog.service.MedicalExamResultService;
import mx.saludocupacional.portal.catalog.service.MedicalExamTypeService;
import mx.saludocupacional.portal.catalog.service.PredioService;
import mx.saludocupacional.portal.catalog.service.PuestoService;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.AttentionCauseItem;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.CatalogItem;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.CatalogRequest;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.CatalogosResponse;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.InjuryTypeItem;
import mx.saludocupacional.portal.catalog.web.dto.CatalogDtos.PredioItem;
import mx.saludocupacional.portal.security.service.PortalUserDetails;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta y administración de los catálogos maestros.
 *
 * <p>Cualquier usuario autenticado puede leerlos, porque los formularios los
 * necesitan para poblar sus listas. Modificarlos exige el permiso
 * {@code admin.catalogs}.
 */
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
public class CatalogController {

    private final PredioService predios;
    private final CuentaService cuentas;
    private final AgenciaService agencias;
    private final AreaService areas;
    private final PuestoService puestos;
    private final AttentionCauseService causasAtencion;
    private final InjuryTypeService tiposLesion;
    private final MedicalExamTypeService tiposExamen;
    private final MedicalExamResultService resultadosExamen;
    private final DisabilityTypeService tiposIncapacidad;
    private final AccidentTypeService tiposAccidente;
    private final AccidentCauseService causasAccidente;
    private final AccidentStatusService estatusAccidente;
    private final DrugTestTypeService tiposPrueba;
    private final DrugTestResultService resultadosPrueba;
    private final DrugTestStatusService estatusPrueba;

    /** Todos los catálogos activos en una sola respuesta, para poblar formularios. */
    @GetMapping
    public CatalogosResponse todos() {
        return new CatalogosResponse(
                predios.listarConAliases(true),
                cuentas.listar(true),
                agencias.listar(true),
                areas.listar(true),
                puestos.listar(true),
                causasAtencion.listarConCategoria(true),
                tiposLesion.listarConRegion(true),
                tiposExamen.listar(true),
                resultadosExamen.listar(true),
                tiposIncapacidad.listar(true),
                tiposAccidente.listar(true),
                causasAccidente.listar(true),
                estatusAccidente.listar(true),
                tiposPrueba.listar(true),
                resultadosPrueba.listar(true),
                estatusPrueba.listar(true));
    }

    @GetMapping("/predios")
    public List<PredioItem> listarPredios(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return predios.listarConAliases(soloActivos);
    }

    /** Predios que el usuario tiene autorizados; para el rol de gerencia de predio. */
    @GetMapping("/predios/accesibles")
    public List<PredioItem> prediosAccesibles(@AuthenticationPrincipal PortalUserDetails usuario) {
        return predios.listarAccesibles(usuario.getUsuarioId(), usuario.getPrediosPermitidos());
    }

    @GetMapping("/causas-atencion")
    public List<AttentionCauseItem> listarCausas(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return causasAtencion.listarConCategoria(soloActivos);
    }

    @GetMapping("/causas-atencion/categorias")
    public List<String> categoriasDeCausa() {
        return causasAtencion.listarCategorias();
    }

    @GetMapping("/tipos-lesion")
    public List<InjuryTypeItem> listarLesiones(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return tiposLesion.listarConRegion(soloActivos);
    }

    @GetMapping("/cuentas")
    public List<CatalogItem> listarCuentas(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return cuentas.listar(soloActivos);
    }

    @GetMapping("/areas")
    public List<CatalogItem> listarAreas(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return areas.listar(soloActivos);
    }

    @GetMapping("/puestos")
    public List<CatalogItem> listarPuestos(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return puestos.listar(soloActivos);
    }

    @GetMapping("/agencias")
    public List<CatalogItem> listarAgencias(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return agencias.listar(soloActivos);
    }

    // ---------- Administración ----------

    @PostMapping("/cuentas")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public CatalogItem crearCuenta(@Valid @RequestBody CatalogRequest request,
                                   @AuthenticationPrincipal PortalUserDetails usuario) {
        return cuentas.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/cuentas/{id}")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    public CatalogItem actualizarCuenta(@PathVariable Long id,
                                        @Valid @RequestBody CatalogRequest request,
                                        @AuthenticationPrincipal PortalUserDetails usuario) {
        return cuentas.actualizar(id, request, usuario.getUsuarioId());
    }

    @DeleteMapping("/cuentas/{id}")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    public ResponseEntity<Void> desactivarCuenta(@PathVariable Long id,
                                                 @AuthenticationPrincipal PortalUserDetails usuario) {
        cuentas.desactivar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/predios")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public CatalogItem crearPredio(@Valid @RequestBody CatalogRequest request,
                                   @AuthenticationPrincipal PortalUserDetails usuario) {
        return predios.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/predios/{id}")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    public CatalogItem actualizarPredio(@PathVariable Long id,
                                        @Valid @RequestBody CatalogRequest request,
                                        @AuthenticationPrincipal PortalUserDetails usuario) {
        return predios.actualizar(id, request, usuario.getUsuarioId());
    }

    @PostMapping("/predios/{id}/alias")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    public ResponseEntity<Void> agregarAlias(@PathVariable Long id,
                                             @RequestParam String alias,
                                             @AuthenticationPrincipal PortalUserDetails usuario) {
        predios.agregarAlias(id, alias, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/causas-atencion")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public CatalogItem crearCausa(@Valid @RequestBody CatalogRequest request,
                                  @AuthenticationPrincipal PortalUserDetails usuario) {
        return causasAtencion.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/causas-atencion/{id}")
    @PreAuthorize("hasAuthority('admin.catalogs')")
    public CatalogItem actualizarCausa(@PathVariable Long id,
                                       @Valid @RequestBody CatalogRequest request,
                                       @AuthenticationPrincipal PortalUserDetails usuario) {
        return causasAtencion.actualizar(id, request, usuario.getUsuarioId());
    }
}
