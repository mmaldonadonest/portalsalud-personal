package com.onest.app.catalog.antidoping.client;

import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingAltaRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingSeleccionAltaRequest;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.dto.AntidopingReporteDto;
import com.onest.app.catalog.antidoping.dto.AntidopingSeleccionDto;
import java.util.List;

/**
 * Gateway hacia el WS ORDS de antidoping/alcoholimetria (por NSS).
 * Backend aplicado y verificado 2026-08-14, ver docs/ords-antidoping.sql.
 */
public interface AntidopingClient {

    /** POST .../Servcio/consulta_antidoping. */
    List<AntidopingDto> findAntidopings(String nss);

    /** POST .../Servcio/antidoping. Devuelve el mensaje Proceso. */
    String crearAntidoping(BiowsAntidopingAltaRequest request);

    /**
     * Reporte administrativo por rango de fechas (todas las NSS), para el dashboard.
     * Backend aplicado y verificado 2026-08-17. fechaInicial/fechaFinal en formato "dd/MM/yy".
     */
    List<AntidopingReporteDto> reportePorFecha(String fechaInicial, String fechaFinal);

    /**
     * POST .../Servcio/antidoping_seleccion. Traza de la ronda de seleccion aleatoria
     * (docs/ords-antidoping-seleccion.sql), separada del resultado real de la prueba.
     */
    String registrarSeleccion(BiowsAntidopingSeleccionAltaRequest request);

    /** POST .../Servcio/consulta_antidoping_seleccion. nss null = todo el historial. */
    List<AntidopingSeleccionDto> findSelecciones(String nss);
}
