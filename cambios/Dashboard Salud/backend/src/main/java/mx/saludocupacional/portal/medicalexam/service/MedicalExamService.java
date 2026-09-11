package mx.saludocupacional.portal.medicalexam.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.repository.CuentaRepository;
import mx.saludocupacional.portal.catalog.repository.MedicalExamResultRepository;
import mx.saludocupacional.portal.catalog.repository.MedicalExamTypeRepository;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.service.EmployeeService;
import mx.saludocupacional.portal.medicalexam.domain.MedicalExam;
import mx.saludocupacional.portal.medicalexam.repository.MedicalExamRepository;
import mx.saludocupacional.portal.medicalexam.web.dto.MedicalExamDtos.MedicalExamItem;
import mx.saludocupacional.portal.medicalexam.web.dto.MedicalExamDtos.MedicalExamRequest;
import mx.saludocupacional.portal.security.service.PredioScopeService;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import mx.saludocupacional.portal.shared.service.PeriodService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Registro y consulta de exámenes médicos.
 *
 * <p>Cada resultado capturado actualiza los indicadores de aptitud sin que nadie
 * tenga que concentrarlos aparte.
 */
@Service
@RequiredArgsConstructor
public class MedicalExamService {

    private static final String MODULO = "examenes";

    private final MedicalExamRepository repository;
    private final MedicalExamTypeRepository tipos;
    private final MedicalExamResultRepository resultados;
    private final CuentaRepository cuentas;
    private final EmployeeService employeeService;
    private final PeriodService periodService;
    private final PredioScopeService alcance;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<MedicalExamItem> buscar(Integer anio, Integer mes, Long predioId,
                                        Long tipoId, Long resultadoId, Pageable pageable) {
        return repository.buscar(anio, mes, predioId, tipoId, resultadoId,
                        alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public MedicalExamItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<MedicalExamItem> historialDe(Long empleadoId) {
        return repository.findByEmployeeIdAndDeletedAtIsNullOrderByFechaExamenDesc(empleadoId)
                .stream().map(this::aItem).toList();
    }

    @Transactional
    public MedicalExamItem crear(MedicalExamRequest request, Long usuarioId) {
        MedicalExam examen = new MedicalExam();
        aplicar(examen, request);
        MedicalExam guardado = repository.save(examen);

        auditService.registrar(AuditAction.CREATE, MODULO, "MedicalExam",
                guardado.getId(), null, null, usuarioId, true);
        return aItem(guardado);
    }

    @Transactional
    public MedicalExamItem actualizar(Long id, MedicalExamRequest request, Long usuarioId) {
        MedicalExam examen = obtener(id);
        aplicar(examen, request);
        MedicalExam guardado = repository.save(examen);

        auditService.registrar(AuditAction.UPDATE, MODULO, "MedicalExam",
                id, null, null, usuarioId, true);
        return aItem(guardado);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        MedicalExam examen = obtener(id);
        examen.markDeleted();
        repository.save(examen);

        auditService.registrar(AuditAction.DELETE, MODULO, "MedicalExam",
                id, null, null, usuarioId, true);
    }

    @Transactional(readOnly = true)
    public MedicalExam obtener(Long id) {
        MedicalExam examen = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("examen médico", id));
        alcance.verificarAcceso(examen.getPredio().getId());
        return examen;
    }

    private void aplicar(MedicalExam examen, MedicalExamRequest request) {
        Employee empleado = employeeService.obtener(request.empleadoId());
        alcance.verificarAcceso(empleado.getPredio().getId());

        examen.setEmployee(empleado);
        examen.setPredio(empleado.getPredio());
        examen.setFechaExamen(request.fechaExamen());
        examen.setPeriod(periodService.resolverParaCaptura(request.fechaExamen()));
        examen.setObservaciones(request.observaciones());

        examen.setExamType(tipos.findById(request.tipoId())
                .orElseThrow(() -> new ResourceNotFoundException("tipo de examen", request.tipoId())));
        examen.setExamResult(resultados.findById(request.resultadoId())
                .orElseThrow(() -> new ResourceNotFoundException("resultado", request.resultadoId())));

        examen.setCuenta(request.cuentaId() != null
                ? cuentas.findById(request.cuentaId())
                    .orElseThrow(() -> new ResourceNotFoundException("cuenta", request.cuentaId()))
                : empleado.getCuenta());
    }

    public MedicalExamItem aItem(MedicalExam e) {
        return new MedicalExamItem(
                e.getId(),
                e.getEmployee().getId(),
                e.getEmployee().getNombre(),
                e.getEmployee().getNumeroEmpleado(),
                e.getPredio().getNombre(),
                e.getPredio().getId(),
                e.getFechaExamen(),
                e.getExamType().getNombre(),
                e.getExamType().getId(),
                e.getExamResult().getNombre(),
                e.getExamResult().getId(),
                e.getPeriod().getEtiqueta());
    }
}
