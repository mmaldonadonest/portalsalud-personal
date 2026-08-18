package com.onest.app.catalog.expediente.client;

import com.onest.app.catalog.expediente.client.dto.BiowsConsultaAltaRequest;
import com.onest.app.catalog.expediente.dto.ConsultaDetalleDto;
import com.onest.app.catalog.expediente.dto.ConsultaDto;
import com.onest.app.catalog.expediente.dto.ConsultaReporteDto;
import java.util.List;
import java.util.Optional;

/**
 * Gateway hacia el WS ORDS del expediente (consultas medicas por NSS).
 * Equivale a app::showConsults() de php-old/app/app.php.
 */
public interface ExpedienteClient {

    /** showConsults -> POST .../Servcio/conuslta_medica_usuario. */
    List<ConsultaDto> findConsultas(String nss);

    /**
     * Detalle de una consulta. Como en viewConsultaDetails.php, reconsulta el WS
     * y filtra por id_consulta == serieId.
     */
    Optional<ConsultaDetalleDto> findConsultaDetail(String nss, String idConsulta);

    /** addConsultM -> POST .../Servcio/consulta. Devuelve el mensaje Proceso. */
    String crearConsulta(BiowsConsultaAltaRequest request);

    /** showICD -> POST .../Servcio/indice. Devuelve el catalogo ICD que matchea. */
    List<com.onest.app.catalog.expediente.dto.IcdDto> buscarIcd(String texto);

    /**
     * Reporte administrativo por rango de fechas (todas las NSS), para el dashboard.
     * Backend aplicado y verificado 2026-08-17. fechaInicial/fechaFinal en formato "dd/MM/yy".
     */
    List<ConsultaReporteDto> reportePorFecha(String fechaInicial, String fechaFinal);
}
