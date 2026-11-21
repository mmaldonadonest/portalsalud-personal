package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.ConteoDictamenDto;
import com.onest.app.catalog.dashboard.repository.VistaExamenRepository;
import com.onest.app.catalog.dashboard.repository.VistaPretestRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * C-006 (18-nov-2026): Excel del comparativo examenes medicos vs Pretest, por predio y por mes, con el mismo
 * periodo y predio que las tarjetas de Analisis > Examenes. Las cuentas son por registro: no se cruza quien hizo
 * Pretest y no examen (eso seria otro reporte).
 */
@Service
public class ComparativoPretestService {

    private final ObjectProvider<VistaPretestRepository> vista;
    private final ObjectProvider<VistaExamenRepository> examenes;

    public ComparativoPretestService(ObjectProvider<VistaPretestRepository> vista,
                                     ObjectProvider<VistaExamenRepository> examenes) {
        this.vista = vista;
        this.examenes = examenes;
    }

    /** Un renglon de la grafica examen vs Pretest. */
    public record Comparativo(String clave, long examenes, long pretest) {
    }

    /** 19-nov-2026 (pedido de SM): la grafica del mismo comparativo que el Excel, por mes y por predio. */
    public Map<String, List<Comparativo>> grafica(String fechaInicial, String fechaFinal, String predio) {
        VistaPretestRepository v = repositorio();
        LocalDate[] r = rango(fechaInicial, fechaFinal);
        return Map.of("porMes", renglones(v.porMes(r[0], r[1], predio, null)),
                "porPredio", renglones(v.porPredio(r[0], r[1], predio, null)));
    }

    /** 19-nov-2026 (pedido de SM): examenes por genero con su desglose por dictamen. */
    public List<ConteoDictamenDto> porGenero(String fechaInicial, String fechaFinal, String predio) {
        VistaExamenRepository v = examenes.getIfAvailable();
        if (v == null) {
            throw new IllegalStateException("La grafica por genero requiere portal.historico.fuente=VISTA.");
        }
        LocalDate[] r = rango(fechaInicial, fechaFinal);
        return v.porGenero(r[0], r[1], predio, null);
    }

    private static List<Comparativo> renglones(Map<String, long[]> filas) {
        List<Comparativo> out = new ArrayList<>();
        filas.forEach((k, n) -> out.add(new Comparativo(k == null ? "" : k, n[0], n[1])));
        return out;
    }

    private VistaPretestRepository repositorio() {
        VistaPretestRepository v = vista.getIfAvailable();
        if (v == null) {
            throw new IllegalStateException("El comparativo requiere portal.historico.fuente=VISTA.");
        }
        return v;
    }

    private static LocalDate[] rango(String fechaInicial, String fechaFinal) {
        LocalDate desde = FechaFiltro.aFecha(fechaInicial).orElse(null);
        LocalDate hasta = FechaFiltro.aFecha(fechaFinal).orElse(null);
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final es anterior a la inicial.");
        }
        return new LocalDate[] {desde, hasta};
    }

    public byte[] excel(String fechaInicial, String fechaFinal, String predio) {
        VistaPretestRepository v = repositorio();
        LocalDate[] r = rango(fechaInicial, fechaFinal);
        LocalDate desde = r[0];
        LocalDate hasta = r[1];
        String alcance = "Exámenes médicos vs Pretest · " + (fechaInicial == null ? "" : fechaInicial.trim()) + " a "
                + (fechaFinal == null ? "" : fechaFinal.trim())
                + (predio == null || predio.isBlank() ? " · todos los predios" : " · " + predio.trim());
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font f = wb.createFont();
            f.setBold(true);
            CellStyle negrita = wb.createCellStyle();
            negrita.setFont(f);
            CellStyle entero = wb.createCellStyle();
            entero.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            hoja(wb, "Por predio", "Predio", v.porPredio(desde, hasta, predio, null), alcance, negrita, entero);
            hoja(wb, "Por mes", "Mes", v.porMes(desde, hasta, predio, null), alcance, negrita, entero);
            Sheet n = wb.createSheet("Notas");
            String[] notas = {
                    "Exámenes: los capturados en el portal y los del histórico de Excel (la misma vista de las tarjetas).",
                    "Pretest: desde el 18-nov-2026 cuenta cada Pretest guardado.",
                    "Antes del 18-nov-2026 el portal solo conservaba el último Pretest de cada persona: de esas fechas se cuenta uno por persona.",
                    "Predio y cuenta del Pretest salen de los datos del empleado; «Sin asignar» = el empleado no tiene predio.",
                    "Son conteos por registro: no indican si la misma persona hizo los dos."};
            for (int i = 0; i < notas.length; i++) {
                n.createRow(i).createCell(0).setCellValue(notas[i]);
            }
            n.setColumnWidth(0, 120 * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo armar el Excel: " + ex.getMessage(), ex);
        }
    }

    private static void hoja(XSSFWorkbook wb, String nombre, String clave, Map<String, long[]> filas, String alcance,
                             CellStyle negrita, CellStyle entero) {
        Sheet s = wb.createSheet(nombre);
        s.createRow(0).createCell(0).setCellValue(alcance);
        s.getRow(0).getCell(0).setCellStyle(negrita);
        Row h = s.createRow(2);
        String[] cols = {clave, "Exámenes médicos", "Pretest", "Diferencia (exámenes − Pretest)"};
        for (int i = 0; i < cols.length; i++) {
            h.createCell(i).setCellValue(cols[i]);
            h.getCell(i).setCellStyle(negrita);
        }
        int r = 3;
        long te = 0;
        long tp = 0;
        for (Map.Entry<String, long[]> e : filas.entrySet()) {
            Row row = s.createRow(r++);
            row.createCell(0).setCellValue(e.getKey() == null ? "" : e.getKey());
            numeros(row, e.getValue()[0], e.getValue()[1], entero);
            te += e.getValue()[0];
            tp += e.getValue()[1];
        }
        Row t = s.createRow(r);
        t.createCell(0).setCellValue("Total");
        t.getCell(0).setCellStyle(negrita);
        numeros(t, te, tp, entero);
        s.setColumnWidth(0, 32 * 256);
        for (int c = 1; c <= 3; c++) {
            s.setColumnWidth(c, 20 * 256);
        }
        s.createFreezePane(0, 3);
    }

    private static void numeros(Row row, long examenes, long pretest, CellStyle estilo) {
        long[] v = {examenes, pretest, examenes - pretest};
        for (int i = 0; i < v.length; i++) {
            row.createCell(i + 1).setCellValue(v[i]);
            row.getCell(i + 1).setCellStyle(estilo);
        }
    }
}
