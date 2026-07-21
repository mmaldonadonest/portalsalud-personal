package com.onest.app.catalog.pretest.service;

import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-Test (U03): cuestionario EAV en MED_TAG. Cada campo se guarda con TYPE = <base>PRETEST
 * y TAG_GROUP='PRETEST'. Equivale a pretest.php (load) + savePretest.php (save).
 */
@Service
public class PretestService {

    private static final String SUFFIX = "PRETEST";
    private static final String GROUP = "PRETEST";

    private final MedTagRepository repository;

    public PretestService(MedTagRepository repository) {
        this.repository = repository;
    }

    /** Carga los campos del pretest, mapeados por su nombre base (sin sufijo PRETEST). */
    public Map<String, String> load(String nss) {
        Map<String, String> byType = repository.latestByNssAndTypeSuffix(normalizeNss(nss), SUFFIX);
        Map<String, String> byBase = new HashMap<>();
        byType.forEach((type, content) -> {
            String base = type.endsWith(SUFFIX) ? type.substring(0, type.length() - SUFFIX.length()) : type;
            byBase.put(base, content);
        });
        return byBase;
    }

    /** Guarda cada campo (base -> base+PRETEST) con DELETE+INSERT. */
    @Transactional
    public void save(String nss, Map<String, String> fields) {
        String normalized = normalizeNss(nss);
        String usuario = usuarioActual();
        fields.forEach((base, value) ->
                repository.upsert(normalized, base + SUFFIX, value, GROUP, usuario));
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
