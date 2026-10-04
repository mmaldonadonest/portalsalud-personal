package com.onest.app.catalog.file.repository;

import com.onest.app.catalog.file.dto.MedFileMeta;
import com.onest.app.catalog.file.dto.StoredFileLocation;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Acceso JDBC a SERV_MED_FS_FILE (metadatos de archivos). El binario NO se guarda
 * aqui: vive en el filesystem y esta tabla solo referencia su STORAGE_PATH.
 */
@Repository
public class FsFileRepository {

    private static final Logger log = LoggerFactory.getLogger(FsFileRepository.class);

    private final JdbcTemplate jdbc;

    public FsFileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String nss, String businessKey, String originalName, String extension, String mimeType,
                       long sizeBytes, String checksumSha256, String storagePath, String storageProvider,
                       String fileType, String createdBy) {
        return insert(nss, businessKey, originalName, extension, mimeType, sizeBytes, checksumSha256, storagePath,
                storageProvider, fileType, createdBy, LocalDateTime.now());
    }

    /** Variante con fecha de alta explicita (migraciones historicas: {@code files.date_upload} del legacy). */
    public long insert(String nss, String businessKey, String originalName, String extension, String mimeType,
                       long sizeBytes, String checksumSha256, String storagePath, String storageProvider,
                       String fileType, String createdBy, LocalDateTime dateUpload) {
        return insert(nss, businessKey, originalName, extension, mimeType, sizeBytes, checksumSha256, storagePath,
                storageProvider, fileType, createdBy, dateUpload, 1);
    }

    /** Siguiente VERSION para un archivo del mismo nombre y tipo (importador Excel: se versiona, nunca se pisa). */
    public int siguienteVersion(String fileType, String originalName) {
        Integer max = jdbc.queryForObject(
                "SELECT COALESCE(MAX(VERSION), 0) FROM SERV_MED_FS_FILE WHERE FILE_TYPE = ? AND UPPER(ORIGINAL_NAME) = UPPER(?)",
                Integer.class, fileType, originalName);
        return (max == null ? 0 : max) + 1;
    }

    /** Variante con VERSION explicita (el importador Excel conserva todas las versiones de un mismo nombre). */
    public long insert(String nss, String businessKey, String originalName, String extension, String mimeType,
                       long sizeBytes, String checksumSha256, String storagePath, String storageProvider,
                       String fileType, String createdBy, LocalDateTime dateUpload, int version) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO SERV_MED_FS_FILE (NSS, BUSINESS_KEY, FILE_TYPE, ORIGINAL_NAME, EXTENSION, MIME_TYPE, "
                            + "SIZE_BYTES, CHECKSUM_SHA256, STORAGE_PATH, STORAGE_PROVIDER, STATUS, VERSION, "
                            + "DATE_UPLOAD, CREATED_BY) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?, 'ACTIVE', ?, ?, ?)",
                    new String[]{"ID"});
            int i = 1;
            ps.setString(i++, nss);
            ps.setString(i++, businessKey);
            ps.setString(i++, fileType);
            ps.setString(i++, originalName);
            ps.setString(i++, extension);
            ps.setString(i++, mimeType);
            ps.setLong(i++, sizeBytes);
            ps.setString(i++, checksumSha256);
            ps.setString(i++, storagePath);
            ps.setString(i++, storageProvider);
            ps.setInt(i++, version);
            ps.setTimestamp(i++, Timestamp.valueOf(dateUpload == null ? LocalDateTime.now() : dateUpload));
            ps.setString(i, createdBy);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    /** Para idempotencia del ETL legacy: evita reinsertar un {@code BUSINESS_KEY} ya migrado. */
    public boolean existsByBusinessKey(String businessKey) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SERV_MED_FS_FILE WHERE BUSINESS_KEY = ?", Integer.class, businessKey);
        return count != null && count > 0;
    }

    /**
     * Todas las {@code BUSINESS_KEY} ya migradas por el ETL, en <b>una sola consulta</b>.
     *
     * <p>Sustituye al {@link #existsByBusinessKey} por fila cuando hay que recorrer 22 mil
     * archivos. Con el destino de produccion a <b>99 ms de latencia</b> (medido el 1-oct-2026),
     * preguntar uno por uno son 22,196 viajes = <b>37 minutos</b> gastados solo en confirmar que
     * la tabla esta vacia. Asi es un viaje y 22 mil cadenas en memoria.
     *
     * <p>Acotado al prefijo {@code legacy-} a proposito: lo que escribe el portal en runtime no
     * interesa aqui y no tiene por que viajar.
     */
    public Set<String> businessKeysMigradas() {
        Set<String> claves = new HashSet<>(1 << 16);
        jdbc.setFetchSize(5000);
        try {
            jdbc.query("SELECT BUSINESS_KEY FROM SERV_MED_FS_FILE WHERE BUSINESS_KEY LIKE 'legacy-%'",
                    rs -> { claves.add(rs.getString(1)); });
        } finally {
            jdbc.setFetchSize(-1);
        }
        return claves;
    }

    /**
     * Inserta un lote de metadatos en un viaje. Para el ETL historico, donde no hace falta el
     * {@code ID} generado de cada fila.
     *
     * <p>Si el lote falla se reintenta fila por fila: Oracle aborta el batch entero por una sola
     * fila mala, y sin esto se perderian las buenas sin saber cual era el problema. Devuelve
     * cuantas entraron de verdad.
     */
    public int insertBatchEtl(List<FilaEtl> filas) {
        if (filas == null || filas.isEmpty()) {
            return 0;
        }
        try {
            jdbc.batchUpdate(INSERT_ETL_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    bind(ps, filas.get(i));
                }

                @Override
                public int getBatchSize() {
                    return filas.size();
                }
            });
            return filas.size();
        } catch (RuntimeException ex) {
            log.warn("[etl-files] el lote de {} fallo ({}). Se reintenta fila por fila para aislar.",
                    filas.size(), ex.getMessage());
            int ok = 0;
            for (FilaEtl f : filas) {
                try {
                    jdbc.update(con -> {
                        PreparedStatement ps = con.prepareStatement(INSERT_ETL_SQL);
                        bind(ps, f);
                        return ps;
                    });
                    ok++;
                } catch (RuntimeException fila) {
                    log.error("[etl-files] {} NO se pudo insertar: {}", f.businessKey(), fila.getMessage());
                }
            }
            return ok;
        }
    }

    private static final String INSERT_ETL_SQL =
            "INSERT INTO SERV_MED_FS_FILE (NSS, BUSINESS_KEY, FILE_TYPE, ORIGINAL_NAME, EXTENSION, MIME_TYPE, "
            + "SIZE_BYTES, CHECKSUM_SHA256, STORAGE_PATH, STORAGE_PROVIDER, STATUS, VERSION, "
            + "DATE_UPLOAD, CREATED_BY) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?, 'ACTIVE', 1, ?, ?)";

    private static void bind(PreparedStatement ps, FilaEtl f) throws SQLException {
        int i = 1;
        ps.setString(i++, f.nss());
        ps.setString(i++, f.businessKey());
        ps.setString(i++, f.fileType());
        ps.setString(i++, f.originalName());
        ps.setString(i++, f.extension());
        ps.setString(i++, f.mimeType());
        ps.setLong(i++, f.sizeBytes());
        ps.setString(i++, f.checksumSha256());
        ps.setString(i++, f.storagePath());
        ps.setString(i++, f.storageProvider());
        ps.setTimestamp(i++, Timestamp.valueOf(f.dateUpload() == null ? LocalDateTime.now() : f.dateUpload()));
        ps.setString(i, f.createdBy());
    }

    /** Una fila lista para insertar en lote. Mismo orden de campos que {@code INSERT_ETL_SQL}. */
    public record FilaEtl(String nss, String businessKey, String originalName, String extension, String mimeType,
                          long sizeBytes, String checksumSha256, String storagePath, String storageProvider,
                          String fileType, String createdBy, LocalDateTime dateUpload) {
    }

    /** Ubicacion (nombre + mime + ruta) para descargar un archivo activo. */
    public Optional<StoredFileLocation> findLocation(long id) {
        List<StoredFileLocation> list = jdbc.query(
                "SELECT ORIGINAL_NAME, MIME_TYPE, STORAGE_PATH FROM SERV_MED_FS_FILE WHERE ID = ? AND STATUS = 'ACTIVE'",
                (rs, i) -> new StoredFileLocation(rs.getString("ORIGINAL_NAME"), rs.getString("MIME_TYPE"),
                        rs.getString("STORAGE_PATH")),
                id);
        return list.stream().findFirst();
    }

    /** Ruta relativa del binario, para borrarlo del filesystem. */
    public Optional<String> findStoragePathById(long id) {
        List<String> list = jdbc.query(
                "SELECT STORAGE_PATH FROM SERV_MED_FS_FILE WHERE ID = ?",
                (rs, i) -> rs.getString("STORAGE_PATH"),
                id);
        return list.stream().findFirst();
    }

    public List<MedFileMeta> listByFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return List.of();
        }
        return jdbc.query(
                "SELECT ID, ORIGINAL_NAME, SIZE_BYTES, DATE_UPLOAD, MIME_TYPE FROM SERV_MED_FS_FILE "
                        + "WHERE FILE_TYPE = ? AND STATUS = 'ACTIVE' ORDER BY ID DESC",
                META_MAPPER,
                fileType);
    }

    public List<MedFileMeta> listByNssAndType(String nss, String fileType) {
        if (nss == null || nss.isBlank() || fileType == null || fileType.isBlank()) {
            return List.of();
        }
        return jdbc.query(
                "SELECT ID, ORIGINAL_NAME, SIZE_BYTES, DATE_UPLOAD, MIME_TYPE FROM SERV_MED_FS_FILE "
                        + "WHERE NSS = ? AND FILE_TYPE = ? AND STATUS = 'ACTIVE' ORDER BY ID DESC",
                META_MAPPER,
                nss.trim(), fileType);
    }

    public int deleteById(long id) {
        return jdbc.update("DELETE FROM SERV_MED_FS_FILE WHERE ID = ?", id);
    }

    private static final RowMapper<MedFileMeta> META_MAPPER = (rs, i) -> new MedFileMeta(
            rs.getLong("ID"),
            rs.getString("ORIGINAL_NAME"),
            rs.getLong("SIZE_BYTES"),
            rs.getTimestamp("DATE_UPLOAD") != null ? rs.getTimestamp("DATE_UPLOAD").toLocalDateTime() : null,
            rs.getString("MIME_TYPE"));
}
