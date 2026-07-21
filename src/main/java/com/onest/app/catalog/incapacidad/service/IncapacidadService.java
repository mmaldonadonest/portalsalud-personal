package com.onest.app.catalog.incapacidad.service;

import com.onest.app.catalog.incapacidad.client.IncapacidadClient;
import com.onest.app.catalog.incapacidad.client.dto.BiowsIncapacidadAltaRequest;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDetalleDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDto;
import com.onest.app.catalog.incapacidad.web.IncapacidadAltaForm;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Incapacidades por NSS (lista + detalle). Replica el flujo viewArchivoInc /
 * showDetailsIncap de php-old (WS consulta_incapacidad).
 */
@Service
public class IncapacidadService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
    private static final String USUARIO_FIJO = "747849849";

    private final IncapacidadClient client;

    public IncapacidadService(IncapacidadClient client) {
        this.client = client;
    }

    public List<IncapacidadDto> byNss(String nss) {
        return client.findIncapacidades(normalizeNss(nss));
    }

    public Optional<IncapacidadDetalleDto> detalle(String nss, String idConsulta) {
        if (idConsulta == null || idConsulta.isBlank()) {
            throw new IllegalArgumentException("El id de incapacidad es obligatorio");
        }
        return client.findIncapacidadDetail(normalizeNss(nss), idConsulta.trim());
    }

    /** Alta de incapacidad (addIncapacidad -> /Servcio/incapacidades). Devuelve el mensaje Proceso. */
    public String crearIncapacidad(IncapacidadAltaForm form) {
        String nss = normalizeNss(form.getNss());
        String relacion = (form.getIdArchivoRel() != null && !form.getIdArchivoRel().isBlank())
                ? form.getIdArchivoRel().trim()
                : UUID.randomUUID().toString().replace("-", "");

        // incap.php: si fecha_inicio vacia -> fecha_primera; si salario_acumulado vacio -> 0
        String fechaInicio = blankToNull(form.getFechaInicio()) != null ? form.getFechaInicio() : form.getFechaPrimera();
        String salarioAcumulado = blankToNull(form.getSalarioAcumulado()) != null ? form.getSalarioAcumulado() : "0";

        BiowsIncapacidadAltaRequest request = new BiowsIncapacidadAltaRequest(
                LocalDateTime.now().format(FECHA),
                nss,
                form.getFolio(),
                form.getRamo(),
                form.getTipo(),
                fechaInicio,
                form.getFechaTermino(),
                form.getDiasAutorizados(),
                form.getSalarioIntegrado(),
                form.getCosto(),
                form.getImputable(),
                form.getEstadoDictamen(),
                form.getGoceSueldo(),
                form.getComplementoSalarial(),
                relacion,
                USUARIO_FIJO,
                usuarioActual(),
                form.getFirma(),
                form.getRubro(),
                form.getFechaAlta(),
                form.getFechaPrimera(),
                form.getSt2(),
                salarioAcumulado,
                form.getAlta()
        );
        return client.crearIncapacidad(request);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
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
}
