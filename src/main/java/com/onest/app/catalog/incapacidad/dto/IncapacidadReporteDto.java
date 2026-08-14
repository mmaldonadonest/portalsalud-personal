package com.onest.app.catalog.incapacidad.dto;

/**
 * Fila del reporte de incapacidades por rango de fechas (todas las NSS, no una sola).
 * Equivale a cada elemento de Datos de traerDatosGeneralIncap (consulta_incapacidades_fecha),
 * pensado para RH/contabilidad, no para el medico tratante. Ver docs/contextoWS.txt:2259-2388.
 */
public record IncapacidadReporteDto(
        String fechaConsulta,
        String folioIncapacidad,
        String nss,
        String nombre,
        String rfc,
        String curp,
        String rubro,
        String ramo,
        String tipoIncapacidad,
        String fechaInicio,
        String fechaTermino,
        String diasAutorizados,
        String salarioIntegrado,
        String costo,
        String estadoDictamen
) {
}
