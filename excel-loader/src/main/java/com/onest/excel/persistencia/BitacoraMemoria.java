package com.onest.excel.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La bitacora de la carga vieja, pero en memoria: lo que el lector de los Excel originales
 * escribiria en SERV_MED_BITACORA_* se queda en listas.
 *
 * <p>Existe para el modo {@code nss-generar-excel} (12-oct-2026): armar layouts de prueba desde los
 * Excel originales de un predio y un mes (TOLUCA enero 2025) <b>sin tocar la base</b>. Reusa tal
 * cual el lector de {@code CargaService} &mdash;encabezados de tres filas, bloques one-hot, hojas
 * por predio&mdash;, que es lo que costo semanas afinar; solo cambia a donde van los renglones.
 *
 * <p>No es un bean: se instancia a mano. Por eso el {@code JdbcTemplate} del padre va nulo y
 * <b>todos</b> los metodos que lo usarian estan sobreescritos.
 */
public class BitacoraMemoria extends BitacoraRepository {

    /** Lo que se abrio por archivo y hoja, para el reporte. */
    public record Lote(long id, String archivo, String hoja, String familia, String predio, Integer anio) {
    }

    /** Un evento con sus atributos ya colgados, como quedaria tras releerlo de la base. */
    public record Evento(long id, EventoFila fila, List<AtributoFila> atributos) {
    }

    private final Map<Long, Lote> lotes = new LinkedHashMap<>();
    private final Map<Long, Evento> eventos = new LinkedHashMap<>();
    /** loteId -> (fila del Excel -> id del evento), lo que en la base resuelve idsPorFila. */
    private final Map<Long, Map<Integer, Long>> filas = new HashMap<>();
    private final List<MetricaFila> metricas = new ArrayList<>();
    private long siguienteLote = 1;
    private long siguienteEvento = 1;

    public BitacoraMemoria() {
        super(null);
    }

    @Override
    public void verificarEsquema() {
        // sin base no hay esquema que verificar
    }

    @Override
    public boolean loteYaProcesado(String archivo, String hoja) {
        return false;
    }

    @Override
    public long abrirLote(String archivo, String ruta, String checksum, String hoja,
                          String familia, String predio, Integer anio) {
        long id = siguienteLote++;
        lotes.put(id, new Lote(id, archivo, hoja, familia, predio, anio));
        return id;
    }

    @Override
    public void cerrarLote(long loteId, int leidas, int insertadas, int descartadas,
                           Integer acumuladoEsperado, String mensajes, String estado) {
        // el resultado ya viaja en CargaService.Resultado
    }

    @Override
    public int insertarMetricas(List<MetricaFila> nuevas) {
        metricas.addAll(nuevas);
        return nuevas.size();
    }

    @Override
    public int insertarEventos(List<EventoFila> nuevos) {
        for (EventoFila e : nuevos) {
            long id = siguienteEvento++;
            eventos.put(id, new Evento(id, e, new ArrayList<>()));
            filas.computeIfAbsent(e.loteId(), k -> new HashMap<>()).put(e.origenFila(), id);
        }
        return nuevos.size();
    }

    @Override
    public Map<Integer, Long> idsPorFila(long loteId) {
        return filas.getOrDefault(loteId, Map.of());
    }

    @Override
    public int insertarAtributos(List<AtributoFila> atributos, Map<Integer, Long> idsPorFila) {
        int n = 0;
        for (AtributoFila a : atributos) {
            Long id = idsPorFila.get(a.origenFila());
            if (id != null) {
                eventos.get(id).atributos().add(a);
                n++;
            }
        }
        return n;
    }

    @Override
    public Map<String, Integer> conteos() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("LOTE", lotes.size());
        m.put("EVENTO", eventos.size());
        m.put("ATRIBUTO", eventos.values().stream().mapToInt(e -> e.atributos().size()).sum());
        m.put("METRICA", metricas.size());
        return m;
    }

    public List<Evento> eventos() {
        return List.copyOf(eventos.values());
    }

    public List<MetricaFila> metricas() {
        return List.copyOf(metricas);
    }

    public Map<Long, Lote> lotes() {
        return Map.copyOf(lotes);
    }
}
