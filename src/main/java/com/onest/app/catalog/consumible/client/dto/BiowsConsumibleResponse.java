package com.onest.app.catalog.consumible.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_consumibles. Ver docs/ords-antidoping-consumibles.sql BLOQUE 2.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsConsumibleResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_registro") String idRegistro,
            @JsonProperty("predio") String predio,
            @JsonProperty("anio") int anio,
            @JsonProperty("mes") int mes,
            @JsonProperty("cantidad_inicial") int cantidadInicial,
            @JsonProperty("entrega_mensual") int entregaMensual,
            @JsonProperty("consumo_mensual") int consumoMensual,
            @JsonProperty("observaciones") String observaciones,
            @JsonProperty("fecha_registro") String fechaRegistro
    ) {
    }
}
