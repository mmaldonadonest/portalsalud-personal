package com.onest.app.catalog.nss.dto;

/**
 * Resultado de la busqueda por NSS. Replica la bifurcacion de searchEmploye.php:
 * - tipo = "EMPLEADO" cuando el usuario tiene registro (se devuelve {@code empleado}).
 * - tipo = "CANDIDATO" cuando "completo" == "Sin Registro de Usuario" (se devuelve {@code candidato}).
 */
public record NssSearchResponse(
        String tipo,
        EmpleadoDto empleado,
        CandidatoDto candidato
) {

    public static NssSearchResponse empleado(EmpleadoDto empleado) {
        return new NssSearchResponse("EMPLEADO", empleado, null);
    }

    public static NssSearchResponse candidato(CandidatoDto candidato) {
        return new NssSearchResponse("CANDIDATO", null, candidato);
    }
}
