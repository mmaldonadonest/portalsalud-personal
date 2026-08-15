package com.onest.app.catalog.antidoping.service;

import com.onest.app.catalog.antidoping.client.AntidopingClient;
import com.onest.app.catalog.antidoping.client.dto.BiowsAntidopingAltaRequest;
import com.onest.app.catalog.antidoping.dto.AntidopingDto;
import com.onest.app.catalog.antidoping.web.AntidopingAltaForm;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Antidoping/alcoholimetria por NSS (lista + alta). Backend aplicado y verificado
 * 2026-08-14 contra el WS ORDS real (docs/ords-antidoping.sql).
 */
@Service
public class AntidopingService {

    private static final String USUARIO_FIJO = "747849849";

    private final AntidopingClient client;

    public AntidopingService(AntidopingClient client) {
        this.client = client;
    }

    public List<AntidopingDto> byNss(String nss) {
        return client.findAntidopings(normalizeNss(nss));
    }

    /** Alta de antidoping (POST /Servcio/antidoping). Devuelve el mensaje Proceso. */
    public String crearAntidoping(AntidopingAltaForm form) {
        String nss = normalizeNss(form.getNss());
        if (form.getTipoPrueba() == null || form.getTipoPrueba().isBlank()) {
            throw new IllegalArgumentException("El tipo de prueba es obligatorio");
        }
        if (form.getResultado() == null || form.getResultado().isBlank()) {
            throw new IllegalArgumentException("El resultado es obligatorio");
        }

        BiowsAntidopingAltaRequest request = new BiowsAntidopingAltaRequest(
                nss,
                form.getFolio(),
                form.getTipoPrueba(),
                form.getSustancia(),
                form.getResultado(),
                form.getStatusConclusion(),
                form.getObservaciones(),
                USUARIO_FIJO,
                usuarioActual());
        return client.crearAntidoping(request);
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
