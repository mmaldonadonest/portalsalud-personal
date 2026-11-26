package com.onest.app.catalog.costo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Reglas de costo de SM (C-043, C-047, C-086). SDI de prueba 300.00. Las pruebas del proyecto estan apagadas en el
 * pom (skipTests): correr con {@code mvn -o test -DskipTests=false -Dtest=ReglasCostoTest}.
 */
class ReglasCostoTest {

    private static final BigDecimal SDI = new BigDecimal("300.00");

    private static String inc(String ramo, String tipo, String dictamen, int dias) {
        return ReglasCosto.incapacidad(ramo, tipo, dictamen, dias, SDI).costo().toPlainString();
    }

    private static String acc(String status, String abandono, int dias) {
        return ReglasCosto.accidente(status, abandono, dias, SDI).costo().toPlainString();
    }

    @Test
    void maternidadAlCien() {
        assertEquals("25200.00", inc("maternidad", "inicial", "N/A", 84));
    }

    @Test
    void enfermedadGeneralInicialSinLosTresPrimerosDias() {
        // 7 dias: se pagan 4 al 60 % = 300 x 4 x 0.6
        assertEquals("720.00", inc("enfermedad_general", "inicial", "N/A", 7));
        // 3 o menos: no se paga nada, nunca negativo
        assertEquals("0.00", inc("enfermedad_general", "inicial", "N/A", 3));
        assertEquals("0.00", inc("enfermedad_general", "inicial", "N/A", 1));
        // 4 dias: el 4.° ya se paga (P-022)
        assertEquals("180.00", inc("enfermedad_general", "inicial", "N/A", 4));
    }

    @Test
    void enfermedadGeneralSubsecuenteDesdeElPrimerDia() {
        // P-021: los 3 dias sin pago solo en la inicial
        assertEquals("1260.00", inc("enfermedad_general", "subsecuente", "N/A", 7));
    }

    @Test
    void riesgoDeTrabajoSoloCalificado() {
        assertEquals("2100.00", inc("rt1", "inicial", "si_calificado", 7));
        assertEquals("2100.00", inc("RT2", "subsecuente", "si_calificado", 7));
        assertEquals("0.00", inc("rt1", "inicial", "pendiente_calificaciones", 7));
        assertEquals("0.00", inc("rt2", "inicial", "N/A", 7));
        assertFalse(ReglasCosto.incapacidad("rt1", "inicial", "pendiente_calificaciones", 7, SDI).porConfirmar());
    }

    @Test
    void riesgoDeTrabajoNoCalificadoComoEnfermedadGeneral() {
        // P-058 (SM 24-nov): 7 dias -> 4 al 60 % = 720; 3 o menos -> 0
        ReglasCosto.Resultado r = ReglasCosto.incapacidad("rt1", "inicial", "no_calificado", 7, SDI);
        assertEquals("720.00", r.costo().toPlainString());
        assertFalse(r.porConfirmar());
        assertEquals("720.00", inc("rt2", "subsecuente", "no_calificado", 7));
        assertEquals("0.00", inc("rt1", "inicial", "no_calificado", 3));
    }

    @Test
    void enfermedadProfesionalAlCien() {
        // P-057 (SM 24-nov): 100 % de los dias, ya no por confirmar
        ReglasCosto.Resultado r = ReglasCosto.incapacidad("profesional", "inicial", "N/A", 7, SDI);
        assertEquals("2100.00", r.costo().toPlainString());
        assertFalse(r.porConfirmar());
    }

    @Test
    void ramoSinReglaComoAntesYPorConfirmar() {
        ReglasCosto.Resultado r = ReglasCosto.incapacidad("", "inicial", "N/A", 7, SDI);
        assertEquals("2100.00", r.costo().toPlainString());
        assertTrue(r.porConfirmar());
    }

    @Test
    void sinSdiOSinDiasNoHayCosto() {
        assertEquals("0.00", ReglasCosto.incapacidad("maternidad", "inicial", "N/A", 7, null).costo().toPlainString());
        assertEquals("0.00", inc("maternidad", "inicial", "N/A", 0));
    }

    @Test
    void accidentes() {
        assertEquals("3000.00", acc("CALIFICADO", null, 10));
        // improcedente: (10 - 3) x 300 x 0.6
        assertEquals("1260.00", acc("IMPROCEDENTE", null, 10));
        assertEquals("0.00", acc("IMPROCEDENTE", null, 2));
        assertEquals("0.00", acc("PENDIENTE", null, 10));
        assertEquals("3000.00", acc("BAJA_INCONCLUSO", "PROCEDENTE", 10));
        assertEquals("1800.00", acc("BAJA_INCONCLUSO", "IMPROCEDENTE", 10));
        assertEquals("0.00", acc("BAJA_INCONCLUSO", null, 10));
        // P-059 (SM 25-nov-2026): 60 % de todos los dias, ya confirmado
        assertFalse(ReglasCosto.accidente("BAJA_INCONCLUSO", "IMPROCEDENTE", 10, SDI).porConfirmar());
    }

    @Test
    void redondeoADosDecimales() {
        // 292.96 (mediana real de produccion) x 4 dias x 0.6 = 703.104 -> 703.10
        assertEquals("703.10", ReglasCosto.incapacidad("enfermedad_general", "inicial", "N/A", 7,
                new BigDecimal("292.96")).costo().toPlainString());
    }
}
