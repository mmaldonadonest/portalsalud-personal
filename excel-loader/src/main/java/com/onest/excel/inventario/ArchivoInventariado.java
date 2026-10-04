package com.onest.excel.inventario;

import java.nio.file.Path;
import java.util.List;

/**
 * Un archivo del origen, ya identificado: que reporte es, de que predio, de que anio y en que
 * formato. Es lo que produce el inventario y lo que consume el cargador.
 *
 * @param ruta            ubicacion en disco
 * @param nombre          nombre del archivo tal cual
 * @param carpetaPredio   carpeta en la que estaba; NO es fuente de verdad del predio
 * @param familia         clasificada por el texto del nombre, nunca por el prefijo numerico
 * @param anio            el que manda, segun {@code excel.anio.fuente}
 * @param anioSegunContenido lo que dice el bloque de titulo dentro del libro
 * @param anioSegunNombre    lo que dice el nombre del archivo
 * @param anioSegunCarpeta   lo que dice la carpeta raiz (p.ej. "MORBILIDAD 2026")
 * @param formatoReal     leido de los primeros bytes
 * @param formatoExtension lo que promete la extension
 * @param checksumSha256  para detectar copias con nombre distinto (TOLUCA vs TOLUCA1)
 * @param hojas           nombres de todas las hojas del libro
 * @param error           null si se pudo abrir; el mensaje si no
 */
public record ArchivoInventariado(
        Path ruta,
        String nombre,
        String carpetaPredio,
        Familia familia,
        Integer anio,
        Integer anioSegunContenido,
        Integer anioSegunNombre,
        Integer anioSegunCarpeta,
        FormatoArchivo formatoReal,
        FormatoArchivo formatoExtension,
        String checksumSha256,
        List<String> hojas,
        String error) {

    public boolean abrio() {
        return error == null;
    }

    /** La extension promete un formato y el archivo es otro. */
    public boolean extensionMiente() {
        return formatoReal != FormatoArchivo.DESCONOCIDO
                && formatoExtension != FormatoArchivo.DESCONOCIDO
                && formatoReal != formatoExtension;
    }

    /**
     * El archivo esta guardado en la carpeta de un anio distinto al suyo. Pasa seguido:
     * {@code MORBILIDAD 2018/TOLUCA/} contiene el de 2019, y dentro de {@code MORBILIDAD 2026/}
     * hay nueve reportes de maternidad de 2025.
     */
    public boolean carpetaDeOtroAnio() {
        return anio != null && anioSegunCarpeta != null && !anio.equals(anioSegunCarpeta);
    }

    /**
     * El nombre del archivo dice un anio y su contenido dice otro.
     *
     * <p>No es un detalle: los siete reportes de NOM-035 se llaman "NOM035 Y AP <b>2026</b>" y
     * su hoja GENERAL dice <b>2025</b> en el bloque de titulo, con fechas de captura de julio de
     * 2025. Si el contenido manda en silencio, los siete quedan fuera de una carga de 2026 sin
     * que nadie se entere. Por eso se reporta y se decide, no se descarta callando.
     */
    public boolean anioEnConflicto() {
        return anioSegunContenido != null && anioSegunNombre != null
                && !anioSegunContenido.equals(anioSegunNombre);
    }

    public boolean esDelAnio(int objetivo) {
        return anio != null && anio == objetivo;
    }
}
