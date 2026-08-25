package com.onest.app.catalog.causaconsulta.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_causa_consulta: {SOLO_ACTIVOS}. 'Y' filtra solo activas
 * (para el &lt;select&gt; de captura de Consulta); null/vacio regresa todas (pantalla admin).
 * Ver docs/ords-causa-consulta.sql BLOQUE 3.
 */
public record BiowsCausaConsultaRequest(
        @JsonProperty("SOLO_ACTIVOS") String soloActivos
) {
}
