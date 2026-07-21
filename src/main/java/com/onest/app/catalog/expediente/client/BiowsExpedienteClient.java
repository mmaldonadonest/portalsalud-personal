package com.onest.app.catalog.expediente.client;

import com.onest.app.catalog.expediente.client.dto.BiowsConsultaAltaRequest;
import com.onest.app.catalog.expediente.client.dto.BiowsConsultasRequest;
import com.onest.app.catalog.expediente.client.dto.BiowsConsultasResponse;
import com.onest.app.catalog.expediente.client.dto.BiowsIcd;
import com.onest.app.catalog.expediente.client.dto.BiowsIcdRequest;
import com.onest.app.catalog.expediente.client.dto.BiowsProcesoResponse;
import com.onest.app.catalog.expediente.dto.ConsultaDetalleDto;
import com.onest.app.catalog.expediente.dto.ConsultaDto;
import com.onest.app.catalog.expediente.dto.IcdDto;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion del gateway del expediente contra el WS ORDS.
 * Replica app::showConsults() de php-old/app/app.php. Reusa el RestClient de biows.
 */
@Component
public class BiowsExpedienteClient implements ExpedienteClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsExpedienteClient.class);
    private static final String PATH_CONSULTAS = "/Servcio/conuslta_medica_usuario";
    private static final String PATH_CONSULTA_ALTA = "/Servcio/consulta";
    private static final String PATH_ICD = "/Servcio/indice";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsExpedienteClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public List<ConsultaDto> findConsultas(String nss) {
        BiowsConsultasResponse response = fetchConsultas(nss);
        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream()
                .map(d -> new ConsultaDto(d.idConsulta(), d.fechaConsulta(), d.tipoConsulta(), d.areaAccidente()))
                .toList();
    }

    @Override
    public Optional<ConsultaDetalleDto> findConsultaDetail(String nss, String idConsulta) {
        BiowsConsultasResponse response = fetchConsultas(nss);
        if (response == null || response.datos() == null || idConsulta == null) {
            return Optional.empty();
        }
        return response.datos().stream()
                .filter(d -> idConsulta.equals(d.idConsulta()))
                .findFirst()
                .map(BiowsExpedienteClient::toDetalle);
    }

    @Override
    public String crearConsulta(BiowsConsultaAltaRequest request) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTA_ALTA, request.nss());
        BiowsProcesoResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_ALTA)
                .body(request)
                .retrieve()
                .body(BiowsProcesoResponse.class);

        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return "";
        }
        return response.datos().get(0).proceso();
    }

    @Override
    public List<IcdDto> buscarIcd(String texto) {
        log.info("[biows] POST {}{} variable={}", properties.baseUrl(), PATH_ICD, texto);
        List<BiowsIcd> response = biowsRestClient.post()
                .uri(PATH_ICD)
                .body(new BiowsIcdRequest(texto))
                .retrieve()
                .body(new ParameterizedTypeReference<List<BiowsIcd>>() { });

        if (response == null) {
            return List.of();
        }
        return response.stream()
                .map(i -> new IcdDto(i.claveId(), i.nombreClave()))
                .toList();
    }

    private BiowsConsultasResponse fetchConsultas(String nss) {
        log.info("[biows] POST {}{} NSS={}", properties.baseUrl(), PATH_CONSULTAS, nss);
        return biowsRestClient.post()
                .uri(PATH_CONSULTAS)
                .body(new BiowsConsultasRequest(nss))
                .retrieve()
                .body(BiowsConsultasResponse.class);
    }

    private static ConsultaDetalleDto toDetalle(BiowsConsultasResponse.Dato d) {
        return new ConsultaDetalleDto(
                d.idConsulta(), d.fechaConsulta(), d.tipoConsulta(), d.areaAccidente(),
                d.areaInvolucrada(), d.causa(), d.peso(), d.talla(), d.imc(), d.fc(), d.fr(),
                d.ta(), d.temperatura(), d.motivo(), d.exploracion(), d.diagnostico(),
                d.tratamiento(), d.consultaRelacionada(), d.firmaDigital()
        );
    }
}
