package mx.saludocupacional.portal.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.Kpi;
import mx.saludocupacional.portal.analytics.dto.AnalyticsDtos.ResumenEjecutivo;
import mx.saludocupacional.portal.analytics.service.AnalyticsService;
import mx.saludocupacional.portal.analytics.service.InsightEngine;
import mx.saludocupacional.portal.catalog.service.PredioService;
import mx.saludocupacional.portal.shared.service.PeriodService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista del dashboard ejecutivo.
 *
 * <p>Toma las cifras de la capa analítica, la misma que alimenta la API y los
 * reportes, y las entrega ya resueltas a la plantilla. Las series de las
 * gráficas viajan como un bloque JSON incrustado para evitar una segunda ronda
 * de peticiones al cargar la página.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardViewController {

    private static final List<String> MESES = List.of(
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC");

    private final AnalyticsService analytics;
    private final InsightEngine insights;
    private final PredioService predios;
    private final PeriodService periodos;
    private final ObjectMapper objectMapper;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@RequestParam(required = false) Integer anio,
                            @RequestParam(required = false) Integer mes,
                            @RequestParam(required = false) Long predioId,
                            Model model) {

        int anioConsultado = anio != null ? anio : LocalDate.now().getYear();
        ResumenEjecutivo resumen = analytics.resumen(anioConsultado, mes, predioId);

        model.addAttribute("titulo", "Dashboard Ejecutivo");
        model.addAttribute("vista", "dashboard");
        model.addAttribute("contenido", "views/dashboard :: contenido");

        model.addAttribute("resumen", resumen);
        model.addAttribute("insights", insights.generar(anioConsultado, mes, predioId));
        model.addAttribute("ranking", analytics.ranking(anioConsultado, mes));

        model.addAttribute("anio", anioConsultado);
        model.addAttribute("mes", mes);
        model.addAttribute("predioId", predioId);
        model.addAttribute("anios", periodos.aniosDisponibles());
        model.addAttribute("predios", predios.listarConAliases(true));

        agregarVariaciones(model, resumen);
        model.addAttribute("datosJson", datosParaGraficas(anioConsultado, mes, predioId));

        return "layout/base";
    }

    /** Extrae las variaciones de los indicadores para mostrarlas junto a cada cifra. */
    private void agregarVariaciones(Model model, ResumenEjecutivo resumen) {
        Map<String, BigDecimal> porClave = new HashMap<>();
        for (Kpi kpi : resumen.kpis()) {
            porClave.put(kpi.clave(), kpi.variacion());
        }
        model.addAttribute("variacionAtenciones", porClave.get("atenciones"));
        model.addAttribute("variacionPersonas", porClave.get("personasIncapacitadas"));
        model.addAttribute("variacionDias", porClave.get("diasIncapacidad"));
    }

    /** Series y distribuciones que las gráficas consumen, serializadas en JSON. */
    private String datosParaGraficas(int anio, Integer mes, Long predioId) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("meses", MESES);
        datos.put("serieAtenciones", analytics.serie("atenciones", anio, predioId).valores());
        datos.put("serieDias", analytics.serie("dias", anio, predioId).valores());
        datos.put("tipos", analytics.diasPorTipoIncapacidad(anio, mes, predioId));
        datos.put("causas", analytics.topCausas(anio, mes, predioId, 10));
        datos.put("accidentes", analytics.causasDeAccidente(anio, mes, predioId));

        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException ex) {
            log.warn("No fue posible preparar los datos de las gráficas", ex);
            return "{}";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "views/login";
    }
}
