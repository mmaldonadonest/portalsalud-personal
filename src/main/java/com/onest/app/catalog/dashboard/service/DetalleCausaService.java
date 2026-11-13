package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.repository.VistaAtencionRepository;
import com.onest.app.catalog.predio.service.PredioService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Detalle de UNA causa de atencion (13-nov-2026). SM lo pidio para FISIOTERAPIA, con las mismas
 * graficas de su reporte en PowerPoint: acumulado mensual, por predio, por rango de edad, por genero,
 * por predio de un mes, y exportar a Excel. Sirve para cualquier causa del ranking de Analisis > Causas.
 *
 * <p>Sale de SERV_MED_V_ATENCION (portal + Excel, "manda el portal"), igual que el ranking de causas:
 * la barra que se pica y el total del detalle cuentan exactamente lo mismo.
 */
@Service
public class DetalleCausaService {

    static final String[] MESES = {"ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"};
    private static final List<String> RANGOS = List.of("18-25", "26-35", "36-45", "46-55", "55+", "Sin dato");

    private final ObjectProvider<VistaAtencionRepository> vista;
    private final PredioService predios;

    public DetalleCausaService(ObjectProvider<VistaAtencionRepository> vista, PredioService predios) {
        this.vista = vista;
        this.predios = predios;
    }

    /** Un predio con sus 12 meses del anio (ENE..DIC) y el total. */
    public record PredioMeses(String predio, long[] meses, long total) {
    }

    public record Detalle(String causa, int anio, String predio, long atenciones, long personas,
                          long[] mensual, List<PredioMeses> porPredio, List<ConteoSimpleDto> porEdad,
                          List<ConteoSimpleDto> porGenero) {
    }

    public Detalle detalle(String causa, int anio, String predio) {
        if (causa == null || causa.isBlank()) {
            throw new IllegalArgumentException("Falta la causa.");
        }
        if (anio < 2000 || anio > LocalDate.now().getYear() + 1) {
            throw new IllegalArgumentException("Año fuera de rango: " + anio);
        }
        VistaAtencionRepository v = vista.getIfAvailable();
        if (v == null) {
            // Sin la vista no hay forma honesta de cortar portal + Excel por causa
            throw new IllegalStateException("El detalle por causa requiere portal.historico.fuente=VISTA.");
        }
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);
        String c = causa.trim();

        long[] totales = v.totales(desde, hasta, predio, null, c);

        long[] mensual = new long[12];
        v.tendenciaMensual(desde, hasta, predio, null, c).forEach(p -> {
            int m = mesIndice(p.mes());
            if (m >= 0) {
                mensual[m] += p.cantidad();
            }
        });

        // Todos los predios del catalogo, aunque esten en cero: la grafica de SM los muestra asi
        // (CHARCON 0, FLORA 0...) y un predio ausente se lee como "no se capturo"
        Map<String, long[]> matriz = new LinkedHashMap<>();
        if (predio == null || predio.isBlank()) {
            predios.listarPredios().stream()
                    .map(p -> p.nombre())
                    .filter(n -> n != null && !n.isBlank())
                    .sorted(String::compareToIgnoreCase)
                    .forEach(n -> matriz.put(n.trim().toUpperCase(Locale.ROOT), new long[12]));
        }
        Map<String, String> nombres = new LinkedHashMap<>();
        matriz.keySet().forEach(k -> nombres.put(k, nombreCatalogo(k)));
        for (Object[] f : v.tendenciaPorPredio(desde, hasta, predio, null, c)) {
            String nombre = (String) f[0];
            String clave = nombre.trim().toUpperCase(Locale.ROOT);
            int m = mesIndice((String) f[1]);
            if (m < 0) {
                continue;
            }
            matriz.computeIfAbsent(clave, k -> new long[12])[m] += (Long) f[2];
            nombres.putIfAbsent(clave, nombre.trim());
        }
        List<PredioMeses> porPredio = new ArrayList<>();
        matriz.forEach((clave, meses) -> {
            long total = 0;
            for (long n : meses) {
                total += n;
            }
            // "SIN DATO" del catalogo no es un predio: solo se ve si de verdad trae atenciones
            if (total == 0 && "SIN DATO".equals(clave)) {
                return;
            }
            porPredio.add(new PredioMeses(nombres.getOrDefault(clave, clave), meses, total));
        });

        Map<String, Long> edades = new LinkedHashMap<>();
        v.porDimension("EDAD", desde, hasta, predio, null, c).forEach(e -> edades.merge(e.clave(), e.cantidad(), Long::sum));
        List<ConteoSimpleDto> porEdad = new ArrayList<>();
        for (String r : RANGOS) {
            Long n = edades.get(r);
            if (n != null || !"Sin dato".equals(r)) {
                porEdad.add(new ConteoSimpleDto(r, n == null ? 0 : n));   // rangos en cero tambien se ven
            }
        }

        return new Detalle(c, anio, predio == null || predio.isBlank() ? null : predio.trim(), totales[0], totales[1],
                mensual, porPredio, porEdad, v.porDimension("GENERO", desde, hasta, predio, null, c));
    }

    /** Nombre del catalogo tal cual (MACRO I), no la clave en mayusculas. */
    private String nombreCatalogo(String clave) {
        return predios.listarPredios().stream()
                .map(p -> p.nombre())
                .filter(n -> n != null && n.trim().toUpperCase(Locale.ROOT).equals(clave))
                .map(String::trim)
                .findFirst().orElse(clave);
    }

    private static int mesIndice(String yyyyMm) {
        if (yyyyMm == null || yyyyMm.length() < 7) {
            return -1;
        }
        try {
            int m = Integer.parseInt(yyyyMm.substring(5, 7));
            return m >= 1 && m <= 12 ? m - 1 : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /** Excel con una hoja por grafica (mismos numeros que la pantalla). */
    public byte[] excel(Detalle d) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle negrita = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            negrita.setFont(f);
            String alcance = d.causa() + " · " + d.anio() + (d.predio() == null ? " · todos los predios" : " · " + d.predio());

            Sheet s = hoja(wb, "Acumulado mensual", alcance, negrita, "Mes", "Atenciones");
            int r = 3;
            for (int m = 0; m < 12; m++) {
                fila(s, r++, MESES[m], d.mensual()[m]);
            }
            total(s, r, negrita, d.atenciones());

            s = hoja(wb, "Por predio", alcance, negrita, "Predio", "Atenciones");
            r = 3;
            for (PredioMeses p : d.porPredio()) {
                fila(s, r++, p.predio(), p.total());
            }
            total(s, r, negrita, d.porPredio().stream().mapToLong(PredioMeses::total).sum());

            s = hoja(wb, "Por edad", alcance, negrita, "Rango de edad", "Atenciones");
            r = 3;
            for (ConteoSimpleDto c : d.porEdad()) {
                fila(s, r++, c.clave(), c.cantidad());
            }

            s = hoja(wb, "Por genero", alcance, negrita, "Género", "Atenciones", "%");
            r = 3;
            long totalGenero = d.porGenero().stream().mapToLong(ConteoSimpleDto::cantidad).sum();
            for (ConteoSimpleDto c : d.porGenero()) {
                Row row = fila(s, r++, c.clave(), c.cantidad());
                row.createCell(2).setCellValue(totalGenero == 0 ? 0 : Math.round(c.cantidad() * 1000.0 / totalGenero) / 10.0);
            }

            // Predio x mes: de aqui sale la grafica «por predio» de cualquier mes
            String[] enc = new String[14];
            enc[0] = "Predio";
            System.arraycopy(MESES, 0, enc, 1, 12);
            enc[13] = "Total";
            s = hoja(wb, "Predio por mes", alcance, negrita, enc);
            r = 3;
            long[] col = new long[12];
            for (PredioMeses p : d.porPredio()) {
                Row row = s.createRow(r++);
                row.createCell(0).setCellValue(p.predio());
                for (int m = 0; m < 12; m++) {
                    row.createCell(m + 1).setCellValue(p.meses()[m]);
                    col[m] += p.meses()[m];
                }
                row.createCell(13).setCellValue(p.total());
            }
            Row t = s.createRow(r);
            t.createCell(0).setCellValue("Total");
            t.getCell(0).setCellStyle(negrita);
            long suma = 0;
            for (int m = 0; m < 12; m++) {
                t.createCell(m + 1).setCellValue(col[m]);
                suma += col[m];
            }
            t.createCell(13).setCellValue(suma);
            s.setColumnWidth(0, 24 * 256);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo armar el Excel: " + ex.getMessage(), ex);
        }
    }

    private static Sheet hoja(XSSFWorkbook wb, String nombre, String alcance, CellStyle negrita, String... encabezado) {
        Sheet s = wb.createSheet(nombre);
        s.createRow(0).createCell(0).setCellValue(alcance);
        s.getRow(0).getCell(0).setCellStyle(negrita);
        Row h = s.createRow(2);
        for (int i = 0; i < encabezado.length; i++) {
            h.createCell(i).setCellValue(encabezado[i]);
            h.getCell(i).setCellStyle(negrita);
        }
        s.setColumnWidth(0, 22 * 256);
        s.setColumnWidth(1, 12 * 256);
        return s;
    }

    private static Row fila(Sheet s, int r, String clave, long n) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(clave);
        row.createCell(1).setCellValue(n);
        return row;
    }

    private static void total(Sheet s, int r, CellStyle negrita, long n) {
        Row row = fila(s, r, "Total", n);
        row.getCell(0).setCellStyle(negrita);
        row.getCell(1).setCellStyle(negrita);
    }
}
