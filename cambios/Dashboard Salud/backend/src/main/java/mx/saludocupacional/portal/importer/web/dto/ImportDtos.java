package mx.saludocupacional.portal.importer.web.dto;

import mx.saludocupacional.portal.importer.domain.ImportBatch.Estado;
import mx.saludocupacional.portal.importer.domain.ImportBatchRow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Objetos de entrada y salida del importador. */
public final class ImportDtos {

    private ImportDtos() {
    }

    /** Resumen de un lote de importación. */
    public record LoteResumen(
            Long id,
            String archivoNombre,
            OffsetDateTime fechaImportacion,
            Estado estado,
            int registrosTotales,
            int registrosCorrectos,
            int registrosAdvertencia,
            int registrosRechazados
    ) {
    }

    /** Fila detectada, con su contenido original y el resultado de validarla. */
    public record FilaDetectada(
            Long id,
            Integer filaOrigen,
            String hojaOrigen,
            String entidadDestino,
            Map<String, Object> contenido,
            ImportBatchRow.Estado estado,
            String motivoRechazo
    ) {
    }

    /** Vista previa que el usuario revisa antes de confirmar. */
    public record PreviewResponse(
            LoteResumen lote,
            List<String> hojasDetectadas,
            List<FilaDetectada> filas
    ) {
    }

    /** Resultado de confirmar la importación. */
    public record ResultadoImportacion(
            Long loteId,
            int importados,
            int rechazados,
            List<String> mensajes
    ) {
    }
}
