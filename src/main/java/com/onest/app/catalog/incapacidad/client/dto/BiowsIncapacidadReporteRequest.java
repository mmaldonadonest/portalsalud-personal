package com.onest.app.catalog.incapacidad.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body de .../Servcio/consulta_incapacidades_fecha. Equivale a app::traerDatosGeneralIncap().
 * OJO: las fechas van con formato "dd/MM/yy" (anio de 2 digitos) - confirmado contra
 * php-old/rest/dashboard/generarExcelIncapa.php:13,15 (DateTime::format('d/m/y')) y contra
 * el WS real (docs/contextoWS.txt:2307-2308, to_date(kfechaini,'dd/mm/yy')). No es un typo.
 */
public record BiowsIncapacidadReporteRequest(
        @JsonProperty("fecha_inicial") String fechaInicial,
        @JsonProperty("fecha_final") String fechaFinal
) {
}
