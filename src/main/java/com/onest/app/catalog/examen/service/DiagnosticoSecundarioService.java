package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.dto.DiagnosticoSecundarioDto;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diagnosticos secundarios del Examen (hasta 3), EAV via MED_TAG - mismo patron que
 * ContactoEmergenciaService. NSS-scoped (no por consulta/examen individual), igual que
 * Contactos de emergencia - es un dato de la persona, no de un evento puntual.
 */
@Service
public class DiagnosticoSecundarioService {

    private static final String PREFIX = "diagnosticoSecundario";
    private static final String GROUP = "DIAGNOSTICO_SECUNDARIO";
    private static final int MAX_DIAGNOSTICOS = 3;

    private final MedTagRepository repository;

    public DiagnosticoSecundarioService(MedTagRepository repository) {
        this.repository = repository;
    }

    /** Carga los 3 slots (aunque esten vacios) para precargar el formulario. */
    public List<DiagnosticoSecundarioDto> cargar(String nss) {
        Map<String, String> byType = repository.latestByNssAndTypePrefix(normalizeNss(nss), PREFIX);
        List<DiagnosticoSecundarioDto> out = new ArrayList<>();
        for (int i = 1; i <= MAX_DIAGNOSTICOS; i++) {
            out.add(new DiagnosticoSecundarioDto(i, byType.getOrDefault(PREFIX + i, "")));
        }
        return out;
    }

    /** Guarda los 3 slots (DELETE+INSERT por campo, como el resto de MED_TAG). */
    @Transactional
    public void guardar(String nss, Map<String, String> campos) {
        String normalized = normalizeNss(nss);
        String usuario = usuarioActual();
        for (int i = 1; i <= MAX_DIAGNOSTICOS; i++) {
            String type = PREFIX + i;
            repository.upsert(normalized, type, campos.getOrDefault(type, ""), GROUP, usuario);
        }
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

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
