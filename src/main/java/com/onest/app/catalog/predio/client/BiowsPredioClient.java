package com.onest.app.catalog.predio.client;

import com.onest.app.catalog.predio.client.dto.BiowsCuentaPredioAsignarRequest;
import com.onest.app.catalog.predio.client.dto.BiowsCuentaPredioListResponse;
import com.onest.app.catalog.predio.client.dto.BiowsPredioAsignarResponse;
import com.onest.app.catalog.predio.client.dto.BiowsPredioEmpleadoResponse;
import com.onest.app.catalog.predio.client.dto.BiowsPredioListResponse;
import com.onest.app.catalog.predio.dto.CuentaPredioDto;
import com.onest.app.catalog.predio.dto.PredioDto;
import com.onest.app.config.BiowsProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion de los gateways ORDS del modulo de predios (dashboard analitico,
 * rol MEDICO_ANALISTA). Backend aplicado 08-sep-2026, ver docs/ords-predio-empleado.sql
 * y docs/ords-predio-cuenta.sql. Reusa el RestClient de biows.
 */
@Component
public class BiowsPredioClient implements PredioClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsPredioClient.class);
    private static final String PATH_PREDIO_EMPLEADO = "/Servcio/predio_empleado";
    private static final String PATH_PREDIOS = "/Servcio/predios";
    private static final String PATH_CUENTA_PREDIO_LISTA = "/Servcio/cuenta_predio_lista";
    private static final String PATH_CUENTA_PREDIO_ASIGNAR = "/Servcio/cuenta_predio_asignar";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsPredioClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<PredioDto> resolverPorNss(String nss) {
        log.info("[biows] POST {}{} Nss={}", properties.baseUrl(), PATH_PREDIO_EMPLEADO, nss);
        BiowsPredioEmpleadoResponse response = biowsRestClient.post()
                .uri(PATH_PREDIO_EMPLEADO)
                .body(Map.of("Nss", nss))
                .retrieve()
                .body(BiowsPredioEmpleadoResponse.class);

        return first(response == null ? null : response.datos())
                .filter(d -> d.predioId() != null)
                .map(d -> new PredioDto(d.predioId(), d.predioDesc()));
    }

    @Override
    public List<PredioDto> listar() {
        log.info("[biows] POST {}{}", properties.baseUrl(), PATH_PREDIOS);
        BiowsPredioListResponse response = biowsRestClient.post()
                .uri(PATH_PREDIOS)
                .body(Map.of())
                .retrieve()
                .body(BiowsPredioListResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream()
                .map(d -> new PredioDto(d.predioId(), d.nombre()))
                .toList();
    }

    @Override
    public List<CuentaPredioDto> listarCuentaPredio() {
        log.info("[biows] POST {}{}", properties.baseUrl(), PATH_CUENTA_PREDIO_LISTA);
        BiowsCuentaPredioListResponse response = biowsRestClient.post()
                .uri(PATH_CUENTA_PREDIO_LISTA)
                .body(Map.of())
                .retrieve()
                .body(BiowsCuentaPredioListResponse.class);

        if (response == null || response.datos() == null) {
            return List.of();
        }
        return response.datos().stream()
                .map(d -> new CuentaPredioDto(d.cuentaId(), d.cuentaNombre(), d.predioId(), d.predioNombre()))
                .toList();
    }

    @Override
    public String asignar(String cuentaNombre, Long predioId, String idUsuario, String nombreUsuario) {
        log.info("[biows] POST {}{} CuentaNombre={} PredioId={}", properties.baseUrl(), PATH_CUENTA_PREDIO_ASIGNAR,
                cuentaNombre, predioId);
        BiowsPredioAsignarResponse response = biowsRestClient.post()
                .uri(PATH_CUENTA_PREDIO_ASIGNAR)
                .body(new BiowsCuentaPredioAsignarRequest(cuentaNombre, predioId, idUsuario, nombreUsuario))
                .retrieve()
                .body(BiowsPredioAsignarResponse.class);

        return first(response == null ? null : response.datos())
                .map(BiowsPredioAsignarResponse.Dato::proceso)
                .orElse("");
    }

    private static <T> Optional<T> first(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(0));
    }
}
