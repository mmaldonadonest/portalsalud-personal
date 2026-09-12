package com.onest.app.importer.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Paso 1 del importador: lee el archivo a una estructura neutra (hoja -> encabezados ->
 * filas con valores tipados) sin interpretar nada del negocio. Soporta .xlsx/.xlsm (POI) y
 * .csv (delimitador detectado). Las formulas se leen por su valor cacheado - el reporte
 * gerencial es ~95% formulas a 8 libros externos que aqui no existen (ver
 * cambios/Dashboard Salud/docs/01_ANALISIS_ACTUAL.md), asi que el valor cacheado es lo unico
 * disponible y se marca como tal para que el validador lo reporte.
 */
@Component
public class ExcelLector {

    /** Tope por hoja: el reporte real tiene ~250 filas x ~160 columnas; esto protege de archivos absurdos. */
    static final int MAX_FILAS_HOJA = 20_000;
    static final int MAX_COLUMNAS = 300;

    private static final Pattern LIBRO_EXTERNO = Pattern.compile("\\[\\d+\\]");

    public record Celda(Object valor, boolean formula, boolean externa, String error) {
    }

    public record FilaLeida(int numero, LinkedHashMap<String, Object> valores, List<String> erroresFormula,
                            int formulasExternas, int celdasConDato) {
    }

    public record HojaLeida(String nombre, int filaEncabezado, List<String> encabezados, List<FilaLeida> filas,
                            int formulasExternas, int erroresFormula, boolean truncada, int columnasIgnoradas) {
    }

    public record ArchivoLeido(List<HojaLeida> hojas, List<String> hojasOmitidas) {
    }

    public ArchivoLeido leer(byte[] bytes, String extension) throws IOException {
        String ext = extension == null ? "" : extension.toLowerCase();
        return switch (ext) {
            case "xlsx", "xlsm" -> leerWorkbook(bytes);
            case "csv" -> leerCsv(bytes);
            default -> throw new IllegalArgumentException("Formato no soportado: ." + extension + " (acepta .xlsx, .xlsm, .csv)");
        };
    }

    // ------------------------------------------------------------------ xlsx / xlsm

    private ArchivoLeido leerWorkbook(byte[] bytes) throws IOException {
        List<HojaLeida> hojas = new ArrayList<>();
        List<String> omitidas = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                if (wb.isSheetHidden(s) || wb.isSheetVeryHidden(s)) {
                    omitidas.add(sheet.getSheetName() + " (oculta)");
                    continue;
                }
                HojaLeida hoja = leerHoja(sheet);
                if (hoja == null) {
                    omitidas.add(sheet.getSheetName() + " (vacía)");
                } else {
                    hojas.add(hoja);
                }
            }
        }
        return new ArchivoLeido(hojas, omitidas);
    }

    private HojaLeida leerHoja(Sheet sheet) {
        // Encabezado = primera fila con al menos 2 celdas de texto (los reportes traen titulos
        // y celdas combinadas arriba; una sola celda es titulo, no encabezado).
        int filaEnc = -1;
        List<String> encabezados = new ArrayList<>();
        int ultimaFila = Math.min(sheet.getLastRowNum(), MAX_FILAS_HOJA + 50);
        for (int r = sheet.getFirstRowNum(); r <= ultimaFila && r >= 0; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int textos = 0;
            for (Cell c : row) {
                Celda celda = leerCelda(c);
                if (celda.valor() instanceof String t && !t.isBlank()) {
                    textos++;
                }
            }
            if (textos >= 2) {
                filaEnc = r;
                short ultimaCol = row.getLastCellNum();
                int cols = Math.min(ultimaCol < 0 ? 0 : ultimaCol, MAX_COLUMNAS);
                for (int c = 0; c < cols; c++) {
                    Cell cell = row.getCell(c);
                    Object v = cell == null ? null : leerCelda(cell).valor();
                    encabezados.add(nombreColumna(v, c, encabezados));
                }
                break;
            }
        }
        if (filaEnc < 0) {
            return null;
        }
        List<FilaLeida> filas = new ArrayList<>();
        int externas = 0, errores = 0;
        boolean truncada = false;
        short ultimaColHoja = 0;
        for (int r = filaEnc + 1; r <= sheet.getLastRowNum(); r++) {
            if (filas.size() >= MAX_FILAS_HOJA) {
                truncada = true;
                break;
            }
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            if (row.getLastCellNum() > ultimaColHoja) {
                ultimaColHoja = row.getLastCellNum();
            }
            LinkedHashMap<String, Object> valores = new LinkedHashMap<>();
            List<String> erroresFila = new ArrayList<>();
            int externasFila = 0, conDato = 0;
            for (int c = 0; c < encabezados.size(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null) {
                    valores.put(encabezados.get(c), null);
                    continue;
                }
                Celda celda = leerCelda(cell);
                if (celda.error() != null) {
                    erroresFila.add(encabezados.get(c) + ": " + celda.error());
                    errores++;
                }
                if (celda.externa()) {
                    externasFila++;
                }
                if (celda.valor() != null && !(celda.valor() instanceof String t && t.isBlank())) {
                    conDato++;
                }
                valores.put(encabezados.get(c), celda.valor());
            }
            if (conDato == 0 && erroresFila.isEmpty()) {
                continue; // renglon vacio: ni se cuenta
            }
            externas += externasFila;
            filas.add(new FilaLeida(r + 1, valores, erroresFila, externasFila, conDato));
        }
        int ignoradas = Math.max(0, ultimaColHoja - encabezados.size());
        return new HojaLeida(sheet.getSheetName().trim(), filaEnc + 1, encabezados, filas, externas, errores, truncada, ignoradas);
    }

    private static Celda leerCelda(Cell cell) {
        CellType tipo = cell.getCellType();
        boolean formula = tipo == CellType.FORMULA;
        boolean externa = false;
        if (formula) {
            try {
                externa = LIBRO_EXTERNO.matcher(cell.getCellFormula()).find();
            } catch (RuntimeException ignored) {
                // formula ilegible: se sigue con el valor cacheado
            }
            tipo = cell.getCachedFormulaResultType();
        }
        return switch (tipo) {
            case STRING -> new Celda(cell.getStringCellValue().trim(), formula, externa, null);
            case BOOLEAN -> new Celda(cell.getBooleanCellValue(), formula, externa, null);
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDateTime d = cell.getLocalDateTimeCellValue();
                    yield new Celda(d == null ? null : (d.toLocalTime().toSecondOfDay() == 0 ? d.toLocalDate().toString() : d.toString()),
                            formula, externa, null);
                }
                double n = cell.getNumericCellValue();
                yield new Celda(n == Math.rint(n) && Math.abs(n) < 1e15 ? (Object) (long) n : (Object) n, formula, externa, null);
            }
            case ERROR -> {
                String err;
                try {
                    err = FormulaError.forInt(cell.getErrorCellValue()).getString();
                } catch (RuntimeException ex) {
                    err = "#ERROR";
                }
                yield new Celda(null, formula, externa, err);
            }
            default -> new Celda(null, formula, externa, null);
        };
    }

    // ------------------------------------------------------------------ csv

    private ArchivoLeido leerCsv(byte[] bytes) throws IOException {
        String texto = new String(bytes, StandardCharsets.UTF_8);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        List<List<String>> lineas = new ArrayList<>();
        char delim = detectarDelimitador(texto);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                lineas.add(partir(linea, delim));
            }
        }
        // mismo criterio de encabezado que en Excel
        int filaEnc = -1;
        List<String> encabezados = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            long textos = lineas.get(i).stream().filter(v -> !v.isBlank() && parseNumero(v) == null).count();
            if (textos >= 2) {
                filaEnc = i;
                List<String> l = lineas.get(i);
                for (int c = 0; c < Math.min(l.size(), MAX_COLUMNAS); c++) {
                    encabezados.add(nombreColumna(l.get(c), c, encabezados));
                }
                break;
            }
        }
        if (filaEnc < 0) {
            return new ArchivoLeido(List.of(), List.of("csv (sin encabezado reconocible)"));
        }
        List<FilaLeida> filas = new ArrayList<>();
        boolean truncada = false;
        for (int i = filaEnc + 1; i < lineas.size(); i++) {
            if (filas.size() >= MAX_FILAS_HOJA) {
                truncada = true;
                break;
            }
            List<String> l = lineas.get(i);
            LinkedHashMap<String, Object> valores = new LinkedHashMap<>();
            int conDato = 0;
            for (int c = 0; c < encabezados.size(); c++) {
                String v = c < l.size() ? l.get(c).trim() : "";
                Object tipado = v.isEmpty() ? null : (parseNumero(v) != null ? parseNumero(v) : v);
                if (tipado != null) {
                    conDato++;
                }
                valores.put(encabezados.get(c), tipado);
            }
            if (conDato == 0) {
                continue;
            }
            filas.add(new FilaLeida(i + 1, valores, List.of(), 0, conDato));
        }
        return new ArchivoLeido(List.of(new HojaLeida("csv", filaEnc + 1, encabezados, filas, 0, 0, truncada, 0)), List.of());
    }

    private static char detectarDelimitador(String texto) {
        String primera = texto.lines().findFirst().orElse("");
        long comas = primera.chars().filter(ch -> ch == ',').count();
        long puntoComa = primera.chars().filter(ch -> ch == ';').count();
        long tabs = primera.chars().filter(ch -> ch == '\t').count();
        if (tabs > comas && tabs > puntoComa) {
            return '\t';
        }
        return puntoComa > comas ? ';' : ',';
    }

    private static List<String> partir(String linea, char delim) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (ch == '"') {
                if (enComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    enComillas = !enComillas;
                }
            } else if (ch == delim && !enComillas) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        return out;
    }

    private static Object parseNumero(String v) {
        String t = v.trim().replace(",", "");
        if (!t.matches("^-?\\d+(\\.\\d+)?$")) {
            return null;
        }
        try {
            return t.contains(".") ? (Object) Double.parseDouble(t) : (Object) Long.parseLong(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ comun

    /** Encabezado legible y unico: celda vacia -> COL_n; repetido -> sufijo _2, _3... */
    private static String nombreColumna(Object v, int indice, List<String> previos) {
        String base = v == null ? "" : String.valueOf(v).trim().replaceAll("\\s+", " ");
        if (base.isEmpty()) {
            base = "COL_" + (indice + 1);
        }
        if (base.length() > 80) {
            base = base.substring(0, 80);
        }
        String nombre = base;
        int n = 2;
        while (previos.contains(nombre)) {
            nombre = base + "_" + n++;
        }
        return nombre;
    }

    static boolean esFechaIso(Object v) {
        if (!(v instanceof String s)) {
            return false;
        }
        try {
            LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
            return s.matches("^\\d{4}-\\d{2}-\\d{2}.*");
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
