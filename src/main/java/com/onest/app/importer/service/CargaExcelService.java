package com.onest.app.importer.service;

import com.onest.app.catalog.file.repository.FsFileRepository;
import com.onest.app.catalog.file.dto.StoredFileLocation;
import com.onest.app.catalog.file.storage.StorageProvider;
import com.onest.app.catalog.file.storage.StoredBinary;
import com.onest.app.importer.dto.CargaExcelDto.Escritura;
import com.onest.app.importer.dto.CargaExcelDto.Fila;
import com.onest.app.importer.dto.CargaExcelDto.Resumen;
import com.onest.app.importer.dto.CargaExcelDto.Trabajo;
import com.onest.app.importer.nss.BiometricoNss;
import com.onest.app.importer.nss.CargaNssService;
import com.onest.app.importer.nss.CargaNssService.ResultadoArchivo;
import com.onest.app.importer.nss.CorreccionesNss;
import com.onest.app.importer.nss.LayoutNss;
import com.onest.app.importer.nss.ValidadorNss;
import com.onest.app.importer.nss.XlsNssRepository;
import com.onest.app.importer.nss.XlsNssRepository.LoteResumen;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga de Excel por pantalla (docs/plan-carga-excel-pantalla.md). Envuelve la MISMA logica del
 * cargador de consola (paquete importer.nss): reconoce el layout por encabezados, valida contra
 * catalogos y biometrico, y al confirmar escribe en SERV_MED_XLS_* con reemplazo por predio y mes.
 *
 * <p>Por que en segundo plano: un archivo como Primer1 ATENCIONES (16,866 renglones) cruza el
 * biometrico y tarda mas que el tiempo de espera de nginx; la pantalla sube, recibe un id y consulta
 * el avance. Un solo hilo para todos: dos cargas nunca corren a la vez, asi el reemplazo por predio y
 * mes de una no pisa a la otra a la mitad.
 *
 * <p>Confirmar vuelve a procesar el mismo archivo (guardado en una carpeta temporal) en vez de
 * guardar renglones intermedios: no hace falta tabla de staging y lo que se escribe es exactamente
 * lo que el cargador valida en ese momento. El original se guarda en el filesystem
 * (SERV_MED_FS_FILE, FILE_TYPE=carga-excel, BUSINESS_KEY=XLS_LOTE:id) solo cuando se carga.
 */
@Service
public class CargaExcelService {

    public static final String FILE_TYPE = "carga-excel";
    private static final long MAX_BYTES = 25L * 1024 * 1024;
    private static final String MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    /** Renglones de errores/avisos que viajan a la pantalla; el Excel de errores lleva todos. */
    private static final int MAX_FILAS_PANTALLA = 300;
    /** Un trabajo sin confirmar se olvida pasado este tiempo (y se borra su carpeta temporal). */
    private static final Duration VIGENCIA = Duration.ofHours(8);
    private static final Logger log = LoggerFactory.getLogger(CargaExcelService.class);

    private final BiometricoNss biometrico;
    private final XlsNssRepository repo;
    private final FsFileRepository fsFiles;
    private final StorageProvider storage;
    private final Map<String, Estado> trabajos = new ConcurrentHashMap<>();
    private final ExecutorService hilo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "carga-excel");
        t.setDaemon(true);
        return t;
    });

    public CargaExcelService(BiometricoNss biometrico, XlsNssRepository repo, FsFileRepository fsFiles,
                             StorageProvider storage) {
        this.biometrico = biometrico;
        this.repo = repo;
        this.fsFiles = fsFiles;
        this.storage = storage;
    }

    /** Lo que se sabe de un trabajo; mutable, solo lo cambia el hilo de carga (y descartar). */
    private static final class Estado {
        final String id = UUID.randomUUID().toString();
        final String usuario;
        final Path carpeta;
        final Path archivo;
        final Path correcciones;
        final LayoutNss tipo;          // lo que eligio el usuario; el archivo debe ser de ese layout
        final LocalDateTime creado = LocalDateTime.now();
        volatile String estado = "EN_COLA";
        volatile String mensaje;
        volatile ResultadoArchivo validacion;
        volatile ResultadoArchivo carga;

        Estado(String usuario, Path carpeta, Path archivo, Path correcciones, LayoutNss tipo) {
            this.usuario = usuario;
            this.tipo = tipo;
            this.carpeta = carpeta;
            this.archivo = archivo;
            this.correcciones = correcciones;
        }
    }

    /** Sube el Excel (y, si viene, el de correcciones de NSS) y lo manda validar. No escribe en la base. */
    public Trabajo validar(MultipartFile excel, MultipartFile correcciones, String tipo, String usuario) {
        LayoutNss layout = tipoElegido(tipo);
        exigirXlsx(excel, "el Excel a cargar");
        if (correcciones != null && !correcciones.isEmpty()) {
            exigirXlsx(correcciones, "el Excel de correcciones");
        }
        Estado e;
        try {
            Path carpeta = Files.createTempDirectory("carga-excel-");
            // El nombre original se conserva: el cargador lo usa en sus avisos y en SERV_MED_XLS_LOTE.ARCHIVO
            Path archivo = carpeta.resolve(nombreSeguro(excel.getOriginalFilename()));
            excel.transferTo(archivo);
            Path corr = null;
            if (correcciones != null && !correcciones.isEmpty()) {
                Path sub = Files.createDirectory(carpeta.resolve("correcciones"));
                corr = sub.resolve(nombreSeguro(correcciones.getOriginalFilename()));
                correcciones.transferTo(corr);
            }
            e = new Estado(usuario, carpeta, archivo, corr, layout);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el archivo para procesarlo", ex);
        }
        trabajos.put(e.id, e);
        log.info("[carga-excel] {} validar '{}' tipo={}{} usuario={}", e.id, e.archivo.getFileName(), layout,
                e.correcciones == null ? "" : " con correcciones", usuario);
        hilo.submit(() -> correr(e, false, false));
        return aDto(e);
    }

    /** Carga en SERV_MED_XLS_* lo ya validado. {@code forzar}: recargar un archivo identico a un lote previo. */
    public Trabajo confirmar(String id, String usuario, boolean forzar) {
        Estado e = propio(id, usuario);
        // Reintento: la carga se omitio porque el mismo archivo ya estaba cargado y el usuario pide forzarla
        boolean reintento = forzar && "CARGADO".equals(e.estado) && e.carga != null && e.carga.omitido();
        if (!(reintento || "VALIDADO".equals(e.estado)) || e.validacion == null || e.validacion.motivoRechazo() != null) {
            throw new IllegalArgumentException("Solo se carga un archivo ya validado y reconocido (este está "
                    + e.estado + ")");
        }
        if (e.validacion.validas() == 0) {
            throw new IllegalArgumentException("El archivo no tiene ningún renglón válido: no hay nada que cargar");
        }
        e.estado = "EN_COLA";
        e.mensaje = null;
        e.carga = null;
        log.info("[carga-excel] {} confirmar '{}' forzar={} usuario={}", e.id, e.archivo.getFileName(), forzar, usuario);
        hilo.submit(() -> correr(e, true, forzar));
        return aDto(e);
    }

    public Trabajo consultar(String id, String usuario) {
        return aDto(propio(id, usuario));
    }

    public void descartar(String id, String usuario) {
        Estado e = propio(id, usuario);
        if ("CARGANDO".equals(e.estado)) {
            throw new IllegalArgumentException("El archivo se está cargando; espera a que termine");
        }
        trabajos.remove(id);
        borrar(e.carpeta);
    }

    /** Excel con TODOS los renglones rechazados (ERROR) y los que entran con aviso (AVISO). */
    public byte[] excelErrores(String id, String usuario) {
        Estado e = propio(id, usuario);
        ResultadoArchivo r = e.carga != null ? e.carga : e.validacion;
        if (r == null) {
            throw new IllegalArgumentException("Todavía no hay resultado");
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle enc = wb.createCellStyle();
            Font negrita = wb.createFont();
            negrita.setBold(true);
            negrita.setColor(IndexedColors.WHITE.getIndex());
            enc.setFont(negrita);
            enc.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            enc.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet s = wb.createSheet("Errores");
            String[] titulos = {"Renglón del Excel", "Resultado", "Columna", "Lo que trae", "Qué pasó", "Baja del empleado"};
            Row h = s.createRow(0);
            for (int i = 0; i < titulos.length; i++) {
                h.createCell(i).setCellValue(titulos[i]);
                h.getCell(i).setCellStyle(enc);
            }
            int n = 1;
            for (Fila f : filas(r, Integer.MAX_VALUE)) {
                Row row = s.createRow(n++);
                row.createCell(0).setCellValue(f.renglon());
                row.createCell(1).setCellValue("ERROR".equals(f.tipo()) ? "No entra"
                        : nvl(f.motivo()).startsWith(ValidadorNss.ENTRO_COMO) ? "Entró" : "Entra con ajuste");
                row.createCell(2).setCellValue(nvl(f.columna()));
                row.createCell(3).setCellValue(nvl(f.valor()));
                row.createCell(4).setCellValue(nvl(f.motivo()));
                row.createCell(5).setCellValue(nvl(f.baja()));
            }
            int[] anchos = {18, 10, 28, 34, 70, 22};
            for (int i = 0; i < anchos.length; i++) {
                s.setColumnWidth(i, anchos[i] * 256);
            }
            s.createFreezePane(0, 1);
            s.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, n - 1), 0, titulos.length - 1));

            Sheet a = wb.createSheet("Resumen");
            int k = 0;
            a.createRow(k++).createCell(0).setCellValue("Archivo: " + e.archivo.getFileName());
            a.createRow(k++).createCell(0).setCellValue("Layout: " + (r.layout() == null ? "no reconocido" : r.layout().hoja()));
            a.createRow(k++).createCell(0).setCellValue("Renglones leídos " + r.leidas() + " | válidos " + r.validas());
            if (r.motivoRechazo() != null) {
                a.createRow(k++).createCell(0).setCellValue("Rechazado: " + r.motivoRechazo());
            }
            for (String aviso : r.avisos()) {
                a.createRow(k++).createCell(0).setCellValue(aviso);
            }
            a.setColumnWidth(0, 140 * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo armar el Excel de errores", ex);
        }
    }

    public String nombreExcelErrores(String id, String usuario) {
        return propio(id, usuario).archivo.getFileName().toString().replaceFirst("(?i)\\.xlsx$", "") + ".errores.xlsx";
    }

    public List<LoteResumen> historial() {
        return repo.ultimosLotes(50, FILE_TYPE);
    }

    /** El original de un lote cargado desde la pantalla. */
    public StoredFileLocation original(long archivoId) {
        // Solo archivos de esta pantalla: el id es de SERV_MED_FS_FILE, donde tambien viven los PDF del expediente
        if (!repo.esArchivoDeCarga(archivoId, FILE_TYPE)) {
            throw new IllegalArgumentException("El archivo no existe");
        }
        return fsFiles.findLocation(archivoId).orElseThrow(() -> new IllegalArgumentException("El archivo no existe"));
    }

    public byte[] leer(StoredFileLocation f) {
        return storage.read(f.storagePath());
    }

    // ------------------------------------------------------------------ hilo de carga

    private void correr(Estado e, boolean escribir, boolean forzar) {
        e.estado = escribir ? "CARGANDO" : "VALIDANDO";
        try {
            CorreccionesNss corr = e.correcciones == null ? null : CorreccionesNss.leer(e.correcciones);
            // Salida = carpeta del trabajo: ahi deja el cargador su CSV de errores (no se usa, la
            // pantalla arma el suyo en Excel) y no ensucia nada fuera de la carpeta temporal
            CargaNssService cargador = new CargaNssService(biometrico, repo, e.carpeta, corr).soloLayout(e.tipo);
            ResultadoArchivo r = cargador.procesar(e.archivo, escribir, forzar, e.usuario);
            if (escribir) {
                e.carga = r;
                if (r.escritura() != null) {
                    guardarOriginal(e, r.escritura().loteId());
                }
                e.estado = "CARGADO";
            } else {
                e.validacion = r;
                e.estado = "VALIDADO";
            }
            log.info("[carga-excel] {} {} '{}' layout={} leidas={} validas={} lote={}", e.id, e.estado,
                    e.archivo.getFileName(), r.layout(), r.leidas(), r.validas(),
                    r.escritura() == null ? "-" : r.escritura().loteId());
        } catch (Exception ex) {
            log.error("[carga-excel] {} fallo procesando '{}'", e.id, e.archivo.getFileName(), ex);
            e.estado = "ERROR";
            e.mensaje = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        }
    }

    /**
     * El original (y el de correcciones) al filesystem, ligado al lote. Si falla, la carga ya esta
     * hecha y no se revierte: se avisa en pantalla.
     */
    private void guardarOriginal(Estado e, long loteId) {
        try {
            guardar(e.archivo, "XLS_LOTE:" + loteId, e.usuario);
            if (e.correcciones != null) {
                guardar(e.correcciones, "XLS_LOTE:" + loteId + ":CORRECCIONES", e.usuario);
            }
        } catch (Exception ex) {
            log.warn("[carga-excel] lote {} cargado pero sin guardar el original: {}", loteId, ex.getMessage());
            e.mensaje = "Se cargó el lote " + loteId + ", pero no se pudo guardar una copia del archivo original: "
                    + ex.getMessage();
        }
    }

    private void guardar(Path archivo, String llave, String usuario) throws IOException {
        byte[] bytes = Files.readAllBytes(archivo);
        String nombre = archivo.getFileName().toString();
        StoredBinary b = storage.store(bytes, "xlsx");
        fsFiles.insert(null, llave, nombre, "xlsx", MIME_XLSX, b.sizeBytes(), b.checksumSha256(), b.storagePath(),
                b.storageProvider(), FILE_TYPE, usuario, LocalDateTime.now(), fsFiles.siguienteVersion(FILE_TYPE, nombre));
    }

    /** Trabajos viejos sin confirmar: se olvidan y se borra su carpeta temporal. */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 3_600_000)
    void limpiar() {
        LocalDateTime limite = LocalDateTime.now().minus(VIGENCIA);
        trabajos.values().removeIf(e -> {
            boolean viejo = e.creado.isBefore(limite) && !"CARGANDO".equals(e.estado) && !"VALIDANDO".equals(e.estado);
            if (viejo) {
                borrar(e.carpeta);
            }
            return viejo;
        });
    }

    @PreDestroy
    void detener() {
        hilo.shutdownNow();
    }

    // ------------------------------------------------------------------ apoyo

    private Estado propio(String id, String usuario) {
        Estado e = trabajos.get(id);
        // De otro usuario = como si no existiera: el resultado trae valores de los renglones
        if (e == null || !e.usuario.equals(usuario)) {
            throw new IllegalArgumentException("El trabajo no existe o ya venció; vuelve a subir el archivo");
        }
        return e;
    }

    private Trabajo aDto(Estado e) {
        return new Trabajo(e.id, e.estado, e.mensaje, e.archivo.getFileName().toString(),
                e.correcciones == null ? null : e.correcciones.getFileName().toString(), e.creado,
                resumen(e.validacion), resumen(e.carga));
    }

    private static Resumen resumen(ResultadoArchivo r) {
        if (r == null) {
            return null;
        }
        int conError = (int) r.errores().stream().map(ValidadorNss.Error::fila).distinct().count();
        // % aceptado sobre renglones leidos: con el biometrico, un renglon puede salir de validas despues
        double pct = r.leidas() == 0 ? 0 : Math.round(r.validas() * 1000.0 / r.leidas()) / 10.0;
        List<Fila> errores = agrupados(r.errores()).stream().limit(MAX_FILAS_PANTALLA).map(x -> fila(x, "ERROR", r)).toList();
        List<Fila> avisos = r.avisosFila().stream().limit(MAX_FILAS_PANTALLA).map(x -> fila(x, "AVISO", r)).toList();
        Escritura esc = r.escritura() == null ? null : new Escritura(r.escritura().loteId(), r.escritura().insertados(),
                r.escritura().reemplazados(), r.escritura().duplicados(), r.escritura().avisoDuplicados());
        return new Resumen(r.layout() == null ? null : r.layout().hoja(), r.motivoRechazo(), r.leidas(), r.validas(),
                conError, pct, r.avisos(), errores, r.errores().size(), avisos, r.avisosFila().size(),
                r.reemplazos(), esc, r.omitido());
    }

    private static List<Fila> filas(ResultadoArchivo r, int max) {
        // Primero los que no entran (agrupados igual que en pantalla), despues los ajustes por renglon
        List<Fila> todas = new ArrayList<>();
        agrupados(r.errores()).forEach(x -> todas.add(fila(x, "ERROR", r)));
        r.avisosFila().stream().sorted(Comparator.comparingInt(ValidadorNss.Error::fila))
                .forEach(x -> todas.add(fila(x, "AVISO", r)));
        return todas.size() > max ? todas.subList(0, max) : todas;
    }

    /**
     * Errores agrupados por columna: NSS primero (lo que mas se corrige: el NSS no existe en el
     * biometrico), luego las demas columnas de la que tiene mas errores a la que tiene menos; dentro
     * de cada columna, por renglon del Excel. Pedido del usuario el 3-nov-2026.
     */
    private static List<ValidadorNss.Error> agrupados(List<ValidadorNss.Error> errores) {
        Map<String, Long> porColumna = new HashMap<>();
        errores.forEach(e -> porColumna.merge(nvl(e.encabezado()), 1L, Long::sum));
        return errores.stream().sorted(Comparator
                        .comparing((ValidadorNss.Error e) -> !"NSS".equals(nvl(e.encabezado())))
                        .thenComparing(e -> -porColumna.get(nvl(e.encabezado())))
                        .thenComparing(e -> nvl(e.encabezado()))
                        .thenComparingInt(ValidadorNss.Error::fila))
                .toList();
    }

    private static Fila fila(ValidadorNss.Error x, String tipo, ResultadoArchivo r) {
        return new Fila(x.fila(), tipo, x.encabezado(), x.valor(), x.motivo(), r.bajas().get(x.fila()));
    }

    /** Tipos de archivo que se pueden elegir en la pantalla (nombre del enum + nombre del layout). */
    public static List<Map<String, String>> tipos() {
        return java.util.Arrays.stream(LayoutNss.values())
                .map(l -> Map.of("clave", l.name(), "nombre", l.hoja())).toList();
    }

    private static LayoutNss tipoElegido(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Elige el tipo de archivo que vas a cargar");
        }
        try {
            return LayoutNss.valueOf(tipo.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de archivo desconocido: " + tipo);
        }
    }

    private static void exigirXlsx(MultipartFile f, String que) {
        if (f == null || f.isEmpty()) {
            throw new IllegalArgumentException("Selecciona " + que + " (.xlsx)");
        }
        if (f.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(que + " excede 25 MB");
        }
        String n = f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase();
        // El lector es el de la consola (POI por eventos, solo .xlsx): un .xls viejo no se puede leer
        if (!n.endsWith(".xlsx")) {
            throw new IllegalArgumentException(que + " debe ser .xlsx (en Excel: Guardar como > Libro de Excel)");
        }
    }

    /** Solo el nombre, sin rutas ni caracteres que Windows no acepta. */
    private static String nombreSeguro(String original) {
        String n = original == null ? "archivo.xlsx" : original.replace("\\", "/");
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[:*?\"<>|]", "_").trim();
        if (n.isEmpty() || n.startsWith(".")) {
            n = "archivo" + n;
        }
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }

    private static void borrar(Path carpeta) {
        try (Stream<Path> s = Files.walk(carpeta)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignorada) {
                    // la limpieza no debe tumbar nada
                }
            });
        } catch (IOException ignorada) {
            // carpeta ya borrada
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
