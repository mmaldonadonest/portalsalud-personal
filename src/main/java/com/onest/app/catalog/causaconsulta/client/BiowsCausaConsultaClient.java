package com.onest.app.catalog.causaconsulta.client;

import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaAltaRequest;
import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaEstadoRequest;
import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaRequest;
import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaResponse;
import com.onest.app.catalog.causaconsulta.dto.CausaConsultaDto;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway del catalogo de causas de consulta contra el WS ORDS.
 * Backend aplicado 2026-08-24 (docs/ords-causa-consulta.sql). Reusa el RestClient de biows.
 */
@Component
public class BiowsCausaConsultaClient implements CausaConsultaClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsCausaConsultaClient.class);
    private static final String PATH_CAUSA = "/Servcio/causa_consulta";
    private static final String PATH_CONSULTA_CAUSA = "/Servcio/consulta_causa_consulta";
    private static final String PATH_CAUSA_ESTADO = "/Servcio/causa_consulta_estado";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsCausaConsultaClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<CausaConsultaDto> listar(boolean soloActivos) {
        log.info("[biows] POST {}{} SOLO_ACTIVOS={}", properties.baseUrl(), PATH_CONSULTA_CAUSA, soloActivos);
        BiowsCausaConsultaResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_CAUSA)
                .body(new BiowsCausaConsultaRequest(soloActivos ? "Y" : null))
                .retrieve()
                .body(BiowsCausaConsultaResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        // "Sin datos" llega como objeto sentinel dentro de Datos, no arreglo vacio - filtrar.
        return response.datos().stream()
                .filter(d -> d.idRegistro() != null)
                .map(BiowsCausaConsultaClient::toDto)
                .toList();
    }

    @Override
    public String crear(BiowsCausaConsultaAltaRequest request) {
        log.info("[biows] POST {}{} NOMBRE={}", properties.baseUrl(), PATH_CAUSA, request.nombre());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_CAUSA)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    @Override
    public String cambiarEstado(BiowsCausaConsultaEstadoRequest request) {
        log.info("[biows] POST {}{} REG_ID={} ACTIVO={}", properties.baseUrl(), PATH_CAUSA_ESTADO,
                request.regId(), request.activo());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_CAUSA_ESTADO)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    private static CausaConsultaDto toDto(BiowsCausaConsultaResponse.Dato d) {
        return new CausaConsultaDto(d.idRegistro(), d.nombre(), d.activo(), d.fechaAlta(), d.fechaBaja(), d.usuario());
    }
}
