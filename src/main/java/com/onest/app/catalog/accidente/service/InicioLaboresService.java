package com.onest.app.catalog.accidente.service;

import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * C-092 (P-044, SM 18-nov-2026): fecha de inicio de labores de un accidente. De ella salen el mes y el
 * año del nombre de los ST. El WS de accidentes no tiene la columna, asi que va a SERV_MED_TAG (base del
 * portal) con TYPE "ACC.&lt;REG_ID&gt;.INICIO_LABORES": se captura en el seguimiento del caso, cuando ya existe
 * el REG_ID (al dar de alta el accidente todavia no se conoce esa fecha).
 */
@Service
public class InicioLaboresService {

    static final String PREFIJO = "ACC.";
    static final String CAMPO = ".INICIO_LABORES";
    private static final String GRUPO = "ACCIDENTE";
    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MedTagRepository tags;

    public InicioLaboresService(MedTagRepository tags) {
        this.tags = tags;
    }

    /** REG_ID del accidente -> fecha ISO, de todos los accidentes del NSS. */
    public Map<String, String> porNss(String nss) {
        Map<String, String> r = new LinkedHashMap<>();
        tags.latestByNssAndTypePrefix(nss, PREFIJO).forEach((type, valor) -> {
            if (type.endsWith(CAMPO) && valor != null && !valor.isBlank()) {
                r.put(type.substring(PREFIJO.length(), type.length() - CAMPO.length()), valor.trim());
            }
        });
        return r;
    }

    /**
     * Guarda (o borra, si viene vacia) la fecha. No puede ser anterior al accidente ni posterior a un año
     * desde hoy (una fecha programada de regreso es valida; un año de mas es error de captura).
     */
    public void guardar(String nss, String regId, String fechaIso, String fechaAccidente) {
        if (regId == null || !regId.trim().matches("[0-9]{1,18}")) {
            throw new IllegalArgumentException("Caso de accidente no válido.");
        }
        String valor = null;
        if (fechaIso != null && !fechaIso.isBlank()) {
            LocalDate f;
            try {
                f = LocalDate.parse(fechaIso.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("La fecha de inicio de labores no es válida.");
            }
            LocalDate acc = fecha(fechaAccidente);
            if (acc != null && f.isBefore(acc)) {
                throw new IllegalArgumentException("El inicio de labores no puede ser anterior al accidente ("
                        + acc.format(DMY) + ").");
            }
            if (f.isAfter(LocalDate.now(ZONA).plusYears(1))) {
                throw new IllegalArgumentException("Revisa el año del inicio de labores.");
            }
            valor = f.toString();
        }
        tags.upsert(nss, PREFIJO + regId.trim() + CAMPO, valor, GRUPO, usuario());
    }

    /** El WS manda la fecha del accidente como AAAA-MM-DD o DD/MM/AAAA; otra cosa no se valida. */
    static LocalDate fecha(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String s = v.trim();
        try {
            return s.length() >= 10 && s.charAt(4) == '-' ? LocalDate.parse(s.substring(0, 10))
                    : LocalDate.parse(s.substring(0, Math.min(10, s.length())), DMY);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException ex) {
            return null;
        }
    }

    private static String usuario() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || a.getName() == null ? "SISTEMA" : a.getName();
    }
}
