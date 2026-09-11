package mx.saludocupacional.portal.importer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.catalog.repository.AttentionCauseRepository;
import mx.saludocupacional.portal.catalog.service.PredioService;
import mx.saludocupacional.portal.importer.domain.ImportBatch;
import mx.saludocupacional.portal.importer.domain.ImportBatch.Estado;
import mx.saludocupacional.portal.importer.domain.ImportBatchRow;
import mx.saludocupacional.portal.importer.repository.ImportBatchRepository;
import mx.saludocupacional.portal.importer.repository.ImportBatchRowRepository;
import mx.saludocupacional.portal.importer.web.dto.ImportDtos.FilaDetectada;
import mx.saludocupacional.portal.importer.web.dto.ImportDtos.LoteResumen;
import mx.saludocupacional.portal.importer.web.dto.ImportDtos.PreviewResponse;
import mx.saludocupacional.portal.importer.web.dto.ImportDtos.ResultadoImportacion;
import mx.saludocupacional.portal.morbidity.domain.MedicalAttention;
import mx.saludocupacional.portal.morbidity.repository.MedicalAttentionRepository;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import mx.saludocupacional.portal.shared.service.PeriodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Importación de información histórica desde Excel.
 *
 * <p>El proceso ocurre en dos tiempos. Primero se analiza el archivo y se
 * guardan las filas detectadas con el resultado de validarlas, sin tocar las
 * tablas operativas. Solo cuando alguien revisa la vista previa y confirma, las
 * filas válidas se escriben, todas dentro de una misma transacción.
 *
 * <p>El archivo origen abrevia los nombres de predio en algunas hojas, de modo
 * que la resolución se hace contra el catálogo y sus alias en lugar de comparar
 * textos literales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final String MODULO = "importacion";
    private static final String HOJA_ATENCIONES = "ATEN";
    private static final int FILA_ENCABEZADO = 4;

    private final ImportBatchRepository lotes;
    private final ImportBatchRowRepository filas;
    private final ExcelReader excelReader;
    private final PredioService predios;
    private final AttentionCauseRepository causas;
    private final MedicalAttentionRepository atenciones;
    private final PeriodService periodService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Analiza el archivo y prepara la vista previa.
     *
     * <p>No escribe nada en las tablas operativas: solo registra qué encontró y
     * qué problemas detectó, para que alguien lo revise antes de confirmar.
     */
    @Transactional
    public PreviewResponse analizar(MultipartFile archivo, Long usuarioId) {
        validarArchivo(archivo);

        List<String> hojas = excelReader.nombresDeHoja(archivo);

        ImportBatch lote = new ImportBatch();
        lote.setArchivoNombre(archivo.getOriginalFilename());
        lote.setUsuarioId(usuarioId);
        ImportBatch guardado = lotes.save(lote);

        List<ImportBatchRow> detectadas = new ArrayList<>();
        if (hojas.contains(HOJA_ATENCIONES)) {
            detectadas.addAll(analizarAtenciones(archivo, guardado));
        }

        int correctas = (int) detectadas.stream()
                .filter(f -> f.getEstado() == ImportBatchRow.Estado.PENDIENTE).count();
        int rechazadas = detectadas.size() - correctas;

        guardado.setRegistrosTotales(detectadas.size());
        guardado.setRegistrosCorrectos(correctas);
        guardado.setRegistrosRechazados(rechazadas);
        lotes.save(guardado);

        filas.saveAll(detectadas);

        auditService.registrar(AuditAction.IMPORT, MODULO, "ImportBatch",
                guardado.getId(), null, null, usuarioId, false);

        return new PreviewResponse(
                aResumen(guardado),
                hojas,
                detectadas.stream().map(this::aFilaDetectada).toList());
    }

    /**
     * Escribe las filas válidas del lote.
     *
     * <p>Todas comparten transacción: si una falla, ninguna queda a medias.
     */
    @Transactional
    public ResultadoImportacion confirmar(Long loteId, Long usuarioId) {
        ImportBatch lote = obtener(loteId);

        if (lote.getEstado() != Estado.PREVIEW) {
            throw new BusinessRuleException(
                    "Este lote ya fue %s".formatted(lote.getEstado() == Estado.CONFIRMADO
                            ? "confirmado" : "rechazado"));
        }

        List<ImportBatchRow> pendientes =
                filas.findByImportBatchIdAndEstado(loteId, ImportBatchRow.Estado.PENDIENTE);

        List<String> mensajes = new ArrayList<>();
        int importadas = 0;

        for (ImportBatchRow fila : pendientes) {
            try {
                escribir(fila);
                fila.setEstado(ImportBatchRow.Estado.IMPORTADO);
                importadas++;
            } catch (Exception ex) {
                fila.setEstado(ImportBatchRow.Estado.RECHAZADO);
                fila.setMotivoRechazo(ex.getMessage());
                mensajes.add("Fila %d: %s".formatted(fila.getFilaOrigen(), ex.getMessage()));
            }
        }
        filas.saveAll(pendientes);

        lote.setEstado(Estado.CONFIRMADO);
        lote.setRegistrosCorrectos(importadas);
        lote.setRegistrosRechazados(lote.getRegistrosTotales() - importadas);
        lotes.save(lote);

        auditService.registrar(AuditAction.IMPORT, MODULO, "ImportBatch",
                loteId, null, Map.of("importados", importadas), usuarioId, false);

        return new ResultadoImportacion(loteId, importadas,
                lote.getRegistrosTotales() - importadas, mensajes);
    }

    @Transactional
    public void rechazar(Long loteId, Long usuarioId) {
        ImportBatch lote = obtener(loteId);
        lote.setEstado(Estado.RECHAZADO);
        lotes.save(lote);

        auditService.registrar(AuditAction.IMPORT, MODULO, "ImportBatch",
                loteId, null, null, usuarioId, false);
    }

    @Transactional(readOnly = true)
    public PreviewResponse consultar(Long loteId) {
        ImportBatch lote = obtener(loteId);
        return new PreviewResponse(
                aResumen(lote),
                List.of(),
                filas.findByImportBatchIdOrderByFilaOrigenAsc(loteId).stream()
                        .map(this::aFilaDetectada).toList());
    }

    /**
     * Analiza la hoja de atenciones.
     *
     * <p>La hoja presenta un predio por fila y un mes por columna, de modo que
     * cada celda con valor se convierte en un conteo de atenciones de ese predio
     * en ese mes.
     */
    private List<ImportBatchRow> analizarAtenciones(MultipartFile archivo, ImportBatch lote) {
        List<Map<String, Object>> filasHoja =
                excelReader.leerHoja(archivo, HOJA_ATENCIONES, FILA_ENCABEZADO);

        List<ImportBatchRow> detectadas = new ArrayList<>();
        List<String> meses = List.of("ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
                                     "JUL", "AGO", "SEP", "OCT", "NOV", "DIC");

        for (Map<String, Object> fila : filasHoja) {
            String nombrePredio = excelReader.comoTexto(fila.get("PREDIO"));
            Integer numeroFila = excelReader.comoEntero(fila.get("_fila"));

            if (nombrePredio == null || "TOTAL".equalsIgnoreCase(nombrePredio)) {
                continue;
            }

            Long predioId = null;
            String problema = null;
            try {
                predioId = predios.resolverPorNombreOAlias(nombrePredio).getId();
            } catch (ResourceNotFoundException ex) {
                problema = "El predio «%s» no existe en el catálogo".formatted(nombrePredio);
            }

            for (int i = 0; i < meses.size(); i++) {
                Integer cantidad = excelReader.comoEntero(fila.get(meses.get(i)));
                if (cantidad == null || cantidad == 0) {
                    continue;
                }

                Map<String, Object> contenido = Map.of(
                        "predio", nombrePredio,
                        "predioId", predioId == null ? "" : predioId,
                        "mes", i + 1,
                        "cantidad", cantidad);

                detectadas.add(nuevaFila(lote, numeroFila, HOJA_ATENCIONES,
                        "MedicalAttention", contenido, problema));
            }
        }
        return detectadas;
    }

    /**
     * Escribe una fila validada en su tabla operativa.
     *
     * <p>Las cifras del archivo son totales mensuales por predio, sin identificar
     * a la persona atendida. Se registran como atenciones sin colaborador
     * asociado, que es como el archivo las guardaba.
     */
    private void escribir(ImportBatchRow fila) {
        Map<String, Object> contenido = leerContenido(fila);

        if (!"MedicalAttention".equals(fila.getEntidadDestino())) {
            throw new BusinessRuleException(
                    "Aún no se admite la importación de " + fila.getEntidadDestino());
        }

        Object idPredio = contenido.get("predioId");
        if (idPredio == null || idPredio.toString().isBlank()) {
            throw new BusinessRuleException("La fila no tiene un predio reconocido");
        }

        Predio predio = predios.obtener(Long.valueOf(idPredio.toString()));
        int mes = ((Number) contenido.get("mes")).intValue();
        int cantidad = ((Number) contenido.get("cantidad")).intValue();
        int anio = LocalDate.now().getYear();

        // Una fila del archivo representa un total mensual; se guarda como
        // tantas atenciones como indique la cifra, sin persona identificada.
        for (int i = 0; i < cantidad; i++) {
            MedicalAttention atencion = new MedicalAttention();
            atencion.setPredio(predio);
            atencion.setFechaAtencion(LocalDate.of(anio, mes, 1));
            atencion.setPeriod(periodService.resolver(LocalDate.of(anio, mes, 1)));
            atencion.setAttentionCause(causas.findAll().stream().findFirst()
                    .orElseThrow(() -> new BusinessRuleException(
                            "No hay causas de atención configuradas")));
            atenciones.save(atencion);
        }
    }

    private ImportBatchRow nuevaFila(ImportBatch lote, Integer numeroFila, String hoja,
                                     String entidad, Map<String, Object> contenido, String problema) {
        ImportBatchRow fila = new ImportBatchRow();
        fila.setImportBatch(lote);
        fila.setFilaOrigen(numeroFila == null ? 0 : numeroFila);
        fila.setHojaOrigen(hoja);
        fila.setEntidadDestino(entidad);
        fila.setPayload(serializar(contenido));

        if (problema != null) {
            fila.setEstado(ImportBatchRow.Estado.RECHAZADO);
            fila.setMotivoRechazo(problema);
        }
        return fila;
    }

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("Selecciona un archivo para importar");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !(nombre.endsWith(".xlsx") || nombre.endsWith(".xlsm"))) {
            throw new BusinessRuleException("El archivo debe tener extensión .xlsx o .xlsm");
        }
    }

    @Transactional(readOnly = true)
    public ImportBatch obtener(Long id) {
        return lotes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lote de importación", id));
    }

    private String serializar(Map<String, Object> contenido) {
        try {
            return objectMapper.writeValueAsString(contenido);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> leerContenido(ImportBatchRow fila) {
        try {
            return objectMapper.readValue(fila.getPayload(), new TypeReference<>() { });
        } catch (Exception ex) {
            throw new BusinessRuleException("El contenido de la fila no pudo interpretarse");
        }
    }

    private LoteResumen aResumen(ImportBatch lote) {
        return new LoteResumen(lote.getId(), lote.getArchivoNombre(), lote.getFechaImportacion(),
                lote.getEstado(), lote.getRegistrosTotales(), lote.getRegistrosCorrectos(),
                lote.getRegistrosAdvertencia(), lote.getRegistrosRechazados());
    }

    private FilaDetectada aFilaDetectada(ImportBatchRow fila) {
        return new FilaDetectada(fila.getId(), fila.getFilaOrigen(), fila.getHojaOrigen(),
                fila.getEntidadDestino(), leerContenidoSeguro(fila),
                fila.getEstado(), fila.getMotivoRechazo());
    }

    private Map<String, Object> leerContenidoSeguro(ImportBatchRow fila) {
        try {
            return objectMapper.readValue(fila.getPayload(), new TypeReference<>() { });
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
