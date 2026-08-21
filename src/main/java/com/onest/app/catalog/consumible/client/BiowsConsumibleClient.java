package com.onest.app.catalog.consumible.client;

import com.onest.app.catalog.consumible.client.dto.BiowsConsumibleAltaRequest;
import com.onest.app.catalog.consumible.client.dto.BiowsConsumibleRequest;
import com.onest.app.catalog.consumible.client.dto.BiowsConsumibleResponse;
import com.onest.app.catalog.consumible.dto.ConsumibleDto;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway de consumibles de antidoping contra el WS ORDS.
 * Backend aplicado 2026-08-18 (docs/ords-antidoping-consumibles.sql). Reusa el RestClient de biows.
 */
@Component
public class BiowsConsumibleClient implements ConsumibleClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsConsumibleClient.class);
    private static final String PATH_CONSUMIBLES = "/Servcio/consumibles";
    private static final String PATH_CONSULTA_CONSUMIBLES = "/Servcio/consulta_consumibles";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsConsumibleClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<ConsumibleDto> listar(String predio) {
        log.info("[biows] POST {}{} PREDIO={}", properties.baseUrl(), PATH_CONSULTA_CONSUMIBLES, predio);
        BiowsConsumibleResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_CONSUMIBLES)
                .body(new BiowsConsumibleRequest(predio))
                .retrieve()
                .body(BiowsConsumibleResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream()
                .filter(d -> d.predio() != null && !"0".equals(d.predio()))
                .map(BiowsConsumibleClient::toDto)
                .toList();
    }

    @Override
    public String crear(BiowsConsumibleAltaRequest request) {
        log.info("[biows] POST {}{} PREDIO={}", properties.baseUrl(), PATH_CONSUMIBLES, request.predio());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_CONSUMIBLES)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    private static ConsumibleDto toDto(BiowsConsumibleResponse.Dato d) {
        return new ConsumibleDto(
                d.idRegistro(), d.predio(), d.anio(), d.mes(),
                d.cantidadInicial(), d.entregaMensual(), d.consumoMensual(),
                d.observaciones(), d.fechaRegistro());
    }
}
