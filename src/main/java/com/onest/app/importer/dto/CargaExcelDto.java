package com.onest.app.importer.dto;

import com.onest.app.importer.nss.XlsNssRepository.Reemplazo;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos de la carga de Excel por pantalla (Carga de Excel, menu CARGA_EXCEL). */
public final class CargaExcelDto {

    private CargaExcelDto() {
    }

    /**
     * Un trabajo de la pantalla: un archivo que se valida y, si se confirma, se carga.
     * {@code estado}: EN_COLA | VALIDANDO | VALIDADO | CARGANDO | CARGADO | ERROR.
     */
    public record Trabajo(String id, String estado, String mensaje, String archivo, String correcciones,
                          LocalDateTime creado, Resumen validacion, Resumen carga) {
    }

    /** Lo que dijo el cargador de un archivo (validar o cargar). */
    public record Resumen(String layout, String motivoRechazo, int leidas, int validas, int renglonesConError,
                          double porcentaje, List<String> avisos, List<Fila> errores, int totalErrores,
                          List<Fila> avisosFila, int totalAvisosFila, List<Reemplazo> reemplazos,
                          Escritura escritura, boolean omitido) {
    }

    /** Un renglon del Excel de errores. TIPO = ERROR (no entro) o AVISO (entro, hay que revisarlo). */
    /** baja = fecha y tipo de BIO_EMPLEADO.FECHA_BAJA si el empleado es baja; solo informativo */
    public record Fila(int renglon, String tipo, String columna, String valor, String motivo, String baja) {
    }

    public record Escritura(long loteId, int insertados, int reemplazados, int duplicados, String aviso) {
    }
}
