package com.onest.app.catalog.expediente.dto;

/**
 * Detalle completo de una consulta medica.
 * Equivale a los campos que viewConsultaDetails.php extrae de Datos.
 */
public record ConsultaDetalleDto(
        String idConsulta,
        String fechaConsulta,
        String tipoConsulta,
        String areaAccidente,
        String areaInvolucrada,
        String causa,
        String peso,
        String talla,
        String imc,
        String fc,
        String fr,
        String ta,
        String temperatura,
        String motivo,
        String exploracion,
        String diagnostico,
        String tratamiento,
        String consultaRelacionada,
        String firmaDigital
) {
}
