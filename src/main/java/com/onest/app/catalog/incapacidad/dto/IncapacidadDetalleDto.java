package com.onest.app.catalog.incapacidad.dto;

/**
 * Detalle completo de una incapacidad. Equivale a viewIncapDetails.php.
 */
public record IncapacidadDetalleDto(
        String idConsulta,
        String fechaConsulta,
        String folioIncapacidad,
        String ramo,
        String tipoIncapacidad,
        String fechaInicio,
        String fechaTermino,
        String diasAutorizados,
        String salarioIntegrado,
        String costo,
        String imputable,
        String estadoDictamen,
        String goceSueldo,
        String complementoSalarial,
        String rubro,
        String fechaAlta,
        String st2,
        String alta,
        String salarioAcumulado,
        String urlArchivos,
        String firmaDigital
) {
}
