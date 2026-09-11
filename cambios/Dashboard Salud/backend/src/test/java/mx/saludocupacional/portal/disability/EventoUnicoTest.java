package mx.saludocupacional.portal.disability;

import mx.saludocupacional.portal.IntegrationTestBase;
import mx.saludocupacional.portal.accident.service.AccidentService;
import mx.saludocupacional.portal.accident.web.dto.AccidentDtos.AccidentItem;
import mx.saludocupacional.portal.accident.web.dto.AccidentDtos.AccidentRequest;
import mx.saludocupacional.portal.catalog.repository.AccidentCauseRepository;
import mx.saludocupacional.portal.catalog.repository.AccidentStatusRepository;
import mx.saludocupacional.portal.catalog.repository.AccidentTypeRepository;
import mx.saludocupacional.portal.catalog.repository.DisabilityTypeRepository;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.disability.domain.Disability.Origen;
import mx.saludocupacional.portal.disability.repository.DisabilityRepository;
import mx.saludocupacional.portal.disability.service.DisabilityService;
import mx.saludocupacional.portal.disability.web.dto.DisabilityDtos.DisabilityRequest;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.repository.EmployeeRepository;
import mx.saludocupacional.portal.maternity.service.MaternityService;
import mx.saludocupacional.portal.maternity.web.dto.MaternityDtos.MaternityItem;
import mx.saludocupacional.portal.maternity.web.dto.MaternityDtos.MaternityRequest;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprueba la regla central del portal: un hecho real se captura una sola vez.
 *
 * <p>Un accidente que incapacita y un caso de maternidad generan su incapacidad
 * automáticamente. Nadie puede crearla por separado ni duplicar los días.
 */
class EventoUnicoTest extends IntegrationTestBase {

    private static final Long USUARIO = 1L;

    @Autowired AccidentService accidentes;
    @Autowired MaternityService maternidad;
    @Autowired DisabilityService incapacidades;
    @Autowired DisabilityRepository disabilityRepository;
    @Autowired EmployeeRepository empleados;
    @Autowired PredioRepository predios;
    @Autowired AccidentTypeRepository tiposAccidente;
    @Autowired AccidentCauseRepository causasAccidente;
    @Autowired AccidentStatusRepository estatusAccidente;
    @Autowired DisabilityTypeRepository tiposIncapacidad;

    private Long empleadoId;

    @BeforeEach
    void prepararColaborador() {
        Employee empleado = new Employee();
        empleado.setNombre("Colaborador de prueba");
        empleado.setNumeroEmpleado("T-" + System.nanoTime());
        empleado.setPredio(predios.findByNombreIgnoreCase("AIFA").orElseThrow());
        empleadoId = empleados.save(empleado).getId();
    }

    @Test
    @DisplayName("Un accidente con días de incapacidad genera la incapacidad enlazada")
    void accidenteGeneraIncapacidad() {
        AccidentItem accidente = accidentes.crear(new AccidentRequest(
                empleadoId,
                LocalDate.of(2026, 3, 10),
                tiposAccidente.findByCodigo("LABORAL").orElseThrow().getId(),
                causasAccidente.findByNombreIgnoreCase("CAIDA").orElseThrow().getId(),
                estatusAccidente.findByCodigo("CALIFICADO").orElseThrow().getId(),
                null, 12, null, null, "Caída en zona de embarque"), USUARIO);

        assertThat(accidente.generaIncapacidad()).isTrue();
        assertThat(accidente.incapacidadId()).isNotNull();
        assertThat(accidente.diasPerdidos()).isEqualTo(12);

        var incapacidad = disabilityRepository.findByIdAndDeletedAtIsNull(accidente.incapacidadId()).orElseThrow();
        assertThat(incapacidad.getOrigenTipo()).isEqualTo(Origen.ACCIDENTE);
        assertThat(incapacidad.getOrigenId()).isEqualTo(accidente.id());
        assertThat(incapacidad.getDiasIncapacidad()).isEqualTo(12);
        assertThat(incapacidad.getHorasNoTrabajadas()).isEqualTo(96);
    }

    @Test
    @DisplayName("Un accidente sin días de incapacidad no genera ninguna")
    void accidenteSinDiasNoGeneraIncapacidad() {
        AccidentItem accidente = accidentes.crear(new AccidentRequest(
                empleadoId,
                LocalDate.of(2026, 4, 5),
                tiposAccidente.findByCodigo("TRAYECTO").orElseThrow().getId(),
                causasAccidente.findByNombreIgnoreCase("GOLPE").orElseThrow().getId(),
                estatusAccidente.findByCodigo("PENDIENTE").orElseThrow().getId(),
                null, 0, null, null, "Sin días perdidos"), USUARIO);

        assertThat(accidente.generaIncapacidad()).isFalse();
        assertThat(accidente.incapacidadId()).isNull();
        assertThat(accidente.diasPerdidos()).isZero();
    }

    @Test
    @DisplayName("Un caso de maternidad genera su incapacidad enlazada")
    void maternidadGeneraIncapacidad() {
        MaternityItem caso = maternidad.crear(new MaternityRequest(
                empleadoId,
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 6, 20),
                84, null), USUARIO);

        assertThat(caso.incapacidadId()).isNotNull();

        var incapacidad = disabilityRepository.findByIdAndDeletedAtIsNull(caso.incapacidadId()).orElseThrow();
        assertThat(incapacidad.getOrigenTipo()).isEqualTo(Origen.MATERNIDAD);
        assertThat(incapacidad.getOrigenId()).isEqualTo(caso.id());
        assertThat(incapacidad.getDiasIncapacidad()).isEqualTo(84);
    }

    @Test
    @DisplayName("El módulo de incapacidades rechaza los tipos que genera otro módulo")
    void rechazaCapturaManualDeTipoDerivado() {
        Long tipoAccidente = tiposIncapacidad.findByCodigo("ACC_LABORAL").orElseThrow().getId();

        assertThatThrownBy(() -> incapacidades.crear(new DisabilityRequest(
                empleadoId, tipoAccidente, LocalDate.of(2026, 3, 1), null,
                10, null, false, null, null, null), USUARIO))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("al registrar el accidente");

        Long tipoMaternidad = tiposIncapacidad.findByCodigo("MATERNIDAD").orElseThrow().getId();

        assertThatThrownBy(() -> incapacidades.crear(new DisabilityRequest(
                empleadoId, tipoMaternidad, LocalDate.of(2026, 3, 1), null,
                84, null, false, null, null, null), USUARIO))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("al registrar el caso");
    }

    @Test
    @DisplayName("Una incapacidad generada por otro módulo no se edita desde incapacidades")
    void noSeEditaIncapacidadDerivada() {
        AccidentItem accidente = accidentes.crear(new AccidentRequest(
                empleadoId,
                LocalDate.of(2026, 2, 14),
                tiposAccidente.findByCodigo("LABORAL").orElseThrow().getId(),
                causasAccidente.findByNombreIgnoreCase("HERIDA").orElseThrow().getId(),
                estatusAccidente.findByCodigo("CALIFICADO").orElseThrow().getId(),
                null, 5, null, null, null), USUARIO);

        Long tipoEnfermedad = tiposIncapacidad.findByCodigo("ENF_GENERAL").orElseThrow().getId();

        assertThatThrownBy(() -> incapacidades.actualizar(accidente.incapacidadId(),
                new DisabilityRequest(empleadoId, tipoEnfermedad, LocalDate.of(2026, 2, 14),
                        null, 30, null, false, null, null, null), USUARIO))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("proviene de");
    }

    @Test
    @DisplayName("Dar de baja el accidente también da de baja su incapacidad")
    void bajaDeAccidenteArrastraLaIncapacidad() {
        AccidentItem accidente = accidentes.crear(new AccidentRequest(
                empleadoId,
                LocalDate.of(2026, 6, 1),
                tiposAccidente.findByCodigo("LABORAL").orElseThrow().getId(),
                causasAccidente.findByNombreIgnoreCase("CAIDA").orElseThrow().getId(),
                estatusAccidente.findByCodigo("CALIFICADO").orElseThrow().getId(),
                null, 7, null, null, null), USUARIO);

        Long incapacidadId = accidente.incapacidadId();
        accidentes.eliminar(accidente.id(), USUARIO);

        assertThat(disabilityRepository.findByIdAndDeletedAtIsNull(incapacidadId)).isEmpty();
    }
}
