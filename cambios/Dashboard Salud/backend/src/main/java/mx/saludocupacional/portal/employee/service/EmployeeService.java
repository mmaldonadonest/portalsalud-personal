package mx.saludocupacional.portal.employee.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.repository.AgenciaRepository;
import mx.saludocupacional.portal.catalog.repository.AreaRepository;
import mx.saludocupacional.portal.catalog.repository.CuentaRepository;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.catalog.repository.PuestoRepository;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.repository.EmployeeRepository;
import mx.saludocupacional.portal.employee.web.dto.EmployeeDtos.EmployeeItem;
import mx.saludocupacional.portal.employee.web.dto.EmployeeDtos.EmployeeRequest;
import mx.saludocupacional.portal.security.service.PredioScopeService;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de colaboradores.
 *
 * <p>Todo evento clínico se vincula a un colaborador, de modo que este módulo es
 * la puerta de entrada del resto del portal. Está preparado para recibir
 * sincronización desde Recursos Humanos mediante {@code codigoExternoRh}.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final PredioRepository predios;
    private final CuentaRepository cuentas;
    private final AreaRepository areas;
    private final PuestoRepository puestos;
    private final AgenciaRepository agencias;
    private final AuditService auditService;
    private final PredioScopeService alcance;

    @Transactional(readOnly = true)
    public Page<EmployeeItem> buscar(String texto, Long predioId, Long cuentaId,
                                     Boolean activo, Pageable pageable) {
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();
        return repository.buscar(filtro, predioId, cuentaId, activo, alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public Employee obtener(Long id) {
        Employee empleado = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("colaborador", id));
        alcance.verificarAcceso(empleado.getPredio().getId());
        return empleado;
    }

    @Transactional(readOnly = true)
    public EmployeeItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional
    public EmployeeItem crear(EmployeeRequest request, Long usuarioId) {
        validarNumeroDisponible(request.numeroEmpleado(), null);
        alcance.verificarAcceso(request.predioId());

        Employee empleado = new Employee();
        aplicar(empleado, request);
        Employee guardado = repository.save(empleado);

        auditService.registrar(AuditAction.CREATE, "empleados", "Employee",
                guardado.getId(), null, aItem(guardado), usuarioId, false);
        return aItem(guardado);
    }

    @Transactional
    public EmployeeItem actualizar(Long id, EmployeeRequest request, Long usuarioId) {
        Employee empleado = obtener(id);
        EmployeeItem anterior = aItem(empleado);

        validarNumeroDisponible(request.numeroEmpleado(), id);
        alcance.verificarAcceso(request.predioId());

        aplicar(empleado, request);
        Employee guardado = repository.save(empleado);

        auditService.registrar(AuditAction.UPDATE, "empleados", "Employee",
                id, anterior, aItem(guardado), usuarioId, false);
        return aItem(guardado);
    }

    /** Marca al colaborador como inactivo conservando su historia clínica. */
    @Transactional
    public void darDeBaja(Long id, Long usuarioId) {
        Employee empleado = obtener(id);
        if (!empleado.isActivo()) {
            return;
        }
        empleado.setActivo(false);
        repository.save(empleado);

        auditService.registrar(AuditAction.UPDATE, "empleados", "Employee",
                id, null, aItem(empleado), usuarioId, false);
    }

    @Transactional(readOnly = true)
    public long contarActivos() {
        return repository.contarActivos();
    }

    private void validarNumeroDisponible(String numero, Long idActual) {
        if (numero == null || numero.isBlank()) {
            return;
        }
        repository.findByNumeroEmpleadoAndDeletedAtIsNull(numero.trim())
                .filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new BusinessRuleException(
                            "El número de empleado «%s» ya está registrado".formatted(numero));
                });
    }

    private void aplicar(Employee empleado, EmployeeRequest request) {
        empleado.setNumeroEmpleado(
                request.numeroEmpleado() == null || request.numeroEmpleado().isBlank()
                        ? null : request.numeroEmpleado().trim());
        empleado.setNombre(request.nombre().trim());
        empleado.setGenero(request.genero());
        empleado.setFechaNacimiento(request.fechaNacimiento());
        empleado.setFechaIngreso(request.fechaIngreso());
        empleado.setCodigoExternoRh(request.codigoExternoRh());
        if (request.activo() != null) {
            empleado.setActivo(request.activo());
        }

        empleado.setPredio(predios.findById(request.predioId())
                .orElseThrow(() -> new ResourceNotFoundException("predio", request.predioId())));

        empleado.setCuenta(request.cuentaId() == null ? null
                : cuentas.findById(request.cuentaId())
                    .orElseThrow(() -> new ResourceNotFoundException("cuenta", request.cuentaId())));

        empleado.setArea(request.areaId() == null ? null
                : areas.findById(request.areaId())
                    .orElseThrow(() -> new ResourceNotFoundException("área", request.areaId())));

        empleado.setPuesto(request.puestoId() == null ? null
                : puestos.findById(request.puestoId())
                    .orElseThrow(() -> new ResourceNotFoundException("puesto", request.puestoId())));

        empleado.setAgencia(request.agenciaId() == null ? null
                : agencias.findById(request.agenciaId())
                    .orElseThrow(() -> new ResourceNotFoundException("agencia", request.agenciaId())));
    }

    public EmployeeItem aItem(Employee e) {
        return new EmployeeItem(
                e.getId(), e.getNumeroEmpleado(), e.getNombre(), e.getGenero(), e.getEdad(),
                e.getPredio().getNombre(), e.getPredio().getId(),
                e.getCuenta() == null ? null : e.getCuenta().getNombre(),
                e.getArea() == null ? null : e.getArea().getNombre(),
                e.getPuesto() == null ? null : e.getPuesto().getNombre(),
                e.getFechaIngreso(), e.getAntiguedadAnios(), e.isActivo());
    }
}
