package com.onest.excel.carga;

import java.util.ArrayList;
import java.util.List;

import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;
import com.onest.excel.persistencia.BitacoraRepository;

/**
 * Lee la hoja {@code MATERIAL FIJO} de consumibles: el inventario de equipo.
 *
 * <p>Se titula "CONTROL DE EQUIPO" y <b>no mide consumo sino patrimonio</b>. Un baumanometro no se
 * gasta: se recibe, se usa y algun dia se da de baja. Por eso cada equipo trae hasta <b>tres
 * ciclos</b> de recepcion, cantidad y baja &mdash;el encabezado los llama "CAMBIO · DAÑO · AUMENTO
 * MATERIAL"&mdash; y una {@code CANTIDAD TOTAL SURTIDA} que los suma por formula.
 *
 * <pre>
 *   EQUIPO | TIPO | F.RECEPCION | CANTIDAD | F.BAJA | F.RECEPCION | CANTIDAD | F.BAJA | ... | TOTAL
 * </pre>
 *
 * <p><b>Por que un lector propio y no el camino generico.</b> Esta hoja repite tres veces los
 * nombres {@code CANTIDAD} y {@code FECHA DE BAJA}. El camino generico resuelve las columnas por
 * etiqueta, asi que las tres cantidades caerian en la misma clave y solo sobreviviria una. Leerla
 * por posicion relativa a cada {@code FECHA DE RECEPCION} es mas simple y no se presta a eso.
 *
 * <p><b>Decision de servicio medico del 30-sep-2026:</b> un equipo con las celdas vacias
 * <b>cuenta como cero</b>, no como ausente. "Falta capturarlo" es la lectura correcta de un vacio.
 * La formula del total ya devuelve cero sola cuando sus tres sumandos estan vacios, asi que no hay
 * que forzarlo.
 *
 * <p><b>Lo que esta version NO carga:</b> las fechas de recepcion y de baja. Van a
 * {@code SERV_MED_BITACORA_METRICA}, cuya columna {@code VALOR} es numerica y no tiene donde
 * guardarlas. Se dejan fuera a conciencia y no por descuido: lo que el reporte necesita hoy es
 * cuanto equipo hay en cada predio, y meter las fechas obligaria a partir consumibles en dos
 * tablas por un dato que todavia nadie pidio.
 */
public final class LectorEquipo {

    private LectorEquipo() {
    }

    /** Etiqueta que delata el encabezado de esta hoja. */
    private static final String ANCLA = "EQUIPO";
    private static final String RECEPCION = "FECHA DE RECEPCION";
    private static final String CANTIDAD = "CANTIDAD";
    private static final String TOTAL = "CANTIDAD TOTAL SURTIDA";

    private static final int MAX_FILA_ENCABEZADO = 10;

    /** Lee la hoja y devuelve una metrica por (equipo, medida). */
    public static LectorMatrizMensual.Resultado leer(Hoja hoja, long loteId, String familia,
                                                     String predio, int anio) {
        List<BitacoraRepository.MetricaFila> metricas = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        Integer filaEnc = buscarEncabezado(hoja);
        if (filaEnc == null) {
            avisos.add("no se hallo el encabezado de equipo");
            return new LectorMatrizMensual.Resultado(metricas, 0, avisos);
        }
        List<String> enc = hoja.fila(filaEnc);

        // Las columnas de cantidad de cada ciclo: la que sigue a cada FECHA DE RECEPCION
        List<Integer> cantidades = new ArrayList<>();
        Integer colTotal = null;
        for (int c = 0; c < enc.size(); c++) {
            String t = Normalizador.texto(enc.get(c));
            if (t == null) {
                continue;
            }
            if (t.equals(TOTAL)) {
                colTotal = c;
            } else if (t.startsWith(RECEPCION) && c + 1 < enc.size()
                    && CANTIDAD.equals(Normalizador.texto(enc.get(c + 1)))) {
                cantidades.add(c + 1);
            }
        }
        if (cantidades.isEmpty() && colTotal == null) {
            avisos.add("el encabezado no trae columnas de cantidad");
            return new LectorMatrizMensual.Resultado(metricas, 0, avisos);
        }

        int equipos = 0;
        for (int r = filaEnc + 1; r <= hoja.ultimaFila(); r++) {
            String equipo = Normalizador.etiqueta(hoja.texto(r, 0));
            if (equipo == null || equipo.length() < 3 || equipo.startsWith("TOTAL")) {
                continue;
            }
            equipos++;
            for (int i = 0; i < cantidades.size(); i++) {
                Double v = hoja.numero(r, cantidades.get(i));
                metricas.add(metrica(loteId, familia, predio, anio, equipo,
                        "RECEPCION " + (i + 1), v == null ? 0 : v));
            }
            if (colTotal != null) {
                Double v = hoja.numero(r, colTotal);
                metricas.add(metrica(loteId, familia, predio, anio, equipo,
                        "CANTIDAD TOTAL", v == null ? 0 : v));
            }
        }
        return new LectorMatrizMensual.Resultado(metricas, equipos, avisos);
    }

    private static BitacoraRepository.MetricaFila metrica(long loteId, String familia,
                                                          String predio, int anio, String equipo,
                                                          String medida, double valor) {
        // MES va nulo: el inventario de equipo no es mensual, es el estado del anio.
        return new BitacoraRepository.MetricaFila(
                loteId, familia, "EQUIPO", predio, anio, null, equipo, medida, valor);
    }

    private static Integer buscarEncabezado(Hoja hoja) {
        int hasta = Math.min(hoja.ultimaFila(), MAX_FILA_ENCABEZADO);
        for (int r = 0; r <= hasta; r++) {
            for (String v : hoja.fila(r)) {
                if (ANCLA.equals(Normalizador.texto(v))) {
                    return r;
                }
            }
        }
        return null;
    }
}
