package com.onest.app.catalog.nss.service;

import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.CandidatoDto;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.dto.NssSearchResponse;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Busqueda por NSS contra los WS ORDS legacy. Replica el flujo de searchEmploye.php:
 * 1. searchUser -> datos del usuario.
 * 2. CheckEmploye (CANDIDATO/EMPLEADO segun "completo"): consulta_examen + posible ALTA.
 * 3. Si "Sin Registro de Usuario" -> searchProspecto (candidato); si no -> empleado con edad.
 * La busqueda solo requiere el NSS (sin campo "type").
 */
@Service
public class NssSearchService {

    private static final String SIN_REGISTRO = "Sin Registro de Usuario";
    private static final String TIPO_CANDIDATO = "CANDIDATO";
    private static final String TIPO_EMPLEADO = "EMPLEADO";

    private final NssSearchClient client;

    public NssSearchService(NssSearchClient client) {
        this.client = client;
    }

    public Optional<NssSearchResponse> findByNss(String nss) {
        String normalizedNss = normalizeNss(nss);
        String usuarioConsulta = usuarioConsulta();

        Optional<EmpleadoDto> usuario = client.findUsuario(normalizedNss, usuarioConsulta);
        if (usuario.isEmpty()) {
            return Optional.empty();
        }

        EmpleadoDto empleado = usuario.get();
        boolean sinRegistro = SIN_REGISTRO.equals(trim(empleado.completo()));
        String tipoUsuario = sinRegistro ? TIPO_CANDIDATO : TIPO_EMPLEADO;

        // Efecto colateral (igual que en php): consulta_examen y, de ser necesario, ALTA en Servcio/Medico.
        client.checkEmploye(normalizedNss, tipoUsuario, usuarioConsulta);

        if (sinRegistro) {
            // php pasa el mismo termino de busqueda ($datas) como 'curp' a searchProspecto.
            CandidatoDto candidato = client.findProspecto(normalizedNss).orElse(null);
            return Optional.of(NssSearchResponse.candidato(candidato));
        }

        return Optional.of(NssSearchResponse.empleado(empleado));
    }

    private String normalizeNss(String nss) {
        if (nss == null || nss.isBlank()) {
            throw new IllegalArgumentException("El NSS es obligatorio");
        }

        String value = nss.trim();
        if (value.length() > 50) {
            throw new IllegalArgumentException("El NSS excede la longitud permitida");
        }

        return value;
    }

    /**
     * Usuario que realiza la consulta. En php-old era $_SESSION['nombre'];
     * aqui es el principal autenticado de Spring Security.
     */
    private String usuarioConsulta() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
