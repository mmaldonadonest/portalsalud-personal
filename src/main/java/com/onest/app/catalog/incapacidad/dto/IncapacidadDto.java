package com.onest.app.catalog.incapacidad.dto;

/**
 * Incapacidad (fila de la lista). Equivale a Datos de showIncapacidades
 * (consulta_incapacidad) usado por expedienteShowConsults?type=expedienteIncapacidad.
 */
public record IncapacidadDto(
        String idConsulta,
        String fechaConsulta,
        String folioIncapacidad,
        String rubro,
        String ramo,
        String tipoIncapacidad,
        String diasAutorizados,
        String fechaInicio,
        String fechaTermino,
        String costo
) {
}
