package mx.saludocupacional.portal.accident.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.accident.domain.Accident;
import mx.saludocupacional.portal.accident.repository.AccidentRepository;
import mx.saludocupacional.portal.accident.web.dto.AccidentDtos.AccidentItem;
import mx.saludocupacional.portal.accident.web.dto.AccidentDtos.AccidentRequest;
import mx.saludocupacional.portal.catalog.domain.AccidentType;
import mx.saludocupacional.portal.catalog.domain.DisabilityType;
import mx.saludocupacional.portal.catalog.repository.AccidentCauseRepository;
import mx.saludocupacional.portal.catalog.repository.AccidentStatusRepository;
import mx.saludocupacional.portal.catalog.repository.AccidentTypeRepository;
import mx.saludocupacional.portal.catalog.repository.DisabilityTypeRepository;
import mx.saludocupacional.portal.disability.domain.Disability;
import mx.saludocupacional.portal.disability.service.DisabilityService;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.service.EmployeeService;
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
 * Registro y consulta de accidentabilidad.
 *
 * <p>Un accidente que incapacita produce dos registros en una sola transacción:
 * el accidente y su incapacidad enlazada. El usuario captura una vez; el sistema
 * mantiene la relación. Si la transacción falla, no queda ninguno de los dos.
 */
@Service
@RequiredArgsConstructor
public class AccidentService {

    private static final String MODULO = "accidentabilidad";
    private static final String CODIGO_LABORAL = "LABORAL";

    private final AccidentRepository repository;
    private final AccidentTypeRepository tipos;
    private final AccidentCauseRepository causas;
    private final AccidentStatusRepository estatus;
    private final DisabilityTypeRepository tiposIncapacidad;
    private final DisabilityService disabilityService;
    private final EmployeeService employeeService;
    private final PeriodService periodService;
    private final PredioScopeService alcance;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<AccidentItem> buscar(Integer anio, Integer mes, Long predioId,
                                     Long tipoId, Pageable pageable) {
        return repository.buscar(anio, mes, predioId, tipoId, alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public AccidentItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<AccidentItem> historialDe(Long empleadoId) {
        return repository.findByEmployeeIdAndDeletedAtIsNullOrderByFechaAccidenteDesc(empleadoId)
                .stream().map(this::aItem).toList();
    }

    /**
     * Registra un accidente y, si procede, su incapacidad enlazada.
     *
     * <p>Ambos registros comparten transacción: nunca queda un accidente que
     * declara días perdidos sin la incapacidad que los sustenta.
     */
    @Transactional
    public AccidentItem crear(AccidentRequest request, Long usuarioId) {
        Employee empleado = employeeService.obtener(request.empleadoId());
        alcance.verificarAcceso(empleado.getPredio().getId());

        AccidentType tipo = tipos.findById(request.tipoId())
                .orElseThrow(() -> new ResourceNotFoundException("tipo de accidente", request.tipoId()));

        Accident accidente = new Accident();
        accidente.setEmployee(empleado);
        accidente.setPredio(empleado.getPredio());
        accidente.setCuenta(empleado.getCuenta());
        accidente.setArea(empleado.getArea());
        accidente.setPuesto(empleado.getPuesto());
        accidente.setPeriod(periodService.resolverParaCaptura(request.fechaAccidente()));
        accidente.setFechaAccidente(request.fechaAccidente());
        accidente.setAccidentType(tipo);
        accidente.setAccidentCause(causas.findById(request.causaId())
                .orElseThrow(() -> new ResourceNotFoundException("causa de accidente", request.causaId())));
        accidente.setAccidentStatus(estatus.findById(request.estatusId())
                .orElseThrow(() -> new ResourceNotFoundException("estatus de accidente", request.estatusId())));
        accidente.setGenero(request.genero() != null ? request.genero() : empleado.getGenero());
        accidente.setCostoCalificado(request.costoCalificado());
        accidente.setCostoImprocedente(request.costoImprocedente());
        accidente.setDescripcion(request.descripcion());

        Accident guardado = repository.save(accidente);

        if (request.diasIncapacidad() != null && request.diasIncapacidad() > 0) {
            DisabilityType tipoIncapacidad = tipoIncapacidadPara(tipo);
            Disability incapacidad = disabilityService.crearDesdeAccidente(
                    empleado, tipoIncapacidad, request.fechaAccidente(),
                    request.diasIncapacidad(), guardado.getId());

            guardado.setDisability(incapacidad);
            guardado.setGeneraIncapacidad(true);
            guardado = repository.save(guardado);
        }

        auditService.registrar(AuditAction.CREATE, MODULO, "Accident",
                guardado.getId(), null, null, usuarioId, true);
        return aItem(guardado);
    }

    @Transactional
    public AccidentItem actualizar(Long id, AccidentRequest request, Long usuarioId) {
        Accident accidente = obtener(id);

        accidente.setFechaAccidente(request.fechaAccidente());
        accidente.setPeriod(periodService.resolverParaCaptura(request.fechaAccidente()));
        accidente.setAccidentCause(causas.findById(request.causaId())
                .orElseThrow(() -> new ResourceNotFoundException("causa de accidente", request.causaId())));
        accidente.setAccidentStatus(estatus.findById(request.estatusId())
                .orElseThrow(() -> new ResourceNotFoundException("estatus de accidente", request.estatusId())));
        accidente.setCostoCalificado(request.costoCalificado());
        accidente.setCostoImprocedente(request.costoImprocedente());
        accidente.setDescripcion(request.descripcion());

        Accident guardado = repository.save(accidente);

        auditService.registrar(AuditAction.UPDATE, MODULO, "Accident",
                id, null, null, usuarioId, true);
        return aItem(guardado);
    }

    /** Da de baja el accidente y, con él, la incapacidad que hubiera generado. */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Accident accidente = obtener(id);

        disabilityService.eliminarPorOrigen(accidente.getDisability());
        accidente.markDeleted();
        repository.save(accidente);

        auditService.registrar(AuditAction.DELETE, MODULO, "Accident",
                id, null, null, usuarioId, true);
    }

    @Transactional(readOnly = true)
    public Accident obtener(Long id) {
        Accident accidente = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("accidente", id));
        alcance.verificarAcceso(accidente.getPredio().getId());
        return accidente;
    }

    /** Un accidente laboral genera incapacidad de riesgo de trabajo; el de trayecto, la suya. */
    private DisabilityType tipoIncapacidadPara(AccidentType tipoAccidente) {
        String codigo = CODIGO_LABORAL.equals(tipoAccidente.getCodigo())
                ? "ACC_LABORAL" : "ACC_TRAYECTO";
        return tiposIncapacidad.findByCodigo(codigo)
                .orElseThrow(() -> new BusinessRuleException(
                        "No está configurado el tipo de incapacidad «%s»".formatted(codigo)));
    }

    public AccidentItem aItem(Accident a) {
        return new AccidentItem(
                a.getId(),
                a.getEmployee().getId(),
                a.getEmployee().getNombre(),
                a.getPredio().getNombre(),
                a.getPredio().getId(),
                a.getFechaAccidente(),
                a.getAccidentType().getNombre(),
                a.getAccidentCause().getNombre(),
                a.getAccidentStatus().getNombre(),
                a.getGenero(),
                a.isGeneraIncapacidad(),
                a.getDisability() == null ? null : a.getDisability().getId(),
                a.getDiasPerdidos(),
                a.getCostoCalificado(),
                a.getCostoImprocedente(),
                a.getPeriod().getEtiqueta());
    }
}
