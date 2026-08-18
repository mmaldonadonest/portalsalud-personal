package com.onest.app.catalog.antidoping.client;

import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingAltaRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingReporteRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingReporteResponse;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingResponse;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.dto.AntidopingReporteDto;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway de antidoping/alcoholimetria contra el WS ORDS.
 * Backend aplicado y verificado 2026-08-14 (docs/ords-antidoping.sql). Reusa el RestClient de biows.
 */
@Component
public class BiowsAntidopingClient implements AntidopingClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsAntidopingClient.class);
    private static final String PATH_ANTIDOPING = "/Servcio/antidoping";
    private static final String PATH_CONSULTA_ANTIDOPING = "/Servcio/consulta_antidoping";
    private static final String PATH_REPORTE_FECHA = "/Servcio/consulta_antidoping_fecha";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsAntidopingClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<AntidopingDto> findAntidopings(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTA_ANTIDOPING, nss);
        BiowsAntidopingResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_ANTIDOPING)
                .body(new BiowsAntidopingRequest(nss))
                .retrieve()
                .body(BiowsAntidopingResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream().map(BiowsAntidopingClient::toDto).toList();
    }

    @Override
    public String crearAntidoping(BiowsAntidopingAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_ANTIDOPING, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_ANTIDOPING)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    @Override
    public List<AntidopingReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        log.info("[biows] POST {}{} {} - {}", properties.baseUrl(), PATH_REPORTE_FECHA, fechaInicial, fechaFinal);
        BiowsAntidopingReporteResponse response = biowsRestClient.post()
                .uri(PATH_REPORTE_FECHA)
                .body(new BiowsAntidopingReporteRequest(fechaInicial, fechaFinal))
                .retrieve()
                .body(BiowsAntidopingReporteResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream().map(BiowsAntidopingClient::toReporte).toList();
    }

    private static AntidopingDto toDto(BiowsAntidopingResponse.Dato d) {
        return new AntidopingDto(
                d.idRegistro(), d.fechaRegistro(), d.folio(), d.tipoPrueba(), d.sustancia(),
                d.resultado(), d.statusConclusion(), d.observaciones(), d.usuario());
    }

    private static AntidopingReporteDto toReporte(BiowsAntidopingReporteResponse.Dato d) {
        return new AntidopingReporteDto(
                d.idRegistro(), d.fechaRegistro(), d.nss(), d.nombre(), d.rfc(), d.curp(),
                d.folio(), d.tipoPrueba(), d.sustancia(), d.resultado(), d.statusConclusion(), d.usuario());
    }
}
