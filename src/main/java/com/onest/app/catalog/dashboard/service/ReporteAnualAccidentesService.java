package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.repository.VistaAccidenteRepository;
import com.onest.app.catalog.dashboard.repository.VistaAccidenteRepository.FilaReporte;
import com.onest.app.catalog.dashboard.service.ReporteAnualAccidentes.Renglon;
import com.onest.app.catalog.dashboard.service.ReporteAnualAccidentes.Reporte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * C-044: Excel del reporte anual de accidentes (hoja GENERAL de SM) + una hoja «Detalle» con cada
 * accidente, para que SM lo cuadre renglon por renglon contra su Excel, y una hoja «Notas» con las
 * reglas. Sale de SERV_MED_V_ACCIDENTE (portal + Excel, "manda el portal"), igual que la pantalla
 * de Accidentabilidad: los totales son los mismos numeros que sus tarjetas.
 */
@Service
public class ReporteAnualAccidentesService {

    private static final ZoneId MEXICO = ZoneId.of("America/Mexico_City");
    private static final String[] MESES = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT",
            "NOV", "DIC"};
    private static final int FILA_DATOS = 6;

    private final ObjectProvider<VistaAccidenteRepository> vista;

    public ReporteAnualAccidentesService(ObjectProvider<VistaAccidenteRepository> vista) {
        this.vista = vista;
    }

    public byte[] excel(int anio, String predio) {
        VistaAccidenteRepository v = vista.getIfAvailable();
        if (v == null) {
            // Sin la vista no hay forma de sumar portal + Excel sin contar dos veces
            throw new IllegalStateException("El reporte anual de accidentes requiere portal.historico.fuente=VISTA.");
        }
        int actual = LocalDate.now(MEXICO).getYear();
        if (anio < 2000 || anio > actual) {
            throw new IllegalArgumentException("Año fuera de rango: " + anio + ".");
        }
        String p = predio == null || predio.isBlank() ? null : predio.trim();
        List<FilaReporte> filas = v.filasReporte(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), p);
        Reporte rep = ReporteAnualAccidentes.armar(anio, filas);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(wb);
            hojaGeneral(wb, rep, p, e);
            hojaDetalle(wb, filas, anio, p, e);
            hojaNotas(wb, rep, e);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo armar el Excel: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------------------------------------
    //  Hoja GENERAL
    // ------------------------------------------------------------------------------------------

    /** Una columna de la hoja: tres niveles de encabezado como la de SM y como sacar su valor. */
    private record Col(String grupo, String sub, String hoja, Function<Renglon, Object> valor, boolean moneda,
                       boolean soloTotal) {
    }

    private static List<Col> columnas(Reporte rep) {
        List<Col> c = new ArrayList<>();
        Renglon t = rep.total();
        for (int i = 0; i < 12; i++) {
            int m = i;
            c.add(new Col("MES REPORTADO", "", MESES[i], r -> r.meses[m], false, false));
        }
        if (t.sinMes > 0) {
            c.add(new Col("MES REPORTADO", "", "SIN FECHA", r -> r.sinMes, false, false));
        }
        c.add(new Col("MES REPORTADO", "", "TOTAL", r -> r.total, false, false));

        conteo(c, "TIPO DE RIESGO", ReporteAnualAccidentes.TIPOS, rep.tipoSinDato(), r -> r.tipo);
        conteo(c, "GENERO", ReporteAnualAccidentes.GENEROS, rep.generoSinDato(), r -> r.genero);
        conteo(c, "CUENTA", rep.cuentas(), false, r -> r.cuenta);
        conteo(c, "AREA", rep.areas(), false, r -> r.area);
        conteo(c, "PUESTO", rep.puestos(), false, r -> r.puesto);

        for (String status : List.of("CALIFICADO", "IMPROCEDENTE")) {
            for (String tipo : ReporteAnualAccidentes.TIPOS) {
                String k = status + "|" + tipo;
                c.add(new Col(status, tipo, "DIAS", r -> r.n(r.dias, k), false, false));
                c.add(new Col(status, tipo, "COSTO", r -> r.d(r.costo, k), true, false));
            }
        }

        for (Map.Entry<String, String> ca : ReporteAnualAccidentes.CAUSAS.entrySet()) {
            c.add(new Col("CAUSA DE RT", ca.getValue(), "", r -> r.n(r.causa, ca.getKey()), false, false));
        }
        c.add(new Col("CAUSA DE RT", "TOTAL", "", r -> r.suma(r.causa), false, false));

        c.add(new Col("COSTOS x ACCIDENTE", "CALIFICADO", "", r -> r.d(r.costoStatus, "CALIFICADO"), true, false));
        c.add(new Col("COSTOS x ACCIDENTE", "IMPROCEDENTE", "", r -> r.d(r.costoStatus, "IMPROCEDENTE"), true, false));
        // Como en la hoja de SM: improcedente - calificado, solo en el total
        c.add(new Col("COSTOS x ACCIDENTE", "COMPARATIVO COSTO RT & IMPROCEDENTE", "",
                r -> r.d(r.costoStatus, "IMPROCEDENTE").subtract(r.d(r.costoStatus, "CALIFICADO")), true, true));

        List<String> status = new ArrayList<>(ReporteAnualAccidentes.STATUS);
        if (rep.statusSinDato()) {
            status.add(ReporteAnualAccidentes.SIN_DATO);
        }
        for (String s : status) {
            c.add(new Col("STATUS", s.toUpperCase(), "", r -> r.n(r.status, s), false, false));
        }
        c.add(new Col("STATUS", "TOTAL", "", r -> r.total, false, false));
        return c;
    }

    /** Un grupo de conteos: un valor por columna, «Sin dato» si hace falta y su total. */
    private static void conteo(List<Col> c, String grupo, List<String> valores, boolean sinDato,
                               Function<Renglon, Map<String, Long>> mapa) {
        List<String> todos = new ArrayList<>(valores);
        if (sinDato) {
            todos.add(ReporteAnualAccidentes.SIN_DATO);
        }
        for (String v : todos) {
            c.add(new Col(grupo, "", v.equals(ReporteAnualAccidentes.SIN_DATO) ? "SIN DATO" : v,
                    r -> r.n(mapa.apply(r), v), false, false));
        }
        c.add(new Col(grupo, "", "TOTAL", r -> r.suma(mapa.apply(r)), false, false));
    }

    private static void hojaGeneral(XSSFWorkbook wb, Reporte rep, String predio, Estilos e) {
        Sheet s = wb.createSheet("GENERAL");
        List<Col> cols = columnas(rep);

        celda(s, 0, 0, "REPORTE ANUAL DE ACCIDENTES DE TRABAJO", e.titulo);
        celda(s, 1, 0, rep.anio() + (predio == null ? "" : " · " + predio), e.titulo);
        celda(s, 2, 0, "Portal Salud · generado el " + LocalDate.now(MEXICO) + " · ver hojas «Detalle» y «Notas»", null);

        // Encabezados: fila 3 grupo, 4 subgrupo, 5 hoja; PREDIO ocupa las tres
        Row r3 = s.createRow(3);
        Row r4 = s.createRow(4);
        Row r5 = s.createRow(5);
        for (Row r : List.of(r3, r4, r5)) {
            r.createCell(0).setCellStyle(e.encabezado);
        }
        r3.getCell(0).setCellValue("PREDIO");
        s.addMergedRegion(new CellRangeAddress(3, 5, 0, 0));
        for (int i = 0; i < cols.size(); i++) {
            Col c = cols.get(i);
            int x = i + 1;
            for (Row r : List.of(r3, r4, r5)) {
                r.createCell(x).setCellStyle(e.encabezado);
            }
            r3.getCell(x).setCellValue(c.grupo());
            r4.getCell(x).setCellValue(c.sub());
            r5.getCell(x).setCellValue(c.hoja());
        }
        combinar(s, cols);

        int fila = FILA_DATOS;
        for (Renglon r : rep.predios()) {
            renglon(s.createRow(fila++), r, cols, e, false);
        }
        renglon(s.createRow(fila), rep.total(), cols, e, true);

        s.setColumnWidth(0, 22 * 256);
        for (int i = 0; i < cols.size(); i++) {
            s.setColumnWidth(i + 1, (cols.get(i).moneda() ? 14 : 9) * 256);
        }
        s.createFreezePane(1, FILA_DATOS);
    }

    /**
     * Combina los encabezados iguales y seguidos, como en la hoja de SM: el grupo en la fila 3, el
     * subgrupo en la 4 (dentro de su grupo) y, si una columna no tiene mas niveles abajo, se alarga.
     */
    private static void combinar(Sheet s, List<Col> cols) {
        int i = 0;
        while (i < cols.size()) {
            int j = i;
            while (j + 1 < cols.size() && cols.get(j + 1).grupo().equals(cols.get(i).grupo())) {
                j++;
            }
            boolean sinSub = cols.subList(i, j + 1).stream().allMatch(c -> c.sub().isEmpty());
            if (sinSub) {
                // grupo arriba (filas 3-4) y cada hoja abajo
                rango(s, 3, 4, i + 1, j + 1);
            } else {
                rango(s, 3, 3, i + 1, j + 1);
                int a = i;
                while (a <= j) {
                    int b = a;
                    while (b + 1 <= j && cols.get(b + 1).sub().equals(cols.get(a).sub())) {
                        b++;
                    }
                    boolean sinHoja = cols.subList(a, b + 1).stream().allMatch(c -> c.hoja().isEmpty());
                    rango(s, 4, sinHoja ? 5 : 4, a + 1, b + 1);
                    a = b + 1;
                }
            }
            i = j + 1;
        }
    }

    private static void rango(Sheet s, int f1, int f2, int c1, int c2) {
        if (f1 != f2 || c1 != c2) {
            s.addMergedRegion(new CellRangeAddress(f1, f2, c1, c2));
        }
    }

    private static void renglon(Row row, Renglon r, List<Col> cols, Estilos e, boolean esTotal) {
        row.createCell(0).setCellValue(r.predio);
        row.getCell(0).setCellStyle(esTotal ? e.totalTexto : e.texto);
        for (int i = 0; i < cols.size(); i++) {
            Col c = cols.get(i);
            var cell = row.createCell(i + 1);
            if (c.soloTotal() && !esTotal) {
                cell.setCellStyle(e.entero);
                continue;
            }
            Object v = c.valor().apply(r);
            double d = v instanceof BigDecimal b ? b.doubleValue() : ((Number) v).doubleValue();
            cell.setCellValue(d);
            cell.setCellStyle(esTotal ? (c.moneda() ? e.totalMoneda : e.totalEntero) : (c.moneda() ? e.moneda : e.entero));
        }
    }

    // ------------------------------------------------------------------------------------------
    //  Hoja Detalle y Notas
    // ------------------------------------------------------------------------------------------

    private static void hojaDetalle(XSSFWorkbook wb, List<FilaReporte> filas, int anio, String predio, Estilos e) {
        Sheet s = wb.createSheet("Detalle");
        celda(s, 0, 0, "Accidentes " + anio + (predio == null ? "" : " · " + predio) + " · uno por renglón", e.titulo);
        String[] h = {"Fecha", "Origen", "NSS", "Nombre", "Predio", "Cuenta", "Área", "Puesto", "Género",
                "Tipo de riesgo", "Causa de RT", "Status de calificación", "SDI", "Días", "Costo"};
        Row hr = s.createRow(2);
        for (int i = 0; i < h.length; i++) {
            hr.createCell(i).setCellValue(h[i]);
            hr.getCell(i).setCellStyle(e.encabezado);
        }
        int r = 3;
        for (FilaReporte f : filas) {
            Row row = s.createRow(r++);
            if (f.fecha() != null) {
                row.createCell(0).setCellValue(f.fecha());
                row.getCell(0).setCellStyle(e.fecha);
            }
            row.createCell(1).setCellValue("PORTAL".equals(f.fuente()) ? "Portal" : "Excel");
            row.createCell(2).setCellValue(nvl(f.nss()));
            row.createCell(3).setCellValue(nvl(f.nombre()));
            row.createCell(4).setCellValue(nvl(f.predio()));
            row.createCell(5).setCellValue(nvl(f.cuenta()));
            row.createCell(6).setCellValue(nvl(f.area()));
            row.createCell(7).setCellValue(nvl(f.puesto()));
            row.createCell(8).setCellValue(nvl(f.genero()));
            row.createCell(9).setCellValue(nvl(f.tipoRiesgo()));
            row.createCell(10).setCellValue(nvl(f.causaRt()));
            row.createCell(11).setCellValue(nvl(f.statusCalificacion()));
            numero(row, 12, f.sdi(), e.moneda);
            if (f.dias() != null) {
                row.createCell(13).setCellValue(f.dias());
                row.getCell(13).setCellStyle(e.entero);
            }
            numero(row, 14, f.costo(), e.moneda);
        }
        int[] anchos = {11, 8, 13, 34, 16, 24, 18, 24, 8, 12, 26, 18, 11, 7, 12};
        for (int i = 0; i < anchos.length; i++) {
            s.setColumnWidth(i, anchos[i] * 256);
        }
        s.createFreezePane(0, 3);
        s.setAutoFilter(new CellRangeAddress(2, Math.max(2, r - 1), 0, h.length - 1));
    }

    private static void hojaNotas(XSSFWorkbook wb, Reporte rep, Estilos e) {
        Sheet s = wb.createSheet("Notas");
        Renglon t = rep.total();
        String[] notas = {
                "Cómo se arma este reporte",
                "Sale de los accidentes capturados en el portal más los cargados de los Excel de Servicio Médico; "
                        + "si un accidente está en los dos, cuenta una sola vez (manda el del portal).",
                "El mes es el de la fecha del accidente.",
                "Calificado e improcedente salen del status de calificación de cada accidente "
                        + "(no de la columna donde se capturaban las fechas del ST7 y ST2).",
                "Días y costo son los guardados en cada accidente; el reporte no los vuelve a calcular.",
                "«Comparativo costo RT & improcedente» = costo improcedente − costo calificado (solo en el total), "
                        + "igual que en la hoja GENERAL de Servicio Médico.",
                "Causa de RT: solo cuenta los accidentes que la traen capturada.",
                "«Sin dato»: accidentes sin ese dato capturado; aparece solo si hay alguno.",
                "",
                "Costo que no entra en las columnas de costos de la hoja GENERAL",
                "Pendiente: " + moneda(t.d(t.costoStatus, "PENDIENTE")),
                "Baja / inconcluso: " + moneda(t.d(t.costoStatus, "BAJA INCONCLUSO")),
                "Sin status: " + moneda(t.d(t.costoStatus, ReporteAnualAccidentes.SIN_DATO)),
        };
        for (int i = 0; i < notas.length; i++) {
            boolean titulo = i == 0 || i == 9;
            celda(s, i, 0, notas[i], titulo ? e.titulo : null);
        }
        s.setColumnWidth(0, 120 * 256);
    }

    // ------------------------------------------------------------------------------------------

    private static void celda(Sheet s, int fila, int col, String texto, CellStyle estilo) {
        Row r = s.getRow(fila) == null ? s.createRow(fila) : s.getRow(fila);
        r.createCell(col).setCellValue(texto);
        if (estilo != null) {
            r.getCell(col).setCellStyle(estilo);
        }
    }

    private static void numero(Row row, int c, BigDecimal v, CellStyle estilo) {
        if (v != null) {
            row.createCell(c).setCellValue(v.doubleValue());
            row.getCell(c).setCellStyle(estilo);
        }
    }

    private static String moneda(BigDecimal v) {
        return String.format(java.util.Locale.US, "$%,.2f", v);
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle encabezado;
        final CellStyle texto;
        final CellStyle entero;
        final CellStyle moneda;
        final CellStyle fecha;
        final CellStyle totalTexto;
        final CellStyle totalEntero;
        final CellStyle totalMoneda;

        Estilos(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();
            Font negrita = wb.createFont();
            negrita.setBold(true);
            Font chica = wb.createFont();
            chica.setBold(true);
            chica.setFontHeightInPoints((short) 8);

            titulo = wb.createCellStyle();
            titulo.setFont(negrita);

            encabezado = borde(wb.createCellStyle());
            encabezado.setFont(chica);
            encabezado.setWrapText(true);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            encabezado.setVerticalAlignment(VerticalAlignment.CENTER);
            encabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            texto = borde(wb.createCellStyle());
            entero = borde(wb.createCellStyle());
            entero.setDataFormat(df.getFormat("#,##0;-#,##0;\"\""));
            moneda = borde(wb.createCellStyle());
            moneda.setDataFormat(df.getFormat("$#,##0.00;-$#,##0.00;\"\""));
            fecha = wb.createCellStyle();
            fecha.setDataFormat(df.getFormat("dd/mm/yyyy"));

            totalTexto = borde(wb.createCellStyle());
            totalTexto.setFont(negrita);
            totalEntero = borde(wb.createCellStyle());
            totalEntero.cloneStyleFrom(entero);
            totalEntero.setFont(negrita);
            totalMoneda = borde(wb.createCellStyle());
            totalMoneda.cloneStyleFrom(moneda);
            totalMoneda.setFont(negrita);
        }

        private static CellStyle borde(CellStyle c) {
            c.setBorderTop(BorderStyle.THIN);
            c.setBorderBottom(BorderStyle.THIN);
            c.setBorderLeft(BorderStyle.THIN);
            c.setBorderRight(BorderStyle.THIN);
            return c;
        }
    }
}
