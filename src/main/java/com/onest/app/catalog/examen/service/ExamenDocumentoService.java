package com.onest.app.catalog.examen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onest.app.catalog.examen.client.ExamenClient;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import com.onest.app.catalog.nss.service.NssSearchService;
import com.onest.app.catalog.pretest.repository.MedTagRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * "Imprimir expediente" del examen medico: reproduce 1:1 el documento de 15 paginas de
 * php-old/pdf/pdfGenerator.php (consentimiento FT-SO-11, aviso de privacidad FT-SO-32 y
 * examen FT-SO-04). El PHP arma ~520 variables desde 3 fuentes y la plantilla las imprime;
 * aqui se hace lo mismo con la MISMA logica para que el documento salga identico:
 *
 * <ul>
 *   <li>WS consulta_examen (412 campos, "SECCION.CAMPO") - ya lo trae ExamenClient.getExamenData.</li>
 *   <li>WS del empleado (nombre, RFC, cuenta, puesto...) - NssSearchService.</li>
 *   <li>Tabla legacy "tags" (80 valores: contactos de emergencia, trabajos anteriores, dientes,
 *       tipo de examen, oidos...) - hoy SERV_MED_TAG en la base del portal (migracion U09).</li>
 * </ul>
 *
 * <p>El mapeo variable PHP -> fuente/ruta/transformacion vive en
 * {@code examen-documento-campos.json}, generado por script a partir del PHP (no se edita a
 * mano). Las transformaciones que el PHP hace con codigo (edad desde el RFC, tipo de examen,
 * resultado 1-4, Romberg, firma) estan aqui.
 */
@Service
public class ExamenDocumentoService {

    private static final Logger log = LoggerFactory.getLogger(ExamenDocumentoService.class);

    /** Una variable del PHP: de donde sale y como se transforma. */
    record Campo(String var, String kind, String arg, String transform) {
    }

    /** Lo que ve la plantilla: {@code d.v('X')}, {@code d.negado('X')}, etc. */
    public static final class Documento {
        private final Map<String, String> v;
        private final String firmaTrabajador;
        private final String ahora;

        Documento(Map<String, String> v, String firmaTrabajador, String ahora) {
            this.v = v;
            this.firmaTrabajador = firmaTrabajador;
            this.ahora = ahora;
        }

        /** {@code <?php echo $X; ?>} */
        public String v(String var) {
            String s = v.get(var);
            return s == null ? "" : s;
        }

        /** {@code comprobarValor()}: relleno o vacio -> "Negado". */
        public String negado(String var) {
            return esRelleno(v(var)) ? "Negado" : v(var);
        }

        /** {@code comprobarValor2()}: relleno o vacio -> "No valorado". */
        public String noValorado(String var) {
            return esRelleno(v(var)) ? "No valorado" : v(var);
        }

        /** {@code evaluarCondicionDental()}. */
        public String diente(String var) {
            return DIENTES.getOrDefault(v(var).trim(), "Diente sano");
        }

        public String getFirmaTrabajador() {
            return firmaTrabajador;
        }

        public String getAhora() {
            return ahora;
        }

        /** Mismo criterio que el PHP: strtolower + lista fija; empty("0") tambien es true en PHP. */
        private static boolean esRelleno(String s) {
            String t = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
            return t.isEmpty() || t.equals("0") || t.equals("sin observación") || t.equals("sin observacion")
                    || t.equals("sin obs") || t.equals("vacio") || t.equals("sin observaciones");
        }
    }

    private static final Map<String, String> DIENTES = Map.ofEntries(
            Map.entry("C", "Caries dental"), Map.entry("A", "AMALGAMA"), Map.entry("P", "PROTESIS"),
            Map.entry("PD", "Periodontitis"), Map.entry("PPD", "Profundidad de bolsas periodontales"),
            Map.entry("G", "Gingivitis"), Map.entry("PI", "Placa bacteriana"), Map.entry("S", "Sarro"),
            Map.entry("F", "Fractura dental"), Map.entry("R", "Restauración dental"),
            Map.entry("RC", "Relación céntrica"), Map.entry("O", "Oclusión"), Map.entry("V", "Vitalidad pulpar"),
            Map.entry("E", "Endodoncia"), Map.entry("D", "Desgaste dental"),
            Map.entry("ATM", "Articulación temporomandibular"), Map.entry("Sx", "Síntomas"), Map.entry("Tx", "Tratamiento"));

    private static final Map<String, String> TIPO_EXAMEN = Map.of(
            "0", "No seleccionado", "1", "Admisión", "2", "Périodico", "3", "Cambio de rol",
            "4", "Post incapacidad", "5", "Especial");

    private final ExamenClient client;
    private final NssSearchService nssSearchService;
    private final MedTagRepository tags;
    private final List<Campo> campos;

    public ExamenDocumentoService(ExamenClient client, NssSearchService nssSearchService, MedTagRepository tags) {
        this.client = client;
        this.nssSearchService = nssSearchService;
        this.tags = tags;
        this.campos = cargarCampos();
    }

    public Documento armar(String nss) {
        String nssLimpio = nss.trim();
        Map<String, String> data = client.getExamenData(nssLimpio);
        EmpleadoDto emp = nssSearchService.findByNss(nssLimpio).map(r -> r.empleado()).orElse(null);
        Map<String, String> tagValues;
        try {
            tagValues = tags.latestByNssAndTypeSuffix(nssLimpio, "");
        } catch (RuntimeException ex) {
            log.warn("[examen-doc] sin SERV_MED_TAG para {}: {}", nssLimpio, ex.getMessage());
            tagValues = Map.of();
        }

        Map<String, String> v = new HashMap<>();
        for (Campo c : campos) {
            String valor = switch (c.kind()) {
                case "DATA" -> data.get(c.arg());
                case "USER" -> campoEmpleado(emp, c.arg());
                case "TAG" -> tagValues.get(c.arg());
                case "VAR" -> v.get(c.arg());
                default -> null;
            };
            if ("SINO".equals(c.transform())) {
                valor = esCero(valor) ? "No" : "Sí";
            }
            v.put(c.var(), valor == null ? "" : valor);
        }

        // --- valores que el PHP imprimia crudos y se decidio humanizar (13-sep-2026)
        v.put("sexo", switch (limpio(v.get("sexo"))) {
            case "1" -> "Masculino";
            case "2" -> "Femenino";
            default -> limpio(v.get("sexo"));
        });
        if ("0".equals(limpio(v.get("edoCivil")))) {
            v.put("edoCivil", "");
        }
        if (limpio(v.get("puesto")).toLowerCase(Locale.ROOT).contains("no existe")) {
            v.put("puesto", "");
        }

        // --- lo que el PHP hace con codigo, no con asignaciones
        v.put("edad", edadDesdeRfc(v.get("rfc"), emp));
        v.put("tipoExamenInputO", TIPO_EXAMEN.getOrDefault(limpio(v.get("tipoExamenInputO")), limpio(v.get("tipoExamenInputO"))));
        v.put("OBSERVACIONESRESEXAM1", switch (limpio(data.get("SERV_MED_RESULTADO_EXAMEN.OBSERVACIONES"))) {
            case "4", "no_apto" -> "No apto";
            case "1", "apto" -> "Apto";
            case "2", "apto_condicionado" -> "Apto condicionado";
            case "3", "apto_restringido" -> "Apto restringido";
            default -> "";
        });
        v.put("ROMBERGEXFIS", esCero(tagValues.get("ROMBERGEXFIS")) ? "Negativo" : "Positivo");
        // el PHP imprime el "0" del WS tal cual cuando no hay fecha; se conserva
        String firma = data.get("SERV_MED_RESULTADO_EXAMEN.FIRMA_DIGITAL");
        String firmaTrabajador = firma != null && firma.startsWith("data:image") ? firma : null;

        return new Documento(v, firmaTrabajador,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yy, HH:mm a", new Locale("es", "MX"))));
    }

    /** Campos de $user->... del PHP (searchUser) sobre EmpleadoDto. */
    private static String campoEmpleado(EmpleadoDto e, String campo) {
        if (e == null) {
            return "";
        }
        return switch (campo) {
            case "nombre" -> e.nombre();
            case "apellidoPaterno" -> e.apellidoPaterno();
            case "apellidoMaterno" -> e.apellidoMaterno();
            case "rfc" -> e.rfc();
            case "nss" -> e.nss();
            case "sexo" -> e.sexo();
            case "cuenta" -> e.cuenta();
            case "fecha_nacimiento" -> e.fechaNacimiento();
            case "estado_civil" -> e.estadoCivil();
            case "direccion" -> e.direccion();
            case "celular" -> e.celular();
            case "turno" -> e.turno();
            case "tel_fijo" -> e.telFijo();
            case "completo" -> e.completo();
            case "nombre_puesto" -> e.nombrePuesto();
            case "nombre_empresa" -> e.nombreEmpresa();
            default -> "";
        };
    }

    /**
     * Igual que el PHP: aa/mm/dd en las posiciones 4-9 del RFC, siglo 19 si aa > 50, edad
     * ajustada si aun no cumple anios. Si el RFC no trae fecha valida, vacio.
     */
    static String edadDesdeRfc(String rfc, EmpleadoDto emp) {
        try {
            String r = rfc == null ? "" : rfc.trim();
            int aa = Integer.parseInt(r.substring(4, 6));
            int mm = Integer.parseInt(r.substring(6, 8));
            int dd = Integer.parseInt(r.substring(8, 10));
            int anio = aa > 50 ? 1900 + aa : 2000 + aa;
            LocalDate nac = LocalDate.of(anio, mm, dd);
            LocalDate hoy = LocalDate.now();
            int edad = hoy.getYear() - nac.getYear();
            if (nac.withYear(hoy.getYear()).isAfter(hoy)) {
                edad--;
            }
            return String.valueOf(edad);
        } catch (RuntimeException ex) {
            return "";
        }
    }

    /** PHP: {@code $x == 0} - vacio, "0", "no", "false" cuentan como cero. */
    private static boolean esCero(String v) {
        if (v == null) {
            return true;
        }
        String t = v.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty() || t.equals("0") || t.equals("no") || t.equals("false") || t.equals("sin datos");
    }

    private static String limpio(String v) {
        return v == null ? "" : v.trim();
    }

    private static List<Campo> cargarCampos() {
        try (InputStream in = new ClassPathResource("examen-documento-campos.json").getInputStream()) {
            List<Map<String, String>> raw = new ObjectMapper().readValue(in, new TypeReference<>() { });
            return raw.stream().map(m -> new Campo(m.get("var"), m.get("kind"), m.get("arg"), m.get("transform"))).toList();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer examen-documento-campos.json", ex);
        }
    }
}
