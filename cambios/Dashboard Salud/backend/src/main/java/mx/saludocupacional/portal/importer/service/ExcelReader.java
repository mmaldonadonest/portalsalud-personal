package mx.saludocupacional.portal.importer.service;

import lombok.extern.slf4j.Slf4j;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector de archivos de Excel.
 *
 * <p>Convierte cada hoja en una lista de mapas donde la clave es el encabezado
 * de la columna. De ese modo el resto del importador trabaja con nombres y no
 * con posiciones de celda, y un cambio de orden en el archivo no rompe la carga.
 *
 * <p>El archivo de morbilidad mezcla números, textos y fechas en la misma
 * columna, de modo que cada celda se interpreta según su tipo real y no según lo
 * que se espera de ella.
 */
@Slf4j
@Component
public class ExcelReader {

    /** Número de filas vacías consecutivas que dan por terminada una hoja. */
    private static final int VACIAS_PARA_TERMINAR = 15;

    /** Nombres de las hojas del reporte gerencial. */
    public List<String> nombresDeHoja(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream();
             Workbook libro = WorkbookFactory.create(entrada)) {

            List<String> hojas = new ArrayList<>();
            for (int i = 0; i < libro.getNumberOfSheets(); i++) {
                hojas.add(libro.getSheetName(i));
            }
            return hojas;
        } catch (IOException ex) {
            throw new BusinessRuleException("No fue posible leer el archivo: " + ex.getMessage());
        }
    }

    /**
     * Lee una hoja y devuelve sus filas con los encabezados como clave.
     *
     * @param filaEncabezado índice de la fila con los títulos, contando desde cero
     */
    public List<Map<String, Object>> leerHoja(MultipartFile archivo, String nombreHoja, int filaEncabezado) {
        try (InputStream entrada = archivo.getInputStream();
             Workbook libro = WorkbookFactory.create(entrada)) {

            Sheet hoja = libro.getSheet(nombreHoja);
            if (hoja == null) {
                throw new BusinessRuleException("El archivo no contiene la hoja «%s»".formatted(nombreHoja));
            }

            List<String> encabezados = leerEncabezados(hoja.getRow(filaEncabezado));
            if (encabezados.isEmpty()) {
                throw new BusinessRuleException(
                        "La hoja «%s» no tiene encabezados legibles en la fila indicada".formatted(nombreHoja));
            }

            return leerFilas(hoja, encabezados, filaEncabezado);

        } catch (IOException ex) {
            throw new BusinessRuleException("No fue posible leer el archivo: " + ex.getMessage());
        }
    }

    private List<String> leerEncabezados(Row fila) {
        List<String> encabezados = new ArrayList<>();
        if (fila == null) {
            return encabezados;
        }
        for (int c = 0; c < fila.getLastCellNum(); c++) {
            Object valor = valorDe(fila.getCell(c));
            encabezados.add(valor == null ? "columna_" + c : normalizar(valor.toString()));
        }
        return encabezados;
    }

    /**
     * Recorre las filas de datos.
     *
     * <p>Las hojas del archivo origen declaran rangos mucho mayores que sus
     * datos reales, de modo que la lectura se detiene tras una racha de filas
     * vacías en lugar de recorrer miles de celdas sin contenido.
     */
    private List<Map<String, Object>> leerFilas(Sheet hoja, List<String> encabezados, int filaEncabezado) {
        List<Map<String, Object>> filas = new ArrayList<>();
        int vaciasSeguidas = 0;

        for (int f = filaEncabezado + 1; f <= hoja.getLastRowNum(); f++) {
            Row fila = hoja.getRow(f);
            Map<String, Object> valores = new LinkedHashMap<>();
            boolean tieneContenido = false;

            if (fila != null) {
                for (int c = 0; c < encabezados.size(); c++) {
                    Object valor = valorDe(fila.getCell(c));
                    if (valor != null) {
                        tieneContenido = true;
                    }
                    valores.put(encabezados.get(c), valor);
                }
            }

            if (tieneContenido) {
                valores.put("_fila", f + 1);
                filas.add(valores);
                vaciasSeguidas = 0;
            } else if (++vaciasSeguidas >= VACIAS_PARA_TERMINAR) {
                break;
            }
        }
        return filas;
    }

    /** Interpreta la celda según su tipo real, no según el esperado. */
    private Object valorDe(Cell celda) {
        if (celda == null) {
            return null;
        }
        return switch (celda.getCellType()) {
            case STRING -> {
                String texto = celda.getStringCellValue().trim();
                yield texto.isEmpty() ? null : texto;
            }
            case NUMERIC -> DateUtil.isCellDateFormatted(celda)
                    ? celda.getLocalDateTimeCellValue().toLocalDate()
                    : celda.getNumericCellValue();
            case BOOLEAN -> celda.getBooleanCellValue();
            case FORMULA -> valorDeFormula(celda);
            default -> null;
        };
    }

    /**
     * Valor calculado de una fórmula.
     *
     * <p>El archivo gerencial enlaza libros externos que no acompañan a la
     * carga; en esos casos solo queda el último valor almacenado, que es
     * justamente lo que interesa importar.
     */
    private Object valorDeFormula(Cell celda) {
        try {
            return switch (celda.getCachedFormulaResultType()) {
                case NUMERIC -> DateUtil.isCellDateFormatted(celda)
                        ? celda.getLocalDateTimeCellValue().toLocalDate()
                        : celda.getNumericCellValue();
                case STRING -> {
                    String texto = celda.getStringCellValue().trim();
                    yield texto.isEmpty() ? null : texto;
                }
                case BOOLEAN -> celda.getBooleanCellValue();
                default -> null;
            };
        } catch (IllegalStateException ex) {
            log.debug("Fórmula sin valor almacenado en {}", celda.getAddress());
            return null;
        }
    }

    /** Normaliza un encabezado para usarlo como clave estable. */
    private String normalizar(String texto) {
        return texto.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    /** Convierte a entero un valor que puede venir como número o como texto. */
    public Integer comoEntero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        try {
            return Integer.valueOf(valor.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Convierte a fecha un valor que puede venir como fecha o como texto. */
    public LocalDate comoFecha(Object valor) {
        if (valor instanceof LocalDate fecha) {
            return fecha;
        }
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor.toString().trim());
        } catch (Exception ex) {
            return null;
        }
    }

    /** Devuelve el texto de un valor, o vacío si no lo tiene. */
    public String comoTexto(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }
}
