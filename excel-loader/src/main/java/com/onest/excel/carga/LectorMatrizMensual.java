package com.onest.excel.carga;

import java.util.ArrayList;
import java.util.List;

import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;
import com.onest.excel.persistencia.BitacoraRepository;

/**
 * Lee las hojas donde <b>el renglon es una entidad y las columnas son el tiempo</b>.
 *
 * <p>Las nueve familias anteriores comparten una forma: un renglon del Excel es un hecho de una
 * persona en una fecha. Consumibles no. Su renglon es un <b>medicamento</b>, y sus 168 columnas
 * son doce meses de catorce columnas cada uno. Un renglon no es un evento: son doce.
 *
 * <pre>
 *   fila 4   DESCRIPCION | ENERO ................ | FEBRERO ..............| ... | CONSUMO TOTAL
 *   fila 5               | CANTIDAD | LOTE | CADUCIDAD | CONSUMO SEMANAL | CONSUMO ACUMULADO | ...
 *   fila 6               | MES ANT | RECIBIDO |      | 1o 2o 3o 4o 5o TOTAL x LOTE
 * </pre>
 *
 * <p><b>Por que un lector aparte y no el detector generico.</b> El detector busca la fila de grupo
 * por un ancla &mdash;{@code FECHA}, {@code NOMBRE}&mdash; y aqui esa ancla esta en la fila
 * equivocada: {@code FECHA DE CADUCIDAD} vive en la fila 5, asi que el detector tomaria esa como
 * fila de grupo y <b>perderia el mes</b>, que es justo la dimension que da sentido al archivo.
 * Forzarlo habria sido pelearse con una heuristica que funciona bien para lo que fue hecha.
 *
 * <p>Sirve para mas que consumibles: es la misma forma de la hoja {@code REVISIONES} de maternidad
 * y de los reportes de carnet y CAPA de la fase 3.
 */
public final class LectorMatrizMensual {

    private LectorMatrizMensual() {
    }

    /** Los doce meses como los escribe el origen, en orden. */
    private static final List<String> MESES = List.of(
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE");

    /** Hasta donde se busca la fila con los nombres de mes. */
    private static final int MAX_FILA_MESES = 10;

    /** Lo que se guarda de cada mes. Las tres medidas que pidio servicio medico el 30-sep-2026. */
    private static final List<String> MEDIDAS = List.of(
            "CONSUMO SEMANAL", "CONSUMO ACUMULADO");

    /**
     * Resultado de leer la hoja.
     *
     * @param conceptos cuantas entidades (medicamentos) se encontraron
     */
    public record Resultado(List<BitacoraRepository.MetricaFila> metricas, int conceptos,
                            List<String> avisos) {
    }

    /**
     * Lee la matriz y devuelve una metrica por (concepto, mes, medida).
     *
     * <p><b>Los ceros y los vacios no se guardan.</b> Un medicamento que no se consumio en marzo
     * no produce renglon: guardar el cero serian 240 medicamentos x 12 meses x 7 medidas por
     * predio, casi todo en cero, para no decir nada que la ausencia no diga ya. Con eso la carga
     * pasa de ~240 mil renglones a unos miles.
     */
    public static Resultado leer(Hoja hoja, long loteId, String familia, String predio, int anio) {
        List<BitacoraRepository.MetricaFila> metricas = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        Integer filaMeses = buscarFilaMeses(hoja);
        if (filaMeses == null) {
            avisos.add("no se hallo la fila con los nombres de mes");
            return new Resultado(metricas, 0, avisos);
        }
        int filaMedida = filaMeses + 1;
        int filaSub = filaMeses + 2;
        int primeraFilaDatos = filaMeses + 3;

        List<Bloque> bloques = bloquesDeMes(hoja, filaMeses);
        if (bloques.isEmpty()) {
            avisos.add("la fila " + (filaMeses + 1) + " no trae meses reconocibles");
            return new Resultado(metricas, 0, avisos);
        }

        int conceptos = 0;
        for (int r = primeraFilaDatos; r <= hoja.ultimaFila(); r++) {
            String concepto = Normalizador.etiqueta(hoja.texto(r, 0));
            if (!esConcepto(concepto)) {
                continue;
            }
            conceptos++;
            for (Bloque b : bloques) {
                leerMes(hoja, r, b, filaMedida, filaSub, loteId, familia, predio, anio, metricas);
            }
        }
        return new Resultado(metricas, conceptos, avisos);
    }

    /** Un mes y el rango de columnas que ocupa. */
    private record Bloque(int mes, int desde, int hasta) {
    }

    /**
     * La fila de los meses: la primera que trae al menos tres nombres de mes.
     *
     * <p>Tres y no uno: en la fila del titulo puede aparecer un mes suelto dentro de una frase, y
     * tres seguidos solo pasan en la fila que de verdad los enumera.
     */
    private static Integer buscarFilaMeses(Hoja hoja) {
        int hasta = Math.min(hoja.ultimaFila(), MAX_FILA_MESES);
        for (int r = 0; r <= hasta; r++) {
            long cuantos = hoja.fila(r).stream()
                    .map(Normalizador::texto)
                    .filter(v -> v != null && MESES.contains(v))
                    .count();
            if (cuantos >= 3) {
                return r;
            }
        }
        return null;
    }

    /**
     * Los bloques de mes, cada uno desde su columna hasta la del siguiente.
     *
     * <p>El ancho se deduce de donde empieza el mes siguiente, no se fija en 14: si el origen
     * agrega una columna por mes, esto lo absorbe solo. El ultimo mes llega hasta donde empieza
     * la zona de totales, que es la siguiente etiqueta no-mes.
     */
    private static List<Bloque> bloquesDeMes(Hoja hoja, int filaMeses) {
        List<String> fila = hoja.fila(filaMeses);
        List<Bloque> bloques = new ArrayList<>();
        List<Integer> inicios = new ArrayList<>();
        List<Integer> mesDe = new ArrayList<>();

        for (int c = 0; c < fila.size(); c++) {
            String t = Normalizador.texto(fila.get(c));
            int idx = t == null ? -1 : MESES.indexOf(t);
            if (idx >= 0) {
                inicios.add(c);
                mesDe.add(idx + 1);
            }
        }
        for (int i = 0; i < inicios.size(); i++) {
            int desde = inicios.get(i);
            int hasta = (i + 1 < inicios.size() ? inicios.get(i + 1) : siguienteEtiqueta(fila, desde)) - 1;
            bloques.add(new Bloque(mesDe.get(i), desde, hasta));
        }
        return bloques;
    }

    /** Donde termina el ultimo mes: en la siguiente celda con texto, que ya es el total del anio. */
    private static int siguienteEtiqueta(List<String> fila, int desde) {
        for (int c = desde + 1; c < fila.size(); c++) {
            if (Normalizador.etiqueta(fila.get(c)) != null) {
                return c;
            }
        }
        return fila.size();
    }

    private static void leerMes(Hoja hoja, int fila, Bloque b, int filaMedida, int filaSub,
                                long loteId, String familia, String predio, int anio,
                                List<BitacoraRepository.MetricaFila> destino) {
        String concepto = Normalizador.etiqueta(hoja.texto(fila, 0));
        String medidaActual = null;

        for (int c = b.desde(); c <= b.hasta(); c++) {
            String medida = Normalizador.etiqueta(hoja.texto(filaMedida, c));
            if (medida != null) {
                medidaActual = medida;
            }
            if (medidaActual == null || !esMedidaBuscada(medidaActual)) {
                continue;
            }
            Double valor = hoja.numero(fila, c);
            if (valor == null || valor == 0) {
                continue;   // sin consumo no hay renglon; ver el javadoc de leer()
            }
            String sub = Normalizador.etiqueta(hoja.texto(filaSub, c));
            destino.add(new BitacoraRepository.MetricaFila(
                    loteId, familia, "DETALLE", predio, anio, b.mes(),
                    concepto, subconcepto(medidaActual, sub), valor));
        }
    }

    private static boolean esMedidaBuscada(String medida) {
        return MEDIDAS.stream().anyMatch(medida::contains);
    }

    /**
     * Como se nombra la medida.
     *
     * <p>{@code CONSUMO SEMANAL} se desglosa por semana ({@code 1o}&hellip;{@code 5o}) y trae un
     * {@code TOTAL x LOTE}; {@code CONSUMO ACUMULADO} no tiene subetiqueta y se queda con su
     * nombre. Asi las tres medidas que pidio servicio medico quedan distinguibles en
     * {@code SUBCONCEPTO} sin tener que mirar la columna.
     */
    private static String subconcepto(String medida, String sub) {
        if (sub == null) {
            return medida;
        }
        return medida + " " + sub;
    }

    /**
     * Si un renglon es un concepto y no un total ni una fila de adorno.
     *
     * <p>Mismo criterio que {@code Transpositor.esFilaDeDatos}, por la misma razon: las hojas
     * traen renglones de etiqueta mezclados entre los datos.
     */
    private static boolean esConcepto(String texto) {
        if (texto == null || texto.length() < 3) {
            return false;
        }
        return !texto.startsWith("TOTAL") && !texto.startsWith("ACUMULADO")
                && !texto.startsWith("SUMA");
    }
}
