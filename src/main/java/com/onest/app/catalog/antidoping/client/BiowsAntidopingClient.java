package com.onest.app.catalog.antidoping.client;

import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingAltaRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingReporteRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingReporteResponse;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingResponse;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingSeleccionAltaRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingSeleccionRequest;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingSeleccionResponse;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.dto.AntidopingReporteDto;
import com.onest.app.catalog.antidoping.dto.AntidopingSeleccionDto;
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
    // _cta = clon del WS original que ademas devuelve CUENTA por registro (11-sep-2026,
    // docs/ords-cuenta-en-antidoping.sql). El original sigue publicado e intacto.
    private static final String PATH_REPORTE_FECHA = "/Servcio/consulta_antidoping_fecha_cta";
    private static final String PATH_SELECCION = "/Servcio/antidoping_seleccion";
    private static final String PATH_CONSULTA_SELECCION = "/Servcio/consulta_antidoping_seleccion";

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
        // "Sin datos" llega como un objeto sentinel dentro de Datos (Proceso/Estado/Mensaje/Data),
        // no como arreglo vacio - sin id_registro, se mapearia como una fila fantasma con todo null.
        return response.datos().stream()
                .filter(d -> d.idRegistro() != null)
                .map(BiowsAntidopingClient::toDto)
                .toList();
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

    @Override
    public String registrarSeleccion(BiowsAntidopingSeleccionAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_SELECCION, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_SELECCION)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    @Override
    public List<AntidopingSeleccionDto> findSelecciones(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTA_SELECCION, nss);
        BiowsAntidopingSeleccionResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_SELECCION)
                .body(new BiowsAntidopingSeleccionRequest(nss))
                .retrieve()
                .body(BiowsAntidopingSeleccionResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream()
                .filter(d -> d.idRegistro() != null)
                .map(BiowsAntidopingClient::toSeleccionDto)
                .toList();
    }

    private static AntidopingSeleccionDto toSeleccionDto(BiowsAntidopingSeleccionResponse.Dato d) {
        return new AntidopingSeleccionDto(d.idRegistro(), d.fechaSeleccion(), d.nss(), d.tamanoPool(), d.usuario());
    }

    private static AntidopingDto toDto(BiowsAntidopingResponse.Dato d) {
        return new AntidopingDto(
                d.idRegistro(), d.fechaRegistro(), d.folio(), d.tipoPrueba(), d.sustancia(),
                d.resultado(), d.statusConclusion(), d.observaciones(), d.usuario());
    }

    private static AntidopingReporteDto toReporte(BiowsAntidopingReporteResponse.Dato d) {
        return new AntidopingReporteDto(
                d.idRegistro(), d.fechaRegistro(), d.nss(), d.nombre(), d.rfc(), d.curp(), d.cuenta(),
                d.folio(), d.tipoPrueba(), d.sustancia(), d.resultado(), d.statusConclusion(), d.usuario());
    }
}
