package com.onest.app.integration.ocr;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila de SERV_MED_EXAMEN_SOLICITUD. */
public record SolicitudExamen(
        long id, String origen, String referencia, String nss, String curp, String rfc,
        String nombre, String apellidoPaterno, String apellidoMaterno,
        LocalDate fechaNacimiento, String sexo, String estadoCivil, String escolaridad, String profesionOficio,
        String domicilio, String telefono, String empresa, String cuenta, String agencia, String puestoSolicitado,
        LocalDate fechaCita, String callbackUrl, String estatus, LocalDate caduca, String motivoCancel,
        LocalDateTime canceladoAt, String dictamen, LocalDateTime fechaExamen, Long historicoId,
        LocalDateTime notificadoAt, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_CAPTURA = "EN_CAPTURA";
    public static final String FINALIZADO = "FINALIZADO";
    public static final String CANCELADO = "CANCELADO";

    public String nombreCompleto() {
        return (nombre + " " + apellidoPaterno + " " + apellidoMaterno).trim();
    }

    public boolean activa() {
        return PENDIENTE.equals(estatus) || EN_CAPTURA.equals(estatus);
    }
}
