package com.onest.app.catalog.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.onest.app.catalog.dashboard.repository.VistaAccidenteRepository;
import com.onest.app.catalog.dashboard.repository.VistaAccidenteRepository.FilaReporte;
import com.onest.app.catalog.dashboard.service.ReporteAnualAccidentes.Renglon;
import com.onest.app.catalog.dashboard.service.ReporteAnualAccidentes.Reporte;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * C-044: reporte anual de accidentes. Las pruebas del proyecto estan apagadas en el pom (skipTests):
 * correr con {@code mvn -o test -DskipTests=false -Dtest=ReporteAnualAccidentesTest}.
 */
class ReporteAnualAccidentesTest {

    private static FilaReporte fila(String fuente, String fecha, String predio, String genero, String tipo,
                                    String causa, String status, Integer dias, String costo) {
        return new FilaReporte(fuente, "12345678901", "PRUEBA", fecha == null ? null : LocalDate.parse(fecha),
                predio, "COMEDOR", null, "MANIOBRISTA", genero, tipo, causa, status, new BigDecimal("300"),
                dias, costo == null ? null : new BigDecimal(costo));
    }

    private static final List<FilaReporte> FILAS = List.of(
            // AIFA: calificado laboral, 10 dias, 3,000
            fila("PORTAL", "2026-01-15", "AIFA", "MASC", "LABORAL", "CAIDA", "CALIFICADO", 10, "3000.00"),
            // AIFA: improcedente trayecto con la causa como la guardaba el portal (guion bajo)
            fila("PORTAL", "2026-01-20", "AIFA", "FEM", "TRAYECTO", "ACCID_VIAL", "IMPROCEDENTE", 10, "1260.00"),
            // AIFA: baja / inconcluso tal cual la deja la vista (espacio); su costo no va a calif/improc
            fila("PORTAL", "2026-03-02", "AIFA", "MASC", "LABORAL", null, "BAJA INCONCLUSO", 5, "1500.00"),
            // TOLUCA (Excel): pendiente sin genero
            fila("EXCEL", "2026-03-10", "Toluca", null, "LABORAL", "GOLPE DIRECTO CON O POR OBJETO", "PENDIENTE", null, "0"),
            // sin predio y sin tipo
            fila("EXCEL", "2026-12-31", null, "FEM", null, "OTRO (IMPROCEDENTE)", "CALIFICADO", 4, "1200.00"));

    @Test
    void agrupaPorPredioComoLaHojaGeneral() {
        Reporte rep = ReporteAnualAccidentes.armar(2026, FILAS);

        assertEquals(List.of("AIFA", "TOLUCA", "Sin asignar"), rep.predios().stream().map(r -> r.predio).toList());
        Renglon aifa = rep.predios().get(0);
        assertEquals(3, aifa.total);
        assertEquals(2, aifa.meses[0]);
        assertEquals(1, aifa.meses[2]);
        assertEquals(2, aifa.n(aifa.tipo, "LABORAL"));
        assertEquals(1, aifa.n(aifa.genero, "FEM"));
        assertEquals(10, aifa.n(aifa.dias, "CALIFICADO|LABORAL"));
        assertEquals(new BigDecimal("1260.00"), aifa.d(aifa.costo, "IMPROCEDENTE|TRAYECTO"));
        assertEquals(1, aifa.n(aifa.causa, "ACCID VIAL"));
        assertEquals(2, aifa.suma(aifa.causa), "la baja sin causa no cuenta en causas");
        assertEquals(1, aifa.n(aifa.status, "BAJA INCONCLUSO"));
        assertEquals(new BigDecimal("1500.00"), aifa.d(aifa.costoStatus, "BAJA INCONCLUSO"));
    }

    @Test
    void totalYSinDato() {
        Reporte rep = ReporteAnualAccidentes.armar(2026, FILAS);
        Renglon t = rep.total();
        assertEquals(5, t.total);
        assertEquals(1, t.meses[11]);
        assertTrue(rep.tipoSinDato());
        assertTrue(rep.generoSinDato());
        assertFalse(rep.statusSinDato());
        // el calificado sin tipo cuenta en el costo calificado pero no en el bloque por tipo
        assertEquals(new BigDecimal("4200.00"), t.d(t.costoStatus, "CALIFICADO"));
        assertEquals(10, t.n(t.dias, "CALIFICADO|LABORAL"));
        assertEquals(List.of("COMEDOR"), rep.cuentas());
        assertEquals(List.of("Sin dato"), rep.areas());
    }

    @Test
    void normaliza() {
        assertEquals("BAJA INCONCLUSO", ReporteAnualAccidentes.status("BAJA_INCONCLUSO"));
        assertEquals("CALIFICADO", ReporteAnualAccidentes.status(" calificado "));
        assertEquals("Sin dato", ReporteAnualAccidentes.status("OTRA COSA"));
        assertEquals("GOLPE DIRECTO CON O POR OBJETO", ReporteAnualAccidentes.causa("GOLPE_DIRECTO"));
        assertEquals("OTRO (IMPROCEDENTE)", ReporteAnualAccidentes.causa("OTRO_IMPROCEDENTE"));
        assertEquals(null, ReporteAnualAccidentes.causa("12"));
        assertEquals("TRAYECTO", ReporteAnualAccidentes.tipo("Trayecto"));
    }

    @Test
    void excelSeArmaYCuadra() throws Exception {
        VistaAccidenteRepository repo = new VistaAccidenteRepository(null) {
            @Override
            public List<FilaReporte> filasReporte(LocalDate desde, LocalDate hasta, String predio) {
                assertEquals(LocalDate.of(2026, 1, 1), desde);
                assertEquals(LocalDate.of(2026, 12, 31), hasta);
                return FILAS;
            }
        };
        var provider = new StaticListableBeanFactory(Map.of("v", repo)).getBeanProvider(VistaAccidenteRepository.class);
        byte[] xlsx = new ReporteAnualAccidentesService(provider).excel(2026, null);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertEquals(List.of("GENERAL", "Detalle", "Notas"),
                    List.of(wb.getSheetName(0), wb.getSheetName(1), wb.getSheetName(2)));
            Sheet g = wb.getSheet("GENERAL");
            // encabezado de 3 niveles: leer la columna por grupo + hoja
            Map<String, Integer> col = new HashMap<>();
            String grupo = "";
            String sub = "";
            for (int c = 1; c < g.getRow(5).getLastCellNum(); c++) {
                String g3 = g.getRow(3).getCell(c).getStringCellValue();
                String g4 = g.getRow(4).getCell(c).getStringCellValue();
                grupo = g3.isEmpty() ? grupo : g3;
                sub = g4.isEmpty() && !g3.isEmpty() ? "" : (g4.isEmpty() ? sub : g4);
                col.put(grupo + "/" + sub + "/" + g.getRow(5).getCell(c).getStringCellValue(), c);
            }
            Row total = g.getRow(6 + 3);
            assertEquals("TOTAL", total.getCell(0).getStringCellValue());
            assertEquals(5, (int) total.getCell(col.get("MES REPORTADO//TOTAL")).getNumericCellValue());
            assertEquals(1, (int) total.getCell(col.get("TIPO DE RIESGO//SIN DATO")).getNumericCellValue());
            assertEquals(3000.0, total.getCell(col.get("CALIFICADO/LABORAL/COSTO")).getNumericCellValue());
            assertEquals(4200.0, total.getCell(col.get("COSTOS x ACCIDENTE/CALIFICADO/")).getNumericCellValue());
            // comparativo = improcedente - calificado, solo en el total
            assertEquals(1260.0 - 4200.0,
                    total.getCell(col.get("COSTOS x ACCIDENTE/COMPARATIVO COSTO RT & IMPROCEDENTE/")).getNumericCellValue());
            assertEquals(5, (int) total.getCell(col.get("STATUS/TOTAL/")).getNumericCellValue());
            assertEquals(5, wb.getSheet("Detalle").getLastRowNum() - 2);
        }
    }
}
