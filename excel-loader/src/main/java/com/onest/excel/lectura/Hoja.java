package com.onest.excel.lectura;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Tarea 3: una sola forma de leer celdas, valga el archivo {@code .xls}, {@code .xlsx} o
 * {@code .xlsm}.
 *
 * <p>POI ya abstrae los dos motores &mdash; HSSF para OLE2 y XSSF para OOXML &mdash; pero deja
 * al llamador lidiando con tipos de celda, formulas, fechas serializadas y celdas nulas. Esta
 * clase resuelve eso una vez, para que el transpositor hable de valores y no de {@code CellType}.
 *
 * <p><b>Dos decisiones que importan:</b>
 * <ol>
 *   <li><b>El ancho real se calcula del encabezado, no de {@code getLastCellNum()}.</b> Los
 *       archivos de 2024 y 2025 declaran <b>16,336 columnas</b> por formato residual cuando su
 *       ancho verdadero es 187. Recorrerlas todas por cada fila son 16 mil lecturas inutiles por
 *       renglon.</li>
 *   <li><b>Las formulas se leen por su valor calculado</b>, nunca por su texto. Pero ese valor
 *       hay que desconfiarlo: ver {@code Normalizador.edadExacta} y el caso de los 126 anios.</li>
 * </ol>
 */
public final class Hoja {

    /** Tope duro de columnas. Ningun reporte del origen pasa de 187. */
    private static final int MAX_COLUMNAS = 400;

    private final Sheet sheet;
    private final String nombre;
    private final int anchoReal;

    public Hoja(Sheet sheet) {
        this.sheet = sheet;
        this.nombre = sheet.getSheetName();
        this.anchoReal = calcularAnchoReal(sheet);
    }

    public String nombre() {
        return nombre;
    }

    /** Numero de la ultima fila, 0-based. */
    public int ultimaFila() {
        return sheet.getLastRowNum();
    }

    /**
     * Ancho util de la hoja. No es {@code getLastCellNum()}: ese viene inflado por formato
     * residual (16,336 columnas en los archivos de 2024-2025).
     */
    public int ancho() {
        return anchoReal;
    }

    /**
     * El ancho real sale de la fila mas poblada de las primeras diez: ahi esta el encabezado, y
     * el encabezado sí delimita la tabla. Las filas de datos no sirven porque una fila puede
     * tener huecos al final.
     */
    private static int calcularAnchoReal(Sheet sheet) {
        int max = 0;
        int hasta = Math.min(sheet.getLastRowNum(), 12);
        for (int r = 0; r <= hasta; r++) {
            Row fila = sheet.getRow(r);
            if (fila == null) {
                continue;
            }
            int ultima = Math.min(fila.getLastCellNum(), MAX_COLUMNAS);
            for (int c = ultima - 1; c >= max; c--) {
                if (tieneContenido(fila.getCell(c))) {
                    max = c + 1;
                    break;
                }
            }
        }
        return max;
    }

    private static boolean tieneContenido(Cell celda) {
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            return false;
        }
        String s = textoDe(celda);
        return s != null && !s.isBlank();
    }

    /** Valor de una celda como texto ya recortado, o {@code null} si esta vacia. */
    public String texto(int fila, int columna) {
        Row f = sheet.getRow(fila);
        if (f == null) {
            return null;
        }
        String s = textoDe(f.getCell(columna));
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Una fila completa hasta el ancho real, con {@code null} en las celdas vacias. */
    public List<String> fila(int fila) {
        List<String> valores = new ArrayList<>(anchoReal);
        Row f = sheet.getRow(fila);
        for (int c = 0; c < anchoReal; c++) {
            if (f == null) {
                valores.add(null);
                continue;
            }
            String s = textoDe(f.getCell(c));
            valores.add((s == null || s.isBlank()) ? null : s.trim());
        }
        return valores;
    }

    /** Valor numerico, o {@code null} si la celda no es un numero. */
    public Double numero(int fila, int columna) {
        Row f = sheet.getRow(fila);
        if (f == null) {
            return null;
        }
        Cell c = f.getCell(columna);
        if (c == null) {
            return null;
        }
        try {
            CellType tipo = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
            if (tipo == CellType.NUMERIC) {
                return c.getNumericCellValue();
            }
            if (tipo == CellType.STRING) {
                String s = c.getStringCellValue().trim().replace(",", "");
                return s.isEmpty() ? null : Double.valueOf(s);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Fecha de una celda. Acepta la fecha real de Excel y el texto con formato reconocible.
     * No inventa: si no es una fecha, devuelve {@code null}.
     */
    public LocalDate fecha(int fila, int columna) {
        Row f = sheet.getRow(fila);
        if (f == null) {
            return null;
        }
        Cell c = f.getCell(columna);
        if (c == null) {
            return null;
        }
        try {
            CellType tipo = c.getCellType() == CellType.FORMULA ? c.getCachedFormulaResultType() : c.getCellType();
            if (tipo == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
                return c.getLocalDateTimeCellValue().toLocalDate();
            }
            if (tipo == CellType.NUMERIC) {
                // Serial de Excel sin formato de fecha. Solo se acepta en el rango plausible:
                // 40000 = 2009, 60000 = 2064. Fuera de ahi es un numero cualquiera.
                double d = c.getNumericCellValue();
                if (d >= 40000 && d <= 60000) {
                    return DateUtil.getLocalDateTime(d).toLocalDate();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /** Texto de una celda resolviendo formulas por su valor calculado. */
    private static String textoDe(Cell celda) {
        if (celda == null) {
            return null;
        }
        try {
            CellType tipo = celda.getCellType() == CellType.FORMULA
                    ? celda.getCachedFormulaResultType() : celda.getCellType();
            return switch (tipo) {
                case STRING -> celda.getStringCellValue();
                case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(celda)) {
                        yield celda.getLocalDateTimeCellValue().toLocalDate().toString();
                    }
                    double d = celda.getNumericCellValue();
                    // "1.0" y "1" son la misma marca; se normaliza al entero cuando lo es
                    yield (d == Math.rint(d) && !Double.isInfinite(d))
                            ? String.valueOf((long) d) : String.valueOf(d);
                }
                case ERROR -> null;   // #REF!, #VALUE!: son celdas rotas, no datos
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }
}
