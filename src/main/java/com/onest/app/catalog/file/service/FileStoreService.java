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
 * (SERV_MED_FS_FILE, via {@link FsFileRepository}) solo se guardan los METADATOS + la
 * ruta. Ya NO se guarda el binario en base de datos.
 */
@Service
public class FileStoreService {

    private static final long MAX_BYTES = 25L * 1024 * 1024;
    // Alineado con architecture-contract.json > filesystemRules.allowedMimeTypes
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "png", "jpg", "jpeg", "docx");
    // Laboratorio, Historico E.M. y notas: solo PDF, como uploadFile.php del legacy (16-oct-2026).
    // Las evidencias de consultas y accidentes siguen aceptando imagen y Word.
    private static final Set<String> TIPOS_SOLO_PDF = Set.of("laboratorio", "examen_medico", "nota_medica", "nota_incapacidad");

    // C-040 (18-nov-2026): la fotografia de maternidad es una foto, no un documento
    public static final String TIPO_FOTO_MATERNIDAD = "maternidad_foto";
    private static final Set<String> TIPOS_SOLO_IMAGEN = Set.of(TIPO_FOTO_MATERNIDAD);
    private static final Set<String> IMAGEN_EXT = Set.of("jpg", "jpeg", "png");

    private final FsFileRepository repository;
    private final StorageProvider storage;

    public FileStoreService(FsFileRepository repository, StorageProvider storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public int store(MultipartFile[] files, String nss, String relacion) {
        return store(files, nss, relacion, null);
    }

    /** Archivos que de verdad se van a guardar (los vacios se saltan), para calcular sus nombres antes. */
    public static int cuantos(MultipartFile[] files) {
        int n = 0;
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Extension (minusculas) de cada archivo no vacio, en orden. */
    public static List<String> extensiones(MultipartFile[] files) {
        List<String> r = new java.util.ArrayList<>();
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    r.add(extensionOf(sanitize(f.getOriginalFilename())));
                }
            }
        }
        return r;
    }

    /**
     * Igual que {@link #store(MultipartFile[], String, String)}, pero con el nombre con que queda cada archivo
     * (nomenclatura de SM), en el mismo orden que los archivos no vacios. null = su nombre original.
     */
    public int store(MultipartFile[] files, String nss, String relacion, List<String> nombres) {
        if (relacion == null || relacion.isBlank()) {
            throw new IllegalArgumentException("La relación (idArchivoRel) es obligatoria");
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
            if (TIPOS_SOLO_PDF.contains(relacion.trim()) && !"pdf".equals(extension)) {
                throw new IllegalArgumentException("Aquí solo se aceptan archivos PDF: " + originalName);
            }
            if (TIPOS_SOLO_IMAGEN.contains(relacion.trim()) && !IMAGEN_EXT.contains(extension)) {
                throw new IllegalArgumentException("Aquí solo se aceptan fotografías (JPG o PNG): " + originalName);
            }
            String mime = file.getContentType() != null ? file.getContentType() : mimeForExt(extension);
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo leer el archivo: " + originalName, ex);
            }

            if (nombres != null && count < nombres.size()) {
                originalName = nombres.get(count);
            }
            StoredBinary stored = storage.store(bytes, extension);
            repository.insert(nss, UUID.randomUUID().toString(), originalName, extension, mime,
                    stored.sizeBytes(), stored.checksumSha256(), stored.storagePath(), stored.storageProvider(),
                    relacion.trim(), usuario);
            count++;
        }
        return count;
    }

    /**
     * C-008: guarda un PDF que genero el propio portal (el impreso del examen), sin pasar por la subida del
     * navegador. Regresa el id del archivo.
     */
    public long storePdfGenerado(byte[] pdf, String nss, String relacion, String nombre) {
        if (pdf == null || pdf.length == 0) {
            throw new IllegalArgumentException("El PDF está vacío.");
        }
        StoredBinary stored = storage.store(pdf, "pdf");
        return repository.insert(nss, UUID.randomUUID().toString(), nombre, "pdf", "application/pdf",
                stored.sizeBytes(), stored.checksumSha256(), stored.storagePath(), stored.storageProvider(),
                relacion, usuarioActual());
    }

    public List<FsFileRepository.ConTipo> listByNssAndTypePrefix(String nss, String prefijo) {
        return repository.listByNssAndTypePrefix(nss, prefijo);
    }

    /** Borrado logico de un archivo que el portal reemplaza (no lo pide el usuario). */
    public void reemplazado(long id) {
        repository.markDeleted(id, usuarioActual());
    }

    public List<MedFileMeta> listByRelacion(String relacion) {
        return repository.listByFileType(relacion);
    }

    public List<MedFileMeta> listByNssAndType(String nss, String type) {
        return repository.listByNssAndType(nss, type);
    }

    /**
     * El archivo es del NSS del expediente abierto (16-oct-2026). Antes bastaba el id: cambiando el
     * numero en la direccion se abria o borraba el archivo de otro empleado (el legacy igual).
     * Un archivo sin NSS (no tiene dueño) se permite para no romper adjuntos viejos.
     */
    private boolean esDelNss(long id, String nss) {
        String pedido = nss == null ? "" : nss.trim();
        return !pedido.isEmpty() && repository.findNssActivo(id)
                .map(dueno -> dueno.isEmpty() || dueno.equals(pedido))
                .orElse(false);
    }

    /** Borrado logico: el archivo deja de verse pero se conserva (ver FsFileRepository.markDeleted). */
    public boolean delete(long id, String nss) {
        return esDelNss(id, nss) && repository.markDeleted(id, usuarioActual()) > 0;
    }

    /** Lee el binario desde el filesystem para servirlo (descarga/inline), solo si es del NSS. */
    public Optional<MedFileContent> get(long id, String nss) {
        if (!esDelNss(id, nss)) {
            return Optional.empty();
        }
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
