package mx.saludocupacional.portal.disability.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.domain.DisabilityType;
import mx.saludocupacional.portal.catalog.repository.AreaRepository;
import mx.saludocupacional.portal.catalog.repository.CuentaRepository;
import mx.saludocupacional.portal.catalog.repository.DisabilityTypeRepository;
import mx.saludocupacional.portal.disability.domain.Disability;
import mx.saludocupacional.portal.disability.domain.Disability.Origen;
import mx.saludocupacional.portal.disability.domain.DisabilityCost;
import mx.saludocupacional.portal.disability.repository.DisabilityRepository;
import mx.saludocupacional.portal.disability.web.dto.DisabilityDtos.DisabilityItem;
import mx.saludocupacional.portal.disability.web.dto.DisabilityDtos.DisabilityRequest;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.service.EmployeeService;
import mx.saludocupacional.portal.security.service.PredioScopeService;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import mx.saludocupacional.portal.shared.service.PeriodService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Registro y consulta de incapacidades.
 *
 * <p>Concentra los días perdidos de toda la organización, vengan de una
 * enfermedad general, de un accidente o de un caso de maternidad. Los dos
 * últimos entran por métodos dedicados que solo su módulo dueño invoca, de modo
 * que un mismo hecho nunca se captura dos veces.
 */
@Service
@RequiredArgsConstructor
public class DisabilityService {

    private static final String MODULO = "incapacidades";

    private final DisabilityRepository repository;
    private final DisabilityTypeRepository tipos;
    private final CuentaRepository cuentas;
    private final AreaRepository areas;
    private final EmployeeService employeeService;
    private final PeriodService periodService;
    private final PredioScopeService alcance;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<DisabilityItem> buscar(Integer anio, Integer mes, Long predioId,
                                       Long tipoId, Pageable pageable) {
        return repository.buscar(anio, mes, predioId, tipoId, alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public DisabilityItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<DisabilityItem> historialDe(Long empleadoId) {
        return repository.findByEmployeeIdAndDeletedAtIsNullOrderByFechaInicioDesc(empleadoId)
                .stream().map(this::aItem).toList();
    }

    /**
     * Registra una incapacidad capturada directamente.
     *
     * <p>Solo admite origen manual. Las derivadas de un accidente o de
     * maternidad se crean desde su propio módulo.
     */
    @Transactional
    public DisabilityItem crear(DisabilityRequest request, Long usuarioId) {
        Employee empleado = employeeService.obtener(request.empleadoId());
        DisabilityType tipo = tipos.findById(request.tipoId())
                .orElseThrow(() -> new ResourceNotFoundException("tipo de incapacidad", request.tipoId()));

        rechazarTipoDerivado(tipo);
        validarFechas(request.fechaInicio(), request.fechaFin());

        Disability incapacidad = construir(empleado, tipo, request.fechaInicio(),
                request.fechaFin(), request.dias(), Origen.MANUAL, null);
        incapacidad.setFolioImss(request.folioImss());
        incapacidad.setEsInterna(Boolean.TRUE.equals(request.esInterna()));

        if (request.cuentaId() != null) {
            incapacidad.setCuenta(cuentas.findById(request.cuentaId())
                    .orElseThrow(() -> new ResourceNotFoundException("cuenta", request.cuentaId())));
        }
        if (request.areaId() != null) {
            incapacidad.setArea(areas.findById(request.areaId())
                    .orElseThrow(() -> new ResourceNotFoundException("área", request.areaId())));
        }

        Disability guardada = repository.save(incapacidad);
        asignarCosto(guardada, request.costo());

        auditService.registrar(AuditAction.CREATE, MODULO, "Disability",
                guardada.getId(), null, null, usuarioId, true);
        return aItem(guardada);
    }

    /**
     * Crea la incapacidad que corresponde a un accidente.
     *
     * <p>Solo debe invocarse desde el módulo de accidentabilidad, dentro de la
     * misma transacción que registra el accidente.
     */
    @Transactional
    public Disability crearDesdeAccidente(Employee empleado, DisabilityType tipo, LocalDate fechaInicio,
                                          Integer dias, Long accidenteId) {
        Disability incapacidad = construir(empleado, tipo, fechaInicio, null, dias,
                Origen.ACCIDENTE, accidenteId);
        return repository.save(incapacidad);
    }

    /**
     * Crea la incapacidad que corresponde a un caso de maternidad.
     *
     * <p>Solo debe invocarse desde el módulo de maternidad, dentro de la misma
     * transacción que registra el caso.
     */
    @Transactional
    public Disability crearDesdeMaternidad(Employee empleado, DisabilityType tipo, LocalDate fechaInicio,
                                           Integer dias, Long casoId) {
        Disability incapacidad = construir(empleado, tipo, fechaInicio, null, dias,
                Origen.MATERNIDAD, casoId);
        return repository.save(incapacidad);
    }

    @Transactional
    public DisabilityItem actualizar(Long id, DisabilityRequest request, Long usuarioId) {
        Disability incapacidad = obtener(id);

        if (incapacidad.esGeneradaPorOtroModulo()) {
            throw new BusinessRuleException(
                    "Esta incapacidad proviene de %s; modifícala desde ese módulo"
                            .formatted(incapacidad.getOrigenTipo() == Origen.ACCIDENTE
                                    ? "un accidente" : "un caso de maternidad"));
        }
        validarFechas(request.fechaInicio(), request.fechaFin());

        incapacidad.setFechaInicio(request.fechaInicio());
        incapacidad.setFechaFin(request.fechaFin());
        incapacidad.setDiasIncapacidad(request.dias());
        incapacidad.setFolioImss(request.folioImss());
        incapacidad.setPeriod(periodService.resolverParaCaptura(request.fechaInicio()));

        Disability guardada = repository.save(incapacidad);
        asignarCosto(guardada, request.costo());

        auditService.registrar(AuditAction.UPDATE, MODULO, "Disability",
                id, null, null, usuarioId, true);
        return aItem(guardada);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Disability incapacidad = obtener(id);
        if (incapacidad.esGeneradaPorOtroModulo()) {
            throw new BusinessRuleException(
                    "Esta incapacidad proviene de otro módulo; elimina el registro de origen");
        }
        incapacidad.markDeleted();
        repository.save(incapacidad);

        auditService.registrar(AuditAction.DELETE, MODULO, "Disability",
                id, null, null, usuarioId, true);
    }

    /** Elimina la incapacidad enlazada a un hecho que se está dando de baja. */
    @Transactional
    public void eliminarPorOrigen(Disability incapacidad) {
        if (incapacidad != null) {
            incapacidad.markDeleted();
            repository.save(incapacidad);
        }
    }

    @Transactional(readOnly = true)
    public Disability obtener(Long id) {
        Disability incapacidad = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("incapacidad", id));
        alcance.verificarAcceso(incapacidad.getPredio().getId());
        return incapacidad;
    }

    private Disability construir(Employee empleado, DisabilityType tipo, LocalDate fechaInicio,
                                 LocalDate fechaFin, Integer dias, Origen origen, Long origenId) {
        Period periodo = periodService.resolverParaCaptura(fechaInicio);
        alcance.verificarAcceso(empleado.getPredio().getId());

        Disability incapacidad = new Disability();
        incapacidad.setEmployee(empleado);
        incapacidad.setPredio(empleado.getPredio());
        incapacidad.setCuenta(empleado.getCuenta());
        incapacidad.setArea(empleado.getArea());
        incapacidad.setPeriod(periodo);
        incapacidad.setDisabilityType(tipo);
        incapacidad.setFechaInicio(fechaInicio);
        incapacidad.setFechaFin(fechaFin);
        incapacidad.setDiasIncapacidad(dias);
        incapacidad.setOrigenTipo(origen);
        incapacidad.setOrigenId(origenId);
        return incapacidad;
    }

    private void asignarCosto(Disability incapacidad, BigDecimal monto) {
        if (monto == null) {
            return;
        }
        DisabilityCost costo = incapacidad.getCost();
        if (costo == null) {
            costo = new DisabilityCost();
            costo.setDisability(incapacidad);
            incapacidad.setCost(costo);
        }
        costo.setMonto(monto);
        repository.save(incapacidad);
    }

    private void rechazarTipoDerivado(DisabilityType tipo) {
        String codigo = tipo.getCodigo();
        if ("ACC_LABORAL".equals(codigo) || "ACC_TRAYECTO".equals(codigo)) {
            throw new BusinessRuleException(
                    "Las incapacidades por accidente se generan al registrar el accidente");
        }
        if ("MATERNIDAD".equals(codigo)) {
            throw new BusinessRuleException(
                    "Las incapacidades por maternidad se generan al registrar el caso");
        }
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin != null && fin.isBefore(inicio)) {
            throw new BusinessRuleException("La fecha de término no puede ser anterior al inicio");
        }
    }

    public DisabilityItem aItem(Disability d) {
        return new DisabilityItem(
                d.getId(),
                d.getEmployee().getId(),
                d.getEmployee().getNombre(),
                d.getEmployee().getNumeroEmpleado(),
                d.getPredio().getNombre(),
                d.getPredio().getId(),
                d.getDisabilityType().getNombre(),
                d.getFechaInicio(),
                d.getFechaFin(),
                d.getDiasIncapacidad(),
                d.getHorasNoTrabajadas(),
                d.getFolioImss(),
                d.isEsInterna(),
                d.getOrigenTipo(),
                d.getOrigenId(),
                d.getCost() == null ? null : d.getCost().getMonto(),
                d.getPeriod().getEtiqueta());
    }
}
