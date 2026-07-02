package com.onest.app.catalog.nss.client;

import com.onest.app.catalog.nss.client.dto.BiowsAltaMedicoRequest;
import com.onest.app.catalog.nss.client.dto.BiowsConsultaExamenRequest;
import com.onest.app.catalog.nss.client.dto.BiowsConsultaExamenResponse;
import com.onest.app.catalog.nss.client.dto.BiowsProspectoRequest;
import com.onest.app.catalog.nss.client.dto.BiowsProspectoResponse;
import com.onest.app.catalog.nss.client.dto.BiowsUserResponse;
import com.onest.app.catalog.nss.client.dto.BiowsUserSearchRequest;
import com.onest.app.catalog.nss.dto.CandidatoDto;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.config.BiowsProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion de los gateways ORDS (biows) para la busqueda por NSS.
 * Replica los metodos curl_* de php-old/app/app.php, conservando URLs,
 * nombres de campo y el flujo encadenado de searchEmploye.php.
 */
@Component
public class BiowsNssSearchClient implements NssSearchClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsNssSearchClient.class);

    // php usa date("d/m/y H:i:s")
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    private static final String PATH_USUARIO = "/Catalogo/usuario";
    private static final String PATH_CONSULTA_EXAMEN = "/Servcio/consulta_examen";
    private static final String PATH_SERVICIO_MEDICO = "/Servcio/Medico";
    private static final String PATH_CANDIDATOS = "/Catalogo/Candidatos";

    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsNssSearchClient(RestClient biowsRestClient, BiowsProperties properties) {
        this.biowsRestClient = biowsRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<EmpleadoDto> findUsuario(String nss, String usuarioConsulta) {
        BiowsUserSearchRequest request = new BiowsUserSearchRequest(
                nss,
                ahora(),
                usuarioConsulta,
                properties.aplicacionId(),
                properties.idAplicacion()
        );

        log.info("[biows] POST {}{} Nss={}", properties.baseUrl(), PATH_USUARIO, nss);
        BiowsUserResponse response = biowsRestClient.post()
                .uri(PATH_USUARIO)
                .body(request)
                .retrieve()
                .body(BiowsUserResponse.class);

        return first(response == null ? null : response.datos()).map(BiowsNssSearchClient::toEmpleado);
    }

    @Override
    public void checkEmploye(String nss, String tipoUsuario, String usuarioConsulta) {
        BiowsConsultaExamenResponse response = biowsRestClient.post()
                .uri(PATH_CONSULTA_EXAMEN)
                .body(new BiowsConsultaExamenRequest(nss, ahora(), usuarioConsulta))
                .retrieve()
                .body(BiowsConsultaExamenResponse.class);

        String estado = first(response == null ? null : response.datos())
                .map(BiowsConsultaExamenResponse.Dato::estado)
                .orElse(null);

        // php: if ($check == 0) { } else { ALTA }. Estado ausente/0 => no se da de alta.
        if (!esCero(estado)) {
            altaServicioMedico(nss, tipoUsuario, usuarioConsulta);
        }
    }

    @Override
    public Optional<CandidatoDto> findProspecto(String termino) {
        BiowsProspectoResponse response = biowsRestClient.post()
                .uri(PATH_CANDIDATOS)
                .body(new BiowsProspectoRequest(termino))
                .retrieve()
                .body(BiowsProspectoResponse.class);

        return first(response == null ? null : response.datos()).map(BiowsNssSearchClient::toCandidato);
    }

    private void altaServicioMedico(String nss, String tipoEmpleado, String usuarioConsulta) {
        BiowsAltaMedicoRequest request = new BiowsAltaMedicoRequest(
                nss,
                ahora(),
                "ALTA",
                tipoEmpleado,
                usuarioConsulta,
                new BiowsAltaMedicoRequest.Otras("", "")
        );

        biowsRestClient.post()
                .uri(PATH_SERVICIO_MEDICO)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private static <T> Optional<T> first(List<T> items) {
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(0));
    }

    private static boolean esCero(String estado) {
        if (estado == null || estado.isBlank()) {
            return true;
        }
        try {
            return Double.parseDouble(estado.trim()) == 0d;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static EmpleadoDto toEmpleado(BiowsUserResponse.Dato dato) {
        return new EmpleadoDto(
                dato.nss(),
                dato.nombre(),
                dato.apellidoPaterno(),
                dato.apellidoMaterno(),
                dato.rfc(),
                dato.cuenta(),
                dato.fechaNacimiento(),
                dato.estadoCivil(),
                dato.sexo(),
                dato.direccion(),
                dato.celular(),
                dato.turno(),
                dato.telFijo(),
                dato.completo(),
                dato.nombrePuesto(),
                dato.nombreEmpresa(),
                calcularEdad(dato.rfc())
        );
    }

    private static CandidatoDto toCandidato(BiowsProspectoResponse.Dato dato) {
        return new CandidatoDto(
                dato.curp(),
                dato.nombre(),
                dato.fechaInicioProceso(),
                dato.estadoCandidato(),
                dato.mensaje(),
                dato.status()
        );
    }

    /**
     * Edad a partir del RFC (posiciones 5-10 = AAMMDD). Misma logica que examenMedico en php-old:
     * anio > 50 => siglo XX, en caso contrario siglo XXI.
     */
    private static Integer calcularEdad(String rfc) {
        if (rfc == null || rfc.length() < 10) {
            return null;
        }
        try {
            int anio = Integer.parseInt(rfc.substring(4, 6));
            int mes = Integer.parseInt(rfc.substring(6, 8));
            int dia = Integer.parseInt(rfc.substring(8, 10));
            int anioCompleto = anio > 50 ? 1900 + anio : 2000 + anio;
            LocalDate nacimiento = LocalDate.of(anioCompleto, mes, dia);
            return Period.between(nacimiento, LocalDate.now()).getYears();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String ahora() {
        return LocalDateTime.now().format(FECHA);
    }
}
