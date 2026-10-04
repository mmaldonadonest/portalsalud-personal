package com.onest.excel.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;

/**
 * Tareas 4 y 5: encontrar donde empieza la tabla y construir el indice etiqueta &rarr; columna.
 *
 * <p><b>Por que se busca en vez de fijarse.</b> La fila del encabezado se mueve segun el archivo,
 * y no de forma predecible. Medido sobre los 132 archivos de 2026:
 * <ul>
 *   <li>atenciones, exámenes, antidoping, maternidad, incapacidades: <b>fila 3</b></li>
 *   <li>consumibles: <b>fila 5</b></li>
 *   <li>accidentabilidad: entre la <b>fila 6 y la 10</b>, distinta en cada predio</li>
 *   <li>en los diez anios completos tambien aparece en la fila 4</li>
 * </ul>
 * Fijar la fila funcionaria para ocho familias y fallaria en la novena, en silencio: leeria
 * etiquetas donde hay datos y produciria una tabla con columnas sin nombre.
 *
 * <p><b>Y por que el indice va por etiqueta y nunca por posicion.</b> Los catalogos se recorren
 * cada anio: {@code CUENTA} paso de 30 columnas a 59, {@code PUESTO} de 18 a 73 y de vuelta a 36,
 * {@code AGENCIA} de 13 a 73 a 2. Un mapeo por numero de columna queda mal en cuanto alguien
 * agrega una cuenta nueva, y la carga sigue corriendo sin avisar: solo que los datos quedan
 * corridos una columna.
 */
public final class DetectorEncabezado {

    private DetectorEncabezado() {
    }

    /** Hasta que fila se busca el encabezado. El mas profundo visto esta en la 10. */
    private static final int MAX_FILA_BUSQUEDA = 14;

    /**
     * Palabras que delatan la fila de grupo. {@code FECHA} es la mas confiable: aparece en las
     * nueve familias y nunca como dato en esa zona de la hoja.
     */
    private static final List<String> ANCLAS = List.of("FECHA", "NOMBRE", "N°", "NO.");

    /** Un encabezado creible tiene varias celdas con texto; una fila de datos no. */
    private static final int MIN_CELDAS_ENCABEZADO = 4;

    /**
     * Cuantas filas de subcategoria se aceptan debajo del grupo.
     *
     * <p>Tres es de sobra: el mas profundo medido es antidoping con dos. El tope existe para que
     * un archivo raro no se coma filas de datos como si fueran encabezado.
     */
    private static final int MAX_FILAS_SUB = 3;

    /**
     * Detecta el encabezado de dos filas.
     *
     * @return vacio si no se hallo, que es una senal legitima: hay hojas que no son tablas
     *         (graficas, nomenclaturas, concentrados con otra forma)
     */
    public static Optional<Encabezado> detectar(Hoja hoja) {
        Integer filaGrupo = buscarFilaGrupo(hoja);
        if (filaGrupo == null) {
            return Optional.empty();
        }
        List<Integer> filasSub = buscarFilasSub(hoja, filaGrupo);
        return Optional.of(construir(hoja, filaGrupo, filasSub));
    }

    /**
     * Cuantas filas de subcategoria hay debajo del grupo. Normalmente una; en antidoping son dos.
     *
     * <p><b>El encabezado no tiene profundidad fija.</b> Ocho familias lo traen de dos filas
     * &mdash;grupo y subcategoria&mdash; pero antidoping lo trae de <b>tres</b>, y solo en una
     * parte del ancho:
     * <pre>
     *   fila 3   ... RESULTADO(141) .................. CONCLUSION(156)
     *   fila 4       NEGATIVO POSITIVO DOPING(143) ALCOHOLEMIA(155)
     *   fila 5                        AMF BARB BENZO COC ... OTRO(154)   %(155)
     * </pre>
     * Hasta el 30-sep-2026 se tomaba siempre {@code filaGrupo + 1}, asi que en antidoping se
     * quedaba con la fila 4 &mdash;que solo tiene cuatro celdas, todas bajo RESULTADO&mdash; y
     * <b>perdia las subetiquetas de los otros ocho bloques</b>: meses, edad, genero, agencia,
     * cuenta, area, puesto y tipo de prueba quedaban sin resolver. El analisis reportaba un solo
     * bloque donde hay nueve, y nadie lo habria notado hasta ver la grafica vacia.
     *
     * <p><b>Como se sabe donde termina el encabezado y empiezan los datos.</b> Por las columnas
     * ancla: una fila de encabezado esta <b>vacia</b> en la columna de {@code FECHA} y en la de
     * {@code NOMBRE}, y una fila de datos no &mdash;ahi viven la fecha y el nombre de la
     * persona&mdash;. Es un criterio del contenido y no una cuenta de celdas, que es lo que
     * fallaba: la fila 4 de antidoping tiene cuatro celdas y la 5 tiene 158, pero las dos son
     * encabezado.
     */
    private static List<Integer> buscarFilasSub(Hoja hoja, int filaGrupo) {
        int colFecha = columnaDe(hoja, filaGrupo, "FECHA");
        int colNombre = columnaDe(hoja, filaGrupo, "NOMBRE");

        List<Integer> filas = new ArrayList<>();
        for (int r = filaGrupo + 1; r <= Math.min(filaGrupo + MAX_FILAS_SUB, hoja.ultimaFila()); r++) {
            if (contarConTexto(hoja, r) == 0) {
                // Una fila en blanco no corta la busqueda: puede haber un hueco de formato
                // entre el grupo y sus subcategorias.
                continue;
            }
            if (ocupada(hoja, r, colFecha) || ocupada(hoja, r, colNombre)) {
                break;  // ya son datos
            }
            filas.add(r);
        }
        return filas;
    }

    /** Indice de la columna cuyo grupo contiene la palabra, o -1. */
    private static int columnaDe(Hoja hoja, int filaGrupo, String palabra) {
        List<String> grupos = hoja.fila(filaGrupo);
        for (int c = 0; c < grupos.size(); c++) {
            String t = Normalizador.texto(grupos.get(c));
            if (t != null && t.contains(palabra)) {
                return c;
            }
        }
        return -1;
    }

    private static boolean ocupada(Hoja hoja, int fila, int columna) {
        if (columna < 0) {
            return false;  // sin columna ancla no se puede juzgar; no corta
        }
        String v = hoja.texto(fila, columna);
        return v != null && !v.isBlank();
    }

    private static Integer buscarFilaGrupo(Hoja hoja) {
        int hasta = Math.min(hoja.ultimaFila(), MAX_FILA_BUSQUEDA);
        Integer mejorPorAncla = null;
        Integer mejorPorPoblacion = null;
        int maxCeldas = 0;

        for (int r = 0; r <= hasta; r++) {
            int celdas = contarConTexto(hoja, r);
            if (celdas < MIN_CELDAS_ENCABEZADO) {
                continue;
            }
            if (mejorPorAncla == null && tieneAncla(hoja, r)) {
                mejorPorAncla = r;
            }
            if (celdas > maxCeldas) {
                maxCeldas = celdas;
                mejorPorPoblacion = r;
            }
        }
        // El ancla manda: una fila con FECHA es el encabezado aunque otra tenga mas celdas
        return mejorPorAncla != null ? mejorPorAncla : mejorPorPoblacion;
    }

    private static boolean tieneAncla(Hoja hoja, int fila) {
        for (String valor : hoja.fila(fila)) {
            String t = Normalizador.texto(valor);
            if (t == null) {
                continue;
            }
            for (String ancla : ANCLAS) {
                if (t.contains(ancla)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int contarConTexto(Hoja hoja, int fila) {
        if (fila > hoja.ultimaFila()) {
            return 0;
        }
        return (int) hoja.fila(fila).stream().filter(v -> v != null && !v.isBlank()).count();
    }

    /**
     * Arma el indice de columnas propagando el grupo a la derecha.
     *
     * <p>Esa propagacion es lo que resuelve las celdas combinadas: {@code CUENTA} viene escrito
     * solo en la primera de sus 59 columnas y las otras 58 llegan vacias. Sin propagar, esas 58
     * quedarian sin grupo y el bloque no existiria.
     */
    private static Encabezado construir(Hoja hoja, int filaGrupo, List<Integer> filasSub) {
        List<String> grupos = hoja.fila(filaGrupo);
        List<List<String>> subs = filasSub.stream().map(hoja::fila).toList();

        List<Encabezado.Columna> columnas = new ArrayList<>();
        String grupoActual = null;
        for (int c = 0; c < hoja.ancho(); c++) {
            String g = c < grupos.size() ? Normalizador.etiqueta(grupos.get(c)) : null;
            if (g != null) {
                grupoActual = g;
            }
            String sub = subetiquetaDe(subs, c);

            // Sin grupo ni subcategoria no hay columna que registrar
            if (grupoActual == null && sub == null) {
                continue;
            }
            columnas.add(new Encabezado.Columna(c, grupoActual, sub));
        }
        // filaSub es la mas profunda: de ella depende donde empiezan los datos
        int filaSub = filasSub.isEmpty() ? filaGrupo : filasSub.getLast();
        return new Encabezado(filaGrupo, filaSub, List.copyOf(columnas));
    }

    /**
     * La subetiqueta de una columna: <b>la mas profunda que tenga texto</b>.
     *
     * <p>Con varias filas de subcategoria, cada columna se etiqueta con la de mas abajo que diga
     * algo. En antidoping eso da justo lo que se necesita:
     * <ul>
     *   <li>columna 142 &rarr; {@code POSITIVO}, que solo existe en la fila de enmedio porque no
     *       se subdivide</li>
     *   <li>columna 152 &rarr; {@code THC}, de la fila de abajo, y no {@code DOPING}, que es su
     *       encabezado intermedio</li>
     * </ul>
     * Tomar siempre la de enmedio dejaba las doce sustancias sin nombre; tomar siempre la de
     * abajo dejaba {@code NEGATIVO} y {@code POSITIVO} sin nombre. La mas profunda con texto
     * resuelve las dos.
     */
    private static String subetiquetaDe(List<List<String>> subs, int columna) {
        for (int i = subs.size() - 1; i >= 0; i--) {
            List<String> fila = subs.get(i);
            String v = columna < fila.size() ? Normalizador.etiqueta(fila.get(columna)) : null;
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
