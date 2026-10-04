package com.onest.app.catalog.file.etl;

import com.onest.app.catalog.file.repository.FsFileRepository;
import com.onest.app.catalog.file.storage.StorageProvider;
import com.onest.app.catalog.file.storage.StoredBinary;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Runner del ETL U09: migra {@code servicioMedico.files} (MariaDB legacy) a
 * filesystem + {@code SERV_MED_FS_FILE} (Oracle), llamando a {@link StorageProvider}
 * + {@link FsFileRepository} directo (NO {@code FileStoreService}: ese rechaza
 * >25MB y extensiones fuera de {pdf,png,jpg,jpeg,docx}; con el, cualquier
 * archivo legacy raro se cae en vez de migrarse - ver plan seccion 4).
 *
 * <p>Activar con {@code --spring.profiles.active=local,etl --etl.files.mode=sample|full}.
 * Con {@code mode=none} (default) no hace nada: es seguro tener el perfil activo.</p>
 */
@Component
@Profile("etl")
public class FileEtlRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FileEtlRunner.class);
    private static final String CREATED_BY = "ETL_LEGACY";

    private final LegacyFileReader reader;
    private final FsFileRepository repository;
    private final StorageProvider storage;
    private final String mode;
    private final int batchSize;

    public FileEtlRunner(LegacyFileReader reader, FsFileRepository repository, StorageProvider storage,
                          @Value("${etl.files.mode:none}") String mode,
                          @Value("${etl.files.batch-size:200}") int batchSize) {
        this.reader = reader;
        this.repository = repository;
        this.storage = storage;
        this.mode = mode;
        this.batchSize = batchSize;
    }

    @Override
    public void run(String... args) {
        switch (mode) {
            case "sample" -> runSample();
            case "full" -> runFull();
            default -> log.info("[etl-files] etl.files.mode={} - no se ejecuta nada. Usar 'sample' o 'full'.", mode);
        }
    }

    private void runSample() {
        List<Long> ids = reader.sampleIds();
        log.info("[etl-files] modo=sample, {} ids seleccionados: {}", ids.size(), ids);
        Set<String> yaMigradas = repository.businessKeysMigradas();
        Stats stats = new Stats();
        List<FsFileRepository.FilaEtl> buffer = new ArrayList<>();
        for (LegacyFileRow row : reader.findByIds(ids)) {
            procesar(row, yaMigradas, buffer, stats);
        }
        vaciar(buffer, stats);
        stats.log("sample");
    }

    private void runFull() {
        long minId = reader.minId();
        long maxId = reader.maxId();
        log.info("[etl-files] modo=full, rango de ids [{}, {}], lote={}", minId, maxId, batchSize);

        // UNA consulta en vez de 22 mil. Ver la nota de latencia en MedTagWriter: a 99 ms por
        // viaje, preguntar fila por fila cuesta 37 minutos solo en confirmar lo que ya sabemos.
        Set<String> yaMigradas = repository.businessKeysMigradas();
        log.info("[etl-files] ya migrados en el destino: {}", yaMigradas.size());

        Stats stats = new Stats();
        List<FsFileRepository.FilaEtl> buffer = new ArrayList<>(LOTE_INSERT);
        for (long from = minId; from <= maxId; from += batchSize) {
            long to = Math.min(from + batchSize - 1, maxId);
            List<LegacyFileRow> batch = reader.findByIdRange(from, to);
            for (LegacyFileRow row : batch) {
                procesar(row, yaMigradas, buffer, stats);
            }
            log.info("[etl-files] lote [{}, {}] listo - acumulado: {}", from, to, stats);
        }
        vaciar(buffer, stats);
        stats.log("full");
    }

    /**
     * Cuantos metadatos se mandan por viaje.
     *
     * <p>Mas chico que el de tags a proposito. El binario se escribe a disco ANTES de que su
     * fila entre a la base, asi que un corte deja huerfanos en disco: hasta {@code LOTE_INSERT}
     * de ellos. Con 100 el desperdicio maximo de un corte es despreciable y los roundtrips
     * bajan igual de 22,196 a 222.
     */
    private static final int LOTE_INSERT = 100;

    private void vaciar(List<FsFileRepository.FilaEtl> buffer, Stats stats) {
        if (buffer.isEmpty()) {
            return;
        }
        stats.migrado.addAndGet(repository.insertBatchEtl(buffer));
        buffer.clear();
    }

    private void procesar(LegacyFileRow row, Set<String> yaMigradas,
                          List<FsFileRepository.FilaEtl> buffer, Stats stats) {
        String businessKey = "legacy-" + row.id();
        if (yaMigradas.contains(businessKey)) {
            stats.yaExistia.incrementAndGet();
            return;
        }
        String base64 = row.urlBase64();
        if (base64 == null || base64.isBlank()) {
            log.warn("[etl-files] id={} sin url (huerfano) - se omite", row.id());
            stats.sinUrl.incrementAndGet();
            return;
        }
        byte[] content;
        try {
            content = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("[etl-files] id={} no decodifica como base64 valido - cuarentena: {}", row.id(), ex.getMessage());
            stats.cuarentena.incrementAndGet();
            return;
        }
        String extension = extensionOf(row.name());
        if (extension.isBlank()) {
            String sniffed = MagicBytes.sniffExtension(content);
            extension = sniffed != null ? sniffed : "bin";
        }
        String mime = mimeForExt(extension);
        LocalDateTime fechaAlta = row.dateUpload() != null ? row.dateUpload() : LocalDateTime.now();
        LocalDate fecha = fechaAlta.toLocalDate();

        // El binario PRIMERO, su fila despues. Si el proceso se corta, lo que queda son
        // binarios huerfanos en disco -inofensivo- y nunca una fila apuntando a un archivo
        // que no existe. Al insertar en lote ese margen crece a LOTE_INSERT archivos.
        StoredBinary stored = storage.store(content, extension, fecha);
        buffer.add(new FsFileRepository.FilaEtl(row.nss(), businessKey, sanitizeName(row.name()), extension, mime,
                stored.sizeBytes(), stored.checksumSha256(), stored.storagePath(), stored.storageProvider(),
                row.type(), CREATED_BY, fechaAlta));
        if (buffer.size() >= LOTE_INSERT) {
            vaciar(buffer, stats);
        }
    }

    /** Solo recorta longitud; el encoding se pasa tal cual (decision 2026-08-13, ver memoria u09-etl). */
    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "archivo.bin";
        }
        String base = name.replaceAll(".*[\\\\/]", "").trim();
        return base.length() > 300 ? base.substring(0, 300) : base;
    }

    private static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
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

    private static final class Stats {
        final AtomicInteger migrado = new AtomicInteger();
        final AtomicInteger yaExistia = new AtomicInteger();
        final AtomicInteger sinUrl = new AtomicInteger();
        final AtomicInteger cuarentena = new AtomicInteger();

        void log(String modo) {
            log.info("[etl-files] modo={} TERMINADO - {}", modo, this);
        }

        @Override
        public String toString() {
            return "migrado=" + migrado + " ya_existia=" + yaExistia
                    + " sin_url=" + sinUrl + " cuarentena=" + cuarentena;
        }
    }
}
