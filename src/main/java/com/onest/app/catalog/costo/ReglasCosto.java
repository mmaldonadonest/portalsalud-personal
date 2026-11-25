package com.onest.app.catalog.costo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Costo de incapacidades (C-047) y de accidentes (C-043, C-086) con las reglas que cerro Servicio Medico.
 * Funciones puras: no saben de donde sale el SDI ({@link FuenteSdi}) ni donde se guarda el resultado.
 *
 * <p>Incapacidades (P-021, P-022, P-023, P-042, P-051):
 * <ul>
 *   <li>Maternidad: 100 % de los dias.</li>
 *   <li>Enfermedad General: 60 % a partir del 4.° dia; los 3 primeros no se pagan, y eso solo en la
 *       incapacidad inicial (la subsecuente se paga al 60 % desde su primer dia).</li>
 *   <li>RT1 y RT2: 100 % desde el primer dia si el dictamen es «Si calificado»; «No calificado» como Enfermedad
 *       General (60 % a partir del 4.° dia, P-058); pendiente de calificar no se paga.</li>
 *   <li>Enfermedad profesional: 100 % de los dias (P-057).</li>
 *   <li>Lo que las reglas no cubren (rubro Interna sin ramo, tipos viejos del PHP) sigue como antes,
 *       SDI x dias al 100 %, y se marca para preguntarlo a SM.</li>
 * </ul>
 *
 * <p>Accidentes (P-029, P-030, P-031, P-041): manda el estatus de calificacion.
 * <ul>
 *   <li>Calificado: 100 % de los dias.</li>
 *   <li>Improcedente: 60 % de (dias - 3), nunca negativo.</li>
 *   <li>Pendiente: 0.</li>
 *   <li>Baja / inconcluso: abandono procedente 100 %; abandono improcedente 60 % de los dias (SM no dijo
 *       que se descuenten 3 dias: se marca para preguntarlo); sin abandono capturado, 0.</li>
 * </ul>
 */
public final class ReglasCosto {

    static final BigDecimal CIEN = BigDecimal.ONE;
    static final BigDecimal SESENTA = new BigDecimal("0.60");
    /** Dias de Enfermedad General que no se pagan al inicio (P-022) y que descuenta el accidente improcedente. */
    static final int DIAS_SIN_PAGO = 3;

    private ReglasCosto() {
    }

    /**
     * @param costo        importe con 2 decimales (0 si no se paga)
     * @param diasPagados  dias sobre los que se aplica el porcentaje
     * @param porcentaje   1.00 o 0.60 (0 si no se paga)
     * @param regla        por que salio asi, en palabras del usuario
     * @param porConfirmar true si el caso no lo cubren las reglas de SM y se aplico el criterio anterior
     */
    public record Resultado(BigDecimal costo, int diasPagados, BigDecimal porcentaje, String regla,
                            boolean porConfirmar) {
    }

    /**
     * @param ramo     enfermedad_general, maternidad, rt1, rt2, profesional (o los codigos EG/MAT de la carga)
     * @param tipo     inicial o subsecuente
     * @param dictamen si_calificado, no_calificado, pendiente_calificaciones, N/A
     */
    public static Resultado incapacidad(String ramo, String tipo, String dictamen, int dias, BigDecimal sdi) {
        String r = norm(ramo);
        if (sdi == null || sdi.signum() <= 0 || dias <= 0) {
            return new Resultado(cero(), 0, BigDecimal.ZERO, "Sin SDI o sin días: no se puede calcular.", false);
        }
        switch (r) {
            case "MATERNIDAD", "MAT" -> {
                return pago(sdi, dias, CIEN, "Maternidad: 100 % de los días.", false);
            }
            case "ENFERMEDAD_GENERAL", "ENFERMEDAD GENERAL", "EG" -> {
                if ("SUBSECUENTE".equals(norm(tipo))) {
                    return pago(sdi, dias, SESENTA, "Enfermedad General subsecuente: 60 % desde el primer día.", false);
                }
                int pagados = Math.max(dias - DIAS_SIN_PAGO, 0);
                return pago(sdi, pagados, SESENTA,
                        "Enfermedad General inicial: los 3 primeros días no se pagan; 60 % a partir del 4.° día.", false);
            }
            // SM 24-nov-2026 (preguntas 23-nov, P-057): Enfermedad profesional al 100 % de los dias
            case "PROFESIONAL", "ENFERMEDAD_PROFESIONAL", "ENFERMEDAD PROFESIONAL" -> {
                return pago(sdi, dias, CIEN, "Enfermedad profesional: 100 % de los días.", false);
            }
            case "RT1", "RT2" -> {
                String d = norm(dictamen);
                if ("SI_CALIFICADO".equals(d)) {
                    return pago(sdi, dias, CIEN, "Riesgo de trabajo calificado: 100 % desde el primer día.", false);
                }
                // SM 24-nov-2026 (P-058): «No calificado» = el IMSS no lo reconoce como RT; se paga como
                // Enfermedad General, 60 % a partir del 4.° dia
                if ("NO_CALIFICADO".equals(d)) {
                    return pago(sdi, Math.max(dias - DIAS_SIN_PAGO, 0), SESENTA,
                            "Riesgo de trabajo no calificado: como Enfermedad General, 60 % a partir del 4.° día.", false);
                }
                return new Resultado(cero(), 0, BigDecimal.ZERO,
                        "Riesgo de trabajo sin calificar: no se paga mientras no esté calificado.", false);
            }
            default -> {
                return pago(sdi, dias, CIEN,
                        "Ramo sin regla de Servicio Médico: se calcula como antes, 100 % de los días.", true);
            }
        }
    }

    /**
     * @param status   PENDIENTE, CALIFICADO, IMPROCEDENTE, BAJA_INCONCLUSO
     * @param abandono PROCEDENTE o IMPROCEDENTE (solo con BAJA_INCONCLUSO)
     */
    public static Resultado accidente(String status, String abandono, int dias, BigDecimal sdi) {
        String s = norm(status);
        if ("PENDIENTE".equals(s) || s.isEmpty()) {
            return new Resultado(cero(), 0, BigDecimal.ZERO, "Pendiente de calificar: costo 0.", false);
        }
        if (sdi == null || sdi.signum() <= 0 || dias <= 0) {
            return new Resultado(cero(), 0, BigDecimal.ZERO, "Sin SDI o sin días: no se puede calcular.", false);
        }
        switch (s) {
            case "CALIFICADO" -> {
                return pago(sdi, dias, CIEN, "Calificado: 100 % de los días.", false);
            }
            case "IMPROCEDENTE" -> {
                return pago(sdi, Math.max(dias - DIAS_SIN_PAGO, 0), SESENTA,
                        "Improcedente: 60 % de los días menos 3.", false);
            }
            case "BAJA_INCONCLUSO", "BAJA INCONCLUSO" -> {
                String a = norm(abandono);
                if ("PROCEDENTE".equals(a)) {
                    return pago(sdi, dias, CIEN, "Abandono procedente: 100 % de los días.", false);
                }
                if ("IMPROCEDENTE".equals(a)) {
                    return pago(sdi, dias, SESENTA, "Abandono improcedente: 60 % de los días.", true);
                }
                return new Resultado(cero(), 0, BigDecimal.ZERO,
                        "Baja / inconcluso sin indicar el abandono: costo 0.", false);
            }
            default -> {
                return new Resultado(cero(), 0, BigDecimal.ZERO, "Estatus sin regla: costo 0.", true);
            }
        }
    }

    private static Resultado pago(BigDecimal sdi, int dias, BigDecimal pct, String regla, boolean porConfirmar) {
        BigDecimal costo = sdi.multiply(BigDecimal.valueOf(dias)).multiply(pct).setScale(2, RoundingMode.HALF_UP);
        return new Resultado(costo, dias, pct, regla, porConfirmar);
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static String norm(String v) {
        return v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    }
}
