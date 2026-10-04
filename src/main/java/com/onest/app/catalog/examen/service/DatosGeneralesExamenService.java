package com.onest.app.catalog.examen.service;

import com.onest.app.catalog.examen.dto.DatosGeneralesExamenDto;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos generales del examen que viven en SERV_MED_TAG y no en las tablas SERV_MED_* del WS
 * (mismo patron que ContactoEmergenciaService / DiagnosticoSecundarioService):
 *
 * <ul>
 *   <li><b>Tipo de examen</b> - el legacy lo guardaba en el tag {@code tipoExamenInputO} (grupo
 *       PERMISO_EXAMEN) con un codigo numerico; el documento impreso ya lo lee de ahi
 *       ({@link ExamenDocumentoService}). Se conservan tipo y codigos para que lo migrado y lo
 *       nuevo se lean igual. Es la primera pantalla Java que lo CAPTURA: hasta ahora solo se leia.</li>
 *   <li><b>SpO2</b> - tag {@code STPO2} (grupo EXAMEN_FISICO) del legacy, tambien solo se leia.</li>
 *   <li><b>Cintura y cadera</b> - nuevos en la rev. 04 del FT-SO-04 (sep-2024); no existen en
 *       ninguna tabla del WS, asi que van al mismo grupo EXAMEN_FISICO con tipos nuevos.</li>
 * </ul>
 *
 * <p>Van a SERV_MED_TAG y no al WS a proposito: agregar columnas a SERV_MED_EXPLORACION_FISICA
 * implicaria modificar un WS productivo (PR_SERVICIO_MED_EXAMEN2 y PR_SERVICIO_MED_CONSULTA).
 */
@Service
public class DatosGeneralesExamenService {

    public static final String TIPO_EXAMEN = "tipoExamenInputO";
    public static final String SPO2 = "STPO2";
    public static final String CINTURA = "CINTURA";
    public static final String CADERA = "CADERA";

    private static final String GRUPO_PERMISO = "PERMISO_EXAMEN";
    private static final String GRUPO_FISICO = "EXAMEN_FISICO";

    /**
     * Codigos del legacy (php-old, select tipoExamenInputO) -> etiqueta. El "0" era "No
     * seleccionado" y aqui equivale a vacio. Mismo mapa que ExamenDocumentoService.TIPO_EXAMEN.
     */
    public static final Map<String, String> TIPOS;

    static {
        // LinkedHashMap (no Map.of) para que el select salga en el orden del legacy
        Map<String, String> m = new LinkedHashMap<>();
        m.put("1", "Admisión");
        m.put("2", "Periódico");
        m.put("3", "Cambio de rol");
        m.put("4", "Post incapacidad");
        m.put("5", "Especial");
        TIPOS = Collections.unmodifiableMap(m);
    }

    private final MedTagRepository repository;

    public DatosGeneralesExamenService(MedTagRepository repository) {
        this.repository = repository;
    }

    /** Catalogo para el select, en el orden del legacy. */
    public Map<String, String> tipos() {
        return TIPOS;
    }

    public DatosGeneralesExamenDto cargar(String nss) {
        String limpio = normalizeNss(nss);
        // Dos lecturas por prefijo exacto: los cuatro tipos no comparten prefijo ni sufijo util
        Map<String, String> tags = new LinkedHashMap<>();
        tags.putAll(repository.latestByNssAndTypePrefix(limpio, TIPO_EXAMEN));
        tags.putAll(repository.latestByNssAndTypePrefix(limpio, SPO2));
        tags.putAll(repository.latestByNssAndTypePrefix(limpio, CINTURA));
        tags.putAll(repository.latestByNssAndTypePrefix(limpio, CADERA));
        String tipo = limpio(tags.get(TIPO_EXAMEN));
        return new DatosGeneralesExamenDto(
                "0".equals(tipo) ? "" : tipo,
                limpio(tags.get(SPO2)),
                limpio(tags.get(CINTURA)),
                limpio(tags.get(CADERA)));
    }

    /**
     * Guarda los 4 campos (DELETE+INSERT por campo). Un campo vacio se guarda vacio, igual que
     * Contactos de emergencia: permite "borrar" un dato capturado por error.
     */
    @Transactional
    public void guardar(String nss, Map<String, String> campos) {
        String limpio = normalizeNss(nss);
        String tipo = limpio(campos.get("tipoExamen"));
        if (!tipo.isEmpty() && !TIPOS.containsKey(tipo)) {
            throw new IllegalArgumentException("Tipo de examen no válido");
        }
        String usuario = usuarioActual();
        repository.upsert(limpio, TIPO_EXAMEN, tipo, GRUPO_PERMISO, usuario);
        repository.upsert(limpio, SPO2, limpio(campos.get("spo2")), GRUPO_FISICO, usuario);
        repository.upsert(limpio, CINTURA, limpio(campos.get("cintura")), GRUPO_FISICO, usuario);
        repository.upsert(limpio, CADERA, limpio(campos.get("cadera")), GRUPO_FISICO, usuario);
    }

    private static String limpio(String v) {
        return v == null ? "" : v.trim();
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
