package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.dto.ContactoEmergenciaDto;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contactos de emergencia del Examen (hasta 3), EAV via MED_TAG - mismo patron que
 * PretestService, pero sin sufijo: el TYPE ya es el nombre completo con indice
 * (contactoEmer{campo}{indice}), tal cual viene de los datos migrados.
 */
@Service
public class ContactoEmergenciaService {

    private static final String PREFIX = "contactoEmer";
    private static final String GROUP = "CONTACTO_EMERGENCIA";
    private static final List<String> CAMPOS =
            List.of("nombre", "apellido_paterno", "apellido_materno", "parentesco", "telefono");
    private static final int MAX_CONTACTOS = 3;

    private final MedTagRepository repository;

    public ContactoEmergenciaService(MedTagRepository repository) {
        this.repository = repository;
    }

    /** Carga los 3 contactos (aunque esten vacios) para precargar el formulario. */
    public List<ContactoEmergenciaDto> cargar(String nss) {
        Map<String, String> byType = repository.latestByNssAndTypePrefix(normalizeNss(nss), PREFIX);
        List<ContactoEmergenciaDto> out = new ArrayList<>();
        for (int i = 1; i <= MAX_CONTACTOS; i++) {
            out.add(new ContactoEmergenciaDto(
                    i,
                    byType.getOrDefault(PREFIX + "nombre" + i, ""),
                    byType.getOrDefault(PREFIX + "apellido_paterno" + i, ""),
                    byType.getOrDefault(PREFIX + "apellido_materno" + i, ""),
                    byType.getOrDefault(PREFIX + "parentesco" + i, ""),
                    byType.getOrDefault(PREFIX + "telefono" + i, "")));
        }
        return out;
    }

    /** Guarda los 3x5 campos (DELETE+INSERT por campo, como el resto de MED_TAG). */
    @Transactional
    public void guardar(String nss, Map<String, String> campos) {
        String normalized = normalizeNss(nss);
        String usuario = usuarioActual();
        for (int i = 1; i <= MAX_CONTACTOS; i++) {
            for (String campo : CAMPOS) {
                String type = PREFIX + campo + i;
                repository.upsert(normalized, type, campos.getOrDefault(type, ""), GROUP, usuario);
            }
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
