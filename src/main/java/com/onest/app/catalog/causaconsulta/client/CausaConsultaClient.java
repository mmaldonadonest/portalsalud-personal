package com.onest.app.catalog.causaconsulta.client;

import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaAltaRequest;
import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaEstadoRequest;
import com.onest.app.catalog.causaconsulta.dto.CausaConsultaDto;
import java.util.List;

/**
 * Gateway hacia el WS ORDS del catálogo de causas de consulta.
 * Backend aplicado 2026-08-24, ver docs/ords-causa-consulta.sql.
 */
public interface CausaConsultaClient {

    /** POST .../Servcio/consulta_causa_consulta. soloActivos=true filtra solo ACTIVO='Y'. */
    List<CausaConsultaDto> listar(boolean soloActivos);

    /** POST .../Servcio/causa_consulta (alta). Devuelve el mensaje Proceso. */
    String crear(BiowsCausaConsultaAltaRequest request);

    /** POST .../Servcio/causa_consulta_estado (activar/desactivar). Devuelve el mensaje Proceso. */
    String cambiarEstado(BiowsCausaConsultaEstadoRequest request);
}
