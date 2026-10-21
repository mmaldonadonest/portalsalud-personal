package com.onest.app.catalog.expediente.service;

import com.onest.app.catalog.expediente.web.ConsultaAltaForm;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Datos del pase de salida FT-SO-02 (20-oct-2026) que el WS de consulta no guarda: motivo (cita
 * programada o envio), fecha de cita, horas de salida y entrada, si regresa, incapacidad y
 * observaciones. Van a SERV_MED_TAG (base del portal) con TYPE "FTSO02.<idArchivoRel>.<CAMPO>":
 * el idArchivoRel lo genera el portal para cada alta y la consulta lo guarda, asi que liga sin
 * depender del ID_CONSULTA (que el WS no regresa).
 */
@Service
public class PaseSalidaService {

    static final String GRUPO = "PASE_SALIDA_FTSO02";
    public static final String TIPO = "pase_salida";

    private final MedTagRepository tags;

    public PaseSalidaService(MedTagRepository tags) {
        this.tags = tags;
    }

    /** true si la consulta es un pase de salida; los datos reales traen variantes ("PASE DE SALIDA"). */
    public static boolean esPase(String tipoConsulta) {
        if (tipoConsulta == null) {
            return false;
        }
        String t = tipoConsulta.toLowerCase().replace('_', ' ');
        return t.contains("pase de salida") || t.contains("pase salida");
    }

    /**
     * Motivo y hora de salida son obligatorios en un pase (decision del usuario 20-oct-2026: sin eso el
     * pase no sirve). Se revisa ANTES del WS para no dejar una consulta de pase sin sus datos.
     * null si esta bien o no es pase; si no, el mensaje para el usuario.
     */
    public static String errorObligatorios(ConsultaAltaForm form) {
        if (!esPase(form.getTipoConsulta())) {
            return null;
        }
        if (limpio(form.getPaseMotivo()).isEmpty()) {
            return "En el pase de salida el Motivo es obligatorio.";
        }
        if (limpio(form.getPaseSalida()).isEmpty()) {
            return "En el pase de salida la Hora de salida es obligatoria.";
        }
        return null;
    }

    /** Guarda el pase de la consulta recien dada de alta (solo tipo pase de salida y con idArchivoRel). */
    public void guardar(ConsultaAltaForm form) {
        String rel = limpio(form.getIdArchivoRel());
        if (!esPase(form.getTipoConsulta()) || !rel.matches("[A-Za-z0-9]{8,64}")) {
            return;
        }
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("MOTIVO", form.getPaseMotivo());
        campos.put("FECHA_CITA", form.getPaseFechaCita());
        campos.put("SALIDA", form.getPaseSalida());
        campos.put("ENTRADA", form.getPaseEntrada());
        campos.put("REGRESA", form.getPaseRegresa());
        campos.put("INCAP_DIAS", form.getPaseIncapDias());
        campos.put("INCAP_INICIA", form.getPaseIncapInicia());
        campos.put("INCAP_TERMINA", form.getPaseIncapTermina());
        campos.put("OBSERVACIONES", form.getPaseObservaciones());
        String nss = limpio(form.getNss());
        String usuario = usuarioActual();
        campos.forEach((campo, valor) -> {
            String v = limpio(valor);
            if (!v.isEmpty()) {
                tags.upsert(nss, "FTSO02." + rel + "." + campo, v, GRUPO, usuario);
            }
        });
    }

    /** Campos del pase (sin el prefijo); vacio si la consulta no tiene pase capturado. */
    public Map<String, String> leer(String nss, String rel) {
        Map<String, String> r = new LinkedHashMap<>();
        if (rel == null || rel.isBlank()) {
            return r;
        }
        String prefijo = "FTSO02." + rel.trim() + ".";
        tags.latestByNssAndTypePrefix(nss, prefijo).forEach((k, v) -> r.put(k.substring(prefijo.length()), v == null ? "" : v));
        return r;
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
