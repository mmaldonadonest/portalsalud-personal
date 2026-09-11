package mx.saludocupacional.portal.morbidity.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.repository.AttentionCauseRepository;
import mx.saludocupacional.portal.catalog.repository.CuentaRepository;
import mx.saludocupacional.portal.catalog.repository.InjuryTypeRepository;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.service.EmployeeService;
import mx.saludocupacional.portal.morbidity.domain.MedicalAttention;
import mx.saludocupacional.portal.morbidity.repository.MedicalAttentionRepository;
import mx.saludocupacional.portal.morbidity.web.dto.AttentionDtos.AttentionItem;
import mx.saludocupacional.portal.morbidity.web.dto.AttentionDtos.AttentionRequest;
import mx.saludocupacional.portal.security.service.PredioScopeService;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import mx.saludocupacional.portal.shared.service.PeriodService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro y consulta de atenciones médicas.
 *
 * <p>Cada atención registrada alimenta de inmediato la morbilidad, el ranking de
 * causas y el dashboard: no existe una captura separada de indicadores.
 */
@Service
@RequiredArgsConstructor
public class AttentionService {

    private static final String MODULO = "morbilidad";

    private final MedicalAttentionRepository repository;
    private final PredioRepository predios;
    private final CuentaRepository cuentas;
    private final AttentionCauseRepository causas;
    private final InjuryTypeRepository lesiones;
    private final EmployeeService employeeService;
    private final PeriodService periodService;
    private final PredioScopeService alcance;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<AttentionItem> buscar(Integer anio, Integer mes, Long predioId,
                                      Long causaId, Pageable pageable) {
        return repository.buscar(anio, mes, predioId, causaId, alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public AttentionItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<AttentionItem> historialDe(Long empleadoId) {
        return repository.findByEmployeeIdAndDeletedAtIsNullOrderByFechaAtencionDesc(empleadoId)
                .stream().map(this::aItem).toList();
    }

    @Transactional
    public AttentionItem crear(AttentionRequest request, Long usuarioId) {
        validarClasificacion(request);
        alcance.verificarAcceso(request.predioId());

        MedicalAttention atencion = new MedicalAttention();
        aplicar(atencion, request);
        MedicalAttention guardada = repository.save(atencion);

        auditService.registrar(AuditAction.CREATE, MODULO, "MedicalAttention",
                guardada.getId(), null, null, usuarioId, true);
        return aItem(guardada);
    }

    @Transactional
    public AttentionItem actualizar(Long id, AttentionRequest request, Long usuarioId) {
        validarClasificacion(request);
        MedicalAttention atencion = obtener(id);
        aplicar(atencion, request);
        MedicalAttention guardada = repository.save(atencion);

        auditService.registrar(AuditAction.UPDATE, MODULO, "MedicalAttention",
                id, null, null, usuarioId, true);
        return aItem(guardada);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        MedicalAttention atencion = obtener(id);
        atencion.markDeleted();
        repository.save(atencion);

        auditService.registrar(AuditAction.DELETE, MODULO, "MedicalAttention",
                id, null, null, usuarioId, true);
    }

    @Transactional(readOnly = true)
    public MedicalAttention obtener(Long id) {
        MedicalAttention atencion = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("atención médica", id));
        alcance.verificarAcceso(atencion.getPredio().getId());
        return atencion;
    }

    private void aplicar(MedicalAttention atencion, AttentionRequest request) {
        atencion.setFechaAtencion(request.fechaAtencion());
        atencion.setPeriod(periodService.resolverParaCaptura(request.fechaAtencion()));
        atencion.setPredio(predios.findById(request.predioId())
                .orElseThrow(() -> new ResourceNotFoundException("predio", request.predioId())));
        atencion.setEsPersonalInclusion(Boolean.TRUE.equals(request.esPersonalInclusion()));
        atencion.setObservaciones(request.observaciones());

        if (request.empleadoId() != null) {
            Employee empleado = employeeService.obtener(request.empleadoId());
            atencion.setEmployee(empleado);
            if (atencion.getCuenta() == null) {
                atencion.setCuenta(empleado.getCuenta());
            }
        }
        if (request.cuentaId() != null) {
            atencion.setCuenta(cuentas.findById(request.cuentaId())
                    .orElseThrow(() -> new ResourceNotFoundException("cuenta", request.cuentaId())));
        }
        atencion.setAttentionCause(request.causaId() == null ? null
                : causas.findById(request.causaId())
                    .orElseThrow(() -> new ResourceNotFoundException("causa", request.causaId())));
        atencion.setInjuryType(request.tipoLesionId() == null ? null
                : lesiones.findById(request.tipoLesionId())
                    .orElseThrow(() -> new ResourceNotFoundException("tipo de lesión", request.tipoLesionId())));
    }

    private void validarClasificacion(AttentionRequest request) {
        if (request.causaId() == null && request.tipoLesionId() == null) {
            throw new BusinessRuleException(
                    "Indica la causa médica o el tipo de lesión de la atención");
        }
    }

    public AttentionItem aItem(MedicalAttention a) {
        return new AttentionItem(
                a.getId(),
                a.getEmployee() == null ? null : a.getEmployee().getId(),
                a.getEmployee() == null ? null : a.getEmployee().getNombre(),
                a.getPredio().getNombre(),
                a.getPredio().getId(),
                a.getFechaAtencion(),
                a.getAttentionCause() == null ? null : a.getAttentionCause().getNombre(),
                a.getAttentionCause() == null ? null : a.getAttentionCause().getId(),
                a.getInjuryType() == null ? null : a.getInjuryType().getNombre(),
                a.getInjuryType() == null ? null : a.getInjuryType().getId(),
                a.isEsPersonalInclusion(),
                a.getPeriod().getEtiqueta());
    }
}
