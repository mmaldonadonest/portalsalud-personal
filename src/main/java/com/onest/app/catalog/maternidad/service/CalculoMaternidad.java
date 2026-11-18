package com.onest.app.catalog.maternidad.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * C-037 (18-nov-2026): los calculos del Excel de reporte de maternidad de Servicio Medico, tal cual
 * los dio SM. Dos bloques independientes: lo que sale de la FUM y lo que sale del inicio de la
 * incapacidad. Sin FUM no se calcula el primero; sin inicio, el segundo (asi lo pidio SM).
 *
 * <p>La misma formula vive en JavaScript (nss-search.html, materCalc) para que el formulario la
 * muestre al teclear; si se cambia aqui, cambiarla alla.
 */
public final class CalculoMaternidad {

    /** 280 dias = 40 semanas desde la FUM. */
    static final int DIAS_FPP = 280;
    /** La incapacidad prenatal empieza 42 dias antes del parto probable. */
    static final int DIAS_ANTES_PARTO = 42;
    /** 84 dias de incapacidad por maternidad (42 pre + 42 post). */
    static final int DIAS_INCAPACIDAD = 84;
    /** Termino = inicio + 83: el dia de inicio cuenta como el primero de los 84. */
    static final int DIAS_TERMINO = DIAS_INCAPACIDAD - 1;
    static final int DIAS_LACTANCIA = 180;
    /** Criterio de SM: 4.5 semanas por mes, porque no todos los meses tienen 5 semanas. */
    static final double SEMANAS_POR_MES = 4.5;
    /**
     * Pasadas 45 semanas la cuenta ya no es de gestacion (el parto ya ocurrio): no se muestra ni se
     * guarda. Es el mismo tope que usa la vista SERV_MED_V_MATERNIDAD para el promedio.
     */
    static final int SEMANAS_MAXIMO = 45;

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CalculoMaternidad() {
    }

    /** Todo lo calculado; cada campo es null si su dato de origen falta. */
    public record Resultado(
            LocalDate fum,
            Integer semanas,
            Double mes,
            LocalDate fpp,
            LocalDate fpip,
            LocalDate fprl,
            LocalDate inicio,
            LocalDate termino,
            LocalDate reinicio,
            LocalDate lactanciaInicio,
            LocalDate lactanciaFin,
            LocalDate horarioNormal) {

        public boolean vacio() {
            return fum == null && inicio == null;
        }

        /** Para la plantilla: dd/MM/aaaa o guion. */
        public String f(LocalDate d) {
            return d == null ? "—" : d.format(DMY);
        }

        public String mesTexto() {
            return mes == null ? "—" : String.format(java.util.Locale.ROOT, "%.1f", mes);
        }
    }

    public static Resultado calcular(LocalDate fum, LocalDate inicio, LocalDate hoy) {
        Integer semanas = semanas(fum, hoy);
        Double mes = semanas == null ? null : Math.round(semanas / SEMANAS_POR_MES * 10) / 10.0;
        LocalDate fpp = fum == null ? null : fum.plusDays(DIAS_FPP);
        LocalDate fpip = fpp == null ? null : fpp.minusDays(DIAS_ANTES_PARTO);
        LocalDate fprl = fpip == null ? null : fpip.plusDays(DIAS_INCAPACIDAD);

        LocalDate termino = inicio == null ? null : inicio.plusDays(DIAS_TERMINO);
        LocalDate reinicio = termino == null ? null : termino.plusDays(1);
        LocalDate lactFin = reinicio == null ? null : reinicio.plusDays(DIAS_LACTANCIA);
        LocalDate normal = lactFin == null ? null : lactFin.plusDays(1);
        return new Resultado(fum, semanas, mes, fpp, fpip, fprl, inicio, termino, reinicio, reinicio, lactFin, normal);
    }

    /** (hoy - FUM) / 7, entero. Null sin FUM, con FUM futura o pasadas 45 semanas. */
    public static Integer semanas(LocalDate fum, LocalDate hoy) {
        if (fum == null || hoy == null || fum.isAfter(hoy)) {
            return null;
        }
        long s = ChronoUnit.DAYS.between(fum, hoy) / 7;
        return s > SEMANAS_MAXIMO ? null : (int) s;
    }
}
