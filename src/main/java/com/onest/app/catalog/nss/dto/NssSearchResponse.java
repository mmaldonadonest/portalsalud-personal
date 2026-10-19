package com.onest.app.catalog.nss.dto;

import java.util.List;

/**
 * Resultado de la busqueda por NSS. Replica la bifurcacion de searchEmploye.php:
 * - tipo = "EMPLEADO" cuando el usuario tiene registro (se devuelve {@code empleado}).
 * - tipo = "CANDIDATO" cuando "completo" == "Sin Registro de Usuario" (se devuelve {@code candidato}).
 *
 * <p>{@code asociaciones} son TODAS las relaciones laborales (predio/cuenta) del NSS -
 * {@code empleado} es siempre la primera (misma persona, mismos datos de identidad; solo
 * cuenta/puesto pueden variar entre asociaciones). Un NSS "normal" tiene una sola asociacion;
 * mas de una es el caso multipredio (docs/entregable-liberacion-stoppers-salud.html §4).
 */
public record NssSearchResponse(
        String tipo,
        EmpleadoDto empleado,
        CandidatoDto candidato,
        List<EmpleadoDto> asociaciones,
        /** Solicitud de examen inicial de OCR ligada al NSS (viva o la ultima), o null. */
        com.onest.app.integration.ocr.SolicitudExamen solicitudOcr,
        /** true cuando la persona NO esta en el biometrico y la ficha se armo con la solicitud de OCR. */
        boolean candidatoOcr
) {

    public static NssSearchResponse empleado(EmpleadoDto empleado, List<EmpleadoDto> asociaciones) {
        return new NssSearchResponse("EMPLEADO", empleado, null, asociaciones, null, false);
    }

    public static NssSearchResponse candidato(CandidatoDto candidato) {
        return new NssSearchResponse("CANDIDATO", null, candidato, List.of(), null, false);
    }

    /** Candidato que solo existe en la solicitud de OCR: se presenta como ficha (tipo EMPLEADO) para abrir los modulos. */
    public static NssSearchResponse candidatoOcr(EmpleadoDto ficha, com.onest.app.integration.ocr.SolicitudExamen solicitud) {
        return new NssSearchResponse("EMPLEADO", ficha, null, List.of(ficha), solicitud, true);
    }

    public NssSearchResponse conSolicitud(com.onest.app.integration.ocr.SolicitudExamen solicitud) {
        return new NssSearchResponse(tipo, empleado, candidato, asociaciones, solicitud, candidatoOcr);
    }
}
