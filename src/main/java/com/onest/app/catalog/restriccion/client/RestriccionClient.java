package com.onest.app.catalog.restriccion.client;

import com.onest.app.catalog.restriccion.client.dto.BiowsRestriccionAltaRequest;
import com.onest.app.catalog.restriccion.dto.RestriccionDto;
import java.util.List;

/**
 * Gateway hacia el WS ORDS de restricciones médicas (por NSS).
 * Backend aplicado 2026-08-24, ver docs/ords-restriccion.sql.
 */
public interface RestriccionClient {

    /** POST .../Servcio/consulta_restriccion. */
    List<RestriccionDto> findRestricciones(String nss);

    /** POST .../Servcio/restriccion. Devuelve el mensaje Proceso. */
    String crearRestriccion(BiowsRestriccionAltaRequest request);
}
