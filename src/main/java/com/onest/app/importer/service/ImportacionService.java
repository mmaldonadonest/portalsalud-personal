package com.onest.app.importer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onest.app.catalog.file.repository.FsFileRepository;
import com.onest.app.catalog.file.storage.StorageProvider;
import com.onest.app.catalog.file.storage.StoredBinary;
import com.onest.app.catalog.predio.dto.PredioDto;
import com.onest.app.catalog.predio.service.PredioService;
import com.onest.app.importer.dto.ImportacionDto.FilaPreview;
import com.onest.app.importer.dto.ImportacionDto.Hoja;
import com.onest.app.importer.dto.ImportacionDto.Incidencia;
import com.onest.app.importer.dto.ImportacionDto.Lote;
import com.onest.app.importer.dto.ImportacionDto.Resultado;
import com.onest.app.importer.repository.ImportRepository;
import com.onest.app.importer.service.ExcelLector.ArchivoLeido;
import com.onest.app.importer.service.ExcelLector.FilaLeida;
import com.onest.app.importer.service.ExcelLector.HojaLeida;
import com.onest.app.importer.service.ImportValidador.Evaluacion;
import com.onest.app.importer.service.ImportValidador.ResultadoValidacion;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importador Excel (Analisis > Importar Excel). Orquesta los 6 pasos del prototipo:
 * leer + detectar hojas (ExcelLector), validar formato/catalogos y duplicados
 * (ImportValidador), vista previa, confirmar en transaccion y auditar (@Auditado en el
 * controller). Todo queda en staging (APP_IMPORT_LOTE/FILA) porque aun no hay layouts
 * destino; el binario se guarda en el filesystem via StorageProvider + APP_FS_FILE con
 * FILE_TYPE='importacion', versionado por nombre: nunca se pisa una carga anterior.
 */
@Service
public class ImportacionService {

    static final String FILE_TYPE = "importacion";
    private static final long MAX_BYTES = 25L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("xlsx", "xlsm", "csv");
    private static final int MUESTRA_POR_HOJA = 8;
    private static final int MAX_MENSAJES = 2000;
    private static final Logger log = LoggerFactory.getLogger(ImportacionService.class);

    private final ExcelLector lector;
    private final ImportValidador validador;
    private final ImportRepository repository;
    private final FsFileRepository fsFiles;
    private final StorageProvider storage;
    private final PredioService predioService;
    private final ObjectMapper json = new ObjectMapper();

    public ImportacionService(ExcelLector lector, ImportValidador validador, ImportRepository repository,
                              FsFileRepository fsFiles, StorageProvider storage, PredioService predioService) {
        this.lector = lector;
        this.validador = validador;
        this.repository = repository;
        this.fsFiles = fsFiles;
        this.storage = storage;
        this.predioService = predioService;
    }

    /** Pasos 1-4: guarda el archivo, lo lee, valida y deja el lote en VALIDADO con su vista previa. */
    @Transactional
    public Resultado cargar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo .xlsx, .xlsm o .csv");
        }
        exigirTablas();
        if (archivo.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo excede 25 MB");
        }
        String nombre = sanitize(archivo.getOriginalFilename());
        String ext = extensionOf(nombre);
        if (!EXTENSIONES.contains(ext)) {
            throw new IllegalArgumentException("Formato no aceptado: ." + ext + " (acepta .xlsx, .xlsm, .csv)");
        }
        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo", ex);
        }

        ArchivoLeido leido;
        try {
            leido = lector.leer(bytes, ext);
        } catch (IOException | RuntimeException ex) {
            throw new IllegalArgumentException("No se pudo interpretar el archivo (" + ex.getMessage() + ")");
        }
        if (leido.hojas().isEmpty()) {
            throw new IllegalArgumentException("El archivo no tiene ninguna hoja con encabezado y datos"
                    + (leido.hojasOmitidas().isEmpty() ? "" : " (omitidas: " + String.join(", ", leido.hojasOmitidas()) + ")"));
        }
        ResultadoValidacion val = validador.validar(leido, catalogoPredios());

        // --- versionado: el lote anterior con el mismo nombre queda SUSTITUIDO, nunca se borra
        String usuario = usuarioActual();
        Optional<Lote> anterior = repository.ultimaVersion(nombre);
        int version = anterior.map(l -> l.version() + 1).orElse(1);

        // --- binario al filesystem, misma tuberia que los PDF del expediente
        StoredBinary stored = storage.store(bytes, ext);
        long fsId = fsFiles.insert(null, UUID.randomUUID().toString(), nombre, ext, mimeDe(ext), stored.sizeBytes(),
                stored.checksumSha256(), stored.storagePath(), stored.storageProvider(), FILE_TYPE, usuario,
                null, fsFiles.siguienteVersion(FILE_TYPE, nombre));

        // --- hojas + filas
        List<Hoja> hojas = new ArrayList<>();
        List<Object[]> filasDb = new ArrayList<>();
        int procesados = 0, correctos = 0, advertencias = 0, rechazados = 0;
        for (HojaLeida h : leido.hojas()) {
            List<Evaluacion> evals = val.porHoja().get(h.nombre());
            int ok = 0, adv = 0, rech = 0;
            List<FilaPreview> muestra = new ArrayList<>();
            for (int i = 0; i < h.filas().size(); i++) {
                FilaLeida f = h.filas().get(i);
                Evaluacion e = evals.get(i);
                switch (e.estado()) {
                    case "RECHAZADO" -> rech++;
                    case "ADVERTENCIA" -> adv++;
                    default -> ok++;
                }
                String mensajes = e.mensajes().isEmpty() ? null : recorta(String.join(" · ", e.mensajes()), MAX_MENSAJES);
                filasDb.add(new Object[]{h.nombre(), f.numero(), e.estado(), mensajes, aJson(f.valores())});
                if (muestra.size() < MUESTRA_POR_HOJA) {
                    muestra.add(new FilaPreview(f.numero(), e.estado(), mensajes, f.valores()));
                }
            }
            procesados += h.filas().size();
            correctos += ok;
            advertencias += adv;
            rechazados += rech;
            hojas.add(new Hoja(h.nombre(), Boolean.TRUE.equals(val.reconocida().get(h.nombre())), h.filaEncabezado(),
                    h.encabezados().size(), h.filas().size(), ok, adv, rech, h.encabezados(), muestra));
        }

        boolean identico = anterior.isPresent() && stored.checksumSha256().equalsIgnoreCase(anterior.get().checksum());
        List<Incidencia> incidencias = new ArrayList<>(val.incidencias());
        if (identico) {
            incidencias.add(0, new Incidencia("Todas", "Archivo idéntico a la versión " + anterior.get().version()
                    + " ya cargada (mismo SHA-256)", "Bajo", "Se conserva como versión " + version + "; descartar si fue por error"));
        }

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("hojas", hojas);
        resumen.put("hojasOmitidas", leido.hojasOmitidas());
        resumen.put("incidencias", incidencias);
        resumen.put("sustituyeLoteId", anterior.map(Lote::id).orElse(null));
        resumen.put("identicoAlAnterior", identico);

        long loteId = repository.insertLote(fsId, nombre, ext, stored.sizeBytes(), stored.checksumSha256(), version,
                hojas.size(), procesados, correctos, advertencias, rechazados, aJson(resumen), usuario);
        repository.insertFilas(loteId, filasDb);
        anterior.ifPresent(l -> {
            if ("VALIDADO".equals(l.estado()) || "CONFIRMADO".equals(l.estado())) {
                repository.cambiarEstado(l.id(), "SUSTITUIDO");
            }
        });
        log.info("[importar] lote {} v{} '{}' hojas={} filas={} ok={} adv={} rech={} usuario={}",
                loteId, version, nombre, hojas.size(), procesados, correctos, advertencias, rechazados, usuario);

        Lote lote = repository.findLote(loteId).orElseThrow();
        return new Resultado(lote, hojas, leido.hojasOmitidas(), incidencias, anterior.orElse(null), identico);
    }

    /** Paso 5: confirma el lote (queda en staging; no hay tablas finales todavia). */
    @Transactional
    public Lote confirmar(long id) {
        Lote lote = repository.findLote(id).orElseThrow(() -> new IllegalArgumentException("Lote " + id + " no existe"));
        if (!"VALIDADO".equals(lote.estado())) {
            throw new IllegalArgumentException("Solo se confirma un lote VALIDADO (este está " + lote.estado() + ")");
        }
        repository.confirmar(id, usuarioActual());
        return repository.findLote(id).orElseThrow();
    }

    @Transactional
    public Lote descartar(long id) {
        Lote lote = repository.findLote(id).orElseThrow(() -> new IllegalArgumentException("Lote " + id + " no existe"));
        if ("CONFIRMADO".equals(lote.estado())) {
            throw new IllegalArgumentException("Un lote CONFIRMADO no se descarta");
        }
        repository.descartar(id);
        return repository.findLote(id).orElseThrow();
    }

    public List<Lote> historial() {
        return repository.disponible() ? repository.listar(50) : List.of();
    }

    static final String SIN_TABLAS = "Faltan las tablas APP_IMPORT_LOTE / APP_IMPORT_FILA en la base del portal: "
            + "aplicar db/sql/app_domain/app-import.sql y volver a intentar";

    private void exigirTablas() {
        if (!repository.disponible()) {
            throw new IllegalStateException(SIN_TABLAS);
        }
    }

    public Optional<Resultado> detalle(long id) {
        Optional<Lote> lote = repository.findLote(id);
        if (lote.isEmpty()) {
            return Optional.empty();
        }
        String resumen = repository.findResumen(id).orElse("{}");
        try {
            Map<String, Object> m = json.readValue(resumen, new TypeReference<>() { });
            List<Hoja> hojas = json.convertValue(m.getOrDefault("hojas", List.of()), new TypeReference<>() { });
            List<String> omitidas = json.convertValue(m.getOrDefault("hojasOmitidas", List.of()), new TypeReference<>() { });
            List<Incidencia> inc = json.convertValue(m.getOrDefault("incidencias", List.of()), new TypeReference<>() { });
            Object sust = m.get("sustituyeLoteId");
            Lote anterior = sust == null ? null : repository.findLote(((Number) sust).longValue()).orElse(null);
            return Optional.of(new Resultado(lote.get(), hojas, omitidas, inc, anterior,
                    Boolean.TRUE.equals(m.get("identicoAlAnterior"))));
        } catch (IOException | IllegalArgumentException ex) {
            return Optional.of(new Resultado(lote.get(), List.of(), List.of(), List.of(), null, false));
        }
    }

    /** Filas guardadas de una hoja, paginadas (para revisar mas alla de la muestra). */
    public List<FilaPreview> filas(long loteId, String hoja, int pagina, int porPagina) {
        int size = Math.max(1, Math.min(porPagina, 500));
        List<FilaPreview> out = new ArrayList<>();
        for (var f : repository.filas(loteId, hoja, Math.max(0, pagina) * size, size)) {
            Map<String, Object> valores;
            try {
                valores = json.readValue(f.valoresJson(), new TypeReference<LinkedHashMap<String, Object>>() { });
            } catch (IOException ex) {
                valores = Map.of();
            }
            out.add(new FilaPreview(f.numero(), f.estado(), f.mensajes(), valores));
        }
        return out;
    }

    private List<String> catalogoPredios() {
        try {
            return predioService.listarPredios().stream().map(PredioDto::nombre).toList();
        } catch (RuntimeException ex) {
            log.warn("[importar] sin catalogo de predios ({}), se omite esa validacion", ex.getMessage());
            return List.of();
        }
    }

    private String aJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar", ex);
        }
    }

    private static String usuarioActual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "SISTEMA" : a.getName();
    }

    private static String sanitize(String nombre) {
        String n = nombre == null ? "archivo" : nombre.replace("\\", "/");
        n = n.substring(n.lastIndexOf('/') + 1).trim();
        return n.isEmpty() ? "archivo" : (n.length() > 300 ? n.substring(n.length() - 300) : n);
    }

    private static String extensionOf(String nombre) {
        int i = nombre.lastIndexOf('.');
        return i < 0 ? "" : nombre.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String mimeDe(String ext) {
        return switch (ext) {
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xlsm" -> "application/vnd.ms-excel.sheet.macroEnabled.12";
            default -> "text/csv";
        };
    }

    private static String recorta(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
