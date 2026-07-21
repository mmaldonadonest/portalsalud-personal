package com.onest.app.catalog.file.service;

import com.onest.app.catalog.file.dto.MedFileContent;
import com.onest.app.catalog.file.dto.MedFileMeta;
import com.onest.app.catalog.file.repository.FsFileRepository;
import com.onest.app.catalog.file.storage.StorageProvider;
import com.onest.app.catalog.file.storage.StoredBinary;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Almacen de adjuntos del expediente. Modelo de arquitectura acordado:
 * el BINARIO va al filesystem ({@link StorageProvider}) y en la BD
 * (APP_FS_FILE, via {@link FsFileRepository}) solo se guardan los METADATOS + la
 * ruta. Ya NO se guarda el binario en base de datos.
 */
@Service
public class FileStoreService {

    private static final long MAX_BYTES = 25L * 1024 * 1024;
    // Alineado con architecture-contract.json > filesystemRules.allowedMimeTypes
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "png", "jpg", "jpeg", "docx");

    private final FsFileRepository repository;
    private final StorageProvider storage;

    public FileStoreService(FsFileRepository repository, StorageProvider storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public int store(MultipartFile[] files, String nss, String relacion) {
        if (relacion == null || relacion.isBlank()) {
            throw new IllegalArgumentException("La relacion (idArchivoRel) es obligatoria");
        }
        if (files == null) {
            return 0;
        }
        String usuario = usuarioActual();
        int count = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_BYTES) {
                throw new IllegalArgumentException("El archivo excede 25MB: " + file.getOriginalFilename());
            }
            String originalName = sanitize(file.getOriginalFilename());
            String extension = extensionOf(originalName);
            if (!ALLOWED_EXT.contains(extension)) {
                throw new IllegalArgumentException("Tipo de archivo no permitido: " + originalName);
            }
            String mime = file.getContentType() != null ? file.getContentType() : mimeForExt(extension);
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo leer el archivo: " + originalName, ex);
            }

            StoredBinary stored = storage.store(bytes, extension);
            repository.insert(nss, UUID.randomUUID().toString(), originalName, extension, mime,
                    stored.sizeBytes(), stored.checksumSha256(), stored.storagePath(), stored.storageProvider(),
                    relacion.trim(), usuario);
            count++;
        }
        return count;
    }

    public List<MedFileMeta> listByRelacion(String relacion) {
        return repository.listByFileType(relacion);
    }

    public List<MedFileMeta> listByNssAndType(String nss, String type) {
        return repository.listByNssAndType(nss, type);
    }

    public boolean delete(long id) {
        Optional<String> storagePath = repository.findStoragePathById(id);
        int rows = repository.deleteById(id);
        storagePath.ifPresent(storage::delete);
        return rows > 0;
    }

    /** Lee el binario desde el filesystem para servirlo (descarga/inline). */
    public Optional<MedFileContent> get(long id) {
        return repository.findLocation(id)
                .map(loc -> new MedFileContent(loc.name(), loc.mimeType(), storage.read(loc.storagePath())));
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "archivo.pdf";
        }
        // solo nombre base, sin rutas
        String base = name.replaceAll(".*[\\\\/]", "").trim();
        return base.length() > 300 ? base.substring(0, 300) : base;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot + 1).toLowerCase() : "";
    }

    private static String mimeForExt(String ext) {
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private String usuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "SISTEMA";
        }
        return authentication.getName();
    }
}
