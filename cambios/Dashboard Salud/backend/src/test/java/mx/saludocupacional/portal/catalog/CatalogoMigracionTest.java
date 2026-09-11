package mx.saludocupacional.portal.catalog;

import mx.saludocupacional.portal.IntegrationTestBase;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.catalog.repository.AttentionCauseRepository;
import mx.saludocupacional.portal.catalog.repository.CuentaRepository;
import mx.saludocupacional.portal.catalog.repository.InjuryTypeRepository;
import mx.saludocupacional.portal.catalog.repository.PredioRepository;
import mx.saludocupacional.portal.shared.repository.PeriodRepository;
import mx.saludocupacional.portal.shared.service.ThresholdService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que las migraciones dejan cargados los catálogos reales extraídos
 * del archivo de morbilidad.
 */
class CatalogoMigracionTest extends IntegrationTestBase {

    @Autowired PredioRepository predios;
    @Autowired CuentaRepository cuentas;
    @Autowired AttentionCauseRepository causas;
    @Autowired InjuryTypeRepository lesiones;
    @Autowired PeriodRepository periodos;
    @Autowired ThresholdService umbrales;

    @Test
    @DisplayName("Carga los diecisiete predios del archivo origen")
    void cargaPredios() {
        assertThat(predios.findAll()).hasSize(17);
        assertThat(predios.findByNombreIgnoreCase("AIFA")).isPresent();
        assertThat(predios.findByNombreIgnoreCase("MACRO II")).isPresent();
    }

    @Test
    @DisplayName("Resuelve las abreviaturas de predio usadas en la hoja de abastecimiento")
    void resuelveAlias() {
        Optional<Predio> porAlias = predios.findByNombreOrAlias("M2");
        assertThat(porAlias).isPresent();
        assertThat(porAlias.get().getNombre()).isEqualTo("MACRO II");

        assertThat(predios.findByNombreOrAlias("TULTI"))
                .map(Predio::getNombre).contains("TULTIPARK");
        assertThat(predios.findByNombreOrAlias("Z VALL"))
                .map(Predio::getNombre).contains("Z VALLEJO");
    }

    @Test
    @DisplayName("Carga las veintiséis causas de atención con su categoría")
    void cargaCausas() {
        assertThat(causas.findAll()).hasSize(26);
        assertThat(causas.findByNombreIgnoreCase("RESPIRATORIO"))
                .hasValueSatisfying(c -> assertThat(c.getCategoria()).isEqualTo("RESPIRATORIO"));
        assertThat(causas.findCategorias()).contains("MUSCULOESQUELETICO", "PREVENTIVO");
    }

    @Test
    @DisplayName("Carga los quince tipos de lesión con su región corporal")
    void cargaLesiones() {
        assertThat(lesiones.findAll()).hasSize(15);
        assertThat(lesiones.findByNombreIgnoreCase("ESGUINCE MT"))
                .hasValueSatisfying(l -> assertThat(l.getRegionCorporal()).isEqualTo("MT"));
    }

    @Test
    @DisplayName("Carga las cincuenta y ocho cuentas corporativas")
    void cargaCuentas() {
        assertThat(cuentas.findAll()).hasSize(58);
    }

    @Test
    @DisplayName("Genera los periodos de dos mil veinticinco a dos mil veintisiete")
    void generaPeriodos() {
        assertThat(periodos.findAll()).hasSize(36);
        assertThat(periodos.findByAnioAndMes(2026, 6))
                .hasValueSatisfying(p -> assertThat(p.getEtiqueta()).isEqualTo("JUN 2026"));
        assertThat(periodos.findAniosDisponibles()).containsExactly(2027, 2026, 2025);
    }

    @Test
    @DisplayName("Los umbrales de riesgo viven en la base, no en el código")
    void cargaUmbrales() {
        assertThat(umbrales.valorEntero(ThresholdService.MORBILIDAD_CRITICO)).isEqualTo(280);
        assertThat(umbrales.valorEntero(ThresholdService.CADUCIDAD_CRITICO)).isEqualTo(30);
        assertThat(umbrales.valorEntero(ThresholdService.STOCK_MINIMO)).isEqualTo(20);
    }
}
