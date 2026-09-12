package com.onest.app.catalog.maternidad.client;

import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadAltaRequest;
import com.onest.app.catalog.maternidad.dto.MaternidadDto;
import com.onest.app.catalog.maternidad.dto.MaternidadReporteDto;
import java.util.List;

/**
 * Gateway hacia el WS ORDS de seguimiento de maternidad (por NSS).
 * Backend en docs/ords-maternidad.sql, aplicado 2026-08-21.
 */
public interface MaternidadClient {

    /** POST .../Servcio/consulta_maternidad. Historial ordenado por fecha desc. */
    List<MaternidadDto> findSeguimientos(String nss);

    /** POST .../Servcio/consulta_maternidad_fecha. Todos los casos en el rango (ISO yyyy-MM-dd), para el dashboard. */
    List<MaternidadReporteDto> reportePorFecha(String fechaInicial, String fechaFinal);

    /** POST .../Servcio/maternidad. Devuelve el mensaje Proceso. */
    String crearSeguimiento(BiowsMaternidadAltaRequest request);
}
