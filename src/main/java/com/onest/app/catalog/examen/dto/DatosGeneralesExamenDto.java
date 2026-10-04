package com.onest.app.catalog.examen.dto;

/**
 * Datos del examen que no viajan al WS Servcio/Medico: tipo de examen (codigo numerico del
 * legacy, ver {@code DatosGeneralesExamenService.TIPOS}), oximetria y los dos perimetros nuevos
 * de la rev. 04 del FT-SO-04. Valores tal cual estan en SERV_MED_TAG (vacio si no hay).
 */
public record DatosGeneralesExamenDto(String tipoExamen, String spo2, String cintura, String cadera) {
}
