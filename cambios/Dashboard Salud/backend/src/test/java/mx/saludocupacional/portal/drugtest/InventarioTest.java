package mx.saludocupacional.portal.drugtest;

import mx.saludocupacional.portal.IntegrationTestBase;
import mx.saludocupacional.portal.catalog.repository.DrugTestTypeRepository;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.drugtest.domain.InventoryMovement.Tipo;
import mx.saludocupacional.portal.drugtest.service.DrugTestService;
import mx.saludocupacional.portal.drugtest.service.InventoryService;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.BatchItem;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.BatchRequest;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.DrugTestRequest;
import mx.saludocupacional.portal.drugtest.web.dto.DrugTestDtos.MovementRequest;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.repository.EmployeeRepository;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba que la existencia de un lote siempre se derive de sus movimientos.
 *
 * <p>Nadie edita el stock: se registra lo que entra y lo que se consume, y la
 * cifra disponible es consecuencia de esa historia.
 */
class InventarioTest extends IntegrationTestBase {

    private static final Long USUARIO = 1L;

    @Autowired InventoryService inventario;
    @Autowired DrugTestService pruebas;
    @Autowired EmployeeRepository empleados;
    @Autowired PredioRepository predios;
    @Autowired DrugTestTypeRepository tiposPrueba;

    private Long predioId;
    private Long empleadoId;

    @BeforeEach
    void prepararDatos() {
        predioId = predios.findByNombreIgnoreCase("MACRO II").orElseThrow().getId();

        Employee empleado = new Employee();
        empleado.setNombre("Colaborador de inventario");
        empleado.setNumeroEmpleado("INV-" + System.nanoTime());
        empleado.setPredio(predios.findById(predioId).orElseThrow());
        empleadoId = empleados.save(empleado).getId();
    }

    @Test
    @DisplayName("Un lote nuevo tiene disponible toda su cantidad inicial")
    void loteNuevoConservaSuCantidad() {
        BatchItem lote = crearLote("LOTE-A-" + System.nanoTime(), 100, LocalDate.now().plusMonths(8));

        assertThat(lote.disponible()).isEqualTo(100);
        assertThat(lote.consumo()).isZero();
        assertThat(lote.porcentajeDisponible()).isEqualTo(100);
        assertThat(lote.estadoCaducidad()).isEqualTo("VIGENTE");
    }

    @Test
    @DisplayName("Registrar una prueba descuenta una unidad del lote")
    void pruebaDescuentaInventario() {
        BatchItem lote = crearLote("LOTE-B-" + System.nanoTime(), 50, LocalDate.now().plusMonths(6));

        pruebas.crear(new DrugTestRequest(
                empleadoId, LocalDate.now(),
                tiposPrueba.findByCodigo("ANTIDOPING").orElseThrow().getId(),
                null, null, null, lote.id()), USUARIO);

        assertThat(inventario.disponibleDe(lote.id())).isEqualTo(49);
    }

    @Test
    @DisplayName("La existencia refleja entradas, consumos y ajustes")
    void existenciaSigueLosMovimientos() {
        BatchItem lote = crearLote("LOTE-C-" + System.nanoTime(), 20, LocalDate.now().plusMonths(10));

        inventario.registrarMovimiento(new MovementRequest(
                lote.id(), Tipo.ENTRADA, 30, LocalDate.now(), "Recepción"), USUARIO);
        assertThat(inventario.disponibleDe(lote.id())).isEqualTo(50);

        inventario.registrarMovimiento(new MovementRequest(
                lote.id(), Tipo.CONSUMO, 12, LocalDate.now(), "Aplicación en campo"), USUARIO);
        assertThat(inventario.disponibleDe(lote.id())).isEqualTo(38);

        inventario.registrarMovimiento(new MovementRequest(
                lote.id(), Tipo.AJUSTE, 2, LocalDate.now(), "Conteo físico"), USUARIO);
        assertThat(inventario.disponibleDe(lote.id())).isEqualTo(40);
    }

    @Test
    @DisplayName("No se puede consumir más de lo disponible")
    void rechazaConsumoSinExistencia() {
        BatchItem lote = crearLote("LOTE-D-" + System.nanoTime(), 5, LocalDate.now().plusMonths(4));

        assertThatThrownBy(() -> inventario.registrarMovimiento(new MovementRequest(
                lote.id(), Tipo.CONSUMO, 10, LocalDate.now(), "Excede la existencia"), USUARIO))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("unidades disponibles");
    }

    @Test
    @DisplayName("Un lote próximo a caducar aparece entre las alertas")
    void lotePorCaducarGeneraAlerta() {
        String numeroLote = "LOTE-E-" + System.nanoTime();
        crearLote(numeroLote, 40, LocalDate.now().plusDays(20));

        var alertas = inventario.alertas();

        assertThat(alertas)
                .anySatisfy(a -> {
                    assertThat(a.lote()).isEqualTo(numeroLote);
                    assertThat(a.severidad()).isEqualTo("CRITICA");
                });
    }

    @Test
    @DisplayName("No se registra un lote que ya venció")
    void rechazaLoteVencido() {
        assertThatThrownBy(() -> crearLote("LOTE-F", 10, LocalDate.now().minusDays(1)))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("vencido");
    }

    private BatchItem crearLote(String numeroLote, int cantidad, LocalDate caducidad) {
        return inventario.crearLote(new BatchRequest(numeroLote, predioId, caducidad, cantidad), USUARIO);
    }
}
