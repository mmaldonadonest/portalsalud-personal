package com.onest.excel.transposicion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.onest.excel.layout.Encabezado;
import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;

/**
 * Tareas 6 y 7: convertir el "marca con 1" en un valor, y separar los renglones de datos de los
 * que no lo son.
 *
 * <p>El origen no guarda {@code CAUSA = "RESPIRATORIO"}: guarda 26 columnas, una por causa, y un
 * {@code 1} en la que aplica. Transponer es resolver, por cada bloque y cada renglon, cual de sus
 * columnas trae la marca.
 *
 * <p><b>La calidad del marcado esta medida</b> y es alta: sobre los archivos de 2026, el 99.8%
 * de los renglones tiene exactamente una marca por bloque. Pero el 0.2% restante existe, y hay
 * que decidir que hacer con el en vez de que decida el azar:
 * <ul>
 *   <li><b>cero marcas</b> &rarr; el valor queda nulo. Es legitimo en bloques que solo aplican a
 *       veces: {@code CAUSAS MUSCULO ESQUELETICAS} viene vacio en el 84% de los renglones porque
 *       solo se llena en riesgo de trabajo.</li>
 *   <li><b>mas de una marca</b> &rarr; se toma la primera y <b>se reporta</b>. Nunca se descarta
 *       el renglon entero por esto: perder una atencion completa por una casilla de mas es peor
 *       que registrar la primera y dejar constancia.</li>
 * </ul>
 */
public final class Transpositor {

    private Transpositor() {
    }

    /**
     * Un renglon ya transpuesto.
     *
     * @param filaExcel   numero de fila en el Excel, 1-based, para poder rastrearlo
     * @param valores     grupo &rarr; valor resuelto de los bloques one-hot y las columnas simples
     * @param campos      para los grupos de campos: "GRUPO.SUBETIQUETA" &rarr; columna donde esta
     *                    el dato, para que quien lo consuma lo lea con su tipo real
     * @param avisos      lo que no salio limpio en este renglon
     */
    public record FilaTranspuesta(int filaExcel, Map<String, String> valores,
                                  Map<String, Integer> campos, Map<String, List<String>> marcas,
                                  List<String> avisos) {

        public String valor(String grupo) {
            return valores.get(grupo);
        }

        public boolean limpia() {
            return avisos.isEmpty();
        }
    }

    /** Etiquetas que delatan una fila de totales metida entre los datos. */
    private static final List<String> ETIQUETAS_TOTAL =
            List.of("TOTAL", "ACUMULADO", "SUMA", "GRAN TOTAL", "TOTALES");

    /**
     * Transpone las filas de datos de una hoja.
     *
     * @param columnaNombre   columna que identifica a la persona; un renglon sin ella no es un dato
     * @param gruposDeCampos  grupos que <b>NO</b> son one-hot: sus columnas son campos
     *                        independientes que conviven, no opciones que compiten por una marca
     * @param gruposMultimarca grupos donde <b>varias marcas son correctas</b> y todas se conservan.
     *                        No producen aviso, porque no hay nada que avisar: ver abajo.
     */
    public static List<FilaTranspuesta> transponer(Hoja hoja, Encabezado encabezado,
                                                   int columnaNombre, Set<String> gruposDeCampos,
                                                   Set<String> gruposMultimarca) {
        Set<String> campos = normalizar(gruposDeCampos);
        Set<String> multi = normalizar(gruposMultimarca);

        List<Encabezado.Bloque> todos = encabezado.bloques();
        List<Encabezado.Bloque> bloques = todos.stream()
                .filter(b -> !coincide(b.grupo(), campos) && !coincide(b.grupo(), multi)).toList();
        List<Encabezado.Bloque> deCampos = todos.stream()
                .filter(b -> coincide(b.grupo(), campos)).toList();
        List<Encabezado.Bloque> deMarcas = todos.stream()
                .filter(b -> coincide(b.grupo(), multi)).toList();
        List<Encabezado.Columna> simples = encabezado.columnasSimples();

        List<FilaTranspuesta> resultado = new ArrayList<>();
        for (int r = encabezado.primeraFilaDatos(); r <= hoja.ultimaFila(); r++) {
            if (!esFilaDeDatos(hoja, r, columnaNombre)) {
                continue;
            }
            Map<String, String> valores = new LinkedHashMap<>();
            Map<String, Integer> columnasCampo = new LinkedHashMap<>();
            Map<String, List<String>> marcas = new LinkedHashMap<>();
            List<String> avisos = new ArrayList<>();

            for (Encabezado.Columna c : simples) {
                valores.put(c.grupo(), hoja.texto(r, c.indice()));
            }
            for (Encabezado.Bloque b : bloques) {
                resolver(hoja, r, b, valores, avisos);
            }
            // Los grupos de campos no se resuelven: cada columna es un dato y se registra
            // su posicion para que el consumidor la lea con su tipo (fecha, numero o texto).
            for (Encabezado.Bloque b : deCampos) {
                for (Encabezado.Columna c : b.columnas()) {
                    if (c.subetiqueta() != null) {
                        columnasCampo.put(b.grupo() + "." + c.subetiqueta(), c.indice());
                    }
                }
            }
            for (Encabezado.Bloque b : deMarcas) {
                List<String> m = marcadas(hoja, r, b);
                if (!m.isEmpty()) {
                    marcas.put(b.grupo(), m);
                }
            }
            resultado.add(new FilaTranspuesta(r + 1, valores, Map.copyOf(columnasCampo),
                    Map.copyOf(marcas), List.copyOf(avisos)));
        }
        return resultado;
    }

    /** Compatibilidad: sin grupos de campos, todo bloque se trata como one-hot. */
    public static List<FilaTranspuesta> transponer(Hoja hoja, Encabezado encabezado,
                                                   int columnaNombre) {
        return transponer(hoja, encabezado, columnaNombre, Set.of(), Set.of());
    }

    /** Compatibilidad: con grupos de campos pero sin multimarca. */
    public static List<FilaTranspuesta> transponer(Hoja hoja, Encabezado encabezado,
                                                   int columnaNombre, Set<String> gruposDeCampos) {
        return transponer(hoja, encabezado, columnaNombre, gruposDeCampos, Set.of());
    }

    private static Set<String> normalizar(Set<String> grupos) {
        return grupos == null ? Set.of()
                : grupos.stream().map(Normalizador::texto).collect(Collectors.toSet());
    }

    private static boolean coincide(String grupo, Set<String> declarados) {
        String g = Normalizador.texto(grupo);
        return g != null && declarados.stream().anyMatch(c -> c != null && g.contains(c));
    }

    /**
     * Todas las subetiquetas marcadas de un bloque, sin resolver y <b>sin avisar</b>.
     *
     * <p>Es para los grupos donde varias marcas son lo correcto. El caso que lo motivo es
     * {@code RESULTADO} de antidoping, medido el 30-sep-2026: un renglon positivo lleva
     * <b>dos</b> marcas, {@code POSITIVO} y la sustancia que salio &mdash;{@code AMF},
     * {@code THC}&hellip;&mdash;, porque son dos datos distintos del mismo resultado. Tratado como
     * one-hot se quedaba con {@code POSITIVO}, tiraba la sustancia, y levantaba un aviso por cada
     * renglon: en MACRO 1 eran <b>63 avisos de 63 renglones</b>, que es la forma que tiene un
     * cargador de decir "estoy leyendo esto mal" cuando nadie le hizo caso.
     */
    private static List<String> marcadas(Hoja hoja, int fila, Encabezado.Bloque bloque) {
        List<String> m = new ArrayList<>(2);
        for (Encabezado.Columna c : bloque.columnas()) {
            if (c.subetiqueta() != null && Normalizador.marcado(hoja.texto(fila, c.indice()))) {
                m.add(c.subetiqueta());
            }
        }
        return m;
    }

    /**
     * Resuelve un bloque one-hot para un renglon.
     *
     * <p>Devuelve la <b>subetiqueta</b> de la columna marcada, no el {@code 1}: es lo que se va
     * a guardar y a graficar.
     */
    private static void resolver(Hoja hoja, int fila, Encabezado.Bloque bloque,
                                 Map<String, String> valores, List<String> avisos) {
        List<String> marcadas = new ArrayList<>(2);
        for (Encabezado.Columna c : bloque.columnas()) {
            if (c.subetiqueta() == null) {
                continue;
            }
            if (Normalizador.marcado(hoja.texto(fila, c.indice()))) {
                marcadas.add(c.subetiqueta());
            }
        }
        if (marcadas.isEmpty()) {
            valores.put(bloque.grupo(), null);
            return;
        }
        // Una etiqueta neutra pierde contra cualquier valor real, y eso NO es una ambiguedad que
        // haya que reportar. Confirmado por servicio medico el 30-sep-2026: en antidoping,
        // "N/A" quiere decir "no aplica", o sea la ausencia de desenlace. Dos renglones traian
        // N/A y NO CONTRATADO marcados a la vez; tomar la primera se quedaba con N/A -por estar
        // antes en la hoja- y borraba el unico dato real del renglon.
        List<String> reales = marcadas.stream().filter(m -> !esNeutra(m)).toList();
        List<String> efectivas = reales.isEmpty() ? marcadas : reales;

        valores.put(bloque.grupo(), efectivas.getFirst());
        if (efectivas.size() > 1) {
            avisos.add(bloque.grupo() + ": " + efectivas.size() + " marcas ("
                    + String.join(", ", efectivas) + "), se tomo la primera");
        }
    }

    /**
     * Etiquetas que significan "sin valor" y no son una opcion del catalogo.
     *
     * <p>Se comparan completas y no por contencion: {@code NO APLICA} es neutra, pero
     * {@code NO APTO} y {@code NO CONTRATADO} son respuestas de verdad y empiezan igual.
     */
    private static final Set<String> ETIQUETAS_NEUTRAS =
            Set.of("N/A", "NA", "N / A", "NO APLICA", "SIN DATO", "S/D", "NINGUNO");

    private static boolean esNeutra(String etiqueta) {
        String t = Normalizador.texto(etiqueta);
        return t != null && ETIQUETAS_NEUTRAS.contains(t);
    }

    /**
     * Si un renglon es un dato y no otra cosa.
     *
     * <p>Las hojas de detalle traen renglones de etiqueta <b>mezclados en el area de datos</b>,
     * en posiciones variables: en ENE 2017 de MACRO I los datos terminan en la fila 108 y las
     * etiquetas {@code TOTAL} y {@code ACUMULADO GENERAL} estan en la 123 y la 125, con numeros
     * al lado que parecen datos. Contarlos suma personas que no existen.
     */
    private static boolean esFilaDeDatos(Hoja hoja, int fila, int columnaNombre) {
        String nombre = hoja.texto(fila, columnaNombre);
        if (nombre == null || nombre.isBlank()) {
            return false;
        }
        String t = Normalizador.texto(nombre);
        if (t == null || t.length() < 4) {
            return false;
        }
        for (String etiqueta : ETIQUETAS_TOTAL) {
            if (t.equals(etiqueta) || t.startsWith(etiqueta + " ")) {
                return false;
            }
        }
        return true;
    }
}
