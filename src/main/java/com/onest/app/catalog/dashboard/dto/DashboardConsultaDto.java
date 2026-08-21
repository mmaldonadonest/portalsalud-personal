package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de consultas medicas (Morbilidad) para el dashboard ligero, calculados en el
 * rango [fechaInicial,fechaFinal] a partir de Servcio/consulta_medica_fecha (ver
 * docs/ords-consulta-dashboard.sql). CAUSA es texto clinico libre, no un catalogo
 * cerrado (ver "Para validar con Product Owner" en docs/plan-tareas-concretas.html) -
 * se agrupa tal cual viene, sin normalizar.
 */
public record DashboardConsultaDto(
        String fechaInicial,
        String fechaFinal,
        long totalConsultas,
        long totalAccidentesEmergencias,
        List<ConteoSimpleDto> porTipoConsulta,
        List<ConteoSimpleDto> porAreaAccidente,
        List<ConteoSimpleDto> porCausa,
        List<ConteoSimpleDto> porGenero,
        List<ConteoSimpleDto> porEdad,
        List<ConteoSimpleDto> porCuenta,
        List<PuntoMensualDto> tendenciaMensual
) {
}
