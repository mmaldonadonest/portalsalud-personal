package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.dto.DashboardResumenGeneralDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualCombinadoDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Resumen General: compuesto de los 4 dashboards ya existentes (Incapacidades,
 * Accidentabilidad, Consulta/Morbilidad, Examen). No tiene WS propio - simplemente
 * llama a los 4 servicios ya construidos y combina sus totales + tendencia mensual.
 * Ver docs/plan-tareas-concretas.html, fila "Resumen General".
 */
@Service
public class DashboardResumenGeneralService {

    private final DashboardIncapacidadesService incapacidadesService;
    private final DashboardAccidentesService accidentesService;
    private final DashboardConsultaService consultaService;
    private final DashboardExamenService examenService;

    public DashboardResumenGeneralService(
            DashboardIncapacidadesService incapacidadesService,
            DashboardAccidentesService accidentesService,
            DashboardConsultaService consultaService,
            DashboardExamenService examenService) {
        this.incapacidadesService = incapacidadesService;
        this.accidentesService = accidentesService;
        this.consultaService = consultaService;
        this.examenService = examenService;
    }

    /** Sin corte por predio/cuenta - el que usa /home. */
    public DashboardResumenGeneralDto resumen(String fechaInicial, String fechaFinal) {
        return resumen(fechaInicial, fechaFinal, null, null);
    }

    /**
     * Con corte opcional por predio y/o cuenta: se delega tal cual a los 4 dashboards que
     * combina, para que el total unificado no pueda contradecir a las tarjetas individuales.
     */
    public DashboardResumenGeneralDto resumen(String fechaInicial, String fechaFinal, String predio, String cuenta) {
        var incapacidades = incapacidadesService.resumen(fechaInicial, fechaFinal, predio, cuenta);
        var accidentes = accidentesService.resumen(fechaInicial, fechaFinal, predio, cuenta);
        var consultas = consultaService.resumen(fechaInicial, fechaFinal, predio, cuenta);
        var examenes = examenService.resumen(fechaInicial, fechaFinal, predio, cuenta);

        long total = incapacidades.totalIncapacidades() + accidentes.totalAccidentes()
                + consultas.totalConsultas() + examenes.totalExamenes();

        Map<String, long[]> porMes = new TreeMap<>();
        acumular(porMes, incapacidades.tendenciaMensual(), 0);
        acumular(porMes, accidentes.tendenciaMensual(), 1);
        acumular(porMes, consultas.tendenciaMensual(), 2);
        acumular(porMes, examenes.tendenciaMensual(), 3);

        List<PuntoMensualCombinadoDto> tendencia = new ArrayList<>();
        porMes.forEach((mes, valores) ->
                tendencia.add(new PuntoMensualCombinadoDto(mes, valores[0], valores[1], valores[2], valores[3])));

        return new DashboardResumenGeneralDto(
                fechaInicial, fechaFinal,
                incapacidades.totalIncapacidades(), accidentes.totalAccidentes(),
                consultas.totalConsultas(), examenes.totalExamenes(),
                total, tendencia);
    }

    // indice: 0=incapacidades, 1=accidentes, 2=consultas, 3=examenes
    private static void acumular(Map<String, long[]> porMes, List<PuntoMensualDto> puntos, int indice) {
        for (PuntoMensualDto punto : puntos) {
            long[] valores = porMes.computeIfAbsent(punto.mes(), k -> new long[4]);
            valores[indice] += punto.cantidad();
        }
    }
}
