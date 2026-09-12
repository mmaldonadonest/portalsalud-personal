package com.onest.app.importer.dto;

import java.util.List;
import java.util.Map;

/** Contratos del importador Excel (Analisis > Importar Excel). */
public final class ImportacionDto {

    private ImportacionDto() {
    }

    /** Cabecera de un lote (archivo cargado). {@code estado}: VALIDADO | CONFIRMADO | DESCARTADO | SUSTITUIDO. */
    public record Lote(
            Long id,
            String nombreArchivo,
            String extension,
            long sizeBytes,
            String checksum,
            int version,
            String estado,
            int hojas,
            int procesados,
            int correctos,
            int advertencias,
            int rechazados,
            String creadoEn,
            String creadoPor,
            String confirmadoEn,
            String confirmadoPor,
            Long fsFileId
    ) {
    }

    /** Resumen de una hoja tal como se pinta (tambien se guarda en RESUMEN_JSON). */
    public record Hoja(
            String nombre,
            boolean reconocida,
            int filaEncabezado,
            int columnas,
            int filas,
            int correctos,
            int advertencias,
            int rechazados,
            List<String> encabezados,
            List<FilaPreview> muestra
    ) {
    }

    /** Fila de la vista previa (primeras N por hoja). */
    public record FilaPreview(int numero, String estado, String mensajes, Map<String, Object> valores) {
    }

    /** Incidencia a nivel archivo/hoja. {@code impacto}: Alto | Medio | Bajo. */
    public record Incidencia(String hoja, String incidencia, String impacto, String resolucion) {
    }

    /** Respuesta de cargar/validar y de consultar un lote. */
    public record Resultado(
            Lote lote,
            List<Hoja> hojas,
            List<String> hojasOmitidas,
            List<Incidencia> incidencias,
            /** Lote anterior con el mismo nombre que quedo SUSTITUIDO (null si es la primera version). */
            Lote sustituye,
            /** true si el binario es identico (mismo SHA-256) a la version anterior. */
            boolean identicoAlAnterior
    ) {
    }
}
