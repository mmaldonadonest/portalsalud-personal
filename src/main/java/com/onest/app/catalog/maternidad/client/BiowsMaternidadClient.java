package com.onest.app.catalog.maternidad.client;

import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadAltaRequest;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadReporteRequest;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadReporteResponse;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadRequest;
import com.onest.app.catalog.maternidad.client.dto.BiowsMaternidadResponse;
import com.onest.app.catalog.maternidad.dto.MaternidadDto;
import com.onest.app.catalog.maternidad.dto.MaternidadReporteDto;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway de seguimiento de maternidad contra el WS ORDS.
 * Backend en docs/ords-maternidad.sql, aplicado 2026-08-21. Reusa el RestClient de biows.
 */
@Component
public class BiowsMaternidadClient implements MaternidadClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsMaternidadClient.class);
    private static final String PATH_MATERNIDAD = "/Servcio/maternidad";
    private static final String PATH_CONSULTA_MATERNIDAD = "/Servcio/consulta_maternidad";
    private static final String PATH_REPORTE_FECHA = "/Servcio/consulta_maternidad_fecha";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsMaternidadClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<MaternidadDto> findSeguimientos(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTA_MATERNIDAD, nss);
        BiowsMaternidadResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_MATERNIDAD)
                .body(new BiowsMaternidadRequest(nss))
                .retrieve()
                .body(BiowsMaternidadResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        // "Sin datos" llega como objeto sentinel dentro de Datos, no arreglo vacio - filtrar.
        return response.datos().stream()
                .filter(d -> d.idRegistro() != null)
                .map(BiowsMaternidadClient::toDto)
                .toList();
    }

    @Override
    public String crearSeguimiento(BiowsMaternidadAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_MATERNIDAD, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_MATERNIDAD)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    private static MaternidadDto toDto(BiowsMaternidadResponse.Dato d) {
        return new MaternidadDto(
                d.idRegistro(), d.fechaRegistro(), d.semanasGestacion(), d.fechaProbableParto(),
                d.restriccionesLaborales(), d.proximaRevision(), d.observaciones(), d.estatus(),
                d.incapacidad(), d.reincorporacion(), d.usuario());
    }

    @Override
    public List<MaternidadReporteDto> reportePorFecha(String fechaInicial, String fechaFinal) {
        log.info("[biows] POST {}{} {} - {}", properties.baseUrl(), PATH_REPORTE_FECHA, fechaInicial, fechaFinal);
        BiowsMaternidadReporteResponse response = biowsRestClient.post()
                .uri(PATH_REPORTE_FECHA)
                .body(new BiowsMaternidadReporteRequest(aDdMmYy(fechaInicial), aDdMmYy(fechaFinal)))
                .retrieve()
                .body(BiowsMaternidadReporteResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream()
                // Fila "fantasma" (objeto Proceso/Estado/Mensaje dentro de Datos) cuando no hay datos.
                .filter(d -> d.nss() != null && !d.nss().isBlank())
                .map(d -> new MaternidadReporteDto(
                        d.idRegistro(), d.fechaRegistro(), d.nss(), d.nombre(), d.cuenta(),
                        d.semanasGestacion(), d.fechaProbableParto(), d.restriccionesLaborales(),
                        d.proximaRevision(), d.estatus(), d.incapacidad(), d.reincorporacion(), d.usuario()))
                .toList();
    }

    /** El WS espera dd/mm/yy; el resto de la app trabaja en ISO yyyy-MM-dd. */
    private static String aDdMmYy(String iso) {
        if (iso == null || iso.length() < 10) {
            return iso;
        }
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(2, 4);
    }
}
