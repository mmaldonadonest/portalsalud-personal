package mx.saludocupacional.portal.drugtest.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.catalog.repository.AgenciaRepository;
import mx.saludocupacional.portal.catalog.repository.DrugTestResultRepository;
import mx.saludocupacional.portal.catalog.repository.DrugTestStatusRepository;
import mx.saludocupacional.portal.catalog.repository.DrugTestTypeRepository;
import mx.saludocupacional.portal.drugtest.domain.DrugTest;
import mx.saludocupacional.portal.drugtest.domain.DrugTestBatch;
import mx.saludocupacional.portal.drugtest.repository.DrugTestRepository;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.DrugTestItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.DrugTestRequest;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.service.EmployeeService;
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
 * Registro y consulta de pruebas de antidoping y alcoholimetría.
 *
 * <p>Cuando la prueba proviene de un lote, el descuento de inventario ocurre en
 * la misma transacción: no existe forma de aplicar una prueba sin que la
 * existencia lo refleje.
 */
@Service
@RequiredArgsConstructor
public class DrugTestService {

    private static final String MODULO = "antidoping";

    private final DrugTestRepository repository;
    private final DrugTestTypeRepository tipos;
    private final DrugTestResultRepository resultados;
    private final DrugTestStatusRepository estatus;
    private final AgenciaRepository agencias;
    private final InventoryService inventoryService;
    private final EmployeeService employeeService;
    private final PeriodService periodService;
    private final PredioScopeService alcance;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<DrugTestItem> buscar(Integer anio, Integer mes, Long predioId,
                                     Long tipoId, Pageable pageable) {
        return repository.buscar(anio, mes, predioId, tipoId, alcance.filtroDePredios(), pageable)
                .map(this::aItem);
    }

    @Transactional(readOnly = true)
    public DrugTestItem detalle(Long id) {
        return aItem(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<DrugTestItem> historialDe(Long empleadoId) {
        return repository.findByEmployeeIdAndDeletedAtIsNullOrderByFechaPruebaDesc(empleadoId)
                .stream().map(this::aItem).toList();
    }

    @Transactional
    public DrugTestItem crear(DrugTestRequest request, Long usuarioId) {
        Employee empleado = employeeService.obtener(request.empleadoId());
        alcance.verificarAcceso(empleado.getPredio().getId());

        DrugTest prueba = new DrugTest();
        prueba.setEmployee(empleado);
        prueba.setPredio(empleado.getPredio());
        prueba.setCuenta(empleado.getCuenta());
        prueba.setArea(empleado.getArea());
        prueba.setPuesto(empleado.getPuesto());
        prueba.setFechaPrueba(request.fechaPrueba());
        prueba.setPeriod(periodService.resolverParaCaptura(request.fechaPrueba()));

        prueba.setTestType(tipos.findById(request.tipoId())
                .orElseThrow(() -> new ResourceNotFoundException("tipo de prueba", request.tipoId())));

        if (request.resultadoId() != null) {
            prueba.setResult(resultados.findById(request.resultadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("resultado", request.resultadoId())));
        }
        if (request.estatusId() != null) {
            prueba.setStatus(estatus.findById(request.estatusId())
                    .orElseThrow(() -> new ResourceNotFoundException("estatus", request.estatusId())));
        }
        prueba.setAgencia(request.agenciaId() != null
                ? agencias.findById(request.agenciaId())
                    .orElseThrow(() -> new ResourceNotFoundException("agencia", request.agenciaId()))
                : empleado.getAgencia());

        // El descuento de inventario comparte transacción con el alta de la prueba.
        if (request.loteId() != null) {
            DrugTestBatch lote = inventoryService.obtenerLote(request.loteId());
            prueba.setBatch(lote);
            inventoryService.consumirUnidad(lote, request.fechaPrueba(),
                    "Prueba aplicada a " + empleado.getNombre(), usuarioId);
        }

        DrugTest guardada = repository.save(prueba);
        auditService.registrar(AuditAction.CREATE, MODULO, "DrugTest",
                guardada.getId(), null, null, usuarioId, true);
        return aItem(guardada);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        DrugTest prueba = obtener(id);
        prueba.markDeleted();
        repository.save(prueba);

        auditService.registrar(AuditAction.DELETE, MODULO, "DrugTest",
                id, null, null, usuarioId, true);
    }

    @Transactional(readOnly = true)
    public DrugTest obtener(Long id) {
        DrugTest prueba = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("prueba", id));
        alcance.verificarAcceso(prueba.getPredio().getId());
        return prueba;
    }

    public DrugTestItem aItem(DrugTest t) {
        return new DrugTestItem(
                t.getId(),
                t.getEmployee().getId(),
                t.getEmployee().getNombre(),
                t.getPredio().getNombre(),
                t.getPredio().getId(),
                t.getFechaPrueba(),
                t.getTestType().getNombre(),
                t.getResult() == null ? null : t.getResult().getNombre(),
                t.getStatus() == null ? null : t.getStatus().getNombre(),
                t.getAgencia() == null ? null : t.getAgencia().getNombre(),
                t.getBatch() == null ? null : t.getBatch().getLote(),
                t.getPeriod().getEtiqueta());
    }
}
