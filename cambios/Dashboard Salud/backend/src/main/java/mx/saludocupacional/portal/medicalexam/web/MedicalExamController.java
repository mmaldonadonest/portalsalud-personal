package mx.saludocupacional.portal.medicalexam.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.medicalexam.service.MedicalExamService;
import mx.saludocupacional.portal.medicalexam.web.dto.MedicalExamDtos.MedicalExamItem;
import mx.saludocupacional.portal.medicalexam.web.dto.MedicalExamDtos.MedicalExamRequest;
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

/** Consulta y captura de exámenes médicos. */
@RestController
@RequestMapping("/api/examenes")
@RequiredArgsConstructor
public class MedicalExamController {

    private static final int TAMANO_MAXIMO = 100;

    private final MedicalExamService service;

    @GetMapping
    @PreAuthorize("hasAuthority('exam.read')")
    public Page<MedicalExamItem> buscar(
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Long predioId,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) Long resultadoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamano) {

        int limite = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return service.buscar(anio, mes, predioId, tipoId, resultadoId, PageRequest.of(pagina, limite));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('exam.read')")
    public MedicalExamItem detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @GetMapping("/empleado/{empleadoId}")
    @PreAuthorize("hasAuthority('exam.read')")
    public List<MedicalExamItem> historial(@PathVariable Long empleadoId) {
        return service.historialDe(empleadoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('exam.create')")
    public MedicalExamItem crear(@Valid @RequestBody MedicalExamRequest request,
                                 @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.crear(request, usuario.getUsuarioId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('exam.create')")
    public MedicalExamItem actualizar(@PathVariable Long id,
                                      @Valid @RequestBody MedicalExamRequest request,
                                      @AuthenticationPrincipal PortalUserDetails usuario) {
        return service.actualizar(id, request, usuario.getUsuarioId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('exam.create')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @AuthenticationPrincipal PortalUserDetails usuario) {
        service.eliminar(id, usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
