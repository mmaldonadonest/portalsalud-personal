package com.onest.app.importer.nss;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Lee un .xlsx renglon por renglon SIN cargarlo completo en memoria (API de eventos de POI).
 *
 * <p>Por que streaming: los layouts del ano completo de todos los predios pueden traer decenas de
 * miles de renglones; el cargador anterior leia los libros enteros y necesitaba -Xmx2g.
 *
 * <p>Las celdas salen como texto, con dos reglas para que el valor no dependa del formato visual:
 * <ul>
 *   <li>fecha con formato de fecha &rarr; {@code yyyy-MM-dd} (no "3/15/26" segun el formato)</li>
 *   <li>numero &rarr; su valor plano ({@code 30048315698}, no "3.00483E+10" ni "30,048,315,698")</li>
 * </ul>
 */
public final class LectorXlsx {

    /** Un renglon: numero como lo ve el usuario en Excel (1 = encabezado) y sus valores. */
    public record Fila(int numero, List<String> valores) {
        public String valor(int indice) {
            return indice < valores.size() ? valores.get(indice) : null;
        }

        public boolean vacia() {
            return valores.stream().allMatch(v -> v == null || v.isBlank());
        }
    }

    private LectorXlsx() {
    }

    /**
     * Recorre la hoja {@code hoja} (o la primera, si no existe con ese nombre) y entrega cada
     * renglon al consumidor, incluido el encabezado (renglon 1).
     *
     * @return el nombre de la hoja que se leyo
     */
    public static String leer(Path archivo, String hoja, Consumer<Fila> consumidor) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(archivo.toFile(), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable sst = new ReadOnlySharedStringsTable(pkg);
            StylesTable estilos = reader.getStylesTable();

            // Primero se busca la hoja por nombre; si no esta, la primera
            String elegida = null;
            int indice = -1;
            int i = 0;
            XSSFReader.SheetIterator it = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (it.hasNext()) {
                try (InputStream ignorada = it.next()) {
                    String nombre = it.getSheetName();
                    if (elegida == null && hoja != null && nombre.trim().equalsIgnoreCase(hoja.trim())) {
                        elegida = nombre;
                        indice = i;
                    }
                }
                i++;
            }
            if (indice < 0) {
                indice = 0;
            }

            it = (XSSFReader.SheetIterator) reader.getSheetsData();
            for (int k = 0; it.hasNext(); k++) {
                try (InputStream datos = it.next()) {
                    if (k != indice) {
                        continue;
                    }
                    elegida = it.getSheetName();
                    XMLReader parser = XMLHelper.newXMLReader();
                    parser.setContentHandler(new XSSFSheetXMLHandler(estilos, null, sst,
                            new Recolector(consumidor), new Formateador(), false));
                    parser.parse(new InputSource(datos));
                }
            }
            return elegida;
        }
    }

    /** Arma cada renglon con huecos en las celdas vacias, para que el indice = columna. */
    private static final class Recolector implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final Consumer<Fila> consumidor;
        private List<String> actual;

        Recolector(Consumer<Fila> consumidor) {
            this.consumidor = consumidor;
        }

        @Override
        public void startRow(int rowNum) {
            actual = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            consumidor.accept(new Fila(rowNum + 1, actual));
        }

        @Override
        public void cell(String ref, String valor, XSSFComment comentario) {
            int col = new CellReference(ref).getCol();
            while (actual.size() < col) {
                actual.add(null);
            }
            actual.add(valor == null ? null : valor.trim());
        }
    }

    /** Fechas como ISO y numeros sin formato, independientemente de como se ven en Excel. */
    private static final class Formateador extends DataFormatter {
        @Override
        public String formatRawCellContents(double valor, int indiceFormato, String formato) {
            if (DateUtil.isADateFormat(indiceFormato, formato) && DateUtil.isValidExcelDate(valor)) {
                return DateUtil.getLocalDateTime(valor).toLocalDate().toString();
            }
            return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
        }
    }
}
