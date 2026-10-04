package com.onest.excel.layout;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.onest.excel.normalizacion.Normalizador;

/**
 * El encabezado de dos filas de una hoja, ya interpretado.
 *
 * <p>Los archivos del origen no tienen una fila de encabezado sino dos: arriba el <b>grupo</b>
 * en una celda combinada ({@code CUENTA}, {@code RANGO DE EDAD}, {@code CAUSA}) y abajo la
 * <b>subcategoria</b>, una por columna ({@code 18 - 25}, {@code 26 - 35}, …). El grupo solo
 * aparece escrito en la primera de sus columnas; las demas vienen vacias por la combinacion.
 *
 * @param filaGrupo fila del grupo, 0-based
 * @param filaSub   fila de las subcategorias, 0-based
 * @param columnas  una entrada por columna util
 */
public record Encabezado(int filaGrupo, int filaSub, List<Columna> columnas) {

    /**
     * Una columna del encabezado.
     *
     * @param indice    posicion 0-based
     * @param grupo     texto del grupo, ya propagado a las columnas combinadas
     * @param subetiqueta texto de la subcategoria, o {@code null} si el grupo ocupa una sola columna
     */
    public record Columna(int indice, String grupo, String subetiqueta) {

        /** La etiqueta con la que se identifica esta columna: la subcategoria si hay, si no el grupo. */
        public String etiqueta() {
            return subetiqueta != null ? subetiqueta : grupo;
        }
    }

    /**
     * Un grupo cuyas columnas forman un bloque "marca con 1": varias subcategorias de las que
     * exactamente una lleva el 1 en cada renglon.
     *
     * @param grupo      nombre del grupo
     * @param columnas   sus columnas, en orden
     */
    public record Bloque(String grupo, List<Columna> columnas) {

        public int ancho() {
            return columnas.size();
        }
    }

    /**
     * Los grupos que son bloques one-hot: los de mas de una subcategoria etiquetada.
     *
     * <p>Los grupos de una sola columna ({@code FECHA}, {@code NOMBRE}, {@code DIAGNOSTICO})
     * no son bloques: su valor se lee directo de la celda.
     */
    public List<Bloque> bloques() {
        return agrupar().entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .filter(e -> e.getValue().stream().anyMatch(c -> c.subetiqueta() != null))
                .map(e -> new Bloque(e.getKey(), e.getValue()))
                .toList();
    }

    /** Los grupos de una sola columna: se leen directo, sin transponer. */
    public List<Columna> columnasSimples() {
        return agrupar().values().stream()
                .filter(cols -> cols.size() == 1)
                .map(List::getFirst)
                .toList();
    }

    /**
     * Agrupa las columnas en <b>corridas contiguas</b> del mismo grupo, no por nombre.
     *
     * <p>Una celda combinada del encabezado es contigua por definicion, asi que una corrida es
     * exactamente un grupo del archivo. Agrupar por nombre parecia equivalente y no lo es: cuando
     * el <b>mismo nombre aparece dos veces</b> en la misma fila de encabezado, los junta en un
     * grupo falso que abarca las dos zonas y todo lo que hay en medio.
     *
     * <p>Lo destapo accidentabilidad el 30-sep-2026: su fila de grupo trae {@code STATUS} en la
     * columna 164 &mdash;con PENDIENTE, CALIFICADO, IMPROCEDENTE, BAJA INCONCLUSO&mdash; y otra vez
     * {@code STATUS} en la 169, que es el estado del tramite. Unidos daban un bloque de cinco
     * opciones donde hay dos preguntas distintas, y cada renglon levantaba un aviso de "2 marcas"
     * perdiendo una de las dos.
     *
     * <p>Cuando un nombre se repite, la clave lleva un sufijo para que las dos corridas se puedan
     * distinguir; la primera conserva el nombre limpio, que es la que encuentran las busquedas por
     * etiqueta.
     */
    private Map<String, List<Columna>> agrupar() {
        Map<String, List<Columna>> porGrupo = new LinkedHashMap<>();
        List<Columna> corrida = new java.util.ArrayList<>();
        String grupoCorrida = null;
        Integer ultimoIndice = null;

        for (Columna c : columnas) {
            if (c.grupo() == null) {
                continue;
            }
            boolean sigueLaCorrida = c.grupo().equals(grupoCorrida)
                    && ultimoIndice != null && c.indice() == ultimoIndice + 1;
            if (!sigueLaCorrida) {
                guardarCorrida(porGrupo, grupoCorrida, corrida);
                corrida = new java.util.ArrayList<>();
                grupoCorrida = c.grupo();
            }
            corrida.add(c);
            ultimoIndice = c.indice();
        }
        guardarCorrida(porGrupo, grupoCorrida, corrida);
        return porGrupo;
    }

    private static void guardarCorrida(Map<String, List<Columna>> porGrupo, String grupo,
                                       List<Columna> corrida) {
        if (grupo == null || corrida.isEmpty()) {
            return;
        }
        String clave = grupo;
        for (int n = 2; porGrupo.containsKey(clave); n++) {
            clave = grupo + " (" + n + ")";
        }
        porGrupo.put(clave, List.copyOf(corrida));
    }

    /**
     * Busca una columna por etiqueta, mirando <b>tanto el grupo como la subetiqueta</b>.
     *
     * <p>Acepta coincidencia parcial porque el mismo campo cambia de redaccion cada anio:
     * {@code FECHA}, {@code FECHA ATENCION}; {@code NOMBRE}, {@code NOMBRE COMPLETO},
     * {@code NOMBRE (INICIAR POR APELLIDO Y NOMBRE COMPLETO)}.
     *
     * <p>Mirar en la subetiqueta no es un detalle: en el seguimiento medico especial la fila de
     * grupo dice {@code DATOS PERSONALES} y {@code NOMBRE} esta una fila mas abajo, como
     * subetiqueta. Buscando solo en el grupo, esos siete archivos quedaban sin columna de nombre
     * y el transpositor no producia un solo renglon.
     *
     * <p>Se busca primero en los grupos y despues en las subetiquetas: cuando las dos coinciden
     * gana el grupo, que es el nivel mas general y el que identifica la columna de verdad.
     */
    public Optional<Columna> columnaPorGrupo(String... alternativas) {
        for (String alt : alternativas) {
            String buscado = Normalizador.texto(alt);
            if (buscado == null) {
                continue;
            }
            for (Columna c : columnas) {
                String g = Normalizador.texto(c.grupo());
                if (g != null && g.contains(buscado)) {
                    return Optional.of(c);
                }
            }
            for (Columna c : columnas) {
                String s = Normalizador.texto(c.subetiqueta());
                if (s != null && s.contains(buscado)) {
                    return Optional.of(c);
                }
            }
        }
        return Optional.empty();
    }

    /** Busca un bloque one-hot por el nombre de su grupo, con coincidencia parcial. */
    public Optional<Bloque> bloquePorGrupo(String... alternativas) {
        List<Bloque> bs = bloques();
        for (String alt : alternativas) {
            String buscado = Normalizador.texto(alt);
            for (Bloque b : bs) {
                String g = Normalizador.texto(b.grupo());
                if (g != null && buscado != null && g.contains(buscado)) {
                    return Optional.of(b);
                }
            }
        }
        return Optional.empty();
    }

    /** Primera fila de datos: la de abajo del encabezado. */
    public int primeraFilaDatos() {
        return Math.max(filaGrupo, filaSub) + 1;
    }
}
