package com.onest.app.catalog.nss.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Catalogo/Candidatos. Campos usados por searchEmploye.php
 * al recorrer $dataP->Datos (nombre, curp, fecha_inicio_proceso, estado_candidato, mensaje, status).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsProspectoResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("nombre") String nombre,
            @JsonProperty("curp") String curp,
            @JsonProperty("fecha_inicio_proceso") String fechaInicioProceso,
            @JsonProperty("estado_candidato") String estadoCandidato,
            @JsonProperty("mensaje") String mensaje,
            @JsonProperty("status") Integer status
    ) {
    }
}
