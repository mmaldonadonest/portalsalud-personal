package com.onest.app.importer.repository;

import com.onest.app.importer.dto.ImportacionDto.Lote;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * SERV_MED_IMPORT_LOTE / SERV_MED_IMPORT_FILA (db/sql/app_domain/app-import.sql). JdbcTemplate en vez
 * de JPA a proposito, igual que FsFileRepository: con ddl-auto=validate una entidad de una
 * tabla que aun no existe tumba el arranque; asi el portal levanta y solo falla este modulo
 * si el DDL no se ha aplicado.
 */
@Repository
public class ImportRepository {

    private static final RowMapper<Lote> LOTE = (rs, i) -> new Lote(
            rs.getLong("ID"), rs.getString("NOMBRE_ARCHIVO"), rs.getString("EXTENSION"), rs.getLong("SIZE_BYTES"),
            rs.getString("CHECKSUM_SHA256"), rs.getInt("VERSION"), rs.getString("ESTADO"), rs.getInt("HOJAS"),
            rs.getInt("PROCESADOS"), rs.getInt("CORRECTOS"), rs.getInt("ADVERTENCIAS"), rs.getInt("RECHAZADOS"),
            ts(rs.getTimestamp("CREATED_AT")), rs.getString("CREATED_BY"),
            ts(rs.getTimestamp("CONFIRMED_AT")), rs.getString("CONFIRMED_BY"),
            rs.getObject("FS_FILE_ID") == null ? null : rs.getLong("FS_FILE_ID"));

    private static final String COLS = "ID, NOMBRE_ARCHIVO, EXTENSION, SIZE_BYTES, CHECKSUM_SHA256, VERSION, ESTADO, HOJAS, "
            + "PROCESADOS, CORRECTOS, ADVERTENCIAS, RECHAZADOS, CREATED_AT, CREATED_BY, CONFIRMED_AT, CONFIRMED_BY, FS_FILE_ID";

    private final JdbcTemplate jdbc;

    public ImportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** false si el DDL (db/sql/app_domain/app-import.sql) no se ha aplicado todavia. */
    public boolean disponible() {
        try {
            jdbc.queryForObject("SELECT COUNT(*) FROM SERV_MED_IMPORT_LOTE WHERE ROWNUM = 0", Integer.class);
            jdbc.queryForObject("SELECT COUNT(*) FROM SERV_MED_IMPORT_FILA WHERE ROWNUM = 0", Integer.class);
            return true;
        } catch (org.springframework.dao.DataAccessException ex) {
            return false;
        }
    }

    public long insertLote(Long fsFileId, String nombre, String extension, long size, String checksum, int version,
                           int hojas, int procesados, int correctos, int advertencias, int rechazados,
                           String resumenJson, String usuario) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO SERV_MED_IMPORT_LOTE (FS_FILE_ID, NOMBRE_ARCHIVO, EXTENSION, SIZE_BYTES, CHECKSUM_SHA256, VERSION, "
                            + "ESTADO, HOJAS, PROCESADOS, CORRECTOS, ADVERTENCIAS, RECHAZADOS, RESUMEN_JSON, CREATED_BY) "
                            + "VALUES (?,?,?,?,?,?,'VALIDADO',?,?,?,?,?,?,?)", new String[]{"ID"});
            int i = 1;
            if (fsFileId == null) {
                ps.setNull(i++, java.sql.Types.NUMERIC);
            } else {
                ps.setLong(i++, fsFileId);
            }
            ps.setString(i++, nombre);
            ps.setString(i++, extension);
            ps.setLong(i++, size);
            ps.setString(i++, checksum);
            ps.setInt(i++, version);
            ps.setInt(i++, hojas);
            ps.setInt(i++, procesados);
            ps.setInt(i++, correctos);
            ps.setInt(i++, advertencias);
            ps.setInt(i++, rechazados);
            ps.setString(i++, resumenJson);
            ps.setString(i, usuario);
            return ps;
        }, kh);
        Number k = kh.getKey();
        return k == null ? -1L : k.longValue();
    }

    /** Inserta en bloques para no ir fila por fila: el reporte real trae ~1,500 renglones. */
    public void insertFilas(long loteId, List<Object[]> filas) {
        jdbc.batchUpdate("INSERT INTO SERV_MED_IMPORT_FILA (LOTE_ID, HOJA, FILA_NUM, ESTADO, MENSAJES, VALORES_JSON) VALUES (?,?,?,?,?,?)",
                filas, 500, (ps, f) -> {
                    ps.setLong(1, loteId);
                    ps.setString(2, (String) f[0]);
                    ps.setInt(3, (Integer) f[1]);
                    ps.setString(4, (String) f[2]);
                    ps.setString(5, (String) f[3]);
                    ps.setString(6, (String) f[4]);
                });
    }

    public Optional<Lote> findLote(long id) {
        return jdbc.query("SELECT " + COLS + " FROM SERV_MED_IMPORT_LOTE WHERE ID = ?", LOTE, id).stream().findFirst();
    }

    public Optional<String> findResumen(long id) {
        return jdbc.query("SELECT RESUMEN_JSON FROM SERV_MED_IMPORT_LOTE WHERE ID = ?", (rs, i) -> rs.getString(1), id)
                .stream().findFirst();
    }

    public List<Lote> listar(int limite) {
        return jdbc.query("SELECT " + COLS + " FROM (SELECT " + COLS + " FROM SERV_MED_IMPORT_LOTE ORDER BY ID DESC) WHERE ROWNUM <= ?",
                LOTE, limite);
    }

    /** Ultima version viva (no DESCARTADA) de un archivo por nombre. */
    public Optional<Lote> ultimaVersion(String nombreArchivo) {
        return jdbc.query("SELECT " + COLS + " FROM (SELECT " + COLS + " FROM SERV_MED_IMPORT_LOTE WHERE UPPER(NOMBRE_ARCHIVO) = UPPER(?) "
                + "AND ESTADO <> 'DESCARTADO' ORDER BY VERSION DESC, ID DESC) WHERE ROWNUM = 1", LOTE, nombreArchivo)
                .stream().findFirst();
    }

    public int cambiarEstado(long id, String estado) {
        return jdbc.update("UPDATE SERV_MED_IMPORT_LOTE SET ESTADO = ? WHERE ID = ?", estado, id);
    }

    public int confirmar(long id, String usuario) {
        return jdbc.update("UPDATE SERV_MED_IMPORT_LOTE SET ESTADO = 'CONFIRMADO', CONFIRMED_AT = ?, CONFIRMED_BY = ? "
                + "WHERE ID = ? AND ESTADO = 'VALIDADO'", Timestamp.valueOf(LocalDateTime.now()), usuario, id);
    }

    /** Descartar borra las filas (son staging) pero deja la cabecera con ESTADO=DESCARTADO para la historia. */
    public int descartar(long id) {
        jdbc.update("DELETE FROM SERV_MED_IMPORT_FILA WHERE LOTE_ID = ?", id);
        return jdbc.update("UPDATE SERV_MED_IMPORT_LOTE SET ESTADO = 'DESCARTADO' WHERE ID = ? AND ESTADO IN ('VALIDADO','SUSTITUIDO')", id);
    }

    public record FilaGuardada(String hoja, int numero, String estado, String mensajes, String valoresJson) {
    }

    public List<FilaGuardada> filas(long loteId, String hoja, int desde, int cuantas) {
        return jdbc.query("SELECT HOJA, FILA_NUM, ESTADO, MENSAJES, VALORES_JSON FROM ("
                        + "SELECT f.*, ROW_NUMBER() OVER (ORDER BY FILA_NUM) RN FROM SERV_MED_IMPORT_FILA f WHERE LOTE_ID = ? AND HOJA = ?) "
                        + "WHERE RN > ? AND RN <= ?",
                (rs, i) -> new FilaGuardada(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                loteId, hoja, desde, desde + cuantas);
    }

    private static String ts(Timestamp t) {
        return t == null ? null : t.toLocalDateTime().withNano(0).toString();
    }
}
