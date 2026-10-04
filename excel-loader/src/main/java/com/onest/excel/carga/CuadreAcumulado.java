package com.onest.excel.carga;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * El cuadre: cuantos renglones dice el propio Excel que tiene.
 *
 * <p>Cada archivo trae una hoja {@code ACUMULADO} con un renglon por predio y su total. Leerla y
 * compararla contra lo que el cargador conto es lo que convierte "creo que cargo bien" en "el
 * archivo dice 1,635 y se cargaron 1,635".
 *
 * <p>Verificado el 30-sep-2026 sobre atenciones 2026: los cuatro predios revisados cuadran exacto
 * &mdash; MACRO I 1,635 · Z VALLEJO 738 · MIKELS 59 · U TEPALCAPA 54, los mismos numeros que
 * conto el transpositor.
 *
 * <p><b>El problema no son los numeros sino los nombres.</b> El ACUMULADO y la hoja de detalle
 * llaman distinto al mismo predio, de tres formas:
 * <ul>
 *   <li><b>Romano contra arabigo:</b> la hoja es {@code MACRO 1} y el ACUMULADO dice
 *       {@code MACRO I}.</li>
 *   <li><b>Abreviatura por prefijo:</b> la hoja es {@code UT} y el ACUMULADO
 *       {@code U TEPALCAPA}.</li>
 *   <li><b>Abreviatura sin vocales:</b> la hoja es {@code MKLS} y el ACUMULADO
 *       {@code MIKELS}.</li>
 * </ul>
 *
 * <p>Cuando ninguna regla empareja, <b>se devuelve vacio en vez de adivinar</b>. Un cuadre
 * equivocado es peor que no tenerlo: diria que el archivo no cuadra cuando el problema es que se
 * comparo contra el predio de al lado.
 */
public final class CuadreAcumulado {

    private CuadreAcumulado() {
    }

    /** Hasta donde se busca la fila de encabezado dentro de la hoja de resumen. */
    private static final int MAX_FILA_ENCABEZADO = 10;
    /** Un prefijo mas corto que esto empareja cualquier cosa. */
    private static final int MIN_PREFIJO = 2;

    /**
     * El total que el Excel declara para un predio.
     *
     * @param nombreHoja hoja de detalle que se acaba de contar
     * @return vacio si no hay hoja de resumen, si no se halla la columna TOTAL o si ningun
     *         renglon empareja con el predio
     */
    public static Optional<Integer> totalDe(Workbook libro, String nombreHoja) {
        Hoja resumen = buscarHojaResumen(libro);
        if (resumen == null) {
            return Optional.empty();
        }
        Integer filaEncabezado = buscarFilaEncabezado(resumen);
        if (filaEncabezado == null) {
            return Optional.empty();
        }
        Integer columnaTotal = buscarPrimerTotal(resumen, filaEncabezado);
        if (columnaTotal == null) {
            return Optional.empty();
        }
        Map<String, Double> porPredio = leerTotales(resumen, filaEncabezado + 1, columnaTotal);
        return emparejar(porPredio, nombreHoja).map(d -> (int) Math.round(d));
    }

    private static Hoja buscarHojaResumen(Workbook libro) {
        for (int i = 0; i < libro.getNumberOfSheets(); i++) {
            String t = Normalizador.texto(libro.getSheetName(i));
            if (t != null && t.startsWith("ACUM")) {
                return new Hoja(libro.getSheetAt(i));
            }
        }
        return null;
    }

    /** La fila del encabezado es la que trae un {@code TOTAL}. */
    private static Integer buscarFilaEncabezado(Hoja hoja) {
        int hasta = Math.min(hoja.ultimaFila(), MAX_FILA_ENCABEZADO);
        for (int r = 0; r <= hasta; r++) {
            for (String v : hoja.fila(r)) {
                if ("TOTAL".equals(Normalizador.texto(v))) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * El PRIMER {@code TOTAL}, que es el de las atenciones.
     *
     * <p>La hoja trae varios: uno despues de los doce meses, otro despues de los rangos de edad,
     * otro despues del genero. Los tres valen lo mismo cuando el archivo esta bien, pero el
     * primero es el que corresponde al conteo de renglones.
     */
    private static Integer buscarPrimerTotal(Hoja hoja, int filaEncabezado) {
        java.util.List<String> fila = hoja.fila(filaEncabezado);
        for (int c = 0; c < fila.size(); c++) {
            if ("TOTAL".equals(Normalizador.texto(fila.get(c)))) {
                return c;
            }
        }
        return null;
    }

    /** Predio (columna 0) &rarr; total, de todos los renglones con nombre. */
    private static Map<String, Double> leerTotales(Hoja hoja, int desde, int columnaTotal) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int r = desde; r <= hoja.ultimaFila(); r++) {
            String predio = Normalizador.texto(hoja.texto(r, 0));
            if (predio == null || predio.isBlank()) {
                continue;
            }
            Double total = hoja.numero(r, columnaTotal);
            if (total != null) {
                m.put(predio, total);
            }
        }
        return m;
    }

    /**
     * Empareja el nombre de la hoja con un renglon del resumen, probando las tres formas en que
     * difieren. Cada regla esta aqui por un caso real; ninguna es preventiva.
     */
    private static Optional<Double> emparejar(Map<String, Double> porPredio, String nombreHoja) {
        String hoja = canonico(nombreHoja);
        if (hoja == null || hoja.isBlank()) {
            return Optional.empty();
        }
        // 1. Igual, ya normalizado y con los romanos convertidos: MACRO 1 == MACRO I
        for (Map.Entry<String, Double> e : porPredio.entrySet()) {
            if (canonico(e.getKey()).equals(hoja)) {
                return Optional.of(e.getValue());
            }
        }
        // 2. Uno es prefijo del otro sin espacios: UT -> U TEPALCAPA
        String hojaSinEspacios = hoja.replace(" ", "");
        for (Map.Entry<String, Double> e : porPredio.entrySet()) {
            String otro = canonico(e.getKey()).replace(" ", "");
            if (hojaSinEspacios.length() >= MIN_PREFIJO
                    && (otro.startsWith(hojaSinEspacios) || hojaSinEspacios.startsWith(otro))) {
                return Optional.of(e.getValue());
            }
        }
        // 3. Mismo esqueleto de consonantes: MKLS -> MIKELS
        String hojaConsonantes = consonantes(hoja);
        if (hojaConsonantes.length() >= MIN_PREFIJO) {
            for (Map.Entry<String, Double> e : porPredio.entrySet()) {
                if (consonantes(canonico(e.getKey())).equals(hojaConsonantes)) {
                    return Optional.of(e.getValue());
                }
            }
        }
        return Optional.empty();
    }

    /** Normaliza y vuelve arabigo cualquier numero romano suelto, para que MACRO I == MACRO 1. */
    private static String canonico(String s) {
        String t = Normalizador.texto(s);
        if (t == null) {
            return "";
        }
        return t.replaceAll("\\bIII\\b", "3")
                .replaceAll("\\bII\\b", "2")
                .replaceAll("\\bI\\b", "1");
    }

    private static String consonantes(String s) {
        return s.replaceAll("[AEIOU ]", "");
    }
}
