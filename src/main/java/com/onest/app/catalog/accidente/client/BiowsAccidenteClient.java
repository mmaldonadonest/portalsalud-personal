package com.onest.app.catalog.accidente.client;

import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteAltaRequest;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteReporteRequest;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteReporteResponse;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteRequest;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteResponse;
import com.onest.app.catalog.accidente.dto.AccidenteDto;
import com.onest.app.catalog.accidente.dto.AccidenteReporteDto;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway de accidentes de trabajo contra el WS ORDS.
 * Backend aplicado y verificado 2026-08-14 (docs/ords-accidentes.sql). Reusa el RestClient de biows.
 */
@Component
public class BiowsAccidenteClient implements AccidenteClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsAccidenteClient.class);
    private static final String PATH_ACCIDENTE = "/Servcio/accidente";
    private static final String PATH_CONSULTA_ACCIDENTE = "/Servcio/consulta_accidente";
    private static final String PATH_REPORTE_FECHA = "/Servcio/consulta_accidentes_fecha";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsAccidenteClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<AccidenteDto> findAccidentes(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTA_ACCIDENTE, nss);
        BiowsAccidenteResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_ACCIDENTE)
                .body(new BiowsAccidenteRequest(nss))
                .retrieve()
                .body(BiowsAccidenteResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream().map(BiowsAccidenteClient::toDto).toList();
    }

    @Override
    public String crearAccidente(BiowsAccidenteAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_ACCIDENTE, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_ACCIDENTE)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    @Override
    public List<AccidenteReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        log.info("[biows] POST {}{} {} - {}", properties.baseUrl(), PATH_REPORTE_FECHA, fechaInicial, fechaFinal);
        BiowsAccidenteReporteResponse response = biowsRestClient.post()
                .uri(PATH_REPORTE_FECHA)
                .body(new BiowsAccidenteReporteRequest(fechaInicial, fechaFinal))
                .retrieve()
                .body(BiowsAccidenteReporteResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream().map(BiowsAccidenteClient::toReporte).toList();
    }

    private static AccidenteDto toDto(BiowsAccidenteResponse.Dato d) {
        return new AccidenteDto(
                d.idRegistro(), d.fechaRegistro(), d.fechaAccidente(), d.tipoRiesgo(), d.causaRt(),
                d.diagnostico(), d.sdi(), d.statusCalificacion(), d.costo(), d.observaciones(), d.usuario());
    }

    private static AccidenteReporteDto toReporte(BiowsAccidenteReporteResponse.Dato d) {
        return new AccidenteReporteDto(
                d.idRegistro(), d.fechaRegistro(), d.nss(), d.nombre(), d.rfc(), d.curp(),
                d.fechaAccidente(), d.tipoRiesgo(), d.causaRt(), d.diagnostico(), d.sdi(),
                d.statusCalificacion(), d.costo(), d.observaciones(), d.usuario());
    }
}
