package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoDto;
import com.onest.app.catalog.dashboard.repository.VistaIncapacidadRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * C-056 (totales de incapacidades y costos) y C-048 (perdida de horas laborales), 17-nov-2026.
 * Un Excel con el periodo y el predio de la pantalla de Analisis > Incapacidades: resumen y una hoja
 * por mes, ramo, rubro, cuenta, predio y trabajador, cada una con incapacidades, dias, horas y costo.
 *
 * <p>Sale de SERV_MED_V_INCAPACIDAD (portal + Excel, "manda el portal"), igual que las tarjetas de
 * la pantalla: el total del Excel y el de las tarjetas son el mismo numero.
 *
 * <p>Horas = dias x 8, la misma jornada que ya usa la tarjeta «Horas no trabajadas». Si algun dia
 * la jornada cambia por predio o turno, se cambia aqui y en esa tarjeta.
 */
@Service
public class TotalesIncapacidadService {

    public static final int HORAS_JORNADA = 8;
    private static final String NSS_CEROS = "00000000000";
    private static final Map<String, String> RAMOS = Map.of(
            "EG", "Enfermedad General", "MAT", "Maternidad", "RT1", "RT1 (riesgo de trabajo)",
            "RT2", "RT2 (riesgo de trayecto)", "INTERNA", "Interna", "profesional", "Enfermedad profesional");

    private final ObjectProvider<VistaIncapacidadRepository> vista;

    public TotalesIncapacidadService(ObjectProvider<VistaIncapacidadRepository> vista) {
        this.vista = vista;
    }

    public byte[] excel(String fechaInicial, String fechaFinal, String predio) {
        VistaIncapacidadRepository v = vista.getIfAvailable();
        if (v == null) {
            // Sin la vista no hay forma de sumar portal + Excel sin contar dos veces
            throw new IllegalStateException("Los totales de incapacidades requieren portal.historico.fuente=VISTA.");
        }
        LocalDate desde = FechaFiltro.aFecha(fechaInicial).orElse(null);
        LocalDate hasta = FechaFiltro.aFecha(fechaFinal).orElse(null);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        String p = predio == null || predio.isBlank() ? null : predio.trim();

        VistaIncapacidadRepository.Totales t = v.totales(desde, hasta, p, null);
        String alcance = "Incapacidades · " + (desde == null ? "inicio" : desde) + " a " + (hasta == null ? "hoy" : hasta)
                + " · " + (p == null ? "todos los predios" : p);

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(wb);

            Sheet s = wb.createSheet("Resumen");
            titulo(s, alcance, e);
            int r = 2;
            r = par(s, r, "Incapacidades", t.incapacidades(), e.entero);
            r = par(s, r, "Personas", t.personas(), e.entero);
            r = par(s, r, "Días", t.dias(), e.entero);
            r = par(s, r, "Horas no trabajadas (días × " + HORAS_JORNADA + ")", t.dias() * HORAS_JORNADA, e.entero);
            r = par(s, r, "Costo", t.costo(), e.moneda);
            par(s, r, "Promedio de días por incapacidad",
                    t.incapacidades() == 0 ? 0 : Math.round(t.dias() * 10.0 / t.incapacidades()) / 10.0, e.decimal);
            s.setColumnWidth(0, 40 * 256);
            s.setColumnWidth(1, 16 * 256);

            hojaConteo(wb, "Por mes", "Mes", v.porMes(desde, hasta, p, null), alcance, e, false);
            hojaConteo(wb, "Por ramo", "Ramo", v.porDimension("RAMO", desde, hasta, p, null), alcance, e, true);
            hojaConteo(wb, "Por rubro", "Rubro", v.porDimension("RUBRO", desde, hasta, p, null), alcance, e, false);
            hojaConteo(wb, "Por cuenta", "Cuenta", v.porDimension("CUENTA", desde, hasta, p, null), alcance, e, false);
            hojaConteo(wb, "Por predio", "Predio", v.porDimension("PREDIO", desde, hasta, p, null), alcance, e, false);
            hojaTrabajador(wb, v.porTrabajador(desde, hasta, p, null), alcance, e);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo armar el Excel: " + ex.getMessage(), ex);
        }
    }

    private static void hojaConteo(XSSFWorkbook wb, String nombre, String clave, List<ConteoDto> filas, String alcance,
                                   Estilos e, boolean etiquetaRamo) {
        Sheet s = wb.createSheet(nombre);
        titulo(s, alcance, e);
        encabezado(s, e, clave, "Incapacidades", "Días", "Horas", "Costo");
        int r = 3;
        long inc = 0;
        long dias = 0;
        double costo = 0;
        for (ConteoDto c : filas) {
            String k = etiquetaRamo ? RAMOS.getOrDefault(c.clave(), c.clave()) : c.clave();
            numeros(s.createRow(r++), k, c.cantidad(), c.diasAutorizados(), c.costo(), e, null);
            inc += c.cantidad();
            dias += c.diasAutorizados();
            costo += c.costo();
        }
        numeros(s.createRow(r), "Total", inc, dias, costo, e, e.negrita);
        anchos(s, 30);
    }

    private static void hojaTrabajador(XSSFWorkbook wb, List<VistaIncapacidadRepository.Trabajador> filas,
                                       String alcance, Estilos e) {
        Sheet s = wb.createSheet("Por trabajador");
        titulo(s, alcance, e);
        encabezado(s, e, "NSS", "Nombre", "Predio", "Cuenta", "Incapacidades", "Días", "Horas", "Costo");
        int r = 3;
        for (VistaIncapacidadRepository.Trabajador w : filas) {
            Row row = s.createRow(r++);
            row.createCell(0).setCellValue(w.nss());
            // 00000000000 = registros sin persona: se juntan en un solo renglon y se dice que son eso
            row.createCell(1).setCellValue(NSS_CEROS.equals(w.nss()) ? "(sin NSS)" : nvl(w.nombre()));
            row.createCell(2).setCellValue(nvl(w.predio()));
            row.createCell(3).setCellValue(nvl(w.cuenta()));
            entero(row, 4, w.incapacidades(), e.entero);
            entero(row, 5, w.dias(), e.entero);
            entero(row, 6, w.dias() * HORAS_JORNADA, e.entero);
            row.createCell(7).setCellValue(w.costo());
            row.getCell(7).setCellStyle(e.moneda);
        }
        s.setColumnWidth(0, 14 * 256);
        s.setColumnWidth(1, 36 * 256);
        s.setColumnWidth(2, 18 * 256);
        s.setColumnWidth(3, 30 * 256);
        for (int c = 4; c <= 7; c++) {
            s.setColumnWidth(c, 14 * 256);
        }
        s.createFreezePane(0, 3);
        s.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(2, Math.max(2, r - 1), 0, 7));
    }

    private static void numeros(Row row, String clave, long inc, long dias, double costo, Estilos e, CellStyle negrita) {
        row.createCell(0).setCellValue(clave);
        entero(row, 1, inc, e.entero);
        entero(row, 2, dias, e.entero);
        entero(row, 3, dias * HORAS_JORNADA, e.entero);
        row.createCell(4).setCellValue(costo);
        row.getCell(4).setCellStyle(e.moneda);
        if (negrita != null) {
            row.getCell(0).setCellStyle(negrita);
        }
    }

    private static void entero(Row row, int c, long n, CellStyle estilo) {
        row.createCell(c).setCellValue(n);
        row.getCell(c).setCellStyle(estilo);
    }

    private static int par(Sheet s, int r, String etiqueta, double valor, CellStyle estilo) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(etiqueta);
        row.createCell(1).setCellValue(valor);
        row.getCell(1).setCellStyle(estilo);
        return r + 1;
    }

    private static void titulo(Sheet s, String alcance, Estilos e) {
        s.createRow(0).createCell(0).setCellValue(alcance);
        s.getRow(0).getCell(0).setCellStyle(e.negrita);
    }

    private static void encabezado(Sheet s, Estilos e, String... columnas) {
        Row h = s.createRow(2);
        for (int i = 0; i < columnas.length; i++) {
            h.createCell(i).setCellValue(columnas[i]);
            h.getCell(i).setCellStyle(e.negrita);
        }
    }

    private static void anchos(Sheet s, int primera) {
        s.setColumnWidth(0, primera * 256);
        for (int c = 1; c <= 4; c++) {
            s.setColumnWidth(c, 14 * 256);
        }
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static final class Estilos {
        final CellStyle negrita;
        final CellStyle entero;
        final CellStyle decimal;
        final CellStyle moneda;

        Estilos(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();
            Font f = wb.createFont();
            f.setBold(true);
            negrita = wb.createCellStyle();
            negrita.setFont(f);
            entero = wb.createCellStyle();
            entero.setDataFormat(df.getFormat("#,##0"));
            decimal = wb.createCellStyle();
            decimal.setDataFormat(df.getFormat("#,##0.0"));
            moneda = wb.createCellStyle();
            moneda.setDataFormat(df.getFormat("$#,##0.00"));
        }
    }
}
