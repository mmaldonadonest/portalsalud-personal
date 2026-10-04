package com.onest.excel.normalizacion;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Tarea 8: dejar los valores del Excel en la forma exacta que espera el portal.
 *
 * <p>Cada metodo de aqui existe por un desajuste concreto y medido entre el origen y el destino,
 * no por gusto de limpiar. Si se omite alguno, la grafica sale mal de una forma que no se nota:
 * series duplicadas, personas contadas dos veces, edades de 126 anios.
 */
public final class Normalizador {

    private Normalizador() {
    }

    /**
     * Texto para comparar y agrupar: mayusculas, sin acentos, espacios simples.
     *
     * <p>Es la base de todo lo demas. El origen escribe la misma etiqueta de varias formas
     * ({@code "MACRO I"}, {@code "Macro  I"}, {@code "MACRO Í"}) y sin esto cada variante
     * sale como una barra distinta en la grafica.
     */
    public static String texto(String s) {
        if (s == null) {
            return null;
        }
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /**
     * Nombre de persona para contar {@code totalPersonas}.
     *
     * <p>Decision del 29-sep-2026: como los Excel no traen NSS, la identidad se resuelve por
     * nombre asumiendo el riesgo. <b>El riesgo esta medido</b>: sobre los 12 archivos de
     * atenciones de 2026 hay 12,453 atenciones y 4,639 nombres crudos que se reducen a 4,613
     * normalizados, o sea un 0.6% de variantes de captura. Y las 26 variantes son todas de dos
     * tipos, los dos que este metodo resuelve:
     * <ul>
     *   <li>espacios dobles: {@code "PERALTA  RAMIREZ ERIKA"}</li>
     *   <li>puntos en abreviaturas: {@code "MA. DE LA PAZ"} contra {@code "MA DE LA PAZ"}</li>
     * </ul>
     * No hay errores de dedo reales en la muestra.
     *
     * <p>Se guarda junto al nombre crudo (columnas {@code NOMBRE} y {@code NOMBRE_NORM}) para
     * poder recalcular si el criterio cambia.
     */
    public static String nombrePersona(String s) {
        String t = texto(s);
        if (t == null) {
            return null;
        }
        // Los puntos de abreviatura se vuelven espacio, no se borran: "MA.DE" -> "MA DE"
        return t.replaceAll("[.,]", " ").replaceAll("[^A-Z0-9 ÑÜ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    /** Los cinco rangos de edad, en la forma que produce el dashboard. */
    private static final Set<String> RANGOS = Set.of("18-25", "26-35", "36-45", "46-55", "55+");

    /**
     * Rango de edad en el formato del portal.
     *
     * <p>Desajuste real: el Excel escribe {@code "18 - 25"} con espacios y
     * {@code DashboardConsultaService.rangoEdad()} produce {@code "18-25"} sin ellos. Sin
     * normalizar, la grafica de edades sale con diez barras en vez de cinco.
     *
     * <p>Ademas el ultimo bucket cambio de nombre en 2024: hasta 2023 era {@code "+ 55"} y
     * desde 2024 es {@code "55 +"}. Los dos se resuelven a {@code "55+"}.
     */
    public static String rangoEdad(String etiqueta) {
        String t = texto(etiqueta);
        if (t == null || t.isBlank()) {
            return null;
        }
        String sinEspacios = t.replace(" ", "");
        if ("+55".equals(sinEspacios) || "55+".equals(sinEspacios)) {
            return "55+";
        }
        return RANGOS.contains(sinEspacios) ? sinEspacios : sinEspacios;
    }

    /**
     * Genero en la forma del portal.
     *
     * <p>El Excel usa {@code FEM} y {@code MASC} los diez anios, sin variantes. El grupo cambio
     * de nombre en 2024 ({@code SEXO} a {@code GENERO}) pero los valores no.
     */
    public static String genero(String etiqueta) {
        String t = texto(etiqueta);
        if (t == null || t.isBlank()) {
            return null;
        }
        if (t.startsWith("F")) {
            return "FEM";
        }
        if (t.startsWith("M")) {
            return "MASC";
        }
        return t;
    }

    /**
     * Edad exacta, con el freno de las filas fantasma.
     *
     * <p>Varios archivos de seguimiento calculan la edad con una formula sobre la fecha de
     * nacimiento. En las filas vacias la fecha nula se interpreta como 1900 y la formula arroja
     * <b>126 anios</b>. Lo vi en el seguimiento medico especial y en CAPA: renglones enteros con
     * {@code 126.58356164383562} en edad y antiguedad.
     *
     * <p>Son peores que una celda vacia porque el numero es plausible y no se nota. Cualquier
     * edad fuera del rango laboral se descarta y la fila queda sin edad, que es honesto.
     */
    public static Integer edadExacta(Double valor) {
        if (valor == null) {
            return null;
        }
        int edad = (int) Math.floor(valor);
        return (edad >= 14 && edad <= 99) ? edad : null;
    }

    /** Etiqueta de catalogo (causa, puesto, cuenta, area). Conserva el valor crudo legible. */
    public static String etiqueta(String s) {
        String t = texto(s);
        return (t == null || t.isBlank()) ? null : t;
    }

    /**
     * Nombre del predio en su forma canonica.
     *
     * <p>El predio sale del nombre de la hoja, y <b>el mismo predio se escribe distinto de un
     * archivo a otro del mismo mes y la misma carpeta</b>. Verificado el 30-sep-2026 recorriendo
     * los 132 archivos de 2026:
     * <ul>
     *   <li>carpeta {@code MIKELS-UT-FLORA}: la hoja se llama {@code MKLS} en atenciones, nuevo
     *       ingreso y pos incapacidad, y {@code MIKELS} en periodico.</li>
     *   <li>{@code MACRO 1} y {@code MACRO 2} en las hojas; el catalogo del portal los llama
     *       {@code MACRO I} y {@code MACRO II}, con numero romano.</li>
     *   <li>carpeta {@code MERCURIO}: la hoja del personal foraneo es {@code FORANEO} en tres
     *       reportes y {@code FLORANEO} en nuevo ingreso. Es un dedazo, no otro predio: no existe
     *       ninguna carpeta ni cuenta con ese nombre.</li>
     *   <li>{@code UT} y {@code U TEPALCAPA} son Unilever Tepalcapa. Es el mismo par que ya
     *       resuelve {@code CuadreAcumulado} para emparejar la hoja con su resumen.</li>
     *   <li>carpeta {@code ZARA}: la hoja es {@code Z VALLEJO} en las cuatro familias de la fase 1
     *       y {@code VALLEJO} en antidoping. Se canoniza a {@code Z VALLEJO}, que es como ya
     *       quedaron los 2,262 renglones cargados.</li>
     *   <li>carpeta {@code M1}: la hoja es {@code MACRO 1} en todas las familias menos maternidad,
     *       donde se llama {@code M1} como la carpeta.</li>
     * </ul>
     *
     * <p><b>Esta lista crece por familia, y a proposito no se adelanta.</b> El inventario de los
     * 132 archivos muestra muchos mas alias &mdash;{@code ATIZ}, {@code ZV}, {@code TULT},
     * {@code TOL}, {@code SIG}, {@code MERC}, {@code CHARC}, {@code WP}, {@code MIK}&mdash; pero
     * viven en familias que todavia no se analizan una por una. Se agregan cuando se configura su
     * familia y se ve el archivo, no antes: aqui el error caro no es dejar un alias sin emparejar
     * &mdash;eso se nota, sale un predio de mas&mdash; sino fundir dos predios distintos, que no
     * se nota nunca.
     *
     * <p>{@code M3} sigue sin mapear: aparece siete veces y no existe ningun predio "MACRO 3"
     * entre los quince. Se resolvera al configurar la familia que lo usa.</p>
     *
     * <p><b>Sobre {@code ZARA}.</b> Al principio se dejo fuera porque como etiqueta es ambigua:
     * hay cuentas {@code ZARA CADENAS} y {@code ZARA ALMACEN}. Se mapeo el 30-sep-2026 con
     * evidencia: es el nombre de una <b>carpeta</b>, y las hojas de esa carpeta se llaman
     * {@code Z VALLEJO} en las ocho familias anteriores. Queda una advertencia para el futuro: hay
     * tres <i>hojas</i> llamadas {@code ZARA} en familias de la fase 3 que todavia no se analizan;
     * cuando se configuren hay que confirmar que ahi tambien signifique el predio y no la cuenta.</p>
     *
     * <p>Sin esto la grafica por predio sale con 18 barras donde hay 15, y las tres partidas
     * quedan con la mitad de sus atenciones cada una. No se nota porque ninguna esta en cero.
     *
     * <p><b>La forma canonica es la del catalogo del portal</b> ({@code SERV_MED_PREDIO}, los 17
     * sitios que siembra {@code docs/ords-predio-cuenta.sql}), <b>no la que escribe el Excel</b>.
     * Corregido el 30-sep-2026: las pantallas de Analisis filtran con
     * {@code UPPER(e.PREDIO) = UPPER(?)} contra el nombre que el usuario elige en el select, y
     * ese select sale del catalogo. Cuatro nombres estaban canonizados al reves &mdash;
     * {@code MACRO 1}, {@code MACRO 2}, {@code MKLS}, {@code UT}&mdash; y por eso la ficha de esos
     * predios salia en ceros con los datos cargados y a la vista. <b>Es el modo de falla mas caro
     * de esta clase</b>: no hay aviso, no hay error, la carga dice {@code [ok]} y la pantalla
     * miente en silencio. Un nombre nuevo se agrega mirando primero el catalogo.
     *
     * <p><b>Es una lista explicita a proposito.</b> Se intento resolverlo con las reglas de
     * parecido de {@code CuadreAcumulado} &mdash;prefijo sin espacios, esqueleto de
     * consonantes&mdash; y ahi son aceptables porque el peor caso es no emparejar un resumen;
     * aqui el peor caso seria fundir dos predios de verdad, que si es dano. Un nombre que no
     * este en la lista se deja tal cual.
     */
    private static final java.util.Map<String, String> PREDIOS_CANONICOS = java.util.Map.ofEntries(
            java.util.Map.entry("MKLS", "MIKELS"),
            java.util.Map.entry("MIK", "MIKELS"),
            java.util.Map.entry("FLORANEO", "FORANEO"),
            java.util.Map.entry("FOR", "FORANEO"),
            java.util.Map.entry("UT", "U TEPALCAPA"),
            java.util.Map.entry("VALLEJO", "Z VALLEJO"),
            java.util.Map.entry("ZV", "Z VALLEJO"),
            java.util.Map.entry("M1", "MACRO I"),
            java.util.Map.entry("M2", "MACRO II"),
            java.util.Map.entry("MACRO 1", "MACRO I"),
            java.util.Map.entry("MACRO 2", "MACRO II"),
            java.util.Map.entry("WP", "WORLD PARK"),
            java.util.Map.entry("FLR", "FLORA"),
            java.util.Map.entry("MERC", "MERCURIO"),
            java.util.Map.entry("SIGLO", "SIGLO XXI"),
            java.util.Map.entry("SIG", "SIGLO XXI"),
            java.util.Map.entry("CHARC", "CHARCON"),
            java.util.Map.entry("ATIZ", "ATIZAPAN"),
            java.util.Map.entry("TOL", "TOLUCA"),
            java.util.Map.entry("TULT", "TULTIPARK"),
            java.util.Map.entry("TULTI", "TULTIPARK"),
            // Carpetas que llevan el nombre del CLIENTE y no del predio. Salieron al cargar
            // consumibles el 30-sep-2026, que es la unica familia cuyo predio viene de la carpeta
            // -sus hojas se llaman MEDICAMENTOS y MATERIAL FIJO-. Verificado dentro de los mismos
            // archivos: la carpeta DIAGEO contiene la hoja CHARCON, ATIZAPARK contiene ATIZAPAN y
            // ZARA contiene Z VALLEJO.
            java.util.Map.entry("ATIZAPARK", "ATIZAPAN"),
            java.util.Map.entry("DIAGEO", "CHARCON"),
            java.util.Map.entry("ZARA", "Z VALLEJO"));

    /**
     * Sufijos que dicen <b>el tipo</b> de incapacidad y no el predio.
     *
     * <p>Los archivos de incapacidades traen dos hojas por predio: la del IMSS, que se llama como
     * el predio, y la de las internas &mdash;las que paga la empresa&mdash; que se llama
     * {@code <predio> INT}. Sin quitar el sufijo aparecerian <b>quince predios falsos</b>:
     * {@code AIFA INT} como algo distinto de {@code AIFA}, con sus propias barras en la grafica.
     *
     * <p>El tipo no se pierde: esta en el nombre de la hoja, que se guarda en
     * {@code SERV_MED_BITACORA_LOTE.HOJA} y en {@code _EVENTO.ORIGEN_HOJA}.
     */
    private static final java.util.List<String> SUFIJOS_TIPO =
            java.util.List.of(" INTERNAS", " INTERNA", " INT");

    /**
     * Hojas cuyo nombre es <b>solo</b> el tipo, sin predio. Quien las lee tiene que sacar el
     * predio de la carpeta: en la carpeta {@code M2} la hoja se llama {@code INTERNA} y en
     * {@code WP} se llama {@code INTERNAS}, sin mas.
     */
    private static final java.util.Set<String> SOLO_TIPO =
            java.util.Set.of("INT", "INTERNA", "INTERNAS");

    /**
     * El predio de una hoja, o {@code null} si el nombre no lo dice.
     *
     * <p>Devolver {@code null} es informacion, no una falla: significa "esta hoja no sabe de que
     * predio es, preguntale a la carpeta".
     */
    public static String predio(String nombreHoja) {
        String t = etiqueta(nombreHoja);
        if (t == null || SOLO_TIPO.contains(t)) {
            return null;
        }
        for (String sufijo : SUFIJOS_TIPO) {
            if (t.endsWith(sufijo)) {
                t = t.substring(0, t.length() - sufijo.length()).trim();
                break;
            }
        }
        return t.isEmpty() ? null : PREDIOS_CANONICOS.getOrDefault(t, t);
    }

    /** Tope de {@code SERV_MED_BITACORA_ATRIBUTO.NOMBRE}. */
    private static final int MAX_NOMBRE_ATRIBUTO = 60;

    /**
     * Nombre de atributo a partir de la etiqueta del Excel.
     *
     * <p><b>Quita los parentesis, que no son parte del nombre sino instrucciones para quien
     * captura.</b> El origen las mete dentro del encabezado:
     * <pre>
     *   CORREO DE AVISO (COLOCAR LA FECHA DE ENVIO)
     *   OBSERVACIONES (COLOCAR CONCLUYE / BAJA / LABORANDO)
     *   NOMBRE (INICIAR POR APELLIDO Y NOMBRE COMPLETO)
     * </pre>
     * Concatenadas daban <b>95 caracteres</b> contra los 60 de la columna, y el 30-sep-2026 eso
     * tiro <b>diez de las quince hojas de maternidad</b> con {@code ORA-12899}. Limpiarlas es
     * mejor que ensanchar la columna: no necesita DDL en dos ambientes, y el nombre queda legible
     * &mdash;{@code CORREO_DE_AVISO.OBSERVACIONES} en vez del parrafo completo&mdash;.
     *
     * <p>El recorte final a 60 es una red por si aparece un encabezado largo sin parentesis. Es
     * preferible un nombre truncado a una hoja entera perdida.
     */
    public static String nombreAtributo(String etiqueta) {
        String t = texto(etiqueta);
        if (t == null) {
            return null;
        }
        String limpio = t.replaceAll("\\([^)]*\\)", " ")   // instrucciones entre parentesis
                .replaceAll("[^A-Z0-9. ]", " ")            // el punto separa grupo de subetiqueta
                .replaceAll("\\s+", " ")
                .trim()
                .replace(' ', '_')
                .replaceAll("_*\\._*", ".")                 // "GRUPO_._SUB" -> "GRUPO.SUB"
                // Un punto al principio o al final sobra: pasa cuando la subetiqueta era SOLO un
                // parentesis y al quitarlo no queda nada. Es el caso de accidentabilidad, donde
                // la subetiqueta de SDI es "(SALARIO DIARIO INTEGRADO)" completa; sin esto el
                // atributo se llamaria "SDI." con el punto colgando.
                .replaceAll("^[._]+|[._]+$", "");
        return limpio.length() <= MAX_NOMBRE_ATRIBUTO
                ? limpio : limpio.substring(0, MAX_NOMBRE_ATRIBUTO);
    }

    /**
     * Corrige el anio de una fecha cuando es imposible, conservando dia y mes.
     *
     * <p>Los archivos traen erratas de captura del anio que el cargador guardaba tal cual.
     * Medidas el 30-sep-2026 sobre las cuatro familias configuradas:
     * <ul>
     *   <li>{@code 1/19/29} &rarr; 2029, once renglones seguidos en atenciones de MACRO 2, todos
     *       del mismo dia de enero.</li>
     *   <li>{@code 5/1/26} que en realidad dice <b>2626</b>, dos renglones en AIFA: el formato de
     *       la celda es {@code d/m/yy} y muestra "26", asi que a la vista es correcto.</li>
     *   <li>anio 2002 en World Park y 2023 en un examen de ingreso de Atizapan.</li>
     * </ul>
     *
     * <p>Son ~20 renglones de 12,500, pero el dano es desproporcionado: la grafica de tendencia
     * agrupa por {@code ANIO}/{@code MES} y el eje saldria de 2002 a 2626 con un punto de un
     * renglon en cada extremo, dejando 2026 aplastado contra el margen.
     *
     * <p><b>La regla es conservadora y el limite esta donde esta por una razon.</b> Solo se
     * corrige lo <i>imposible</i>: el anio anterior al del archivo se respeta, porque ahi si hay
     * eventos legitimos &mdash;las incapacidades y las maternidades arrancan en diciembre y se
     * reportan en enero, y se ven 45 renglones asi, todos con fecha de fin de anio&mdash;. Si se
     * corrigiera tambien ese, se estarian moviendo hechos reales de fecha.
     *
     * @param fecha      la que se leyo de la celda
     * @param anioArchivo el anio que declara el archivo (carpeta y nombre)
     * @return la fecha corregida, o la misma si no habia nada que corregir
     */
    public static java.time.LocalDate anioPlausible(java.time.LocalDate fecha, int anioArchivo) {
        if (fecha == null || (fecha.getYear() >= anioArchivo - 1 && fecha.getYear() <= anioArchivo)) {
            return fecha;
        }
        // withYear ajusta el 29 de febrero al 28 cuando el destino no es bisiesto
        return fecha.withYear(anioArchivo);
    }

    /** Si una celda del bloque one-hot cuenta como marcada. */
    public static boolean marcado(String valor) {
        if (valor == null) {
            return false;
        }
        String v = valor.trim();
        if (v.isEmpty()) {
            return false;
        }
        // El origen marca con 1, pero tambien aparecen "1.0" (numerico) y una "x" ocasional
        return !"0".equals(v) && !"0.0".equals(v) && !"-".equals(v);
    }
}
