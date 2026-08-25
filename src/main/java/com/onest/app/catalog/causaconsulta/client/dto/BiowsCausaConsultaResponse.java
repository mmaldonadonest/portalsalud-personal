package com.onest.app.catalog.causaconsulta.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_causa_consulta. Ver docs/ords-causa-consulta.sql BLOQUE 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsCausaConsultaResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("nombre") String nombre,
            @JsonProperty("activo") String activo,
            @JsonProperty("fecha_alta") String fechaAlta,
            @JsonProperty("fecha_baja") String fechaBaja,
            @JsonProperty("usuario") String usuario
    ) {
    }
}
