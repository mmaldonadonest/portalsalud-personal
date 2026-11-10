package com.onest.app.catalog.examen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FT-SO-05 Cuestionario de sintomas osteomusculares (rev. 05, 14-oct-2026), pedido el 9-nov-2026 para
 * TODOS los examenes. Referencia: "osteomuscular para plataforma.xlsx" de Servicio Medico.
 *
 * <p>Segun sus anotaciones, cada seccion tiene un "todo normal" que pone 0, y si hay anormalidad se anota el
 * grado de la escala del formato: 0 ausencia de signos, 1 dolor en reposo o sintomatologia sugestiva, 2 + contractura
 * o dolor a la movilizacion, 3 + dolor a la palpacion o percusion, 4 + limitacion funcional evidente.
 *
 * <p>Se guarda TODO en un solo tag de SERV_MED_TAG, {@code EXM_OSTEO}, como JSON {clave: valor}: son ~125 celdas y
 * cada tag cuesta un viaje a la base; asi es una sola escritura y no lleva SQL. Por ser EXM_* el impreso lo recibe
 * solo (ExamenDocumentoService copia todos los EXM_*). Las claves son estables: no renombrarlas, son el dato guardado.
 * <ul>
 *   <li>Curvatura y desviacion: {@code CURV.CERVICAL} = NORMAL | AUMENTADA | DISMINUIDA, y su grado en {@code CURV.CERVICAL.G}.</li>
 *   <li>Lasegue y Schober: {@code EXP.LASEGUE.DER} = grado; 0 = negativo, 1-4 = positivo con ese grado.</li>
 *   <li>Lo demas: {@code MOV.CERVICAL.FLEX}, {@code PALP.LUMBAR.APOF}, {@code ART.HOMBRO.DER.ABD}... = grado 0-4.</li>
 * </ul>
 */
public final class Osteomuscular {

    /** Nombre de la seccion en el catalogo del examen (ExamenService) y en las hojas del examen inicial. */
    public static final String SECCION = "Osteomuscular (FT-SO-05)";
    public static final String TAG = "EXM_OSTEO";
    public static final String CAMPO = "TAG." + TAG;

    /** Fila o columna: clave estable + texto del formato. */
    public record Eje(String clave, String texto) {
    }

    public static final List<Eje> CURVATURA = List.of(
            new Eje("CERVICAL", "Cervical"), new Eje("DORSAL", "Dorsal"), new Eje("LUMBAR", "Lumbar"));
    public static final List<Eje> CURVATURA_OPCIONES = List.of(
            new Eje("NORMAL", "Normal"), new Eje("AUMENTADA", "Aumentada"), new Eje("DISMINUIDA", "Disminuida"));

    public static final List<Eje> DESVIACION = List.of(new Eje("DORSAL", "Dorsal"), new Eje("LUMBAR", "Lumbar"));
    public static final List<Eje> DESVIACION_OPCIONES = List.of(
            new Eje("NORMAL", "Normal"), new Eje("CONC_DER", "Concavidad derecha"), new Eje("CONC_IZQ", "Concavidad izquierda"));

    public static final List<Eje> MOVILIDAD = List.of(new Eje("CERVICAL", "Cervical"), new Eje("DORSOLUMBAR", "Dorso-lumbar"));
    public static final List<Eje> MOVILIDAD_COLUMNAS = List.of(
            new Eje("FLEX", "Flexión"), new Eje("EXT", "Extensión"), new Eje("LAT_IZQ", "Lateral izquierda"),
            new Eje("LAT_DER", "Lateral derecha"), new Eje("ROT_DER", "Rotación derecha"), new Eje("ROT_IZQ", "Rotación izquierda"),
            new Eje("IRRAD", "Irradiación"));

    public static final List<Eje> EXPLORACION = List.of(new Eje("LASEGUE", "Lasègue"), new Eje("SCHOBER", "Schober"));
    public static final List<Eje> LADOS = List.of(new Eje("DER", "Derecha"), new Eje("IZQ", "Izquierda"));

    public static final List<Eje> PALPACION = List.of(
            new Eje("CERVICAL", "Columna cervical"), new Eje("DORSAL", "Columna dorsal"), new Eje("LUMBAR", "Columna lumbar"));
    public static final List<Eje> PALPACION_COLUMNAS = List.of(
            new Eje("APOF", "Apófisis espinosas dolorosas"), new Eje("CONTR", "Contractura muscular"));

    public static final List<Eje> ARTICULACIONES = List.of(
            new Eje("HOMBRO", "Hombro"), new Eje("CODO", "Codo"), new Eje("MUNECA", "Muñeca"),
            new Eje("CADERA", "Cadera"), new Eje("RODILLA", "Rodilla"), new Eje("TOBILLO", "Tobillo"));
    public static final List<Eje> ARTICULACION_COLUMNAS = List.of(
            new Eje("ABD", "Abducción"), new Eje("ADD", "Aducción"), new Eje("FLEX", "Flexión"), new Eje("EXT", "Extensión"),
            new Eje("ROT_EXT", "Rotación externa"), new Eje("ROT_INT", "Rotación interna"), new Eje("IRRAD", "Irradiación"),
            new Eje("ALT_MUSC", "Alteración muscular"));

    /** Escala del formato (texto literal del FT-SO-05). */
    public static final List<Eje> GRADOS = List.of(
            new Eje("0", "Ausencia de signos y síntomas"),
            new Eje("1", "Dolor en reposo y/o existencia de sintomatología sugestiva"),
            new Eje("2", "Grado 1 más contractura y/o dolor a la movilización"),
            new Eje("3", "Grado 2 más dolor a la palpación y/o percusión"),
            new Eje("4", "Grado 3 más limitación funcional evidente clínicamente"));

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAPA = new TypeReference<>() { };
    // Thymeleaf pregunta celda por celda (~125 por pantalla o impreso): se parsea una vez por JSON distinto
    private static final ThreadLocal<Map.Entry<String, Map<String, String>>> ULTIMO = new ThreadLocal<>();

    private Osteomuscular() {
    }

    /**
     * Para las plantillas: th:with="O=${T(...Osteomuscular).INSTANCIA}" y luego O.curvatura, O.valor(j, k)...
     * (una variable con T(...) es la Class y SpEL no lee sus campos estaticos como propiedades).
     */
    public static final Osteomuscular INSTANCIA = new Osteomuscular();

    public List<Eje> getCurvatura() { return CURVATURA; }
    public List<Eje> getCurvaturaOpciones() { return CURVATURA_OPCIONES; }
    public List<Eje> getDesviacion() { return DESVIACION; }
    public List<Eje> getDesviacionOpciones() { return DESVIACION_OPCIONES; }
    public List<Eje> getMovilidad() { return MOVILIDAD; }
    public List<Eje> getMovilidadColumnas() { return MOVILIDAD_COLUMNAS; }
    public List<Eje> getExploracion() { return EXPLORACION; }
    public List<Eje> getLados() { return LADOS; }
    public List<Eje> getPalpacion() { return PALPACION; }
    public List<Eje> getPalpacionColumnas() { return PALPACION_COLUMNAS; }
    public List<Eje> getArticulaciones() { return ARTICULACIONES; }
    public List<Eje> getArticulacionColumnas() { return ARTICULACION_COLUMNAS; }
    public List<Eje> getGrados() { return GRADOS; }

    /** Igual que {@link #valor(String, String)}, como metodo de instancia para las plantillas. */
    public String v(String json, String clave) {
        return valor(json, clave);
    }

    /** Texto de una opcion de curvatura o desviacion ("" si no hay). */
    public String opcionTexto(String clave) {
        return java.util.stream.Stream.concat(CURVATURA_OPCIONES.stream(), DESVIACION_OPCIONES.stream())
                .filter(e -> e.clave().equals(clave)).map(Eje::texto).findFirst().orElse("");
    }

    /** Mapa clave -> valor del JSON guardado; vacio si no hay dato o no se puede leer. */
    public static Map<String, String> leer(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        Map.Entry<String, Map<String, String>> u = ULTIMO.get();
        if (u != null && u.getKey().equals(json)) {
            return u.getValue();
        }
        Map<String, String> m;
        try {
            m = JSON.readValue(json, MAPA);
        } catch (Exception ex) {
            m = Map.of();   // un valor viejo o roto no debe tumbar la pantalla ni el impreso
        }
        ULTIMO.set(Map.entry(json, m));
        return m;
    }

    /** Valor de una clave ("" si no hay). La usan las plantillas: T(...Osteomuscular).valor(json, 'CURV.CERVICAL'). */
    public static String valor(String json, String clave) {
        String v = leer(json).get(clave);
        return v == null ? "" : v;
    }

    /** true si el cuestionario trae al menos un dato capturado. */
    public static boolean tieneDatos(String json) {
        return leer(json).values().stream().anyMatch(v -> v != null && !v.isBlank());
    }

    /** Copia ordenada (para pruebas y para depurar). */
    public static Map<String, String> ordenado(String json) {
        return new LinkedHashMap<>(leer(json));
    }
}
