package mx.saludocupacional.portal.maternity.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.domain.DisabilityType;
import mx.saludocupacional.portal.catalog.repository.DisabilityTypeRepository;
import mx.saludocupacional.portal.disability.domain.Disability;
import mx.saludocupacional.portal.disability.service.DisabilityService;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.service.EmployeeService;
import mx.saludocupacional.portal.maternity.domain.MaternityCase;
import mx.saludocupacional.portal.maternity.domain.MaternityCase.Estatus;
import mx.saludocupacional.portal.maternity.repository.MaternityCaseRepository;
import mx.saludocupacional.portal.maternity.web.dto.MaternityDtos.MaternityItem;
import mx.saludocupacional.portal.maternity.web.dto.MaternityDtos.MaternityRequest;
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

/**
 * Registro y consulta de casos de maternidad.
 *
 * <p>Como en accidentabilidad, el caso y su incapacidad se crean juntos: la
 * captura ocurre una vez y los días quedan contabilizados con el resto de
 * incapacidades, sin sumarse dos veces en el dashboard.
 */
@Service
@RequiredArgsConstructor
public class MaternityService {

    private static final String MODULO = "maternidad";
    private static final String CODIGO_TIPO = "MATERNIDAD";

    private final MaternityCaseRepository repository;
    private final DisabilityTypeRepository tiposIncapacidad;
    private final DisabilityService disabilityService;
    private final EmployeeService employeeService;
    private final PeriodService periodService;
    private final PredioScopeService alcance;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<MaternityItem> buscar(Integer anio, Integer mes, Long predioId, Pageable pageable) {
        return repository.buscar(anio, mes, predioId, alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public MaternityItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional
    public MaternityItem crear(MaternityRequest request, Long usuarioId) {
        Employee empleada = employeeService.obtener(request.empleadaId());
        alcance.verificarAcceso(empleada.getPredio().getId());

        MaternityCase caso = new MaternityCase();
        caso.setEmployee(empleada);
        caso.setPredio(empleada.getPredio());
        caso.setCuenta(empleada.getCuenta());
        caso.setPeriod(periodService.resolverParaCaptura(request.fechaInicioIncapacidad()));
        caso.setFechaInicioIncapacidad(request.fechaInicioIncapacidad());
        caso.setFechaProbableParto(request.fechaProbableParto());
        caso.setDiasIncapacidad(request.diasIncapacidad());
        caso.setEstatus(request.estatus() != null ? request.estatus() : Estatus.ACTIVO);

        MaternityCase guardado = repository.save(caso);

        if (request.diasIncapacidad() != null && request.diasIncapacidad() > 0) {
            DisabilityType tipo = tiposIncapacidad.findByCodigo(CODIGO_TIPO)
                    .orElseThrow(() -> new BusinessRuleException(
                            "No está configurado el tipo de incapacidad por maternidad"));

            Disability incapacidad = disabilityService.crearDesdeMaternidad(
                    empleada, tipo, request.fechaInicioIncapacidad(),
                    request.diasIncapacidad(), guardado.getId());

            guardado.setDisability(incapacidad);
            guardado = repository.save(guardado);
        }

        auditService.registrar(AuditAction.CREATE, MODULO, "MaternityCase",
                guardado.getId(), null, null, usuarioId, true);
        return aItem(guardado);
    }

    @Transactional
    public MaternityItem actualizar(Long id, MaternityRequest request, Long usuarioId) {
        MaternityCase caso = obtener(id);

        caso.setFechaProbableParto(request.fechaProbableParto());
        if (request.estatus() != null) {
            caso.setEstatus(request.estatus());
        }
        MaternityCase guardado = repository.save(caso);

        auditService.registrar(AuditAction.UPDATE, MODULO, "MaternityCase",
                id, null, null, usuarioId, true);
        return aItem(guardado);
    }

    /** Da de baja el caso junto con la incapacidad que hubiera generado. */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        MaternityCase caso = obtener(id);

        disabilityService.eliminarPorOrigen(caso.getDisability());
        caso.markDeleted();
        repository.save(caso);

        auditService.registrar(AuditAction.DELETE, MODULO, "MaternityCase",
                id, null, null, usuarioId, true);
    }

    @Transactional(readOnly = true)
    public MaternityCase obtener(Long id) {
        MaternityCase caso = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("caso de maternidad", id));
        alcance.verificarAcceso(caso.getPredio().getId());
        return caso;
    }

    public MaternityItem aItem(MaternityCase m) {
        return new MaternityItem(
                m.getId(),
                m.getEmployee().getId(),
                m.getEmployee().getNombre(),
                m.getPredio().getNombre(),
                m.getPredio().getId(),
                m.getFechaInicioIncapacidad(),
                m.getFechaProbableParto(),
                m.getDiasIncapacidad(),
                m.getEstatus(),
                m.getDisability() == null ? null : m.getDisability().getId(),
                m.getPeriod().getEtiqueta());
    }
}
