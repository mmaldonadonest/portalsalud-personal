package com.onest.app.catalog.incapacidad.client;

import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.catalog.incapacidad.client.dto.BiowsIncapacidadAltaRequest;
import com.onest.app.catalog.incapacidad.client.dto.BiowsIncapacidadesRequest;
import com.onest.app.catalog.incapacidad.client.dto.BiowsIncapacidadesResponse;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDetalleDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDto;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway de incapacidades contra el WS ORDS.
 * Replica app::showIncapacidades() de php-old/app/app.php. Reusa el RestClient de biows.
 */
@Component
public class BiowsIncapacidadClient implements IncapacidadClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsIncapacidadClient.class);
    private static final String PATH_INCAPACIDADES = "/Servcio/consulta_incapacidad";
    private static final String PATH_INCAPACIDAD_ALTA = "/Servcio/incapacidades";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsIncapacidadClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<IncapacidadDto> findIncapacidades(String nss) {
        BiowsIncapacidadesResponse response = fetch(nss);
        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream().map(BiowsIncapacidadClient::toList).toList();
    }

    @Override
    public Optional<IncapacidadDetalleDto> findIncapacidadDetail(String nss, String idConsulta) {
        BiowsIncapacidadesResponse response = fetch(nss);
        if (response == null || response.datos() == null || idConsulta == null) {
            return Optional.empty();
        }
        return response.datos().stream()
                .filter(d -> idConsulta.equals(d.idConsulta()))
                .findFirst()
                .map(BiowsIncapacidadClient::toDetalle);
    }

    @Override
    public String crearIncapacidad(BiowsIncapacidadAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_INCAPACIDAD_ALTA, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_INCAPACIDAD_ALTA)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    private BiowsIncapacidadesResponse fetch(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_INCAPACIDADES, nss);
        return biowsRestClient.post()
                .uri(PATH_INCAPACIDADES)
                .body(new BiowsIncapacidadesRequest(nss))
                .retrieve()
                .body(BiowsIncapacidadesResponse.class);
    }

    private static IncapacidadDto toList(BiowsIncapacidadesResponse.Dato d) {
        return new IncapacidadDto(
                d.idConsulta(), d.fechaConsulta(), d.folioIncapacidad(), d.rubro(), d.ramo(),
                d.tipoIncapacidad(), d.diasAutorizados(), d.fechaInicio(), d.fechaTermino(), d.costo());
    }

    private static IncapacidadDetalleDto toDetalle(BiowsIncapacidadesResponse.Dato d) {
        return new IncapacidadDetalleDto(
                d.idConsulta(), d.fechaConsulta(), d.folioIncapacidad(), d.ramo(), d.tipoIncapacidad(),
                d.fechaInicio(), d.fechaTermino(), d.diasAutorizados(), d.salarioIntegrado(), d.costo(),
                d.imputable(), d.estadoDictamen(), d.goceSueldo(), d.complementoSalarial(), d.rubro(),
                d.fechaAlta(), d.st2(), d.alta(), d.salarioAcumulado(), d.urlArchivos(), d.firmaDigital());
    }
}
