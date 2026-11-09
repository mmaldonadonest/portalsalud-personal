package com.onest.app.catalog.examen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onest.app.catalog.examen.client.ExamenClient;
import com.onest.app.catalog.examen.repository.FichaBiometricoRepository;
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
            return describirDiente(v(var));
        }

        /** Si/No de los tags EXM_* de los formatos (1/0); vacio si no se capturo. */
        public String siNo(String var) {
            String s = v(var).trim();
            return "1".equals(s) ? "Sí" : "0".equals(s) ? "No" : s;
        }

        /** Fechas de los tags EXM_* (se guardan ISO, yyyy-MM-dd) como dd/MM/yyyy; otro formato sale tal cual. */
        public String fecha(String var) {
            String s = v(var).trim();
            return s.matches("\\d{4}-\\d{2}-\\d{2}.*")
                    ? s.substring(8, 10) + "/" + s.substring(5, 7) + "/" + s.substring(0, 4) : s;
        }

        /**
         * Signo vital con su unidad; en blanco si no se midio (28-oct-2026, pedido del usuario: el WS
         * devuelve "sin observaciones" o 0 y en el impreso salia "sin observaciones °C").
         */
        public String medida(String var, String unidad) {
            String s = v(var).trim();
            return esRelleno(s) ? "" : (unidad == null || unidad.isEmpty() ? s : s + " " + unidad);
        }

        // ---- Hojas con el diseno de cada formato EMLE (fragments/formato-emle.html, 29-oct-2026) ----

        private static final java.util.Set<String> NEGATIVOS = java.util.Set.of("0", "no", "negado", "negados",
                "negativo", "ninguno", "ninguna", "n/a", "na", "false", "ausente", "ausentes");

        /** Respuesta afirmativa: capturado y no es un "no" (el WS guarda 1/0, Si/No o texto libre). */
        public boolean afirma(String var) {
            String s = v(var).trim().toLowerCase(Locale.ROOT);
            return !esRelleno(s) && !NEGATIVOS.contains(s);
        }

        /** Respuesta negativa explicita (0, No, Negado...); vacio no cuenta como "no". */
        public boolean niega(String var) {
            String s = v(var).trim().toLowerCase(Locale.ROOT);
            return !s.isEmpty() && (NEGATIVOS.contains(s) || "0".equals(s));
        }

        /** Casilla SI del formato. */
        public String si(String var) {
            return afirma(var) ? "X" : "";
        }

        /** Casilla NO del formato. */
        public String no(String var) {
            return niega(var) ? "X" : "";
        }

        /**
         * Observacion de un campo SI/NO: la columna _OBS (los repetidos llevan el numero al final:
         * EPILEPSIA2 -> EPILEPSIA_OBS2; alergias: ALRGNEWINP -> ALRGNEWINPOBS). Vacia si es relleno.
         */
        public String obs(String var) {
            String mayus = var.toUpperCase(Locale.ROOT);   // "avc" guarda su observacion en AVC_OBS
            java.util.List<String> candidatos = var.matches(".*\\d$")
                    ? java.util.List.of(var.replaceFirst("(\\d+)$", "_OBS$1"), var + "_OBS")
                    : java.util.List.of(var + "_OBS", var + "OBS", mayus + "_OBS");
            for (String c : candidatos) {
                String s = v(c).trim();
                if (!esRelleno(s)) {
                    return s;
                }
            }
            return "";
        }

        /**
         * Renglon del FT-SO-17 que junta varios campos del portal (el periodico pide "SIST NERVIOSO", el portal
         * captura aneurisma, epilepsia...). Spec "VAR:Etiqueta,VAR:Etiqueta". Lo afirmado, con su observacion;
         * "Negado" si todo se nego; vacio si nada se capturo.
         */
        public String resumen(String spec) {
            java.util.List<String> afirmados = new java.util.ArrayList<>();
            boolean alguno = false;
            for (String par : spec.split(",")) {
                String[] p = par.trim().split(":", 2);
                String var = p[0].trim();
                alguno |= afirma(var) || niega(var);
                if (afirma(var)) {
                    String etiqueta = p.length > 1 ? p[1].trim() : "";
                    String o = obs(var);
                    String valor = v(var).trim();
                    String detalle = !o.isEmpty() ? o : (valor.matches("(?i)1|si|sí|positivo") ? "" : valor);
                    afirmados.add(etiqueta.isEmpty() ? detalle : etiqueta + (detalle.isEmpty() ? "" : " (" + detalle + ")"));
                }
            }
            return !afirmados.isEmpty() ? String.join(", ", afirmados) : (alguno ? "Negado" : "");
        }

        /** Imagen del diente {@code n} para el odontograma impreso (DIENTE_21 se guarda en la columna DIENTE_2). */
        public String odonto(int n) {
            return Odontograma.dataUri(n, v(n == 21 ? "DIENTE_2" : "DIENTE_" + n));
        }

        /** Casilla de dosis de COVID-19 (1, 2 o 3): el campo guarda el numero de dosis. */
        public String dosis(String var, int n) {
            String s = v(var).trim();
            return s.matches("\\d+") && Integer.parseInt(s) >= n ? "X" : "";
        }

        /** Valor de un campo o vacio si es relleno ("sin observaciones", 0...). */
        public String texto(String var) {
            String s = v(var).trim();
            return esRelleno(s) ? "" : s;
        }

        /** "X" si el valor del campo es la opcion (casillas del formato: ramo, tipo, dictamen). */
        public String marca(String var, String opcion) {
            return v(var).trim().equalsIgnoreCase(opcion) ? "X" : "";
        }

        public String getFirmaTrabajador() {
            return firmaTrabajador;
        }

        public String getAhora() {
            return ahora;
        }

        /** Mismo criterio que el PHP: strtolower + lista fija; empty("0") tambien es true en PHP. */
        static boolean esRelleno(String s) {
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

    private static final Map<String, String> CARAS = Map.of(
            "O", "oclusal", "V", "vestibular", "L", "lingual/palatino", "M", "mesial", "D", "distal");
    private static final java.util.regex.Pattern POR_CARA =
            java.util.regex.Pattern.compile("(?i)^([CAE]):([OVLMD](?:,[OVLMD])*)$");

    /**
     * Texto del impreso para un diente. Desde el FT-SO-04 rev. 05 (15-oct-2026) el examen inicial
     * guarda por caras en la misma columna: "C:O,M A:D" -> "Caries dental (oclusal, mesial);
     * AMALGAMA (distal)". Un codigo suelto (del PHP o "X"/"P") se traduce con DIENTES; lo que no
     * se reconoce se imprime tal cual. Vacio = "Diente sano".
     */
    static String describirDiente(String valor) {
        String t = valor == null ? "" : valor.trim();
        if (t.isEmpty()) {
            return "Diente sano";
        }
        List<String> partes = new java.util.ArrayList<>();
        for (String token : t.split("\\s+")) {
            java.util.regex.Matcher m = POR_CARA.matcher(token);
            if (m.matches()) {
                List<String> caras = new java.util.ArrayList<>();
                for (String c : m.group(2).toUpperCase(Locale.ROOT).split(",")) {
                    caras.add(CARAS.get(c));
                }
                partes.add(DIENTES.get(m.group(1).toUpperCase(Locale.ROOT)) + " (" + String.join(", ", caras) + ")");
            } else if ("X".equalsIgnoreCase(token)) {
                partes.add("Ausente");
            } else {
                partes.add(DIENTES.getOrDefault(token, token));
            }
        }
        return String.join("; ", partes);
    }

    /**
     * Encabezado de calidad de cada formato (filas 1-4 de cada Excel; version vigente en
     * docs/formatosnuevos/10202026, 20-oct-2026). Calidad pide que cada tipo de examen imprima SU encabezado.
     */
    public record Encabezado(String titulo, String implementacion, String revision, String clave,
                             String reemplaza, String motivo) {
    }

    private static final String VIGENCIA = "Actualización por vigencia";
    // 20-oct-2026: formatos 2026 del OneDrive de SM (docs/formatosnuevos/10202026). Los 7 cambian solo la
    // fecha de implementacion (14-sep -> 14-oct-2026); revision, reemplaza y motivo quedan igual.
    private static final String IMPLEMENTACION = "14 de octubre de 2026";
    private static final Encabezado FT_SO_04 = new Encabezado("EXAMEN MÉDICO INICIAL",
            IMPLEMENTACION, "05", "FT-SO-04", "04", VIGENCIA);

    /** Tipo de examen (codigo del tag tipoExamenInputO) -> formato. Sin formato propio: FT-SO-04. */
    // 6-8 (16-oct-2026): filas 1-4 de FT-SO-18/19/20; el motivo de 19 y 20 es el de su hoja 1
    static final Map<String, Encabezado> ENCABEZADOS = Map.of(
            "1", FT_SO_04,
            "2", new Encabezado("EXAMEN MÉDICO PERIÓDICO", IMPLEMENTACION, "06", "FT-SO-17", "05", VIGENCIA),
            "3", new Encabezado("EXAMEN MÉDICO CAMBIO DE PUESTO", IMPLEMENTACION, "06", "FT-SO-16", "05", VIGENCIA),
            "4", new Encabezado("EXAMEN MÉDICO POST INCAPACIDAD", IMPLEMENTACION, "06", "FT-SO-13", "05", VIGENCIA),
            "6", new Encabezado("EXAMEN MÉDICO EGRESO", IMPLEMENTACION, "06", "FT-SO-18", "05", VIGENCIA),
            "7", new Encabezado("EXAMEN MÉDICO INICIAL \"MANTENIMIENTO\"", IMPLEMENTACION, "06", "FT-SO-19", "05",
                    "Modificación del formato y actualización por vigencia"),
            "8", new Encabezado("EXAMEN MÉDICO PERIÓDICO \"MANTENIMIENTO\"", IMPLEMENTACION, "06", "FT-SO-20", "05",
                    "Modificación del formato y actualización por vigencia"));

    /** Clave del formato EMLE que se trabaja para el tipo (la misma que imprime el encabezado). */
    public static String claveFormato(String tipoExamen) {
        return encabezado(tipoExamen).clave();
    }

    static Encabezado encabezado(String tipoExamen) {
        return ENCABEZADOS.getOrDefault(tipoExamen == null ? "" : tipoExamen.trim(), FT_SO_04);
    }

    private static final Map<String, String> TIPO_EXAMEN = Map.of(
            "0", "No seleccionado", "1", "Admisión", "2", "Périodico", "3", "Cambio de puesto",
            "4", "Post incapacidad", "5", "Especial", "6", "Egreso", "7", "Inicial mantenimiento",
            "8", "Periódico mantenimiento");

    private final ExamenClient client;
    private final NssSearchService nssSearchService;
    private final MedTagRepository tags;
    private final List<Campo> campos;

    private final com.onest.app.integration.ocr.SolicitudExamenRepository solicitudes;
    private final com.onest.app.catalog.examen.repository.SociedadEmpleadoRepository sociedades;
    private final FichaBiometricoRepository fichaBiometrico;
    private final com.onest.app.catalog.examen.repository.ExamenHistoricoRepository historico;

    public ExamenDocumentoService(ExamenClient client, NssSearchService nssSearchService, MedTagRepository tags,
                                  com.onest.app.integration.ocr.SolicitudExamenRepository solicitudes,
                                  com.onest.app.catalog.examen.repository.SociedadEmpleadoRepository sociedades,
                                  FichaBiometricoRepository fichaBiometrico,
                                  com.onest.app.catalog.examen.repository.ExamenHistoricoRepository historico) {
        this.client = client;
        this.nssSearchService = nssSearchService;
        this.tags = tags;
        this.solicitudes = solicitudes;
        this.sociedades = sociedades;
        this.fichaBiometrico = fichaBiometrico;
        this.historico = historico;
        this.campos = cargarCampos();
    }

    /**
     * AGENCIA del FT-SO-04 rev. 05: no se captura (decision 15-oct-2026), es por empleado.
     * Agencia de la solicitud de OCR > BIO_EMPLEADO.SOCIEDAD (GLI, ORLAMEX...) > vacio.
     * Aqui sin el predeterminado del formato; el impreso SI lo aplica al final (16-oct-2026: el
     * usuario decidio apegarse al layout, antes salia vacio): empleado > elegida > GLI.
     * NO es la empresa del WS (EmpleadoDto.nombreEmpresa: "Onest Mexico" para todos).
     * La usan la pantalla del examen inicial y el impreso, para que digan lo mismo.
     */
    public String agencia(String nss, String agenciaSolicitud) {
        if (agenciaSolicitud != null && !agenciaSolicitud.isBlank()) {
            return agenciaSolicitud.trim();
        }
        return sociedades.sociedad(nss).orElse("");
    }

    /**
     * Cuando la base no trae agencia, el medico la elige en el examen inicial: predeterminado
     * del FT-SO-04 o "Otro" especificando (el texto del formato). Se guarda en este tag.
     */
    public static final String TAG_AGENCIA = "AGENCIAEXAMEN";

    /** Telefono del trabajador actualizado en el examen: solo Portal Salud, no toca el biometrico. */
    public static final String TAG_TELEFONO = "TELSALUD";

    /** Telefono capturado en Portal Salud, o vacio si no hay. */
    public String telefonoSalud(String nss) {
        try {
            return limpio(tags.latestByNssAndTypePrefix(nss.trim(), TAG_TELEFONO).get(TAG_TELEFONO));
        } catch (RuntimeException ex) {
            log.warn("[examen-doc] sin telefono de Portal Salud para {}: {}", nss, ex.getMessage());
            return "";
        }
    }
    public static final String AGENCIA_PREDETERMINADA = "Grupo Logístico Integral SAPI de CV";

    /** Agencia de la base y, si no hay, la que se eligio en el examen (tag); vacio si ninguna. */
    public String agenciaConCaptura(String nss, String agenciaSolicitud) {
        String deBase = agencia(nss, agenciaSolicitud);
        return deBase.isEmpty() ? agenciaCapturada(nss) : deBase;
    }

    /**
     * Agencia de la base (solicitud de OCR > BIO_EMPLEADO.SOCIEDAD) sin pasar la solicitud: la usa el
     * modulo Examen medico (16-oct-2026), que tambien pide AGENCIA en los FT-SO-13/16/17/18/19/20.
     */
    public String agenciaDeBase(String nss) {
        String agenciaSolicitud = null;
        try {
            agenciaSolicitud = solicitudes.ultimaPorNss(nss).map(com.onest.app.integration.ocr.SolicitudExamen::agencia).orElse(null);
        } catch (RuntimeException ex) {
            // sin tabla de solicitudes: se queda con el biometrico
        }
        return agencia(nss, agenciaSolicitud);
    }

    /**
     * Fecha (dd/MM/yyyy) del ultimo examen guardado del NSS, segun el historico; vacio si no hay. Es la
     * FECHA_EXAMEN del impreso y parte de la clave del documento para las firmas (21-oct-2026).
     */
    public String fechaExamen(String nss) {
        try {
            return historico.ultimo(nss)
                    .map(r -> r.fechaExamen().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).orElse("");
        } catch (RuntimeException ex) {
            return ""; // sin historico: la fecha queda vacia
        }
    }

    /** Ultima agencia elegida en un examen para el NSS (tag), o vacio. */
    public String agenciaCapturada(String nss) {
        try {
            return limpio(tags.latestByNssAndTypePrefix(nss, TAG_AGENCIA).get(TAG_AGENCIA));
        } catch (RuntimeException ex) {
            return "";
        }
    }

    public Documento armar(String nss) {
        return armar(nss, null);
    }

    /** Con tipoForzado (1-8) se arma como si el examen fuera de ese tipo: vista previa del impreso. */
    public Documento armar(String nss, String tipoForzado) {
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

        // Correcciones EMNI (6-nov-2026): telefono de Portal Salud, observaciones de APP y fecha de Td
        String telefonoSalud = limpio(tagValues.get(TAG_TELEFONO));
        if (!telefonoSalud.isEmpty()) {
            v.put("telefono", telefonoSalud);
        }
        for (Map.Entry<String, String> e : ExamenService.OBS_TAG_APP.entrySet()) {
            v.put(e.getKey() + "_OBS", limpio(tagValues.get(e.getValue())));
        }
        v.put("fechaTD", limpio(tagValues.get("fechaTD")));

        // --- valores que el PHP imprimia crudos y se decidio humanizar (13-sep-2026)
        v.put("sexo", switch (limpio(v.get("sexo"))) {
            case "1" -> "Masculino";
            case "2" -> "Femenino";
            default -> limpio(v.get("sexo"));
        });
        // 4-nov-2026: el estado civil ya llega con etiqueta (EstadoCivil, en el cliente del WS). Antes aqui se
        // borraba el "0", pero el 0 es Soltero (lo confirman las vistas del biometrico), no "sin dato".
        if (limpio(v.get("puesto")).toLowerCase(Locale.ROOT).contains("no existe")) {
            v.put("puesto", "");
        }

        // --- lo que el PHP hace con codigo, no con asignaciones
        // Edad: el PHP la sacaba del RFC. Un candidato de OCR puede no traer RFC pero si fecha de
        // nacimiento (EmpleadoDto.edad ya viene calculada desde la solicitud).
        String edad = edadDesdeRfc(v.get("rfc"), emp);
        if (edad.isEmpty() && emp != null && emp.edad() != null) {
            edad = String.valueOf(emp.edad());
        }
        v.put("edad", edad);
        // Impreso por tipo (16-oct-2026): el codigo crudo decide que secciones salen
        // (VisibilidadTipo) y los bloques propios de cada formato viven en tags EXM_*
        if (tipoForzado != null) {
            v.put("tipoExamenInputO", tipoForzado);
        }
        v.put("TIPO_CODIGO", limpio(v.get("tipoExamenInputO")));
        for (Map.Entry<String, String> e : tagValues.entrySet()) {
            if (e.getKey() != null && e.getKey().startsWith("EXM_")) {
                v.put(e.getKey(), e.getValue() == null ? "" : e.getValue().trim());
            }
        }
        // Encabezado de calidad segun el tipo de examen (codigo crudo, antes de pasarlo a texto)
        Encabezado enc = encabezado(limpio(v.get("tipoExamenInputO")));
        v.put("ENC_TITULO", enc.titulo());
        v.put("ENC_IMPLEMENTACION", enc.implementacion());
        v.put("ENC_REVISION", enc.revision());
        v.put("ENC_CLAVE", enc.clave());
        v.put("ENC_REEMPLAZA", enc.reemplaza());
        v.put("ENC_MOTIVO", enc.motivo());
        v.put("tipoExamenInputO", TIPO_EXAMEN.getOrDefault(limpio(v.get("tipoExamenInputO")), limpio(v.get("tipoExamenInputO"))));
        // Dictamen: el PHP lo guardaba como codigo 1-4 en OBSERVACIONES; el portal (ambos modulos)
        // lo guarda en las 4 banderas APTO/NO_APTO/APTO_CONDICIONADO/APTO_RESTRINGIDO y usa
        // OBSERVACIONES como texto libre. Se imprimen por separado: DICTAMEN (banderas, con
        // respaldo al codigo viejo para lo migrado) y las observaciones reales (3-oct-2026).
        String obsRes = limpio(data.get("SERV_MED_RESULTADO_EXAMEN.OBSERVACIONES"));
        String dictamen = "";
        if ("1".equals(limpio(data.get("SERV_MED_RESULTADO_EXAMEN.APTO")))) {
            dictamen = "Apto";
        } else if ("1".equals(limpio(data.get("SERV_MED_RESULTADO_EXAMEN.NO_APTO")))) {
            dictamen = "No apto";
        } else if ("1".equals(limpio(data.get("SERV_MED_RESULTADO_EXAMEN.APTO_CONDICIONADO")))) {
            dictamen = "Apto condicionado";
        } else if ("1".equals(limpio(data.get("SERV_MED_RESULTADO_EXAMEN.APTO_RESTRINGIDO")))) {
            dictamen = "Apto restringido";
        }
        String dictamenCodigo = switch (obsRes) {
            case "4", "no_apto" -> "No apto";
            case "1", "apto" -> "Apto";
            case "2", "apto_condicionado" -> "Apto condicionado";
            case "3", "apto_restringido" -> "Apto restringido";
            default -> "";
        };
        v.put("DICTAMEN", dictamen.isEmpty() ? dictamenCodigo : dictamen);
        // observaciones reales: solo si no son el codigo viejo ni relleno del WS
        v.put("OBSERVACIONESRESEXAM1", dictamenCodigo.isEmpty() && !Documento.esRelleno(obsRes) ? obsRes : "");
        v.put("ROMBERGEXFIS", esCero(tagValues.get("ROMBERGEXFIS")) ? "Negativo" : "Positivo");
        // Agudeza CON lentes (CL) del FT-SO-04 (15-oct-2026): el tag guarda la medida ("20/20",
        // "SDPA"...). El PHP la convertia a Si/No (0 -> No, cualquier otra cosa -> Si) y se perdia
        // la medida; aqui se imprime tal cual.
        for (String lente : List.of("LENTEOJODERECHOEXP", "LENTEOJOIZQUIERDOEXP")) {
            v.put(lente, limpio(tagValues.get(lente)));
        }
        com.onest.app.integration.ocr.SolicitudExamen solicitud = null;
        try {
            solicitud = solicitudes.ultimaPorNss(nssLimpio).orElse(null);
        } catch (RuntimeException ex) {
            // sin tabla de solicitudes: se queda con el biometrico o el predeterminado
        }
        String agenciaSolicitud = solicitud == null ? null : solicitud.agencia();
        String agenciaBase = agencia(nssLimpio, agenciaSolicitud);
        // Los 7 formatos EMLE: "Texto predeterminado Grupo Logistico Integral SAPI de CV" (16-oct-2026,
        // decision del usuario: se apega al layout). Orden: la del empleado > la elegida > GLI.
        String agenciaElegida = limpio(tagValues.get(TAG_AGENCIA));
        v.put("AGENCIA", !agenciaBase.isEmpty() ? agenciaBase
                : (!agenciaElegida.isEmpty() ? agenciaElegida : AGENCIA_PREDETERMINADA));
        // Ficha del FT-SO-17 (15-oct-2026): escolaridad y profesion del candidato de la solicitud de
        // OCR; del empleado, escolaridad y departamento del biometrico. Fecha de realizacion = la del
        // ultimo examen registrado en el historico del portal.
        FichaBiometricoRepository.Extra extra = fichaBiometrico.extra(nssLimpio);
        String escolaridadOcr = solicitud == null ? "" : limpio(solicitud.escolaridad());
        v.put("ESCOLARIDAD", escolaridadOcr.isEmpty() ? extra.escolaridad() : escolaridadOcr);
        v.put("PROFESION", solicitud == null ? "" : limpio(solicitud.profesionOficio()));
        v.put("DEPARTAMENTO", extra.departamento());
        v.put("FECHA_EXAMEN", fechaExamen(nssLimpio));
        // --- rev. 04 del FT-SO-04 (sep-2024): cintura y cadera no existian en el PHP ni en el
        // JSON de campos; se capturan en Datos generales del examen (SERV_MED_TAG)
        v.put("CINTURA", limpio(tagValues.get(DatosGeneralesExamenService.CINTURA)));
        v.put("CADERA", limpio(tagValues.get(DatosGeneralesExamenService.CADERA)));
        // el PHP imprime el "0" del WS tal cual cuando no hay fecha; se conserva
        String firma = data.get("SERV_MED_RESULTADO_EXAMEN.FIRMA_DIGITAL");
        String firmaTrabajador = firma != null && firma.startsWith("data:image") ? firma : null;

        // "Impreso:" (29-oct-2026): era "dd/MM/yy, HH:mm a" con la zona del servidor -> en la tarde salia
        // "15:56 p.m." y, si el Java del servidor esta en UTC, 6 horas adelantado. Se calcula aqui (servidor, no
        // la PC que imprime), en hora de Mexico y 24 horas, igual que la fecha del consentimiento.
        return new Documento(v, firmaTrabajador,
                java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Mexico_City"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
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
