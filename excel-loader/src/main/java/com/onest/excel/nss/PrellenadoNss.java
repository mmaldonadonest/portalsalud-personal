package com.onest.excel.nss;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.onest.excel.normalizacion.Normalizador;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <b>SOLO PARA PRUEBAS INTERNAS</b> (modo nss-generar-prueba). Lee la carga vieja por nombre
 * (SERV_MED_BITACORA_*) y genera los 5 layouts nuevos llenos, con un NSS deducido por nombre contra
 * el biometrico. SOLO LECTURA de la base.
 *
 * <p><b>Estos archivos NO se entregan al servicio medico ni son la foto inicial.</b> Decision del
 * usuario (10-oct-2026): el servicio medico es la UNICA fuente de verdad; la foto inicial ene-jun la
 * capturan ellos en archivos limpios. Esto sirve para probar la carga, las graficas y el expediente
 * con volumen y datos reales antes de recibir sus archivos. El NSS por nombre puede equivocarse: por
 * eso nada de lo generado aqui debe quedar como dato oficial (se borra por lote despues de probar).
 *
 * <p>Mapeo acordado con la salida de docs/script-prod/excel-nss-07-atributos-carga-vieja.sql (10-oct).
 */
public class PrellenadoNss {

    private static final Logger log = LoggerFactory.getLogger(PrellenadoNss.class);
    private static final List<String> AYUDA = List.of("AYUDA NOMBRE EN EXCEL", "AYUDA NSS CANDIDATOS",
            "AYUDA NOTAS", "AYUDA ORIGEN");

    /** Un evento viejo con sus atributos (nombre -> valor, numero o fecha). */
    record Evento(long id, LocalDate fecha, String predio, String area, String cuenta, String puesto,
                  String nombre, String nombreNorm, String genero, String estatus, String origen,
                  String hoja, Map<String, Object[]> atr) {
        String texto(String n) {
            Object[] a = atr.get(n);
            return a == null || a[0] == null ? null : a[0].toString().trim();
        }

        BigDecimal numero(String n) {
            Object[] a = atr.get(n);
            if (a == null) {
                return null;
            }
            if (a[1] != null) {
                return (BigDecimal) a[1];
            }
            try {
                return a[0] == null ? null : ValidadorNss.numero(a[0].toString());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        LocalDate fechaAtr(String n) {
            Object[] a = atr.get(n);
            if (a == null) {
                return null;
            }
            // 1900 = celda con formula sobre una fecha vacia en el Excel viejo (dia 0 de Excel): no es
            // un dato, se deja vacia en vez de mandar al layout una fecha que la carga rechaza
            if (a[2] != null) {
                LocalDate d = (LocalDate) a[2];
                return d.getYear() < 2000 ? null : d;
            }
            try {
                return a[0] == null ? null : ValidadorNss.fecha(a[0].toString());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /** Resultado del prellenado de un layout, para el reporte. */
    public record Resumen(LayoutNss layout, Path archivo, int renglones, int nssUnico, int nssVarios, int nssNinguno) {
    }

    private record Candidato(String nss, String nombre, String paterno, String materno, String nombres) {
    }

    // Dos origenes posibles, uno u otro (12-oct-2026):
    //   - jdbc: la carga vieja y el biometrico en la base (modo nss-generar-prueba, lotes 2 a 6).
    //   - memoria + csvBiometrico: lo que el lector saco de los Excel ORIGINALES sin tocar la base, y
    //     el NSS de un CSV del biometrico que exporta el usuario (modo nss-generar-excel).
    private final JdbcTemplate jdbc;
    private final com.onest.excel.persistencia.BitacoraMemoria memoria;
    private final Path csvBiometrico;
    private final String etiqueta;
    private final Map<String, List<Candidato>> porNombre = new HashMap<>();
    private final Map<String, Candidato> porNss = new HashMap<>();

    public PrellenadoNss(JdbcTemplate jdbc) {
        this(jdbc, null, null, "PRUEBA desde carga vieja");
    }

    private PrellenadoNss(JdbcTemplate jdbc, com.onest.excel.persistencia.BitacoraMemoria memoria,
                          Path csvBiometrico, String etiqueta) {
        this.jdbc = jdbc;
        this.memoria = memoria;
        this.csvBiometrico = csvBiometrico;
        this.etiqueta = etiqueta;
    }

    public static PrellenadoNss desdeMemoria(com.onest.excel.persistencia.BitacoraMemoria memoria, Path csvBiometrico) {
        return new PrellenadoNss(null, memoria, csvBiometrico, "PRUEBA desde Excel original");
    }

    public List<Resumen> generar(Path salida, LocalDate desde, LocalDate hasta) throws Exception {
        Files.createDirectories(salida);
        cargarIndiceBiometrico();
        List<Resumen> res = new ArrayList<>();
        res.add(escribir(salida, LayoutNss.ATENCIONES, "ATENCION", desde, hasta, this::filaAtencion));
        res.add(escribir(salida, LayoutNss.EXAMEN_INGRESO, "EXAMEN_INGRESO", desde, hasta, this::filaIngreso));
        res.add(escribir(salida, LayoutNss.EXAMEN_PERIODICO, "EXAMEN_PERIODICO", desde, hasta, this::filaPeriodico));
        res.add(escribir(salida, LayoutNss.INCAPACIDADES, "INCAPACIDAD", desde, hasta, this::filaIncapacidad));
        res.add(escribir(salida, LayoutNss.ACCIDENTES, "ACCIDENTE", desde, hasta, this::filaAccidente));
        return res;
    }

    // ------------------------------------------------------------------------------------------
    //  NSS sugerido
    // ------------------------------------------------------------------------------------------

    /**
     * Todo el biometrico en memoria, indexado por el nombre normalizado igual que NOMBRE_NORM de la
     * carga vieja, en los dos ordenes (apellidos-nombre y nombre-apellidos). Sin los EMP_STATUS 99,
     * que son registros viejos de prueba.
     */
    private void cargarIndiceBiometrico() throws java.io.IOException {
        if (csvBiometrico != null) {
            cargarIndiceDesdeCsv();
            return;
        }
        log.info("[prellenar] leyendo el biometrico para sugerir NSS...");
        jdbc.setFetchSize(2000);
        jdbc.query("SELECT EMP_NSS, EMP_NOMBRE, EMP_APELLIDO, EMP_APELLIDO2, EMP_STATUS FROM BIO_EMPLEADO", rs -> {
            agregarAlIndice(rs.getObject("EMP_NSS"), rs.getString("EMP_NOMBRE"), rs.getString("EMP_APELLIDO"),
                    rs.getString("EMP_APELLIDO2"), rs.getObject("EMP_STATUS"));
        });
        log.info("[prellenar] {} NSS en el indice", porNss.size());
    }

    /**
     * El mismo indice, desde el CSV que exporta SQL Developer (excel-nss-10-biometrico-nombres.sql):
     * encabezado EMP_NSS,EMP_NOMBRE,EMP_APELLIDO,EMP_APELLIDO2,EMP_STATUS, comas y comillas dobles.
     * Las columnas se ubican por nombre, por si el export las reordena.
     */
    private void cargarIndiceDesdeCsv() throws java.io.IOException {
        log.info("[prellenar] leyendo el biometrico de {}", csvBiometrico);
        // SQL Developer escribe el SPOOL en la codificacion de Windows (Cp1252), no en UTF-8: con UTF-8
        // estricto la primera Ñ truena. Se intenta UTF-8 y, si no es, Cp1252.
        List<String> lineas;
        try {
            lineas = Files.readAllLines(csvBiometrico, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException ex) {
            lineas = Files.readAllLines(csvBiometrico, java.nio.charset.Charset.forName("windows-1252"));
        }
        Map<String, Integer> col = new HashMap<>();
        int i = 0;
        for (; i < lineas.size() && col.isEmpty(); i++) {
            List<String> enc = camposCsv(lineas.get(i).replace("﻿", ""));
            if (enc.stream().anyMatch(h -> h.trim().equalsIgnoreCase("EMP_NSS"))) {
                for (int k = 0; k < enc.size(); k++) {
                    col.put(enc.get(k).trim().toUpperCase(), k);
                }
            }
        }
        if (!col.containsKey("EMP_NSS")) {
            throw new IllegalStateException("El CSV no trae el encabezado EMP_NSS: " + csvBiometrico);
        }
        for (; i < lineas.size(); i++) {
            List<String> f = camposCsv(lineas.get(i));
            if (f.size() < col.size()) {
                continue;
            }
            agregarAlIndice(f.get(col.get("EMP_NSS")), f.get(col.get("EMP_NOMBRE")), f.get(col.get("EMP_APELLIDO")),
                    f.get(col.get("EMP_APELLIDO2")), col.containsKey("EMP_STATUS") ? f.get(col.get("EMP_STATUS")) : null);
        }
        log.info("[prellenar] {} NSS en el indice (CSV)", porNss.size());
    }

    /** Separa un renglon CSV con comillas dobles (las comillas dobladas son una comilla literal). */
    static List<String> camposCsv(String linea) {
        List<String> r = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean comillas = false;
        for (int k = 0; k < linea.length(); k++) {
            char ch = linea.charAt(k);
            if (comillas) {
                if (ch == '"' && k + 1 < linea.length() && linea.charAt(k + 1) == '"') {
                    sb.append('"');
                    k++;
                } else if (ch == '"') {
                    comillas = false;
                } else {
                    sb.append(ch);
                }
            } else if (ch == '"') {
                comillas = true;
            } else if (ch == ',') {
                r.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        r.add(sb.toString());
        return r;
    }

    /** Sin los EMP_STATUS 99 (registros viejos de prueba); por nombre en los dos ordenes. */
    private void agregarAlIndice(Object nssCrudo, String nombre, String paterno, String materno, Object status) {
        if (status != null && "99".equals(String.valueOf(status).trim())) {
            return;
        }
        String nss;
        try {
            nss = ValidadorNss.nss(String.valueOf(nssCrudo).trim());
        } catch (IllegalArgumentException ex) {
            return;
        }
        String n = limpio(nombre);
        String p = limpio(paterno);
        String m = limpio(materno);
        Candidato c = new Candidato(nss, (p + " " + m + " " + n).trim().replaceAll("\\s+", " "), p, m, n);
        porNss.putIfAbsent(nss, c);
        for (String clave : List.of(p + " " + m + " " + n, n + " " + p + " " + m)) {
            String k = Normalizador.nombrePersona(clave);
            if (k != null && !k.isBlank()) {
                List<Candidato> l = porNombre.computeIfAbsent(k, x -> new ArrayList<>());
                if (l.stream().noneMatch(x -> x.nss().equals(nss))) {
                    l.add(c);
                }
            }
        }
    }

    private static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    private List<Candidato> candidatos(Evento e) {
        String k = e.nombreNorm() != null ? e.nombreNorm() : Normalizador.nombrePersona(e.nombre());
        return k == null ? List.of() : porNombre.getOrDefault(k, List.of());
    }

    // ------------------------------------------------------------------------------------------
    //  Lectura de la carga vieja y escritura del layout
    // ------------------------------------------------------------------------------------------

    /** Una fila del layout: valores en el orden de los campos + notas para el servicio medico. */
    private record Fila(List<Object> valores, List<String> notas) {
    }

    private interface Mapeo {
        Fila fila(Evento e, String nssSugerido, List<Candidato> candidatos);
    }

    private Resumen escribir(Path salida, LayoutNss layout, String familia, LocalDate desde, LocalDate hasta,
                             Mapeo mapeo) throws Exception {
        List<Evento> eventos = leer(familia, desde, hasta);
        Path archivo = salida.resolve(numero(layout) + " " + layout.hoja() + " " + etiqueta + ".xlsx");
        int unico = 0, varios = 0, ninguno = 0;
        // XSSF y no SXSSF (12-oct-2026): el modo streaming escribe un temporal en %TEMP%\poifiles y en
        // esta maquina no lo pudo borrar (lo retiene el antivirus) y truena al final. Son a lo mas
        // unos miles de renglones: caben en memoria sin problema.
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            Sheet hoja = wb.createSheet(layout.hoja());
            CellStyle enc = wb.createCellStyle();
            Font negrita = wb.createFont();
            negrita.setBold(true);
            enc.setFont(negrita);
            CellStyle encAyuda = wb.createCellStyle();
            encAyuda.cloneStyleFrom(enc);
            encAyuda.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            encAyuda.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle fecha = wb.createCellStyle();
            fecha.setDataFormat(wb.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle textoFmt = wb.createCellStyle();
            textoFmt.setDataFormat(wb.createDataFormat().getFormat("@"));

            Row r0 = hoja.createRow(0);
            int c = 0;
            for (CampoNss campo : layout.campos()) {
                Cell cell = r0.createCell(c++);
                cell.setCellValue(campo.encabezado());
                cell.setCellStyle(enc);
            }
            for (String a : AYUDA) {
                Cell cell = r0.createCell(c++);
                cell.setCellValue(a);
                cell.setCellStyle(encAyuda);
            }

            // Consumibles no lleva NSS: ni se busca por nombre ni se cuenta como "ninguno"
            boolean conNss = layout.campos().stream().anyMatch(x -> "NSS".equals(x.columna()));
            int fila = 1;
            for (Evento e : eventos) {
                List<Candidato> cand = conNss ? candidatos(e) : List.of();
                String sugerido = cand.size() == 1 ? cand.get(0).nss() : null;
                if (!conNss) {
                    // sin conteo de NSS
                } else if (cand.size() == 1) {
                    unico++;
                } else if (cand.isEmpty()) {
                    ninguno++;
                } else {
                    varios++;
                }
                Fila f = mapeo.fila(e, sugerido, cand);
                Row r = hoja.createRow(fila++);
                for (int k = 0; k < f.valores().size(); k++) {
                    poner(r.createCell(k), f.valores().get(k), fecha, textoFmt);
                }
                int base = layout.campos().size();
                r.createCell(base).setCellValue(nvl(e.nombre()));
                r.createCell(base + 1).setCellValue(textoCandidatos(cand));
                List<String> notas = new ArrayList<>(f.notas());
                if (cand.size() > 1) {
                    notas.add(0, "NSS: " + cand.size() + " personas con este nombre, elegir");
                } else if (cand.isEmpty() && conNss) {
                    notas.add(0, "NSS: no se encontro el nombre en el biometrico, capturar");
                }
                r.createCell(base + 2).setCellValue(String.join(" | ", notas));
                r.createCell(base + 3).setCellValue(nvl(e.origen()) + " | " + nvl(e.hoja()));
            }
            try (OutputStream out = Files.newOutputStream(archivo)) {
                wb.write(out);
            }
        }
        log.info("[prellenar] {}: {} renglones -> {}", layout.hoja(), eventos.size(), archivo.getFileName());
        return new Resumen(layout, archivo, eventos.size(), unico, varios, ninguno);
    }

    private static int numero(LayoutNss l) {
        return switch (l) {
            case ATENCIONES -> 1;
            case EXAMEN_INGRESO -> 2;
            case EXAMEN_PERIODICO -> 3;
            case INCAPACIDADES -> 5;
            case ACCIDENTES -> 6;
            // El prellenado de prueba no genera estos dos; el numero solo nombra el archivo
            case EXAMEN_POS_INCAP -> 4;
            case ANTIDOPING -> 7;
            case MATERNIDAD -> 8;
            case MEDICAMENTOS, MATERIAL_FIJO -> 9;
        };
    }

    private static void poner(Cell cell, Object v, CellStyle fecha, CellStyle texto) {
        if (v == null) {
            return;
        }
        if (v instanceof LocalDate d) {
            cell.setCellValue(d);
            cell.setCellStyle(fecha);
        } else if (v instanceof BigDecimal n) {
            cell.setCellValue(n.doubleValue());
        } else if (v instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(v.toString());
            cell.setCellStyle(texto);   // texto: el NSS no pierde ceros ni se vuelve 3.0E10
        }
    }

    private static String textoCandidatos(List<Candidato> cand) {
        if (cand.size() <= 1) {
            return cand.isEmpty() ? "" : cand.get(0).nss() + " " + cand.get(0).nombre();
        }
        StringBuilder sb = new StringBuilder();
        cand.stream().limit(5).forEach(x -> sb.append(sb.length() == 0 ? "" : " / ").append(x.nss()).append(' ')
                .append(x.nombre()));
        if (cand.size() > 5) {
            sb.append(" / ... ").append(cand.size() - 5).append(" mas");
        }
        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /** Eventos de la familia en el periodo (y los que no tienen fecha) con todos sus atributos. */
    private List<Evento> leer(String familia, LocalDate desde, LocalDate hasta) {
        if (memoria != null) {
            return MEDICAMENTOS.equals(familia) ? medicamentosDeMemoria(memoria, desde, hasta)
                    : deMemoria(memoria, familia, desde, hasta);
        }
        String filtro = "e.FAMILIA = ? AND ((e.FECHA >= ? AND e.FECHA <= ?) OR e.FECHA IS NULL)";
        Object[] p = {familia, Date.valueOf(desde), Date.valueOf(hasta)};
        Map<Long, Map<String, Object[]>> atributos = new HashMap<>();
        jdbc.query("SELECT a.EVENTO_ID, a.NOMBRE, a.VALOR, a.VALOR_NUM, a.VALOR_FECHA "
                + "FROM SERV_MED_BITACORA_ATRIBUTO a JOIN SERV_MED_BITACORA_EVENTO e ON e.ID = a.EVENTO_ID WHERE "
                + filtro, rs -> {
                    Timestamp f = rs.getTimestamp("VALOR_FECHA");
                    atributos.computeIfAbsent(rs.getLong("EVENTO_ID"), k -> new LinkedHashMap<>())
                            .put(rs.getString("NOMBRE"), new Object[]{rs.getString("VALOR"), rs.getBigDecimal("VALOR_NUM"),
                                    f == null ? null : f.toLocalDateTime().toLocalDate()});
                }, p);
        List<Evento> res = new ArrayList<>();
        jdbc.query("SELECT e.ID, e.FECHA, e.PREDIO, e.AREA, e.CUENTA, e.PUESTO, e.NOMBRE, e.NOMBRE_NORM, e.GENERO, "
                + "e.ESTATUS, e.ORIGEN_ARCHIVO, e.ORIGEN_HOJA FROM SERV_MED_BITACORA_EVENTO e WHERE " + filtro
                + " ORDER BY e.PREDIO, e.FECHA, e.ORIGEN_FILA", rs -> {
                    Date f = rs.getDate("FECHA");
                    long id = rs.getLong("ID");
                    String archivo = rs.getString("ORIGEN_ARCHIVO");
                    if (archivo != null && archivo.contains("\\")) {
                        archivo = archivo.substring(archivo.lastIndexOf('\\') + 1);
                    }
                    res.add(new Evento(id, f == null ? null : f.toLocalDate(), rs.getString("PREDIO"),
                            rs.getString("AREA"), rs.getString("CUENTA"), rs.getString("PUESTO"),
                            rs.getString("NOMBRE"), rs.getString("NOMBRE_NORM"), rs.getString("GENERO"),
                            rs.getString("ESTATUS"), archivo, rs.getString("ORIGEN_HOJA"),
                            atributos.getOrDefault(id, Map.of())));
                }, p);
        return res;
    }

    /**
     * Los mismos eventos que devolveria la base, pero de la memoria del lector. Mismo filtro: el
     * periodo, mas los que no tienen fecha (casos heredados que el reporte tiene que dejar ver).
     */
    static List<Evento> deMemoria(com.onest.excel.persistencia.BitacoraMemoria mem, String familia,
                                  LocalDate desde, LocalDate hasta) {
        List<Evento> res = new ArrayList<>();
        for (var ev : mem.eventos()) {
            var f = ev.fila();
            if (!familia.equals(f.familia())) {
                continue;
            }
            if (f.fecha() != null && (f.fecha().isBefore(desde) || f.fecha().isAfter(hasta))) {
                continue;
            }
            Map<String, Object[]> atr = new LinkedHashMap<>();
            for (var a : ev.atributos()) {
                // Una prueba de antidoping puede traer VARIAS sustancias: el lector deja un atributo
                // SUSTANCIA por cada marca. Se juntan con "|" en vez de quedarse con la ultima.
                Object[] previo = atr.get(a.nombre());
                if ("SUSTANCIA".equals(a.nombre()) && previo != null && previo[0] != null && a.valor() != null) {
                    atr.put(a.nombre(), new Object[]{previo[0] + "|" + a.valor(), null, null});
                    continue;
                }
                atr.put(a.nombre(), new Object[]{a.valor(),
                        a.valorNum() == null ? null : BigDecimal.valueOf(a.valorNum()), a.valorFecha()});
            }
            res.add(new Evento(ev.id(), f.fecha(), f.predio(), f.area(), f.cuenta(), f.puesto(), f.nombre(),
                    f.nombreNorm(), f.genero(), f.estatus(), f.origenArchivo(), f.origenHoja(), atr));
        }
        res.sort(java.util.Comparator.comparing((Evento e) -> nvl(e.predio()))
                .thenComparing(e -> e.fecha() == null ? LocalDate.MIN : e.fecha()));
        return res;
    }

    /**
     * Vuelca a CSV todo lo que el lector saco de los Excel originales en el periodo (y lo sin fecha),
     * un renglon por evento con su archivo, hoja y fila: para revisar a mano contra el archivo que
     * la prueba lea lo que dice el Excel, antes de mirar los layouts.
     */
    public static Path volcar(com.onest.excel.persistencia.BitacoraMemoria mem, Path dir, LocalDate desde,
                              LocalDate hasta) throws java.io.IOException {
        Path archivo = dir.resolve("0 LEIDO DE LOS EXCEL ORIGINALES.csv");
        StringBuilder sb = new StringBuilder("﻿FAMILIA;FECHA;PREDIO;ARCHIVO;HOJA;FILA;NOMBRE;AREA;CUENTA;PUESTO;ESTATUS;ATRIBUTOS\n");
        int n = 0;
        for (var ev : mem.eventos()) {
            var f = ev.fila();
            if (f.fecha() != null && (f.fecha().isBefore(desde) || f.fecha().isAfter(hasta))) {
                continue;
            }
            StringBuilder atr = new StringBuilder();
            for (var a : ev.atributos()) {
                Object v = a.valorFecha() != null ? a.valorFecha() : a.valorNum() != null ? a.valorNum() : a.valor();
                atr.append(atr.length() == 0 ? "" : " | ").append(a.nombre()).append('=').append(v);
            }
            sb.append(String.join(";", nvl(f.familia()), f.fecha() == null ? "" : f.fecha().toString(), nvl(f.predio()),
                    nvl(f.origenArchivo()), nvl(f.origenHoja()), String.valueOf(f.origenFila()), nvl(f.nombre()),
                    nvl(f.area()), nvl(f.cuenta()), nvl(f.puesto()), nvl(f.estatus()),
                    atr.toString().replace(";", ",").replace("\n", " "))).append('\n');
            n++;
        }
        StringBuilder met = new StringBuilder("﻿ORIGEN;PREDIO;ANIO;MES;CONCEPTO;SUBCONCEPTO;VALOR\n");
        for (var m : mem.metricas()) {
            met.append(String.join(";", nvl(m.origen()), nvl(m.predio()), String.valueOf(m.anio()),
                    String.valueOf(m.mes()), nvl(m.concepto()).replace(";", ","), nvl(m.subconcepto()),
                    String.valueOf(m.valor()))).append('\n');
        }
        Files.writeString(archivo, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("0 LEIDO DE LOS EXCEL ORIGINALES - metricas.csv"), met.toString(),
                java.nio.charset.StandardCharsets.UTF_8);
        log.info("[volcar] {} eventos y {} metricas", n, mem.metricas().size());
        return archivo;
    }

    /**
     * Los 9 layouts (prueba desde los Excel originales, 12-oct-2026): los 5 de la carga vieja mas pos
     * incapacidad, antidoping, maternidad y medicamentos. Material fijo no: el Excel de 2025 no lo trae
     * en una hoja que el lector separe.
     */
    public List<Resumen> generarTodos(Path salida, LocalDate desde, LocalDate hasta) throws Exception {
        List<Resumen> res = new ArrayList<>(generar(salida, desde, hasta));
        res.add(escribir(salida, LayoutNss.EXAMEN_POS_INCAP, "EXAMEN_POS_INCAP", desde, hasta, this::filaPosIncap));
        res.add(escribir(salida, LayoutNss.ANTIDOPING, "ANTIDOPING", desde, hasta, this::filaAntidoping));
        res.add(escribir(salida, LayoutNss.MATERNIDAD, "MATERNIDAD", desde, hasta, this::filaMaternidad));
        res.add(escribir(salida, LayoutNss.MEDICAMENTOS, MEDICAMENTOS, desde, hasta, this::filaMedicamento));
        return res;
    }

    /** Familia sintetica: las metricas de consumibles se vuelven "eventos" medicamento x mes para escribirlas. */
    private static final String MEDICAMENTOS = "MEDICAMENTOS_METRICA";

    /**
     * Medicamentos: la carga vieja los tiene como matriz medicamento x mes x semana (metricas). El
     * layout nuevo pide lote x mes; el Excel viejo no trae lote, asi que sale un renglon por
     * medicamento y mes, con el consumo de cada semana. Lo demas (lote, caducidad, existencias) no
     * estaba y queda vacio.
     */
    private static List<Evento> medicamentosDeMemoria(com.onest.excel.persistencia.BitacoraMemoria mem,
                                                      LocalDate desde, LocalDate hasta) {
        Map<String, Map<String, Object[]>> porClave = new LinkedHashMap<>();
        Map<String, Object[]> base = new HashMap<>();
        // El lector toma el predio de consumibles de la CARPETA; si no lo reconoce queda nulo. En la
        // prueba de un solo predio, el predio es el de los demas renglones leidos.
        String predioComun = mem.eventos().stream().map(x -> x.fila().predio()).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(x -> x, java.util.stream.Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        for (var m : mem.metricas()) {
            if (m.anio() == null || m.mes() == null) {
                continue;
            }
            LocalDate f = LocalDate.of(m.anio(), m.mes(), 1);
            if (f.isBefore(desde) || f.isAfter(hasta)) {
                continue;
            }
            String predio = m.predio() != null ? m.predio() : predioComun;
            String clave = predio + "|" + f + "|" + m.concepto();
            base.putIfAbsent(clave, new Object[]{f, predio, m.concepto()});
            porClave.computeIfAbsent(clave, k -> new LinkedHashMap<>())
                    .put(m.subconcepto(), new Object[]{null, m.valor() == null ? null : BigDecimal.valueOf(m.valor()), null});
        }
        List<Evento> res = new ArrayList<>();
        long id = 1;
        for (var e : porClave.entrySet()) {
            Object[] b = base.get(e.getKey());
            String categoria = mem.categoria((String) b[2]);
            if (categoria != null) {
                e.getValue().put("CATEGORIA", new Object[]{categoria, null, null});
            }
            res.add(new Evento(id++, (LocalDate) b[0], (String) b[1], null, null, null, (String) b[2], null, null,
                    null, null, "MEDICAMENTOS", e.getValue()));
        }
        return res;
    }

    private Fila filaPosIncap(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(e.fecha());
        v.add(cat("CAUSA_EXAMEN_POS_INCAP", e.texto("RAMO_INCAPACIDAD"), n, "CAUSA"));
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        v.add(dictamen(e.texto("DICTAMEN"), n));
        v.add(e.texto("DIAGNOSTICO"));
        v.add(e.texto("RESULTADOS_PENDIENTES"));
        v.add(e.texto("OBSERVACIONES"));
        return new Fila(v, n);
    }

    /**
     * Antidoping. El Excel de 2025 no trae TIPO DE PRUEBA (obligatorio en el layout): se deduce
     * ALCOHOLEMIA si trae porcentaje y ANTIDOPING si no, y queda la nota para que se vea.
     */
    private Fila filaAntidoping(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        v.add(e.fecha());
        BigDecimal alcohol = e.numero("ALCOHOLEMIA");
        String tipo = e.texto("TIPO_PRUEBA");
        if (tipo == null) {
            tipo = alcohol != null ? "ALCOHOLEMIA" : "ANTIDOPING";
            n.add("TIPO DE PRUEBA: no estaba en el Excel original, se dedujo " + tipo);
        }
        v.add(cat("TIPO_PRUEBA", tipo, n, "TIPO DE PRUEBA"));
        v.add(cat("RESULTADO_PRUEBA", e.texto("RESULTADO"), n, "RESULTADO"));
        // Una columna por sustancia: 1 en las que vienen marcadas en el Excel original (pueden ser
        // varias). En 2026 la hoja trae AMF dos veces: se marca una sola columna.
        java.util.Set<String> sust = new java.util.HashSet<>();
        String crudas = e.texto("SUSTANCIA");
        if (crudas != null) {
            for (String s : crudas.split("\\|")) {
                CatalogosNss.buscar("SUSTANCIA", s.trim()).ifPresentOrElse(sust::add,
                        () -> n.add("DOPING: '" + s.trim() + "' no es una de las 11 columnas de sustancia"));
            }
        }
        for (String s : List.of("AMF", "BARB", "BENZO", "COC", "LSD", "MET", "OPI", "PVC", "THC", "SOLVENTE", "OTRO")) {
            v.add(sust.contains(s) ? 1 : null);
        }
        v.add(alcohol);
        v.add(cat("CONCLUSION_ANTIDOPING", e.texto("CONCLUSION"), n, "CONCLUSION"));
        v.add(cat("STATUS_ANTIDOPING", e.estatus(), n, "STATUS"));
        v.add(null);
        return new Fila(v, n);
    }

    /**
     * Maternidad. FECHA DE CORTE = el mes reportado (dia 1). Los casos marcados "antes de 2025" no
     * tienen mes: quedan sin fecha de corte y la carga los rechaza (sirve de prueba de rechazo).
     */
    private Fila filaMaternidad(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        v.add(e.fecha());
        if (e.fecha() == null) {
            n.add("FECHA DE CORTE: caso sin mes reportado (antes de 2025), capturar");
        }
        v.add(cat("ESTATUS_LABORAL", e.estatus(), n, "STATUS LABORAL"));
        v.add(e.fechaAtr("FUM"));
        v.add(e.fechaAtr("FECHA_PROBABLE_PARTO"));
        v.add(e.fechaAtr("FECHA_PROBABLE_INCAP"));
        v.add(e.fechaAtr("FECHA_PROBABLE_REINICIO"));
        v.add(e.fechaAtr("REINICIO_REAL"));
        v.add(e.fechaAtr("INFORMACION_DE_INCAPACIDADES.INICIO"));
        v.add(e.fechaAtr("INFORMACION_DE_INCAPACIDADES.TERMINO"));
        v.add(e.texto("INFORMACION_DE_INCAPACIDADES.FOLIO"));
        v.add(e.fechaAtr("PERIODO_DE_LACTANCIA.INICIA"));
        v.add(e.fechaAtr("PERIODO_DE_LACTANCIA.TERMINA"));
        v.add(e.fechaAtr("PERIODO_DE_LACTANCIA.REANUDA_ACTIVIDADES_EN_HORARIO_NORMAL"));
        // Los CORREO DE AVISO a veces traen "X" o "BAJA" en lugar de fecha: esos quedan vacios
        v.add(e.fechaAtr("CORREO_DE_AVISO.EMBARAZO"));
        v.add(e.fechaAtr("CORREO_DE_AVISO.LACTANCIA"));
        v.add(e.fechaAtr("CORREO_DE_AVISO.TERMINO_DE_LACTANCIA"));
        return new Fila(v, n);
    }

    private Fila filaMedicamento(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(e.fecha() == null ? null : ValidadorNss.MESES.get(e.fecha().getMonthValue() - 1));
        v.add(e.fecha() == null ? null : e.fecha().getYear());
        // CATEGORIA: del renglon-titulo de su grupo en la matriz (VIA ORAL, INYECTABLE...)
        v.add(cat("CATEGORIA_MEDICAMENTO", e.texto("CATEGORIA"), n, "CATEGORIA"));
        v.add(e.nombre());                   // DESCRIPCION = el medicamento
        v.add(null);                         // LOTE, FECHA DE INGRESO, FECHA DE CADUCIDAD: no estaban
        v.add(null);
        v.add(null);
        v.add(null);                         // CANTIDAD MES ANTERIOR, RECIBIDO EN EL MES
        v.add(null);
        for (int s = 1; s <= 5; s++) {
            v.add(semana(e, s));
        }
        v.add(e.numero("CONSUMO SEMANAL CONSUMO"));   // el total del mes que trae la matriz
        v.add(null);                         // CONSUMO ACUMULADO, RESTANTE, EXISTENCIA: no estaban
        v.add(null);
        v.add(null);
        n.add("LOTE/CADUCIDAD/EXISTENCIAS: la matriz vieja no los trae");
        return new Fila(v, n);
    }

    /** "CONSUMO SEMANAL 1°" de la matriz vieja (el simbolo de grado puede variar). */
    private static BigDecimal semana(Evento e, int s) {
        return e.atr().entrySet().stream()
                .filter(x -> x.getKey() != null && x.getKey().matches("CONSUMO SEMANAL " + s + "\\D*"))
                .map(x -> (BigDecimal) x.getValue()[1]).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------------------------------
    //  Mapeo por layout (orden = LayoutNss.campos())
    // ------------------------------------------------------------------------------------------

    /** Valor de catalogo en su forma oficial; si no esta en el catalogo, se deja tal cual (la carga lo marcara). */
    private static String cat(String catalogo, String v, List<String> notas, String columna) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return CatalogosNss.buscar(catalogo, v).orElseGet(() -> {
            notas.add(columna + ": '" + v + "' no esta en el catalogo, corregir");
            return v;
        });
    }

    /** "APTO CONDICIONADO (45)" -> APTO_CONDICIONADO. */
    private static String dictamen(String v, List<String> notas) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String e = v.toUpperCase();
        if (e.contains("(15)")) return "APTO";
        if (e.contains("(30)")) return "NO_APTO";
        if (e.contains("(45)")) return "APTO_CONDICIONADO";
        if (e.contains("(60)")) return "APTO_RESTRINGIDO";
        notas.add("STATUS: '" + v + "' sin codigo de dictamen, corregir");
        return v;
    }

    private Fila filaAtencion(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        v.add(e.fecha());
        v.add(cat("RAMO_ATENCIONES", e.texto("RAMO"), n, "RAMO"));
        v.add(cat("CAUSA_CONSULTA", e.texto("CAUSA"), n, "CAUSA"));
        v.add(cat("LESION_ME", e.texto("LESION_ME"), n, "CAUSAS MUSCULO ESQUELETICAS"));
        v.add(e.texto("DIAGNOSTICO"));
        v.add(e.texto("TRATAMIENTO"));
        return new Fila(v, n);
    }

    /**
     * Nuevo ingreso: nombre y apellidos van separados en el layout y la carga vieja los tiene juntos.
     * Si el NSS se encontro, se toman separados del biometrico; si no, se parte como en el Excel
     * ("PATERNO MATERNO NOMBRE(S)") y se marca para revisar (falla con apellidos compuestos).
     */
    private Fila filaIngreso(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        String nombre = null, paterno = null, materno = null;
        Candidato c = nss == null ? null : porNss.get(nss);
        if (c != null) {
            nombre = c.nombres();
            paterno = c.paterno();
            materno = c.materno();
        } else if (e.nombre() != null) {
            String[] t = e.nombre().trim().split("\\s+");
            if (t.length >= 3) {
                paterno = t[0];
                materno = t[1];
                nombre = String.join(" ", java.util.Arrays.copyOfRange(t, 2, t.length));
            } else {
                nombre = e.nombre().trim();
            }
            n.add("NOMBRE/APELLIDOS: partidos del nombre completo, revisar");
        }
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(nombre);
        v.add(paterno);
        v.add(materno);
        v.add(e.puesto());
        v.add(cat("GENERO", e.genero(), n, "GENERO"));
        v.add(null);                         // EDAD: la carga vieja solo tiene el rango
        v.add(e.cuenta());
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        v.add(e.fecha());
        v.add(cat("TIPO_EXAMEN_INGRESO", e.texto("TIPO_EXAMEN"), n, "TIPO DE EXAMEN"));
        v.add(dictamen(e.texto("DICTAMEN"), n));
        v.add(e.texto("DIAGNOSTICO"));
        v.add(e.texto("ESTADO_CIVIL"));
        v.add(e.texto("ESCOLARIDAD"));
        v.add(e.texto("RESULTADOS_PENDIENTES"));
        v.add(e.texto("OBSERVACIONES"));
        return new Fila(v, n);
    }

    private Fila filaPeriodico(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        v.add(e.fecha());
        v.add(dictamen(e.texto("DICTAMEN"), n));
        v.add(e.texto("DIAGNOSTICO"));
        v.add(e.texto("RESULTADOS_PENDIENTES"));
        v.add(e.texto("OBSERVACIONES"));
        return new Fila(v, n);
    }

    /**
     * Incapacidades: hoja del IMSS (atributos DETALLE_DE_INCAPACIDAD.*, COSTOS.*) o interna (hoja
     * "<predio> INT": DIAS_TOTAL, SALARIO, COSTO_TOTAL). En la del IMSS la FECHA del evento es la
     * FECHA DE PRIMERA INCAPACIDAD; en la interna, el inicio.
     */
    private Fila filaIncapacidad(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        boolean interna = e.hoja() != null && e.hoja().toUpperCase().contains("INT");
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        if (e.area() == null) {
            n.add("AREA: vacia en el Excel original, capturar");
        }
        String folio = e.texto("DETALLE_DE_INCAPACIDAD.FOLIO");
        v.add(folio == null ? null : folio.toUpperCase().replace(" ", ""));
        v.add(interna ? "INTERNA" : "IMSS");
        String ramo = cat("RAMO_INCAPACIDADES", e.texto("DETALLE_DE_INCAPACIDAD.RAMO_RT1_RT2_EG_MAT"), n, "RAMO");
        v.add(ramo);
        if (ramo == null) {
            n.add("RAMO: no estaba en el Excel original, capturar (EG, MAT, RT1, RT2)");
        }
        v.add(cat("TIPO_INCAP", e.texto("DETALLE_DE_INCAPACIDAD.TIPO_DE_INCAP"), n, "TIPO"));
        LocalDate inicio = interna ? e.fecha() : e.fechaAtr("DETALLE_DE_INCAPACIDAD.INICIO_DE_INCAPACIDAD");
        v.add(inicio);
        v.add(e.fechaAtr("DETALLE_DE_INCAPACIDAD.TERMINO_DE_INCAPACIDAD"));
        BigDecimal dias = interna ? e.numero("DIAS_TOTAL") : e.numero("DETALLE_DE_INCAPACIDAD.DIAS_DE_INCAPACIDAD");
        v.add(dias == null ? null : dias.stripTrailingZeros());
        v.add(interna ? null : e.fecha());
        v.add(interna ? e.numero("SALARIO") : e.numero("SDI"));
        v.add(e.numero("PAGO_60"));
        v.add(e.numero("COSTOS.ENF_GRAL"));
        v.add(e.numero("COSTOS.MATERNIDAD"));
        v.add(e.numero("COSTOS.LABORAL"));
        v.add(e.numero("COSTOS.TRAYECTO"));
        v.add(interna ? e.numero("COSTO_TOTAL") : e.numero("COSTOS.TOTAL"));
        v.add(e.texto("DIAGNOSTICO"));
        v.add(e.texto("CAUSA"));
        v.add(cat("SI_NO", e.texto("ALTA_OPERACION"), n, "ALTA INGRESO A LA OPERACION"));
        v.add(cat("SI_NO", e.texto("ST7"), n, "ST7"));
        return new Fila(v, n);
    }

    /** Las 7 causas con marca de la carga vieja (el nombre del atributo es la causa). */
    private static final Map<String, String> CAUSAS_RT = Map.of(
            "CAUSA_DE_RT.ACCID_VIAL", "ACCID VIAL", "CAUSA_DE_RT.AGRESION_ASALTO", "AGRESION / ASALTO",
            "CAUSA_DE_RT.CAIDA", "CAIDA", "CAUSA_DE_RT.GOLPE_DIRECTO_CON_O_POR_OBJETO", "GOLPE DIRECTO CON O POR OBJETO",
            "CAUSA_DE_RT.HERIDA", "HERIDA", "CAUSA_DE_RT.LESION_ERGONOMICA", "LESION ERGONOMICA",
            "CAUSA_DE_RT.OTRO", "OTRO (IMPROCEDENTE)");

    /**
     * Accidentes. El layout viejo partia dias y costo en CALIFICADO e IMPROCEDENTE; el nuevo tiene
     * una sola columna de cada uno y el STATUS DE CALIFICACION. Se suma lo de los dos lados (lo que
     * ya hacia la grafica) y el estatus se toma del lado que trae datos; si trae los dos, se marca.
     * La carga vieja no tiene la fecha del accidente, solo el mes reportado (dia 1).
     */
    private Fila filaAccidente(Evento e, String nss, List<Candidato> cand) {
        List<String> n = new ArrayList<>();
        List<Object> v = new ArrayList<>();
        v.add(nss);
        v.add(cat("PREDIO", e.predio(), n, "PREDIO"));
        v.add(cat("AREA", e.area(), n, "AREA"));
        // El ST7 (aviso de accidente) vive dentro de CALIFICADO o IMPROCEDENTE. Con ST7, la FECHA del
        // accidente se toma de ahi: es la fecha real mas cercana que trae el Excel (13-oct-2026); sin
        // ST7 queda el mes reportado (dia 1).
        // Desde el 13-oct-2026 el lector separa LABORAL y TRAYECTO (CALIFICADO.LABORAL_ST7, ...); antes
        // la de trayecto pisaba a la laboral. Se busca primero el lado del tipo de riesgo del renglon.
        String lado = "TRAYECTO".equalsIgnoreCase(e.texto("TIPO_RIESGO")) ? "TRAYECTO" : "LABORAL";
        String otro = "LABORAL".equals(lado) ? "TRAYECTO" : "LABORAL";
        LocalDate st7 = primeraFecha(e, "CALIFICADO." + lado + "_ST7", "IMPROCEDENTE." + lado + "_ST7",
                "CALIFICADO." + otro + "_ST7", "IMPROCEDENTE." + otro + "_ST7", "CALIFICADO.ST7", "IMPROCEDENTE.ST7");
        v.add(st7 != null ? st7 : e.fecha());
        n.add(st7 != null ? "FECHA: tomada del ST7 (aviso de accidente); confirmar la del accidente"
                : e.fecha() == null ? "FECHA: no estaba, capturar la del accidente"
                : "FECHA: es el MES reportado (dia 1), cambiar por la fecha real del accidente");
        v.add(cat("TIPO_RIESGO", e.texto("TIPO_RIESGO"), n, "TIPO DE RIESGO"));
        v.add(e.texto("DIAGNOSTICO"));
        v.add(e.numero("SDI"));
        v.add(e.numero("SDI.PAGO_60"));
        v.add(st7);
        LocalDate st2 = primeraFecha(e, "CALIFICADO." + lado + "_ST2", "IMPROCEDENTE." + lado + "_ST2",
                "CALIFICADO." + otro + "_ST2", "IMPROCEDENTE." + otro + "_ST2", "CALIFICADO.ST2", "IMPROCEDENTE.ST2");
        v.add(st2);
        // DIAS: laboral + trayecto (+ el nombre anterior, por si el archivo no los separa)
        BigDecimal dc = cero(e.numero("CALIFICADO.DIAS")).add(cero(e.numero("CALIFICADO.LABORAL_DIAS")))
                .add(cero(e.numero("CALIFICADO.TRAYECTO_DIAS")));
        BigDecimal di = cero(e.numero("IMPROCEDENTE.DIAS")).add(cero(e.numero("IMPROCEDENTE.LABORAL_DIAS")))
                .add(cero(e.numero("IMPROCEDENTE.TRAYECTO_DIAS")));
        BigDecimal cc = cero(e.numero("COSTO.CALIFICADO"));
        BigDecimal ci = cero(e.numero("COSTO.IMPROCEDENTE"));
        BigDecimal dias = dc.add(di);
        BigDecimal costo = cc.add(ci);
        v.add(dias.signum() == 0 ? null : dias.stripTrailingZeros());
        v.add(costo.signum() == 0 ? null : costo);
        boolean cal = dc.signum() != 0 || cc.signum() != 0;
        boolean imp = di.signum() != 0 || ci.signum() != 0;
        String estatusViejo = e.estatus();
        String calif = cal && imp ? null : cal ? "CALIFICADO" : imp ? "IMPROCEDENTE"
                : CatalogosNss.buscar("ESTATUS_ACCIDENTE", estatusViejo).orElse(null);
        if (cal && imp) {
            n.add("STATUS DE CALIFICACION: trae dias/costo calificados E improcedentes, elegir");
        }
        v.add(calif);
        v.add(e.texto("CAUSA_DE_RT.DESCRIPCION_BREVE_DEL_ACCIDENTE"));
        String causa = CAUSAS_RT.entrySet().stream().filter(x -> e.atr().containsKey(x.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        v.add(causa);
        v.add(CatalogosNss.buscar("STATUS_CASO", estatusViejo).orElse(null));
        v.add(e.fechaAtr("FECHA_CALIFICACION"));
        v.add(e.texto("OBSERVACIONES"));
        return new Fila(v, n);
    }

    private static LocalDate primeraFecha(Evento e, String... nombres) {
        for (String n : nombres) {
            LocalDate f = e.fechaAtr(n);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    private static BigDecimal cero(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    /** Reporte de consola del prellenado. */
    public static String reporte(List<Resumen> resumenes) {
        StringBuilder sb = new StringBuilder("\n================================================================\n");
        sb.append(" ARCHIVOS DE PRUEBA DESDE LA CARGA VIEJA (solo lectura; NO son para el servicio medico)\n");
        sb.append("================================================================\n");
        sb.append(String.format(" %-22s %9s %9s %9s %9s%n", "layout", "renglones", "nss 1", "varios", "ninguno"));
        for (Resumen r : resumenes) {
            sb.append(String.format(" %-22s %9d %9d %9d %9d%n", r.layout().hoja(), r.renglones(), r.nssUnico(),
                    r.nssVarios(), r.nssNinguno()));
        }
        sb.append("\n nss 1   = una sola persona con ese nombre: el renglon lleva ese NSS\n");
        sb.append(" varios  = varias personas con el nombre: NSS vacio (la carga lo rechazara)\n");
        sb.append(" ninguno = el nombre no esta en el biometrico: NSS vacio (la carga lo rechazara)\n\n Archivos:\n");
        resumenes.forEach(r -> sb.append("   ").append(r.archivo()).append('\n'));
        sb.append("================================================================\n");
        return sb.toString();
    }
}
