package com.onest.app.catalog.restriccion.client;

import com.onest.app.catalog.restriccion.client.dto.BiowsRestriccionAltaRequest;
import com.onest.app.catalog.restriccion.client.dto.BiowsRestriccionRequest;
import com.onest.app.catalog.restriccion.client.dto.BiowsRestriccionResponse;
import com.onest.app.catalog.restriccion.dto.RestriccionDto;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway de restricciones medicas contra el WS ORDS.
 * Backend aplicado 2026-08-24 (docs/ords-restriccion.sql). Reusa el RestClient de biows.
 */
@Component
public class BiowsRestriccionClient implements RestriccionClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsRestriccionClient.class);
    private static final String PATH_RESTRICCION = "/Servcio/restriccion";
    private static final String PATH_CONSULTA_RESTRICCION = "/Servcio/consulta_restriccion";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsRestriccionClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<RestriccionDto> findRestricciones(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTA_RESTRICCION, nss);
        BiowsRestriccionResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_RESTRICCION)
                .body(new BiowsRestriccionRequest(nss))
                .retrieve()
                .body(BiowsRestriccionResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        // "Sin datos" llega como objeto sentinel dentro de Datos (Proceso/Estado/Mensaje/Data),
        // no como arreglo vacio - sin id_registro, se mapearia como una restriccion fantasma
        // con todos los campos null (asi se vio el bug real: "— null" en el historial).
        return response.datos().stream()
                .filter(d -> d.idRegistro() != null)
                .map(BiowsRestriccionClient::toDto)
                .toList();
    }

    @Override
    public String crearRestriccion(BiowsRestriccionAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_RESTRICCION, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_RESTRICCION)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    private static RestriccionDto toDto(BiowsRestriccionResponse.Dato d) {
        return new RestriccionDto(
                d.idRegistro(), d.codigoRestriccion(), d.descripcion(), d.valorLimite(), d.unidad(),
                d.fechaInicio(), d.fechaFin(), d.fechaRevaloracion(), d.temporalidad(), d.observaciones(),
                d.medicoResponsable(), d.estatus(), d.fechaRegistro(), d.usuario());
    }
}
