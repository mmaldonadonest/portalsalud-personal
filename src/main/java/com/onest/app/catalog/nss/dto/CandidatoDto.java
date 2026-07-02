package com.onest.app.catalog.nss.dto;

/**
 * Datos del candidato/prospecto (rama "Sin Registro de Usuario").
 * Mapea los campos que searchEmploye.php extrae de $dataP->Datos (searchProspecto).
 */
public record CandidatoDto(
        String curp,
        String nombre,
        String fechaInicioProceso,
        String estadoCandidato,
        String mensaje,
        Integer status
) {
}
