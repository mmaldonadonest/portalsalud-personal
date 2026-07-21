package com.onest.app.catalog.expediente.dto;

/**
 * Consulta medica del historial de un NSS.
 * Equivale a cada elemento de Datos de showConsults (conuslta_medica_usuario)
 * usado por expedienteShowConsults.php?type=expedienteConsulta.
 */
public record ConsultaDto(
        String idConsulta,
        String fechaConsulta,
        String tipoConsulta,
        String areaAccidente
) {
}
