package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Elemento del catalogo ICD. La respuesta de .../Servcio/indice es un ARRAY
 * de estos objetos (no envuelto en Datos): php hace foreach ($data as $filedat).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsIcd(
        @JsonProperty("clave_id") String claveId,
        @JsonProperty("nombre_clave") String nombreClave
) {
}
