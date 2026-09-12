package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * Lesiones musculoesqueleticas en el rango [fechaInicial,fechaFinal]: consultas medicas cuyo
 * diagnostico trae clave CIE-10 de los capitulos M/S/T (ver LesionMusculoesqueletica).
 * Consultas sin clave no se clasifican - se reportan aparte en {@code sinDiagnostico} para
 * que el analista sepa cuanto del periodo queda fuera del conteo.
 */
public record DashboardMusculoesqueleticoDto(
        String fechaInicial,
        String fechaFinal,
        /** Consultas del periodo (tras el corte por predio/cuenta). */
        long totalConsultas,
        /** Consultas del periodo sin clave CIE-10 al inicio del diagnostico. */
        long sinDiagnostico,
        /** Consultas clasificadas como lesion musculoesqueletica. */
        long totalLesiones,
        long totalPersonas,
        long algiasContusiones,
        long columna,
        long traumatismos,
        long fracturasAmputaciones,
        List<ConteoSimpleDto> porTipo,
        List<ConteoSimpleDto> porRegion,
        List<ConteoSimpleDto> porPredio,
        List<PuntoMensualDto> tendenciaMensual,
        /** Claves CIE-10 (3 caracteres) mas frecuentes con su descripcion capturada. */
        List<ConteoSimpleDto> topDiagnosticos
) {
}
