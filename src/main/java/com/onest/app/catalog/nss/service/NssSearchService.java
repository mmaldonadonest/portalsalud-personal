package com.onest.app.catalog.nss.service;

import com.onest.app.catalog.nss.client.NssSearchClient;
import com.onest.app.catalog.nss.dto.CandidatoDto;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.dto.NssSearchResponse;
import java.util.List;
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
    private final com.onest.app.integration.ocr.SolicitudExamenRepository solicitudesOcr;

    public NssSearchService(NssSearchClient client, com.onest.app.integration.ocr.SolicitudExamenRepository solicitudesOcr) {
        this.client = client;
        this.solicitudesOcr = solicitudesOcr;
    }

    public Optional<NssSearchResponse> findByNss(String nss) {
        String normalizedNss = normalizeNss(nss);
        String usuarioConsulta = usuarioConsulta();

        // Todas las asociaciones (predio/cuenta) del NSS, no solo la primera - un NSS
        // multipredio trae mas de una fila (ver docs/entregable-liberacion-stoppers-salud.html §4).
        List<EmpleadoDto> asociaciones = client.findAsociaciones(normalizedNss, usuarioConsulta);
        if (asociaciones.isEmpty()) {
            // Integracion OCR (docs/integracion-ocr-examen-inicial.md): un candidato que aun no esta
            // en el biometrico pero tiene solicitud de examen inicial se presenta con la ficha de
            // la solicitud, para que abran los modulos y el examen se guarde por su NSS.
            return solicitudOcr(normalizedNss).map(sol -> NssSearchResponse.candidatoOcr(fichaDesdeSolicitud(sol), sol));
        }

        EmpleadoDto empleado = asociaciones.get(0);
        boolean sinRegistro = SIN_REGISTRO.equals(trim(empleado.completo()));
        String tipoUsuario = sinRegistro ? TIPO_CANDIDATO : TIPO_EMPLEADO;

        // Efecto colateral (igual que en php): consulta_examen y, de ser necesario, ALTA en Servcio/Medico.
        client.checkEmploye(normalizedNss, tipoUsuario, usuarioConsulta);

        if (sinRegistro) {
            // Candidato con solicitud de examen inicial de OCR: la ficha sale de la solicitud
            // (el WS contesta "Sin Registro de Usuario" para un NSS que aun no es empleado).
            Optional<com.onest.app.integration.ocr.SolicitudExamen> sol = solicitudOcr(normalizedNss);
            if (sol.isPresent()) {
                return Optional.of(NssSearchResponse.candidatoOcr(fichaDesdeSolicitud(sol.get()), sol.get()));
            }
            // php pasa el mismo termino de busqueda ($datas) como 'curp' a searchProspecto.
            CandidatoDto candidato = client.findProspecto(normalizedNss).orElse(null);
            return Optional.of(NssSearchResponse.candidato(candidato));
        }

        // Empleado ya en biometrico que ademas tiene solicitud de OCR (p.ej. recontratacion): se
        // adjunta para mostrar el aviso de "examen inicial pendiente".
        NssSearchResponse resp = NssSearchResponse.empleado(empleado, asociaciones);
        return Optional.of(solicitudOcr(normalizedNss).map(resp::conSolicitud).orElse(resp));
    }

    /** Solicitud viva del NSS o, si no hay, la ultima (para seguir viendo la ficha del candidato ya examinado). Nunca lanza. */
    private Optional<com.onest.app.integration.ocr.SolicitudExamen> solicitudOcr(String nss) {
        try {
            Optional<com.onest.app.integration.ocr.SolicitudExamen> viva = solicitudesOcr.activaPorNss(nss);
            return viva.isPresent() ? viva : solicitudesOcr.ultimaPorNss(nss);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /** Ficha del candidato con los datos de la solicitud (mismo DTO que el biometrico, para que todo lo demas funcione igual). */
    static EmpleadoDto fichaDesdeSolicitud(com.onest.app.integration.ocr.SolicitudExamen s) {
        String nacimiento = s.fechaNacimiento() == null ? null
                : s.fechaNacimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        Integer edad = s.fechaNacimiento() == null ? null
                : java.time.Period.between(s.fechaNacimiento(), java.time.LocalDate.now()).getYears();
        // EmpleadoDto.sexo usa la convencion del biometrico: 1 = hombre, 2 = mujer
        String sexo = "M".equals(s.sexo()) ? "1" : ("F".equals(s.sexo()) ? "2" : null);
        return new EmpleadoDto(s.nss(), s.nombre(), s.apellidoPaterno(), s.apellidoMaterno(), s.rfc(), s.cuenta(),
                nacimiento, s.estadoCivil(), sexo, s.domicilio(), s.telefono(), null, null,
                s.nombreCompleto(), s.puestoSolicitado(), s.empresa(), edad, null);
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
