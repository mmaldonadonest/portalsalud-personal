package com.onest.app.catalog.dashboard.service;

import java.time.LocalDate;
import java.util.List;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.DashboardConsumoDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import com.onest.app.catalog.dashboard.repository.BitacoraHistoricoRepository;
import org.springframework.stereotype.Service;

/**
 * Consumo de medicamentos. <b>El unico modulo que no fusiona dos origenes.</b>
 *
 * <p>Los otros ocho suman lo que se captura en el portal con el historico de los Excel. Aqui no
 * hay nada que sumar: el portal no captura consumo de medicamentos, y el modulo que el menu llama
 * "Inventario" es otra cosa &mdash;el control de kits de antidoping&mdash;. Todo lo que se ve
 * viene de {@code SERV_MED_BITACORA_METRICA}.
 *
 * <p>Tambien es el unico que lee de esa tabla y no de {@code _EVENTO}: su renglon no es una
 * persona sino un medicamento.
 */
@Service
public class DashboardConsumoService {

    /** Cuantos medicamentos entran al ranking antes de agrupar el resto. */
    private static final int TOP_MEDICAMENTOS = 15;

    private final BitacoraHistoricoRepository historico;

    public DashboardConsumoService(BitacoraHistoricoRepository historico) {
        this.historico = historico;
    }

    public DashboardConsumoDto resumen(String fechaInicial, String fechaFinal, String predio) {
        if (!historico.hayConsumo()) {
            return new DashboardConsumoDto(fechaInicial, fechaFinal, 0, 0,
                    List.of(), List.of(), List.of(), List.of());
        }
        LocalDate desde = FechaFiltro.aFecha(fechaInicial).orElse(null);
        LocalDate hasta = FechaFiltro.aFecha(fechaFinal).orElse(null);

        long[] totales = historico.totalesConsumo(desde, hasta, predio);
        List<ConteoSimpleDto> porMedicamento =
                historico.consumoPorMedicamento(desde, hasta, predio, TOP_MEDICAMENTOS);
        List<ConteoSimpleDto> porPredio = historico.consumoPorPredio(desde, hasta, predio);
        List<PuntoMensualDto> tendencia = historico.consumoTendencia(desde, hasta, predio);

        // El inventario de equipo NO se acota por fecha: es el estado del anio, no un hecho de un
        // mes, y por eso se cargo con MES nulo. Filtrarlo por periodo lo haria desaparecer.
        List<ConteoSimpleDto> equipo = historico.equipoPorPredio(predio);

        return new DashboardConsumoDto(fechaInicial, fechaFinal,
                totales[0], totales[1], porMedicamento, porPredio, tendencia, equipo);
    }
}
