package com.onest.app.importer.nss;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Escritura en las tablas SERV_MED_XLS_* (docs/script-prod/excel-nss-01-ddl-tablas.sql y
 * excel-nss-06-ddl-pos-antidoping.sql).
 *
 * <p>Cada archivo entra en UNA transaccion: o queda completo (lote + renglones + reemplazo +
 * marcas de duplicado) o no queda nada. Asi un archivo que truena a la mitad no deja medio mes.
 */
@Repository
public class XlsNssRepository {

    private static final Logger log = LoggerFactory.getLogger(XlsNssRepository.class);
    private static final int LOTE_BATCH = 500;

    /** Lo que paso con un archivo en la base. */
    public record Escritura(long loteId, int insertados, int reemplazados, int duplicados,
                            String avisoDuplicados) {
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public XlsNssRepository(JdbcTemplate jdbc, DataSource ds) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    /** Corta antes de leer un solo archivo si falta alguna tabla (la del lote y una por layout). */
    public void verificarEsquema() {
        List<String> esperadas = new ArrayList<>(List.of("SERV_MED_XLS_LOTE"));
        for (LayoutNss l : LayoutNss.values()) {
            esperadas.add(l.tabla());
        }
        List<String> existen = jdbc.queryForList("SELECT table_name FROM user_tables WHERE table_name LIKE 'SERV_MED_XLS%'",
                String.class);
        List<String> faltan = esperadas.stream().filter(t -> !existen.contains(t)).toList();
        if (!faltan.isEmpty()) {
            throw new IllegalStateException("Faltan tablas " + faltan + ". Correr docs/script-prod/"
                    + "excel-nss-01-ddl-tablas.sql, excel-nss-06-ddl-pos-antidoping.sql, excel-nss-07-ddl-consumibles.sql y excel-nss-08-ddl-maternidad.sql");
        }
    }

    /** Si este mismo archivo (mismo contenido) ya se cargo bien, para no cargarlo dos veces. */
    public Long loteConMismoContenido(LayoutNss layout, String checksum) {
        List<Long> ids = jdbc.queryForList(
                "SELECT ID FROM SERV_MED_XLS_LOTE WHERE LAYOUT = ? AND CHECKSUM_SHA256 = ? "
                + "AND ESTADO IN ('OK','CON_ERRORES') ORDER BY ID DESC",
                Long.class, layout.name(), checksum);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * Escribe un archivo completo en una transaccion.
     *
     * @param registros renglones validos, ya con los datos del biometrico en sus valores
     */
    public Escritura escribir(LayoutNss layout, String archivo, String checksum, List<ValidadorNss.Registro> registros,
                              int leidas, int errores, String reporte, String usuario) {
        return tx.execute(status -> {
            long lote = abrirLote(layout, archivo, checksum, registros, leidas, errores, reporte, usuario);
            insertar(layout, lote, registros);
            int reemplazados = reemplazar(layout, lote);
            String aviso = null;
            int duplicados = 0;
            try {
                duplicados = marcarDuplicados(layout, lote);
            } catch (RuntimeException ex) {
                // Sin las tablas vivas (otro ambiente) no se puede comparar: se carga igual y se avisa
                aviso = "no se pudo comparar contra el portal: " + ex.getMessage();
                log.warn("[nss] {}", aviso);
            }
            jdbc.update("UPDATE SERV_MED_XLS_LOTE SET FILAS_VALIDAS = ?, FILAS_DUPLICADAS = ?, ESTADO = ? WHERE ID = ?",
                    registros.size() - duplicados, duplicados, errores > 0 ? "CON_ERRORES" : "OK", lote);
            // Lotes anteriores que se quedaron sin renglones por el reemplazo
            jdbc.update("UPDATE SERV_MED_XLS_LOTE l SET ESTADO = 'REEMPLAZADO' WHERE l.LAYOUT = ? AND l.ID <> ? "
                    + "AND l.ESTADO IN ('OK','CON_ERRORES') AND NOT EXISTS (SELECT 1 FROM " + layout.tabla()
                    + " t WHERE t.LOTE_ID = l.ID)", layout.name(), lote);
            return new Escritura(lote, registros.size(), reemplazados, duplicados, aviso);
        });
    }

    private long abrirLote(LayoutNss layout, String archivo, String checksum, List<ValidadorNss.Registro> registros,
                           int leidas, int errores, String reporte, String usuario) {
        LocalDate desde = registros.stream().map(r -> r.fecha("FECHA")).min(LocalDate::compareTo).orElse(null);
        LocalDate hasta = registros.stream().map(r -> r.fecha("FECHA")).max(LocalDate::compareTo).orElse(null);
        List<String> predios = registros.stream().map(r -> r.texto("PREDIO")).distinct().toList();
        String predio = predios.size() == 1 ? predios.get(0) : (predios.isEmpty() ? null : "VARIOS");
        GeneratedKeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_XLS_LOTE (LAYOUT, ARCHIVO, CHECKSUM_SHA256, PREDIO, PERIODO_DESDE, "
                    + "PERIODO_HASTA, FILAS_LEIDAS, FILAS_ERROR, ESTADO, REPORTE, CREATED_BY) "
                    + "VALUES (?,?,?,?,?,?,?,?,'EN_CURSO',?,?)",
                    new String[] {"ID"});
            ps.setString(1, layout.name());
            ps.setString(2, archivo.length() > 400 ? archivo.substring(archivo.length() - 400) : archivo);
            ps.setString(3, checksum);
            ps.setString(4, predio);
            ps.setObject(5, desde == null ? null : java.sql.Date.valueOf(desde), Types.DATE);
            ps.setObject(6, hasta == null ? null : java.sql.Date.valueOf(hasta), Types.DATE);
            ps.setInt(7, leidas);
            ps.setInt(8, errores);
            ps.setString(9, reporte);
            // Mismo default de la columna cuando carga la consola; desde la pantalla, quien cargo
            ps.setString(10, usuario == null ? "CARGA_EXCEL_NSS" : usuario);
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    private void insertar(LayoutNss layout, long lote, List<ValidadorNss.Registro> registros) {
        List<String> columnas = new ArrayList<>(List.of("LOTE_ID", "ORIGEN_FILA"));
        // Los campos MARCA no tienen columna propia: se guardan juntos en una derivada (SUSTANCIA)
        layout.campos().stream().filter(c -> c.columna() != null).forEach(c -> columnas.add(c.columna()));
        columnas.addAll(layout.columnasDerivadas());
        if (layout.empleado()) {
            // EN_BIOMETRICO: 'N' = el NSS no esta en el biometrico y entro marcado (3-nov-2026,
            // DDL carga-excel-pantalla-04); NULL = sin persona (NSS en ceros)
            // sin repetir: atenciones ya trae GENERO, EDAD y CUENTA del Excel (12-nov-2026)
            List.of("NOMBRE", "GENERO", "EDAD", "CUENTA", "PUESTO", "EN_BIOMETRICO").stream()
                    .filter(col -> !columnas.contains(col)).forEach(columnas::add);
        }
        String sql = "INSERT INTO " + layout.tabla() + " (" + String.join(",", columnas) + ") VALUES ("
                + String.join(",", java.util.Collections.nCopies(columnas.size(), "?")) + ")";
        for (int i = 0; i < registros.size(); i += LOTE_BATCH) {
            List<ValidadorNss.Registro> parte = registros.subList(i, Math.min(i + LOTE_BATCH, registros.size()));
            jdbc.batchUpdate(sql, parte, parte.size(), (ps, r) -> {
                ps.setLong(1, lote);
                ps.setInt(2, r.fila());
                for (int k = 2; k < columnas.size(); k++) {
                    poner(ps, k + 1, r.valores().get(columnas.get(k)));
                }
            });
        }
    }

    private static void poner(PreparedStatement ps, int i, Object v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(i, Types.VARCHAR);
        } else if (v instanceof LocalDate d) {
            ps.setDate(i, java.sql.Date.valueOf(d));
        } else if (v instanceof BigDecimal n) {
            ps.setBigDecimal(i, n);
        } else if (v instanceof Integer n) {
            ps.setInt(i, n);
        } else {
            ps.setString(i, v.toString());
        }
    }

    /**
     * Reemplazo por predio y mes: lo que llega de un predio para un mes sustituye lo que ya habia
     * de ese predio en ese mes (de lotes anteriores). Asi volver a mandar el archivo corregido, o
     * el acumulado del ano, no duplica.
     */
    private int reemplazar(LayoutNss layout, long lote) {
        String t = layout.tabla();
        return jdbc.update("DELETE FROM " + t + " x WHERE x.LOTE_ID <> ? AND (x.PREDIO, TRUNC(x.FECHA, 'MM')) IN "
                + "(SELECT DISTINCT n.PREDIO, TRUNC(n.FECHA, 'MM') FROM " + t + " n WHERE n.LOTE_ID = ?)", lote, lote);
    }

    /**
     * "Si el evento ya esta en el portal, manda el portal": el renglon del Excel se queda, marcado
     * DUPLICADO, y no se cuenta. Llaves acordadas el 10-oct-2026.
     */
    private int marcarDuplicados(LayoutNss layout, long lote) {
        String t = layout.tabla();
        // "desde" = FROM + WHERE del cruce. Va por separado del SELECT porque EXISTS necesita
        // "SELECT 1": un "SELECT MIN(...)" sin GROUP BY regresa SIEMPRE un renglon (aunque sea
        // NULL) y EXISTS daria verdadero para todos.
        String desde = switch (layout) {
            // Sin persona (NSS en ceros) no hay con quien empatar: una consulta del portal capturada
            // con ceros ese dia seria de otra persona y esconderia la atencion del Excel
            case ATENCIONES -> "FROM TBL_SERV_CONSULTA_MEDICA p WHERE p.NSS = x.NSS AND TRUNC(p.FECHA) = x.FECHA "
                    + "AND x.NSS <> '" + ValidadorNss.NSS_CEROS + "'";
            case EXAMEN_INGRESO, EXAMEN_PERIODICO, EXAMEN_POS_INCAP -> "FROM SERV_MED_RESULTADO_EXAMEN_HIST p "
                    + "WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_REGISTRO) BETWEEN x.FECHA - 1 AND x.FECHA + 1";
            // La misma prueba (antidoping o alcoholemia) de la misma persona el mismo dia
            case ANTIDOPING -> "FROM SERV_MED_ANTIDOPING_RESULTADO p WHERE p.NSS = x.NSS "
                    + "AND TRUNC(p.FECHA_REGISTRO) = x.FECHA AND UPPER(p.TIPO_PRUEBA) = x.TIPO_PRUEBA";
            // Un chequeo del portal de la misma persona en el mismo mes del corte: el Excel es un
            // reporte mensual, asi que la llave es el mes (decision 12-oct)
            case MATERNIDAD -> "FROM SERV_MED_MATERNIDAD_SEGUIMIENTO p WHERE p.NSS = x.NSS "
                    + "AND TRUNC(p.FECHA_REGISTRO, 'MM') = TRUNC(x.FECHA, 'MM')";
            // Inventario del consultorio: el portal no lo captura (su "consumibles" son los kits de
            // antidoping), no hay con que comparar
            case MEDICAMENTOS, MATERIAL_FIJO -> null;
            case ACCIDENTES -> "FROM SERV_MED_ACCIDENTE p WHERE p.NSS = x.NSS AND TRUNC(p.FECHA_ACCIDENTE) = x.FECHA";
            // Folio cuando hay; las internas (sin folio) por NSS + inicio. FECHA_INICIO es texto en
            // la tabla viva, con dos formatos: misma conversion que el WS de incapacidades.
            case INCAPACIDADES -> "FROM TBL_SERV_INCAPACIDAD_MEDICA p WHERE "
                    + "(x.FOLIO IS NOT NULL AND UPPER(REPLACE(p.FOLIO_INCAPACIDAD, ' ')) = UPPER(REPLACE(x.FOLIO, ' '))) "
                    + "OR (x.FOLIO IS NULL AND p.NSS = x.NSS AND CASE "
                    + "WHEN REGEXP_LIKE(p.FECHA_INICIO, '^[0-9]{4}-[0-9]{2}-[0-9]{2}') THEN TO_DATE(SUBSTR(p.FECHA_INICIO, 1, 10), 'YYYY-MM-DD') "
                    + "WHEN REGEXP_LIKE(p.FECHA_INICIO, '^[0-9]{2}/[0-9]{2}/[0-9]{4}') THEN TO_DATE(SUBSTR(p.FECHA_INICIO, 1, 10), 'DD/MM/YYYY') "
                    + "END = x.FECHA)";
        };
        String origen = switch (layout) {
            case ATENCIONES -> "CONSULTA:";
            case EXAMEN_INGRESO, EXAMEN_PERIODICO, EXAMEN_POS_INCAP -> "EXAMEN_HIST:";
            case ACCIDENTES -> "ACCIDENTE:";
            case INCAPACIDADES -> "INCAPACIDAD:";
            case ANTIDOPING -> "ANTIDOPING:";
            case MATERNIDAD -> "MATERNIDAD:";
            case MEDICAMENTOS, MATERIAL_FIJO -> null;
        };
        if (desde == null) {
            return 0;
        }
        return jdbc.update("UPDATE " + t + " x SET ESTADO_REGISTRO = 'DUPLICADO', DUPLICADO_DE = '" + origen
                + "' || (SELECT MIN(TO_CHAR(p.REG_ID)) " + desde + ") "
                + "WHERE x.LOTE_ID = ? AND EXISTS (SELECT 1 " + desde + ")", lote);
    }

    /** Un predio y mes del archivo: cuantos renglones trae y cuantos de cargas anteriores va a sustituir. */
    public record Reemplazo(String predio, LocalDate mes, int nuevos, int existentes) {
    }

    /**
     * Lo que haria el reemplazo por predio y mes, SIN escribir: para avisar en pantalla antes de
     * confirmar ("este archivo sustituye N renglones de PREDIO, MES"). Solo lectura.
     */
    public List<Reemplazo> reemplazos(LayoutNss layout, List<ValidadorNss.Registro> registros) {
        Map<String, Map<LocalDate, Integer>> nuevos = new java.util.TreeMap<>();
        for (ValidadorNss.Registro r : registros) {
            if (r.texto("PREDIO") == null || r.fecha("FECHA") == null) {
                continue;
            }
            nuevos.computeIfAbsent(r.texto("PREDIO"), k -> new java.util.TreeMap<>())
                    .merge(r.fecha("FECHA").withDayOfMonth(1), 1, Integer::sum);
        }
        if (nuevos.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> existentes = new java.util.HashMap<>();
        List<String> predios = new ArrayList<>(nuevos.keySet());
        for (int i = 0; i < predios.size(); i += LOTE_BATCH) {
            List<String> parte = predios.subList(i, Math.min(i + LOTE_BATCH, predios.size()));
            jdbc.query("SELECT PREDIO, TRUNC(FECHA, 'MM') MES, COUNT(*) N FROM " + layout.tabla()
                    + " WHERE PREDIO IN (" + String.join(",", java.util.Collections.nCopies(parte.size(), "?"))
                    + ") GROUP BY PREDIO, TRUNC(FECHA, 'MM')", rs -> {
                        existentes.put(rs.getString("PREDIO") + "|" + rs.getDate("MES").toLocalDate(), rs.getInt("N"));
                    }, parte.toArray());
        }
        List<Reemplazo> res = new ArrayList<>();
        nuevos.forEach((predio, meses) -> meses.forEach((mes, n) ->
                res.add(new Reemplazo(predio, mes, n, existentes.getOrDefault(predio + "|" + mes, 0)))));
        return res;
    }

    /** Un lote para el historial de la pantalla; {@code archivoId} = el original en SERV_MED_FS_FILE. */
    public record LoteResumen(long id, String layout, String archivo, String predio, LocalDate desde,
                              LocalDate hasta, int leidas, int validas, int duplicadas, int conError,
                              String estado, java.time.LocalDateTime creado, String creadoPor, Long archivoId) {
    }

    /** Ultimos lotes, de la consola y de la pantalla. El original solo existe para los de la pantalla. */
    public List<LoteResumen> ultimosLotes(int maximo, String fileType) {
        return jdbc.query("""
                SELECT l.ID, l.LAYOUT, l.ARCHIVO, l.PREDIO, l.PERIODO_DESDE, l.PERIODO_HASTA, l.FILAS_LEIDAS,
                       l.FILAS_VALIDAS, l.FILAS_DUPLICADAS, l.FILAS_ERROR, l.ESTADO, l.CREATED_AT, l.CREATED_BY,
                       (SELECT MAX(f.ID) FROM SERV_MED_FS_FILE f WHERE f.FILE_TYPE = ? AND f.STATUS = 'ACTIVE'
                           AND f.BUSINESS_KEY = 'XLS_LOTE:' || l.ID) ARCHIVO_ID
                  FROM SERV_MED_XLS_LOTE l
                 ORDER BY l.ID DESC
                 FETCH FIRST ? ROWS ONLY""", (rs, i) -> new LoteResumen(rs.getLong("ID"), rs.getString("LAYOUT"),
                rs.getString("ARCHIVO"), rs.getString("PREDIO"), fechaO(rs.getDate("PERIODO_DESDE")),
                fechaO(rs.getDate("PERIODO_HASTA")), rs.getInt("FILAS_LEIDAS"), rs.getInt("FILAS_VALIDAS"),
                rs.getInt("FILAS_DUPLICADAS"), rs.getInt("FILAS_ERROR"), rs.getString("ESTADO"),
                rs.getTimestamp("CREATED_AT").toLocalDateTime(), rs.getString("CREATED_BY"),
                rs.getObject("ARCHIVO_ID") == null ? null : rs.getLong("ARCHIVO_ID")), fileType, maximo);
    }

    public boolean esArchivoDeCarga(long archivoId, String fileType) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM SERV_MED_FS_FILE WHERE ID = ? AND FILE_TYPE = ?",
                Integer.class, archivoId, fileType);
        return n != null && n > 0;
    }

    private static LocalDate fechaO(java.sql.Date d) {
        return d == null ? null : d.toLocalDate();
    }

    /** Conteos por tabla, para el final del reporte. */
    public Map<String, Integer> conteos() {
        Map<String, Integer> m = new java.util.LinkedHashMap<>();
        for (LayoutNss l : LayoutNss.values()) {
            m.put(l.tabla(), jdbc.queryForObject("SELECT COUNT(*) FROM " + l.tabla(), Integer.class));
        }
        return m;
    }
}
