package com.onest.app.catalog.file.repository;

import com.onest.app.catalog.file.dto.MedFileMeta;
import com.onest.app.catalog.file.dto.StoredFileLocation;
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
 * Acceso JDBC a APP_FS_FILE (metadatos de archivos). El binario NO se guarda
 * aqui: vive en el filesystem y esta tabla solo referencia su STORAGE_PATH.
 */
@Repository
public class FsFileRepository {

    private final JdbcTemplate jdbc;

    public FsFileRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String nss, String businessKey, String originalName, String extension, String mimeType,
                       long sizeBytes, String checksumSha256, String storagePath, String storageProvider,
                       String fileType, String createdBy) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO APP_FS_FILE (NSS, BUSINESS_KEY, FILE_TYPE, ORIGINAL_NAME, EXTENSION, MIME_TYPE, "
                            + "SIZE_BYTES, CHECKSUM_SHA256, STORAGE_PATH, STORAGE_PROVIDER, STATUS, VERSION, "
                            + "DATE_UPLOAD, CREATED_BY) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?, 'ACTIVE', 1, ?, ?)",
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
            ps.setTimestamp(i++, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(i, createdBy);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    /** Ubicacion (nombre + mime + ruta) para descargar un archivo activo. */
    public Optional<StoredFileLocation> findLocation(long id) {
        List<StoredFileLocation> list = jdbc.query(
                "SELECT ORIGINAL_NAME, MIME_TYPE, STORAGE_PATH FROM APP_FS_FILE WHERE ID = ? AND STATUS = 'ACTIVE'",
                (rs, i) -> new StoredFileLocation(rs.getString("ORIGINAL_NAME"), rs.getString("MIME_TYPE"),
                        rs.getString("STORAGE_PATH")),
                id);
        return list.stream().findFirst();
    }

    /** Ruta relativa del binario, para borrarlo del filesystem. */
    public Optional<String> findStoragePathById(long id) {
        List<String> list = jdbc.query(
                "SELECT STORAGE_PATH FROM APP_FS_FILE WHERE ID = ?",
                (rs, i) -> rs.getString("STORAGE_PATH"),
                id);
        return list.stream().findFirst();
    }

    public List<MedFileMeta> listByFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return List.of();
        }
        return jdbc.query(
                "SELECT ID, ORIGINAL_NAME, SIZE_BYTES, DATE_UPLOAD, MIME_TYPE FROM APP_FS_FILE "
                        + "WHERE FILE_TYPE = ? AND STATUS = 'ACTIVE' ORDER BY ID DESC",
                META_MAPPER,
                fileType);
    }

    public List<MedFileMeta> listByNssAndType(String nss, String fileType) {
        if (nss == null || nss.isBlank() || fileType == null || fileType.isBlank()) {
            return List.of();
        }
        return jdbc.query(
                "SELECT ID, ORIGINAL_NAME, SIZE_BYTES, DATE_UPLOAD, MIME_TYPE FROM APP_FS_FILE "
                        + "WHERE NSS = ? AND FILE_TYPE = ? AND STATUS = 'ACTIVE' ORDER BY ID DESC",
                META_MAPPER,
                nss.trim(), fileType);
    }

    public int deleteById(long id) {
        return jdbc.update("DELETE FROM APP_FS_FILE WHERE ID = ?", id);
    }

    private static final RowMapper<MedFileMeta> META_MAPPER = (rs, i) -> new MedFileMeta(
            rs.getLong("ID"),
            rs.getString("ORIGINAL_NAME"),
            rs.getLong("SIZE_BYTES"),
            rs.getTimestamp("DATE_UPLOAD") != null ? rs.getTimestamp("DATE_UPLOAD").toLocalDateTime() : null,
            rs.getString("MIME_TYPE"));
}
