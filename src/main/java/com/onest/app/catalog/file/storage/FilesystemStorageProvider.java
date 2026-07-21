package com.onest.app.catalog.file.storage;

import com.onest.app.catalog.file.config.PortalFilesProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de almacenamiento en filesystem (contrato fileManagementContract):
 * <ul>
 *   <li>raiz por entorno ({@code portal.files.root})</li>
 *   <li>sharding de directorios {@code yyyy/MM/dd/<2 del sha256>}</li>
 *   <li>nombre UUID (sin el nombre original) + extension</li>
 *   <li>escritura atomica temp + move</li>
 *   <li>checksum SHA-256 del contenido</li>
 * </ul>
 * El binario NO se guarda en base de datos; solo sus metadatos (APP_FS_FILE).
 */
@Component
public class FilesystemStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(FilesystemStorageProvider.class);
    private static final String PROVIDER = "FILESYSTEM";

    private final Path root;

    public FilesystemStorageProvider(PortalFilesProperties properties) {
        String configured = properties.root();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("portal.files.root no esta configurado");
        }
        this.root = Paths.get(configured).toAbsolutePath().normalize();
        log.info("[files] almacenamiento en filesystem, raiz={}", root);
    }

    @Override
    public StoredBinary store(byte[] content, String extension) {
        String sha = sha256Hex(content);
        LocalDate today = LocalDate.now();
        String suffix = (extension == null || extension.isBlank()) ? "" : "." + extension;
        String relativePath = String.join("/",
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                sha.substring(0, 2),
                uuid() + suffix);

        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, content);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el archivo en disco: " + relativePath, ex);
        }
        return new StoredBinary(relativePath, sha, content.length, PROVIDER);
    }

    @Override
    public byte[] read(String storagePath) {
        try {
            return Files.readAllBytes(resolve(storagePath));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo: " + storagePath, ex);
        }
    }

    @Override
    public boolean delete(String storagePath) {
        try {
            return Files.deleteIfExists(resolve(storagePath));
        } catch (IOException ex) {
            log.warn("[files] no se pudo borrar el binario {}: {}", storagePath, ex.getMessage());
            return false;
        }
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    /** Resuelve una ruta relativa contra la raiz, evitando path traversal. */
    private Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Ruta fuera de la raiz de almacenamiento: " + relativePath);
        }
        return resolved;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String sha256Hex(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
