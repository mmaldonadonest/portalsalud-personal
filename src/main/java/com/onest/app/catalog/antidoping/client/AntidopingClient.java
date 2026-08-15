package com.onest.app.catalog.antidoping.client;

import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingAltaRequest;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
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
}
