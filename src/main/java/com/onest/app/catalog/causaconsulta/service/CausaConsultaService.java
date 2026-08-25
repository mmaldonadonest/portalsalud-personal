package com.onest.app.catalog.causaconsulta.service;

import com.onest.app.catalog.causaconsulta.client.CausaConsultaClient;
import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaAltaRequest;
import com.onest.app.catalog.causaconsulta.client.dto.BiowsCausaConsultaEstadoRequest;
import com.onest.app.catalog.causaconsulta.dto.CausaConsultaDto;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Catálogo administrable de causas de consulta (23 predefinidas + "Otro"). Backend
 * aplicado 2026-08-24 (docs/ords-causa-consulta.sql). Alcance: docs/ONEST_Proyecto_Salud_Ocupacional_Decisiones.pdf §2.
 */
@Service
public class CausaConsultaService {

    private static final String USUARIO_FIJO = "747849849";

    private final CausaConsultaClient client;

    public CausaConsultaService(CausaConsultaClient client) {
        this.client = client;
    }

    /** soloActivos=true es lo que consume el &lt;select&gt; de captura de Consulta. */
    public List<CausaConsultaDto> listar(boolean soloActivos) {
        return client.listar(soloActivos);
    }

    /** Alta de una causa nueva. El WS valida duplicados (case-insensitive) del lado Oracle. */
    public String crear(String nombre) {
        String normalizado = normalizeNombre(nombre);
        BiowsCausaConsultaAltaRequest request = new BiowsCausaConsultaAltaRequest(normalizado, USUARIO_FIJO, usuarioActual());
        return client.crear(request);
    }

    /** Activa o desactiva (baja logica) una causa existente. */
    public String cambiarEstado(String idRegistro, boolean activar) {
        long regId = parseRegId(idRegistro);
        BiowsCausaConsultaEstadoRequest request = new BiowsCausaConsultaEstadoRequest(regId, activar ? "Y" : "N");
        return client.cambiarEstado(request);
    }

    private static String normalizeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la causa es obligatorio");
        }
        String value = nombre.trim();
        if (value.length() > 200) {
            throw new IllegalArgumentException("El nombre excede la longitud permitida");
        }
        return value;
    }

    private static long parseRegId(String idRegistro) {
        if (idRegistro == null || idRegistro.isBlank()) {
            throw new IllegalArgumentException("El identificador de la causa es obligatorio");
        }
        try {
            return Long.parseLong(idRegistro.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El identificador de la causa no es valido", ex);
        }
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
