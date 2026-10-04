package com.onest.excel.persistencia;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Repository;

/**
 * Tarea 11: escribir en las tres tablas de la bitacora.
 *
 * <p>Por {@code JdbcTemplate} y no por JPA, igual que {@code FsFileRepository} e
 * {@code ImportRepository} del portal: son cargas por lote de decenas de miles de renglones,
 * donde un contexto de persistencia solo estorba.
 *
 * <p><b>Dos decisiones de rendimiento con su razon:</b>
 * <ol>
 *   <li><b>Los eventos se insertan en lote y despues se releen sus ID.</b> Pedir la llave
 *       generada renglon por renglon serian 23 mil viajes a la base; con dos consultas por
 *       archivo (un batch de INSERT y un SELECT del mapa fila&rarr;ID) alcanza.</li>
 *   <li><b>La idempotencia se resuelve por lote, no por fila.</b> Si un archivo+hoja ya se
 *       proceso, se salta entero. Es mas barato que preguntar 23 mil veces, y coincide con el
 *       indice unico {@code SERV_MED_UX_BITACORA_LOTE_AR}.</li>
 * </ol>
 */
@Repository
@ConditionalOnExpression("'${excel.destino.url:}' != ''")
public class BitacoraRepository {

    private static final Logger log = LoggerFactory.getLogger(BitacoraRepository.class);

    private final JdbcTemplate jdbc;

    public BitacoraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Comprueba que las 6 tablas existan antes de intentar nada. */
    public void verificarEsquema() {
        for (String t : List.of("SERV_MED_BITACORA_LOTE", "SERV_MED_BITACORA_EVENTO",
                "SERV_MED_BITACORA_ATRIBUTO", "SERV_MED_BITACORA_ESTADO_MES",
                "SERV_MED_BITACORA_METRICA", "SERV_MED_BITACORA_NOMENCL")) {
            try {
                jdbc.queryForObject("SELECT COUNT(*) FROM " + t + " WHERE ROWNUM = 0", Integer.class);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Falta la tabla " + t + " en la base del portal. "
                        + "Aplicar docs/script-prod/excel-00-ddl-completo.sql antes de cargar.", e);
            }
        }
    }

    /** Los estados en los que una hoja se considera cargada de verdad. */
    private static final String ESTADOS_TERMINADOS = "('OK', 'CON_AVISOS')";

    /**
     * Si este archivo+hoja ya se cargo <b>bien</b> en una corrida anterior.
     *
     * <p><b>Solo cuenta si termino en OK o CON_AVISOS.</b> Un lote que quedo en
     * {@code EN_CURSO} no esta cargado: es una corrida que se cayo a media hoja.
     *
     * <p>Antes contaba cualquier lote, y eso creaba una trampa peor que el fallo original. El
     * 30-sep-2026 la hoja MERCURIO de antidoping murio con un {@code NullPointerException}
     * <b>despues</b> de abrir su lote, asi que quedo una fila en {@code EN_CURSO}; con la regla
     * vieja, cualquier reintento la habria saltado con "ya cargada antes" y sus 462 renglones
     * &mdash;la hoja mas grande de la familia&mdash; se habrian perdido <b>en silencio y para
     * siempre</b>, con el reporte diciendo que todo salio bien.
     */
    public boolean loteYaProcesado(String archivo, String hoja) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SERV_MED_BITACORA_LOTE "
                + "WHERE ARCHIVO = ? AND HOJA = ? AND ESTADO IN " + ESTADOS_TERMINADOS,
                Integer.class, archivo, hoja);
        return n != null && n > 0;
    }

    /**
     * Abre el lote y devuelve su ID.
     *
     * <p>Si quedo un lote a medias de una corrida anterior, se borra con lo que hubiera alcanzado
     * a escribir. Hace falta por el indice unico {@code (ARCHIVO, HOJA)}: sin esto el reintento
     * choca contra el lote muerto y la hoja no se puede recuperar nunca sin meter mano a la base.
     * Se borra poco y bien delimitado &mdash;solo esa hoja&mdash; y en el orden que exigen las
     * llaves foraneas.
     */
    public long abrirLote(String archivo, String ruta, String checksum, String hoja,
                          String familia, String predio, Integer anio) {
        borrarLoteIncompleto(archivo, hoja);
        jdbc.update("""
                INSERT INTO SERV_MED_BITACORA_LOTE
                  (ARCHIVO, RUTA, CHECKSUM_SHA256, HOJA, FAMILIA, PREDIO, ANIO, ESTADO, STARTED_AT)
                VALUES (?,?,?,?,?,?,?, 'EN_CURSO', SYSTIMESTAMP)
                """, archivo, ruta, checksum, hoja, familia, predio, anio);
        return jdbc.queryForObject(
                "SELECT ID FROM SERV_MED_BITACORA_LOTE WHERE ARCHIVO = ? AND HOJA = ?",
                Long.class, archivo, hoja);
    }

    /** Limpia los restos de una hoja que no termino. No toca las que si terminaron. */
    private void borrarLoteIncompleto(String archivo, String hoja) {
        String deQueLote = "SELECT ID FROM SERV_MED_BITACORA_LOTE "
                + "WHERE ARCHIVO = ? AND HOJA = ? AND ESTADO NOT IN " + ESTADOS_TERMINADOS;
        jdbc.update("DELETE FROM SERV_MED_BITACORA_ATRIBUTO WHERE EVENTO_ID IN ("
                + "SELECT ID FROM SERV_MED_BITACORA_EVENTO WHERE LOTE_ID IN (" + deQueLote + "))",
                archivo, hoja);
        jdbc.update("DELETE FROM SERV_MED_BITACORA_EVENTO WHERE LOTE_ID IN (" + deQueLote + ")",
                archivo, hoja);
        // Las metricas cuelgan del LOTE y no del EVENTO: las familias sin persona no producen
        // eventos. Sin esta linea, reintentar una hoja de consumibles duplicaria sus metricas.
        jdbc.update("DELETE FROM SERV_MED_BITACORA_METRICA WHERE LOTE_ID IN (" + deQueLote + ")",
                archivo, hoja);
        int lotes = jdbc.update("DELETE FROM SERV_MED_BITACORA_LOTE "
                + "WHERE ARCHIVO = ? AND HOJA = ? AND ESTADO NOT IN " + ESTADOS_TERMINADOS,
                archivo, hoja);
        if (lotes > 0) {
            log.info("Se limpio un lote incompleto de {} hoja {} para reintentarlo", archivo, hoja);
        }
    }

    /**
     * Cierra el lote con su resultado y el cuadre.
     *
     * @param acumuladoEsperado lo que dice la hoja ACUMULADO del propio Excel, o {@code null}
     */
    public void cerrarLote(long loteId, int leidas, int insertadas, int descartadas,
                           Integer acumuladoEsperado, String mensajes, String estado) {
        // Se compara contra lo LEIDO, no contra lo insertado. El cuadre verifica que el
        // transpositor haya leido el archivo completo; que la insercion haya entrado toda se
        // ve en FILAS_INSERTADAS y FILAS_DESCARTADAS, que son otra cosa. Asi el cuadre tambien
        // sirve en modo muestra, donde se leen todos los renglones pero solo se insertan 25.
        //
        // Tres estados, no dos. 'Z' es "el archivo no lleva su acumulado": la hoja existe, tiene
        // el predio en su renglon y los doce meses en CERO porque el origen nunca la lleno.
        // Verificado el 30-sep-2026 abriendo la hoja ACUMULADO de los examenes periodicos: ocho
        // de las once hojas que salian marcadas "*** NO CUADRA" eran esto, y leidas contra cero
        // daba diferencias de 183, 152, 90 renglones que parecian perdida de datos. No hay nada
        // roto; simplemente de esos archivos no hay contra que cuadrar.
        String cuadra;
        if (acumuladoEsperado == null) {
            cuadra = null;
        } else if (acumuladoEsperado == 0 && leidas > 0) {
            cuadra = "Z";
        } else {
            cuadra = acumuladoEsperado == leidas ? "S" : "N";
        }
        jdbc.update("""
                UPDATE SERV_MED_BITACORA_LOTE
                   SET FILAS_LEIDAS = ?, FILAS_INSERTADAS = ?, FILAS_DESCARTADAS = ?,
                       ACUMULADO_ESPERADO = ?, CUADRA = ?, MENSAJES = ?,
                       ESTADO = ?, FINISHED_AT = SYSTIMESTAMP
                 WHERE ID = ?
                """, leidas, insertadas, descartadas, acumuladoEsperado, cuadra, mensajes, estado, loteId);
    }

    /** Un evento listo para insertar. Los campos nulos se aceptan: el Excel es disparejo. */
    public record EventoFila(
            long loteId, String familia,
            LocalDate fecha, Integer anio, Integer mes,
            String predio, String cuenta, String area, String puesto, String agencia,
            String nombre, String nombreNorm, String rangoEdad, String genero,
            Integer edad, LocalDate fechaNacimiento, LocalDate fechaIngreso, String estatus,
            String origenArchivo, String origenHoja, int origenFila) {
    }

    /** Un atributo de un evento. Solo uno de los tres valores suele venir lleno. */
    public record AtributoFila(int origenFila, String nombre, String valor,
                               Double valorNum, LocalDate valorFecha) {
    }

    /**
     * Una metrica: un numero sin persona detras.
     *
     * @param concepto    la entidad medida. En consumibles, el medicamento
     * @param subconcepto que se mide de ella: {@code SEMANA 1}, {@code TOTAL}, {@code ACUMULADO}
     */
    public record MetricaFila(long loteId, String familia, String origen, String predio,
                              Integer anio, Integer mes, String concepto, String subconcepto,
                              Double valor) {
    }

    /**
     * Inserta metricas en un solo lote.
     *
     * <p>{@code SERV_MED_BITACORA_METRICA} existia desde el DDL de la fase 1 con el comentario
     * "conteos sin persona" y nadie la habia usado. Consumibles es justo eso: el renglon del Excel
     * no es una persona sino un medicamento, y lo que se mide es cuantas piezas se consumieron.
     */
    public int insertarMetricas(List<MetricaFila> metricas) {
        if (metricas.isEmpty()) {
            return 0;
        }
        int[] r = jdbc.batchUpdate("""
                INSERT INTO SERV_MED_BITACORA_METRICA
                  (LOTE_ID, FAMILIA, ORIGEN, PREDIO, ANIO, MES, CONCEPTO, SUBCONCEPTO, VALOR)
                VALUES (?,?,?,?,?,?,?,?,?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                MetricaFila m = metricas.get(i);
                ps.setLong(1, m.loteId());
                ps.setString(2, m.familia());
                ps.setString(3, m.origen());
                ps.setString(4, m.predio());
                setEntero(ps, 5, m.anio());
                setEntero(ps, 6, m.mes());
                ps.setString(7, m.concepto());
                ps.setString(8, m.subconcepto());
                if (m.valor() == null) {
                    ps.setNull(9, Types.NUMERIC);
                } else {
                    ps.setDouble(9, m.valor());
                }
            }

            @Override
            public int getBatchSize() {
                return metricas.size();
            }
        });
        return r.length;
    }

    /** Inserta los eventos en un solo lote. */
    public int insertarEventos(List<EventoFila> eventos) {
        if (eventos.isEmpty()) {
            return 0;
        }
        int[] r = jdbc.batchUpdate("""
                INSERT INTO SERV_MED_BITACORA_EVENTO
                  (LOTE_ID, FAMILIA, FECHA, ANIO, MES,
                   PREDIO, CUENTA, AREA, PUESTO, AGENCIA,
                   NOMBRE, NOMBRE_NORM, RANGO_EDAD, GENERO,
                   EDAD, FECHA_NACIMIENTO, FECHA_INGRESO, ESTATUS,
                   ORIGEN_ARCHIVO, ORIGEN_HOJA, ORIGEN_FILA)
                VALUES (?,?,?,?,?, ?,?,?,?,?, ?,?,?,?, ?,?,?,?, ?,?,?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                EventoFila e = eventos.get(i);
                int p = 1;
                ps.setLong(p++, e.loteId());
                ps.setString(p++, e.familia());
                setFecha(ps, p++, e.fecha());
                setEntero(ps, p++, e.anio());
                setEntero(ps, p++, e.mes());
                ps.setString(p++, e.predio());
                ps.setString(p++, e.cuenta());
                ps.setString(p++, e.area());
                ps.setString(p++, e.puesto());
                ps.setString(p++, e.agencia());
                ps.setString(p++, e.nombre());
                ps.setString(p++, e.nombreNorm());
                ps.setString(p++, e.rangoEdad());
                ps.setString(p++, e.genero());
                setEntero(ps, p++, e.edad());
                setFecha(ps, p++, e.fechaNacimiento());
                setFecha(ps, p++, e.fechaIngreso());
                ps.setString(p++, e.estatus());
                ps.setString(p++, e.origenArchivo());
                ps.setString(p++, e.origenHoja());
                ps.setInt(p, e.origenFila());
            }

            @Override
            public int getBatchSize() {
                return eventos.size();
            }
        });
        return r.length;
    }

    /**
     * Mapa fila del Excel &rarr; ID generado, para poder colgarle los atributos.
     *
     * <p>Se relee en vez de pedir las llaves generadas en el batch: es una sola consulta contra
     * 23 mil viajes, y el indice unico de origen garantiza que no hay ambiguedad.
     */
    public Map<Integer, Long> idsPorFila(long loteId) {
        Map<Integer, Long> mapa = new HashMap<>();
        jdbc.query("SELECT ORIGEN_FILA, ID FROM SERV_MED_BITACORA_EVENTO WHERE LOTE_ID = ?",
                rs -> { mapa.put(rs.getInt(1), rs.getLong(2)); }, loteId);
        return mapa;
    }

    /** Inserta los atributos resolviendo el ID del evento por su fila de origen. */
    public int insertarAtributos(List<AtributoFila> atributos, Map<Integer, Long> idsPorFila) {
        List<AtributoFila> validos = atributos.stream()
                .filter(a -> idsPorFila.containsKey(a.origenFila()))
                .toList();
        if (validos.isEmpty()) {
            return 0;
        }
        int[] r = jdbc.batchUpdate("""
                INSERT INTO SERV_MED_BITACORA_ATRIBUTO
                  (EVENTO_ID, NOMBRE, VALOR, VALOR_NUM, VALOR_FECHA)
                VALUES (?,?,?,?,?)
                """, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                AtributoFila a = validos.get(i);
                ps.setLong(1, idsPorFila.get(a.origenFila()));
                ps.setString(2, a.nombre());
                ps.setString(3, a.valor());
                if (a.valorNum() == null) {
                    ps.setNull(4, Types.NUMERIC);
                } else {
                    ps.setDouble(4, a.valorNum());
                }
                setFecha(ps, 5, a.valorFecha());
            }

            @Override
            public int getBatchSize() {
                return validos.size();
            }
        });
        return r.length;
    }

    /** Conteos actuales, para el reporte final. */
    public Map<String, Integer> conteos() {
        Map<String, Integer> m = new java.util.LinkedHashMap<>();
        // METRICA entro el 30-sep-2026 con consumibles. Sin ella, el reporte decia "en la base
        // ahora" y no mencionaba los 4,742 renglones que acababa de escribir.
        for (String t : List.of("LOTE", "EVENTO", "ATRIBUTO", "METRICA")) {
            try {
                m.put(t, jdbc.queryForObject(
                        "SELECT COUNT(*) FROM SERV_MED_BITACORA_" + t, Integer.class));
            } catch (EmptyResultDataAccessException e) {
                m.put(t, 0);
            }
        }
        return m;
    }

    private static void setFecha(PreparedStatement ps, int pos, LocalDate f) throws java.sql.SQLException {
        if (f == null) {
            ps.setNull(pos, Types.DATE);
        } else {
            ps.setDate(pos, Date.valueOf(f));
        }
    }

    private static void setEntero(PreparedStatement ps, int pos, Integer v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(pos, Types.INTEGER);
        } else {
            ps.setInt(pos, v);
        }
    }
}
