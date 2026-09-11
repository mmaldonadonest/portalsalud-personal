package mx.saludocupacional.portal.report.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Distribucion;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.RankingPredio;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.ResumenEjecutivo;
import mx.saludocupacional.portal.analytics.service.AnalyticsService;
import mx.saludocupacional.portal.shared.audit.AuditAction;
import mx.saludocupacional.portal.shared.audit.AuditService;
import mx.saludocupacional.portal.shared.exception.BusinessRuleException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Generación de reportes.
 *
 * <p>Toma sus cifras de la capa analítica, la misma que alimenta el dashboard.
 * De ese modo un reporte descargado y la pantalla que lo originó siempre
 * coinciden, sin fórmulas paralelas que puedan divergir.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String MODULO = "reportes";

    private final AnalyticsService analytics;
    private final AuditService auditService;

    /** Reporte ejecutivo en formato de hoja de cálculo. */
    @Transactional(readOnly = true)
    public byte[] ejecutivoExcel(int anio, Integer mes, Long predioId, Long usuarioId) {
        ResumenEjecutivo resumen = analytics.resumen(anio, mes, predioId);
        List<RankingPredio> ranking = analytics.ranking(anio, mes);
        List<Distribucion> causas = analytics.topCausas(anio, mes, predioId, 15);

        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            CellStyle estiloTitulo = estiloDeTitulo(libro);
            CellStyle estiloEncabezado = estiloDeEncabezado(libro);

            escribirResumen(libro, resumen, estiloTitulo, estiloEncabezado);
            escribirRanking(libro, ranking, estiloTitulo, estiloEncabezado);
            escribirCausas(libro, causas, estiloTitulo, estiloEncabezado);

            libro.write(salida);
            auditService.registrar(AuditAction.EXPORT, MODULO, "ReporteEjecutivo",
                    null, null, null, usuarioId, false);
            return salida.toByteArray();

        } catch (IOException ex) {
            throw new BusinessRuleException("No fue posible generar el reporte: " + ex.getMessage());
        }
    }

    /** Ranking de predios en formato de valores separados por comas. */
    @Transactional(readOnly = true)
    public byte[] rankingCsv(int anio, Integer mes, Long usuarioId) {
        List<RankingPredio> ranking = analytics.ranking(anio, mes);

        StringBuilder csv = new StringBuilder();
        csv.append("Predio,Atenciones,Incapacidades,Dias,Accidentes,Examenes,Costo,Riesgo\n");
        for (RankingPredio p : ranking) {
            csv.append(escapar(p.predio())).append(',')
               .append(p.atenciones()).append(',')
               .append(p.incapacidades()).append(',')
               .append(p.diasIncapacidad()).append(',')
               .append(p.accidentes()).append(',')
               .append(p.examenes()).append(',')
               .append(p.costo()).append(',')
               .append(p.riesgo()).append('\n');
        }

        auditService.registrar(AuditAction.EXPORT, MODULO, "RankingPredios",
                null, null, null, usuarioId, false);
        // El indicador de orden de bytes permite que Excel reconozca los acentos.
        return ('﻿' + csv.toString()).getBytes(StandardCharsets.UTF_8);
    }

    private void escribirResumen(Workbook libro, ResumenEjecutivo r,
                                 CellStyle titulo, CellStyle encabezado) {
        Sheet hoja = libro.createSheet("Resumen");
        int f = 0;

        celda(hoja.createRow(f++), 0, "Reporte ejecutivo de salud ocupacional", titulo);
        celda(hoja.createRow(f++), 0, "Periodo: " + r.periodo(), null);
        f++;

        Row cabecera = hoja.createRow(f++);
        celda(cabecera, 0, "Indicador", encabezado);
        celda(cabecera, 1, "Valor", encabezado);

        f = agregarDato(hoja, f, "Atenciones médicas", r.atenciones());
        f = agregarDato(hoja, f, "Personas incapacitadas", r.personasIncapacitadas());
        f = agregarDato(hoja, f, "Días de incapacidad", r.diasIncapacidad());
        f = agregarDato(hoja, f, "Horas no trabajadas", r.horasNoTrabajadas());
        f = agregarDato(hoja, f, "Accidentes", r.accidentes());
        f = agregarDato(hoja, f, "Accidentes laborales", r.accidentesLaborales());
        f = agregarDato(hoja, f, "Accidentes de trayecto", r.accidentesTrayecto());
        f = agregarDato(hoja, f, "Exámenes médicos", r.examenes());
        f = agregarDato(hoja, f, "Exámenes aptos", r.examenesAptos());
        f = agregarDato(hoja, f, "Exámenes no aptos", r.examenesNoAptos());
        f = agregarDato(hoja, f, "Exámenes condicionados", r.examenesCondicionados());
        f = agregarDato(hoja, f, "Pruebas de antidoping", r.pruebasAntidoping());
        f = agregarDato(hoja, f, "Casos de maternidad", r.casosMaternidad());

        Row costo = hoja.createRow(f);
        celda(costo, 0, "Costo de incapacidades", null);
        costo.createCell(1).setCellValue(r.costoIncapacidades().doubleValue());

        hoja.autoSizeColumn(0);
        hoja.autoSizeColumn(1);
    }

    private void escribirRanking(Workbook libro, List<RankingPredio> ranking,
                                 CellStyle titulo, CellStyle encabezado) {
        Sheet hoja = libro.createSheet("Predios");
        int f = 0;

        celda(hoja.createRow(f++), 0, "Ranking de predios", titulo);
        f++;

        Row cabecera = hoja.createRow(f++);
        String[] columnas = {"Predio", "Atenciones", "Incapacidades", "Días",
                             "Accidentes", "Exámenes", "Costo", "Riesgo"};
        for (int c = 0; c < columnas.length; c++) {
            celda(cabecera, c, columnas[c], encabezado);
        }

        for (RankingPredio p : ranking) {
            Row fila = hoja.createRow(f++);
            celda(fila, 0, p.predio(), null);
            fila.createCell(1).setCellValue(p.atenciones());
            fila.createCell(2).setCellValue(p.incapacidades());
            fila.createCell(3).setCellValue(p.diasIncapacidad());
            fila.createCell(4).setCellValue(p.accidentes());
            fila.createCell(5).setCellValue(p.examenes());
            fila.createCell(6).setCellValue(p.costo().doubleValue());
            celda(fila, 7, p.riesgo(), null);
        }
        for (int c = 0; c < columnas.length; c++) {
            hoja.autoSizeColumn(c);
        }
    }

    private void escribirCausas(Workbook libro, List<Distribucion> causas,
                                CellStyle titulo, CellStyle encabezado) {
        Sheet hoja = libro.createSheet("Causas");
        int f = 0;

        celda(hoja.createRow(f++), 0, "Principales causas de atención", titulo);
        f++;

        Row cabecera = hoja.createRow(f++);
        celda(cabecera, 0, "Causa", encabezado);
        celda(cabecera, 1, "Atenciones", encabezado);
        celda(cabecera, 2, "Porcentaje", encabezado);

        for (Distribucion d : causas) {
            Row fila = hoja.createRow(f++);
            celda(fila, 0, d.categoria(), null);
            fila.createCell(1).setCellValue(d.valor());
            fila.createCell(2).setCellValue(d.porcentaje().doubleValue());
        }
        hoja.autoSizeColumn(0);
        hoja.autoSizeColumn(1);
        hoja.autoSizeColumn(2);
    }

    private int agregarDato(Sheet hoja, int indiceFila, String etiqueta, long valor) {
        Row fila = hoja.createRow(indiceFila);
        celda(fila, 0, etiqueta, null);
        fila.createCell(1).setCellValue(valor);
        return indiceFila + 1;
    }

    private void celda(Row fila, int columna, String texto, CellStyle estilo) {
        var celda = fila.createCell(columna);
        celda.setCellValue(texto);
        if (estilo != null) {
            celda.setCellStyle(estilo);
        }
    }

    private CellStyle estiloDeTitulo(Workbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 14);

        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }

    private CellStyle estiloDeEncabezado(Workbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());

        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    /** Entrecomilla el texto si contiene separadores que romperían el formato. */
    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.contains(",") || texto.contains("\"")
                ? '"' + texto.replace("\"", "\"\"") + '"'
                : texto;
    }
}
