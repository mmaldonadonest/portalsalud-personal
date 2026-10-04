package com.onest.app.catalog.dashboard.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Agregados del historico cargado desde los Excel del servicio medico.
 *
 * <p>Lee {@code SERV_MED_BITACORA_EVENTO} y {@code _ATRIBUTO}, que llena el cargador del
 * proyecto {@code excel-loader/}. Es la otra mitad de {@link
 * com.onest.app.catalog.dashboard.service.DashboardConsultaService}: ese trae lo vigente de un WS
 * de ORDS, este trae lo historico de la base del portal.
 *
 * <p><b>Agrega en SQL y no en memoria</b>, al reves que el lado de ORDS. La razon es el volumen:
 * ORDS devuelve pocas filas y sumarlas en Java no cuesta nada, pero aqui hay ~20 mil atenciones
 * de 2026 y traerlas todas a cada carga de pantalla seria absurdo. Cada consulta devuelve ya el
 * conteo por categoria.
 *
 * <p>Por {@code JdbcTemplate} y no por JPA, igual que {@code FsFileRepository} e
 * {@code ImportRepository}: son consultas de agregacion, no un modelo de dominio.
 */
@Repository
public class BitacoraHistoricoRepository {

    /** Familia de eventos que alimenta las tres pantallas del grupo Morbilidad. */
    private static final String FAMILIA_ATENCION = "ATENCION";

    /**
     * Las tres familias de examen medico, que alimentan la pantalla Examenes.
     *
     * <p>Van juntas porque la pantalla pregunta "cuantos examenes y con que dictamen", sin
     * distinguir el tipo: el WS de ORDS tampoco lo distingue &mdash;{@code ExamenReporteDto} no
     * trae tipo de examen, y por eso la grafica es "dictamen por mes" y no "tipo por mes"&mdash;.
     * Si algun dia se quiere el desglose por tipo, el dato ya esta en la columna {@code FAMILIA}.
     */
    private static final String[] FAMILIAS_EXAMEN =
            {"EXAMEN_INGRESO", "EXAMEN_PERIODICO", "EXAMEN_POS_INCAP"};

    /** Atributo donde vive el dictamen, para las tres familias de examen. */
    private static final String ATRIBUTO_DICTAMEN = "DICTAMEN";

    /** Lo que se muestra cuando la celda venia vacia en el Excel. */
    private static final String SIN_DATO = "Sin dato";

    private final JdbcTemplate jdbc;

    public BitacoraHistoricoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Si hay historico cargado. Se consulta una vez por peticion para no armar las demas
     * consultas cuando la tabla esta vacia, que es el estado normal hasta que corra la carga.
     */
    public boolean hayDatos() {
        try {
            Integer n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM (SELECT 1 FROM SERV_MED_BITACORA_EVENTO WHERE ROWNUM = 1)",
                    Integer.class);
            return n != null && n > 0;
        } catch (Exception e) {
            // La tabla puede no existir todavia en este ambiente: no es un error, es que no hay
            // historico. El dashboard sigue funcionando solo con ORDS.
            return false;
        }
    }

    /** Total de atenciones en el rango. */
    public long totalAtenciones(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta("SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e");
        c.filtros(desde, hasta, predio, cuenta);
        Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
        return n == null ? 0 : n;
    }

    /**
     * Personas distintas, contadas por {@code NOMBRE_NORM}.
     *
     * <p>Los Excel no traen NSS, asi que la identidad se resuelve por nombre normalizado
     * &mdash;mayusculas, sin acentos, espacios simples, sin puntos&mdash;, con el riesgo medido y
     * aceptado el 29-sep-2026: sobre 12,453 atenciones de 2026 salen 4,613 personas, con un 0.6%
     * de variantes de captura que la normalizacion resuelve.
     *
     * <p>Los 48 nombres que aparecen en mas de un predio se cuentan <b>una sola vez</b>: es lo
     * consistente con el portal a partir de 2027, donde manda el NSS y quien cambia de predio
     * sigue siendo una persona. La diferencia contra el otro criterio es del 1%.
     */
    public long totalPersonas(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT COUNT(DISTINCT e.NOMBRE_NORM) FROM SERV_MED_BITACORA_EVENTO e");
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.NOMBRE_NORM IS NOT NULL");
        Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
        return n == null ? 0 : n;
    }

    /** Conteo por una columna de EVENTO: PREDIO, CUENTA, PUESTO, AREA, GENERO, RANGO_EDAD. */
    public List<ConteoSimpleDto> porColumna(String columna, LocalDate desde, LocalDate hasta,
                                            String predio, String cuenta, int limite) {
        String col = columnaValida(columna);
        Consulta c = new Consulta(
                "SELECT NVL(e." + col + ", '" + SIN_DATO + "') clave, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e");
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY NVL(e." + col + ", '" + SIN_DATO + "') "
                + "ORDER BY COUNT(*) DESC", c.params(), limite);
    }

    /**
     * Conteo por un atributo: CAUSA, RAMO, LESION_ME, DICTAMEN, DIAGNOSTICO.
     *
     * <p>Lo que cambia de familia a familia vive en {@code _ATRIBUTO} como renglones, asi que
     * aqui se pivotea con un JOIN. El indice {@code SERV_MED_IX_ATRIB_NOM_VAL} sobre
     * {@code (NOMBRE, VALOR)} existe justo para esta consulta.
     */
    public List<ConteoSimpleDto> porAtributo(String nombreAtributo, LocalDate desde, LocalDate hasta,
                                             String predio, String cuenta, int limite) {
        Consulta c = new Consulta(
                "SELECT NVL(a.VALOR, '" + SIN_DATO + "') clave, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID");
        c.and("a.NOMBRE = ?", nombreAtributo);
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY NVL(a.VALOR, '" + SIN_DATO + "') "
                + "ORDER BY COUNT(*) DESC", c.params(), limite);
    }

    /**
     * Serie mensual.
     *
     * <p>Se agrupa por las columnas {@code ANIO} y {@code MES}, que el cargador ya calculo de la
     * fecha de cada renglon. Por eso no hace falta {@code EXTRACT} aqui, y por eso existe el
     * indice {@code SERV_MED_IX_BITACORA_FAM_MES}.
     */
    public List<PuntoMensualDto> tendenciaMensual(LocalDate desde, LocalDate hasta,
                                                  String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e");
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    /** Serie mensual por predio, para la gráfica comparada. */
    public List<Object[]> tendenciaPorPredio(LocalDate desde, LocalDate hasta,
                                             String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT NVL(e.PREDIO, '" + SIN_DATO + "') predio, "
                + "TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e");
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "'), e.ANIO, e.MES "
                        + "ORDER BY 1, 2",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)},
                c.params());
    }

    /**
     * Lesiones musculoesqueleticas del historico, por su etiqueta cruda del Excel.
     *
     * <p>Devuelve las 15 etiquetas del bloque {@code CAUSAS MUSCULO ESQUELETICAS} con su conteo.
     * Quien la consume las pasa por {@code LesionMusculoesqueletica.clasificarDeEtiquetaExcel}
     * para obtener los mismos cuatro grupos que produce el lado de ORDS desde CIE-10.
     *
     * <p>El bloque viene vacio en la mayoria de los renglones y eso es correcto: solo se llena
     * en riesgo de trabajo. En 2026 ronda el 15% de las atenciones.
     */
    public List<ConteoSimpleDto> lesionesMusculoesqueleticas(LocalDate desde, LocalDate hasta,
                                                             String predio, String cuenta) {
        return porAtributo("LESION_ME", desde, hasta, predio, cuenta, 0);
    }

    /**
     * Personas distintas que tuvieron al menos una lesion musculoesqueletica.
     *
     * <p>Se cuenta aparte y no se deriva del conteo de lesiones: una misma persona puede tener
     * varias en el periodo, y el KPI pregunta cuantas personas, no cuantos eventos.
     */
    public long personasConLesion(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT COUNT(DISTINCT e.NOMBRE_NORM) FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID");
        c.and("a.NOMBRE = ?", "LESION_ME");
        c.and("a.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.NOMBRE_NORM IS NOT NULL");
        Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
        return n == null ? 0 : n;
    }

    /** Lesiones por predio, para el ranking de la pantalla. */
    public List<ConteoSimpleDto> lesionesPorPredio(LocalDate desde, LocalDate hasta,
                                                   String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID");
        c.and("a.NOMBRE = ?", "LESION_ME");
        c.and("a.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "') "
                + "ORDER BY COUNT(*) DESC", c.params(), 0);
    }

    /** Serie mensual de lesiones musculoesqueleticas. */
    public List<PuntoMensualDto> tendenciaLesiones(LocalDate desde, LocalDate hasta,
                                                   String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID");
        c.and("a.NOMBRE = ?", "LESION_ME");
        c.and("a.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Examenes medicos
    //
    //  Las tres familias de examen se cargaron en la fase 1 junto con las atenciones, pero la
    //  pantalla Examenes no las leia: la fase se definio alrededor de las tres pantallas de
    //  Morbilidad. Son 8,616 renglones que ya estaban en la base, verificados, sin usar.
    // ---------------------------------------------------------------------------------------

    /**
     * Un cruce de dos dimensiones: la clave que agrupa (predio o mes) y el valor de un atributo.
     *
     * <p>Se devuelve crudo y quien llama lo pivotea, en vez de armar aqui las columnas fijas.
     * La razon es que el valor vive en la tabla de atributos como <i>dato</i> y no como columna:
     * si el origen agrega un dictamen o una sustancia nueva, esta consulta la trae sola y se nota,
     * en lugar de perderla en un {@code CASE} que solo contempla las conocidas.
     */
    public record Cruce(String clave, String valor, long cantidad) {
    }

    /**
     * Si hay examenes historicos cargados.
     *
     * <p>{@code ROWNUM = 1} corta en cuanto encuentra el primero, sin recorrer los 8 mil.
     */
    public boolean hayExamenes() {
        try {
            Consulta c = new Consulta(
                    "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIAS_EXAMEN);
            c.and("ROWNUM = 1");
            Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
            return n != null && n > 0;
        } catch (Exception e) {
            // La tabla puede no existir en este ambiente: no hay historico, no es un error.
            return false;
        }
    }

    /** Total de examenes en el rango, de las tres familias. */
    public long totalExamenes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIAS_EXAMEN);
        c.filtros(desde, hasta, predio, cuenta);
        Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
        return n == null ? 0 : n;
    }

    /** Conteo por dictamen, sin desglosar. */
    public List<ConteoSimpleDto> examenesPorDictamen(LocalDate desde, LocalDate hasta,
                                                     String predio, String cuenta) {
        Consulta c = consultaDictamen(
                "SELECT NVL(a.VALOR, '" + SIN_DATO + "') clave, COUNT(*) cantidad",
                desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY NVL(a.VALOR, '" + SIN_DATO + "') "
                + "ORDER BY COUNT(*) DESC", c.params(), 0);
    }

    /** Examenes por predio y dictamen, para la barra apilada horizontal. */
    public List<Cruce> examenesPorPredioYDictamen(LocalDate desde, LocalDate hasta,
                                                          String predio, String cuenta) {
        Consulta c = consultaDictamen(
                "SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, a.VALOR valor, COUNT(*) cantidad",
                desde, hasta, predio, cuenta);
        return cruce(c.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "'), a.VALOR "
                + "ORDER BY COUNT(*) DESC", c.params());
    }

    /**
     * Examenes por mes y dictamen.
     *
     * <p>Se agrupa por {@code ANIO}/{@code MES}, que el cargador ya calculo, y no por
     * {@code EXTRACT(FECHA)}: asi los renglones cuya celda de fecha no se pudo leer &mdash;46 en
     * la carga de 2026&mdash; siguen apareciendo en su mes, que lo tomaron del bloque MES del
     * propio Excel.
     */
    public List<Cruce> examenesPorMesYDictamen(LocalDate desde, LocalDate hasta,
                                                        String predio, String cuenta) {
        Consulta c = consultaDictamen(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') clave, "
                + "a.VALOR valor, COUNT(*) cantidad",
                desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return cruce(c.sql() + " GROUP BY e.ANIO, e.MES, a.VALOR ORDER BY e.ANIO, e.MES", c.params());
    }

    /** El JOIN y los filtros que comparten las tres consultas de dictamen. */
    private Consulta consultaDictamen(String select, LocalDate desde, LocalDate hasta,
                                      String predio, String cuenta) {
        Consulta c = new Consulta(select
                + " FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIAS_EXAMEN);
        c.and("a.NOMBRE = ?", ATRIBUTO_DICTAMEN);
        c.and("a.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        return c;
    }

    // ---------------------------------------------------------------------------------------
    //  Antidoping y alcoholimetria
    //
    //  Es la primera familia de la fase 2. Su bloque RESULTADO lleva DOS marcas en los renglones
    //  positivos -el veredicto y la sustancia- asi que el cargador las separa en dos atributos:
    //  RESULTADO con NEGATIVO/POSITIVO, y SUSTANCIA con AMF, THC, COC, BENZO...  Guardarlas bajo
    //  el mismo nombre habria hecho que una grafica "por resultado" pintara POSITIVO y THC como
    //  barras hermanas, que son respuestas a preguntas distintas.
    // ---------------------------------------------------------------------------------------

    private static final String[] FAMILIA_ANTIDOPING = {"ANTIDOPING"};

    /** Si hay pruebas de antidoping historicas cargadas. */
    public boolean hayAntidoping() {
        try {
            Consulta c = new Consulta(
                    "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_ANTIDOPING);
            c.and("ROWNUM = 1");
            Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Total de pruebas en el rango. */
    public long totalAntidoping(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_ANTIDOPING);
        c.filtros(desde, hasta, predio, cuenta);
        Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
        return n == null ? 0 : n;
    }

    /**
     * Conteo por uno de sus atributos: {@code RESULTADO}, {@code SUSTANCIA},
     * {@code TIPO_PRUEBA}, {@code CONCLUSION} o {@code ALCOHOLEMIA}.
     *
     * <p>Ojo con {@code SUSTANCIA}: <b>no suma al total de pruebas</b>. Solo existe en los
     * renglones positivos, que son pocos, y un renglon positivo podria traer mas de una si la
     * persona salio positiva a dos cosas. Es un conteo de hallazgos, no de personas ni de pruebas.
     */
    public List<ConteoSimpleDto> antidopingPorAtributo(String nombreAtributo, LocalDate desde,
                                                       LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT a.VALOR clave, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_ANTIDOPING);
        c.and("a.NOMBRE = ?", nombreAtributo);
        c.and("a.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY a.VALOR ORDER BY COUNT(*) DESC", c.params(), 0);
    }

    /** Pruebas por predio. */
    public List<ConteoSimpleDto> antidopingPorPredio(LocalDate desde, LocalDate hasta,
                                                     String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_ANTIDOPING);
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "') "
                + "ORDER BY COUNT(*) DESC", c.params(), 0);
    }

    /** Predio x tipo de prueba, para separar antidoping de alcoholimetria en la misma grafica. */
    public List<Cruce> antidopingPorPredioYTipo(LocalDate desde, LocalDate hasta,
                                                String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, a.VALOR valor, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_ANTIDOPING);
        c.and("a.NOMBRE = ?", "TIPO_PRUEBA");
        c.and("a.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        return cruce(c.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "'), a.VALOR "
                + "ORDER BY COUNT(*) DESC", c.params());
    }

    /** Serie mensual de pruebas. */
    public List<PuntoMensualDto> antidopingTendencia(LocalDate desde, LocalDate hasta,
                                                     String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_ANTIDOPING);
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Maternidad
    //
    //  Su FECHA no es la del hecho sino la del MES EN QUE SE REPORTA el caso: un expediente de
    //  maternidad se sigue durante meses y sus fechas -FUM, parto probable, incapacidad,
    //  lactancia- son todas de otra cosa. Viven como atributos, con su tipo.
    // ---------------------------------------------------------------------------------------

    private static final String[] FAMILIA_MATERNIDAD = {"MATERNIDAD"};

    /** Las semanas de gestacion. Se guardan con tipo, asi que el promedio sale de VALOR_NUM. */
    private static final String ATR_SEMANAS = "EDAD_GESTACIONAL.SEMANA";

    /** Si hay seguimientos de maternidad historicos cargados. */
    public boolean hayMaternidad() {
        try {
            Consulta c = new Consulta(
                    "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_MATERNIDAD);
            c.and("ROWNUM = 1");
            Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Totales de maternidad en una sola consulta.
     *
     * <p>Van juntos porque son cuatro agregados sobre la misma tabla y el mismo filtro: pedirlos
     * por separado serian cuatro recorridos para lo mismo.
     *
     * @param nombreAtributoReincorporacion atributo cuya presencia indica reincorporacion
     */
    public TotalesMaternidad totalesMaternidad(LocalDate desde, LocalDate hasta, String predio,
                                               String cuenta, String nombreAtributoReincorporacion) {
        Consulta c = new Consulta("""
                SELECT COUNT(*) seguimientos,
                       COUNT(DISTINCT e.NOMBRE_NORM) personas,
                       COUNT(DISTINCT CASE WHEN EXISTS (
                             SELECT 1 FROM SERV_MED_BITACORA_ATRIBUTO r
                              WHERE r.EVENTO_ID = e.ID AND r.NOMBRE = ?
                                AND (r.VALOR_FECHA IS NOT NULL OR r.VALOR IS NOT NULL))
                            THEN e.NOMBRE_NORM END) reincorporadas
                  FROM SERV_MED_BITACORA_EVENTO e""", FAMILIA_MATERNIDAD);
        c.filtros(desde, hasta, predio, cuenta);

        // El ? del atributo esta en el SELECT, o sea ANTES de los del WHERE: en JDBC los
        // parametros van por posicion, asi que tiene que ir primero en el arreglo.
        Object[] todos = new Object[c.params().length + 1];
        todos[0] = nombreAtributoReincorporacion;
        System.arraycopy(c.params(), 0, todos, 1, c.params().length);

        return jdbc.queryForObject(c.sql(),
                (rs, i) -> new TotalesMaternidad(rs.getLong("seguimientos"),
                        rs.getLong("personas"), rs.getLong("reincorporadas")),
                todos);
    }

    /** Los tres totales de maternidad. */
    public record TotalesMaternidad(long seguimientos, long personas, long reincorporadas) {
    }

    /**
     * Las semanas de gestacion registradas, para promediarlas y agruparlas por trimestre.
     *
     * <p>Devuelve los valores y no el promedio ya hecho: el corte por trimestre lo define el
     * negocio (1&ndash;13, 14&ndash;27, 28+) y vive en el servicio, junto al del lado de ORDS,
     * para que no haya dos definiciones del mismo trimestre.
     *
     * <p>Solo las numericas. En los archivos, cuando la persona ya dio a luz la celda dice
     * {@code TERMINO} en lugar de un numero, porque la columna es una formula sobre la FUM.
     */
    public List<Integer> semanasGestacion(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT a.VALOR_NUM semanas FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_MATERNIDAD);
        c.and("a.NOMBRE = ?", ATR_SEMANAS);
        c.and("a.VALOR_NUM IS NOT NULL");
        // 0 a 45: fuera de ahi es una formula que se calculo sobre una FUM vacia, el mismo caso
        // de las edades de 126 anios del seguimiento medico especial
        c.and("a.VALOR_NUM BETWEEN 1 AND 45");
        c.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(c.sql(), (rs, i) -> rs.getInt("semanas"), c.params());
    }

    /** Conteo por una columna de EVENTO, acotado a maternidad. */
    public List<ConteoSimpleDto> maternidadPorColumna(String columna, LocalDate desde, LocalDate hasta,
                                                      String predio, String cuenta) {
        String col = columnaValida(columna);
        Consulta c = new Consulta(
                "SELECT NVL(e." + col + ", '" + SIN_DATO + "') clave, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_MATERNIDAD);
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY NVL(e." + col + ", '" + SIN_DATO + "') "
                + "ORDER BY COUNT(*) DESC", c.params(), 0);
    }

    /** Serie mensual de seguimientos, por el mes en que se reporta el caso. */
    public List<PuntoMensualDto> maternidadTendencia(LocalDate desde, LocalDate hasta,
                                                     String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_MATERNIDAD);
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    /**
     * Partos probables por mes, para anticipar ausencias.
     *
     * <p>La fecha probable de parto es una <b>formula</b> en el Excel ({@code FUM + 280 dias}), asi
     * que se guarda como texto ISO cuando la celda tiene formato de fecha y como el numero de serie
     * de Excel cuando no. Por eso el {@code REGEXP_LIKE}: solo se agrupan los valores que de verdad
     * parecen una fecha, y lo demas se ignora en vez de ensuciar la grafica con un mes inventado.
     */
    public List<PuntoMensualDto> partosPorMes(LocalDate desde, LocalDate hasta, String predio,
                                              String cuenta, String nombreAtributo) {
        Consulta c = new Consulta(
                "SELECT SUBSTR(a.VALOR, 1, 7) mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_MATERNIDAD);
        c.and("a.NOMBRE = ?", nombreAtributo);
        c.and("REGEXP_LIKE(a.VALOR, '^[0-9]{4}-[0-9]{2}-[0-9]{2}')");
        c.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(c.sql() + " GROUP BY SUBSTR(a.VALOR, 1, 7) ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Incapacidades
    //
    //  Es la familia con mas volumen del proyecto: ~1,929 episodios de 2026. Cada archivo trae
    //  DOS hojas -las del IMSS y las internas que paga la empresa- y las dos cargan con el mismo
    //  mapeo.
    // ---------------------------------------------------------------------------------------

    private static final String[] FAMILIA_INCAPACIDAD = {"INCAPACIDAD"};

    /** Nombres que produjo el cargador. Verificados, no supuestos: un nombre mal escrito aqui
     *  no falla, devuelve cero, y una grafica en cero se lee como "no hubo incapacidades". */
    private static final String ATR_RAMO = "DETALLE_DE_INCAPACIDAD.RAMO_RT1_RT2_EG_MAT";
    private static final String ATR_DIAS = "DETALLE_DE_INCAPACIDAD.DIAS_DE_INCAPACIDAD";
    private static final String ATR_COSTO_IMSS = "COSTOS.TOTAL";
    private static final String ATR_COSTO_INT = "COSTO_TOTAL";
    private static final String ATR_DIAS_INT = "DIAS_TOTAL";

    /**
     * El rubro se deriva del nombre de la hoja, no se guarda.
     *
     * <p>Las incapacidades internas viven en una hoja {@code <predio> INT}, y al cargar se le quita
     * ese sufijo al PREDIO para no inventar quince predios falsos. Pero el nombre crudo queda en
     * {@code ORIGEN_HOJA}, asi que el rubro se recupera de ahi.
     *
     * <p><b>Por que derivarlo en la consulta y no guardarlo como columna.</b> Es el mismo criterio
     * que el comentario de {@code _ATRIBUTO.VALOR} en el DDL: se guarda el dato crudo y las
     * equivalencias de negocio se recalculan, para no tener que releer los 132 archivos si la regla
     * cambia. Y es seguro porque ninguno de los quince predios lleva "INT" en su nombre.
     */
    private static final String RUBRO =
            "CASE WHEN UPPER(e.ORIGEN_HOJA) LIKE '%INT%' THEN 'Interna' ELSE 'IMSS' END";

    /** Si hay incapacidades historicas cargadas. */
    public boolean hayIncapacidades() {
        try {
            Consulta c = new Consulta(
                    "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_INCAPACIDAD);
            c.and("ROWNUM = 1");
            Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Episodios y personas distintas. Un renglon del Excel = un episodio. */
    public TotalesIncapacidad totalesIncapacidad(LocalDate desde, LocalDate hasta,
                                                 String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT COUNT(*) episodios, COUNT(DISTINCT e.NOMBRE_NORM) personas "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_INCAPACIDAD);
        c.filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(c.sql(),
                (rs, i) -> new TotalesIncapacidad(rs.getLong("episodios"), rs.getLong("personas")),
                c.params());
    }

    public record TotalesIncapacidad(long episodios, long personas) {
    }

    /**
     * Suma de un atributo numerico. Sirve para dias y para costo, que en las dos hojas se llaman
     * distinto: el IMSS guarda {@code COSTOS.TOTAL} y la interna {@code COSTO_TOTAL}.
     *
     * <p>Se suma {@code VALOR_NUM} y no {@code VALOR}: el cargador ya guardo cada campo con su
     * tipo real, asi que aqui no hay que convertir texto ni arriesgarse a un formato raro.
     */
    public double sumaAtributoIncapacidad(LocalDate desde, LocalDate hasta, String predio,
                                          String cuenta, String... nombresAtributo) {
        String huecos = String.join(",", java.util.Collections.nCopies(nombresAtributo.length, "?"));
        Consulta c = new Consulta(
                "SELECT NVL(SUM(a.VALOR_NUM), 0) total FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_INCAPACIDAD);
        c.and("a.NOMBRE IN (" + huecos + ")", (Object[]) nombresAtributo);
        c.filtros(desde, hasta, predio, cuenta);
        Double n = jdbc.queryForObject(c.sql(), Double.class, c.params());
        return n == null ? 0 : n;
    }

    /**
     * Un grupo de incapacidades con sus tres medidas.
     *
     * <p>Las tres van juntas porque la pantalla las pinta juntas: cada barra lleva episodios,
     * dias y costo. Pedirlas por separado serian tres recorridos de la misma tabla con el mismo
     * filtro, y tres oportunidades de que una quede desalineada de las otras.
     */
    public record AgrupadoIncapacidad(String clave, long episodios, long personas,
                                      long dias, double costo) {
    }

    /** Las dimensiones por las que se puede agrupar. Lo demas se rechaza. */
    private String dimensionIncapacidad(String dimension) {
        return switch (dimension == null ? "" : dimension.toUpperCase()) {
            case "PREDIO" -> "NVL(e.PREDIO, '" + SIN_DATO + "')";
            case "CUENTA" -> "NVL(e.CUENTA, '" + SIN_DATO + "')";
            case "AREA" -> "NVL(e.AREA, '" + SIN_DATO + "')";
            case "RUBRO" -> RUBRO;
            default -> throw new IllegalArgumentException(
                    "Dimension no permitida para incapacidades: " + dimension);
        };
    }

    /**
     * Agrupa incapacidades por una dimension que vive en {@code _EVENTO}: predio, cuenta, area o
     * el rubro derivado del nombre de la hoja.
     *
     * <p>Los dos {@code LEFT JOIN} no multiplican renglones: un episodio tiene <b>un</b> atributo
     * de dias y <b>uno</b> de costo &mdash;la hoja del IMSS usa unos nombres y la interna otros, y
     * ningun episodio esta en las dos&mdash;. Aun asi se cuenta con {@code COUNT(DISTINCT e.ID)},
     * para que si un archivo llega algun dia con el atributo duplicado el total no se infle.
     */
    public List<AgrupadoIncapacidad> incapacidadPorDimension(String dimension, LocalDate desde,
                                                             LocalDate hasta, String predio,
                                                             String cuenta) {
        String dim = dimensionIncapacidad(dimension);
        Consulta c = new Consulta("""
                SELECT %s clave,
                       COUNT(DISTINCT e.ID) episodios,
                       COUNT(DISTINCT e.NOMBRE_NORM) personas,
                       NVL(SUM(d.VALOR_NUM), 0) dias,
                       NVL(SUM(k.VALOR_NUM), 0) costo
                  FROM SERV_MED_BITACORA_EVENTO e
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO d
                         ON d.EVENTO_ID = e.ID AND d.NOMBRE IN (?, ?)
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO k
                         ON k.EVENTO_ID = e.ID AND k.NOMBRE IN (?, ?)""".formatted(dim),
                FAMILIA_INCAPACIDAD);
        c.filtros(desde, hasta, predio, cuenta);
        return agrupado(c.sql() + " GROUP BY " + dim + " ORDER BY 2 DESC",
                conParametrosDeAtributo(c));
    }

    /**
     * Agrupa por ramo, que no es una columna sino un atributo, asi que lleva un JOIN mas.
     *
     * <p>Los valores son los del catalogo del IMSS: {@code EG} enfermedad general, {@code RT1}
     * riesgo de trabajo laboral, {@code RT2} de trayecto y {@code MAT} maternidad.
     */
    public List<AgrupadoIncapacidad> incapacidadPorRamo(LocalDate desde, LocalDate hasta,
                                                        String predio, String cuenta) {
        Consulta c = new Consulta("""
                SELECT r.VALOR clave,
                       COUNT(DISTINCT e.ID) episodios,
                       COUNT(DISTINCT e.NOMBRE_NORM) personas,
                       NVL(SUM(d.VALOR_NUM), 0) dias,
                       NVL(SUM(k.VALOR_NUM), 0) costo
                  FROM SERV_MED_BITACORA_EVENTO e
                  JOIN SERV_MED_BITACORA_ATRIBUTO r
                         ON r.EVENTO_ID = e.ID AND r.NOMBRE = ?
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO d
                         ON d.EVENTO_ID = e.ID AND d.NOMBRE IN (?, ?)
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO k
                         ON k.EVENTO_ID = e.ID AND k.NOMBRE IN (?, ?)""",
                FAMILIA_INCAPACIDAD);
        c.and("r.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);

        Object[] todos = new Object[c.params().length + 5];
        todos[0] = ATR_RAMO;
        todos[1] = ATR_DIAS;
        todos[2] = ATR_DIAS_INT;
        todos[3] = ATR_COSTO_IMSS;
        todos[4] = ATR_COSTO_INT;
        System.arraycopy(c.params(), 0, todos, 5, c.params().length);
        return agrupado(c.sql() + " GROUP BY r.VALOR ORDER BY 2 DESC", todos);
    }

    /** Los cuatro ? de los JOIN van antes de los del WHERE: en JDBC el orden es posicional. */
    private Object[] conParametrosDeAtributo(Consulta c) {
        Object[] todos = new Object[c.params().length + 4];
        todos[0] = ATR_DIAS;
        todos[1] = ATR_DIAS_INT;
        todos[2] = ATR_COSTO_IMSS;
        todos[3] = ATR_COSTO_INT;
        System.arraycopy(c.params(), 0, todos, 4, c.params().length);
        return todos;
    }

    private List<AgrupadoIncapacidad> agrupado(String sql, Object[] params) {
        return jdbc.query(sql, (rs, i) -> new AgrupadoIncapacidad(
                rs.getString("clave"), rs.getLong("episodios"), rs.getLong("personas"),
                rs.getLong("dias"), rs.getDouble("costo")), params);
    }

    /** Serie mensual de episodios. */
    public List<PuntoMensualDto> incapacidadTendencia(LocalDate desde, LocalDate hasta,
                                                      String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_INCAPACIDAD);
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    /** Dias por mes, sumados. */
    public List<PuntoMensualDto> incapacidadDiasPorMes(LocalDate desde, LocalDate hasta,
                                                       String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, "
                + "NVL(SUM(a.VALOR_NUM), 0) cantidad FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_INCAPACIDAD);
        c.and("a.NOMBRE IN (?, ?)", ATR_DIAS, ATR_DIAS_INT);
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    /** Dias por mes y por ramo, para la barra apilada del modulo. */
    public List<Cruce> incapacidadDiasPorMesYRamo(LocalDate desde, LocalDate hasta,
                                                  String predio, String cuenta) {
        Consulta c = new Consulta("""
                SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') clave,
                       r.VALOR valor,
                       NVL(SUM(d.VALOR_NUM), 0) cantidad
                  FROM SERV_MED_BITACORA_EVENTO e
                  JOIN SERV_MED_BITACORA_ATRIBUTO r ON r.EVENTO_ID = e.ID AND r.NOMBRE = ?
                  JOIN SERV_MED_BITACORA_ATRIBUTO d ON d.EVENTO_ID = e.ID AND d.NOMBRE IN (?, ?)""",
                FAMILIA_INCAPACIDAD);
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        c.and("r.VALOR IS NOT NULL");

        // Los tres ? del FROM van antes de los del WHERE: en JDBC el orden es posicional
        Object[] todos = new Object[c.params().length + 3];
        todos[0] = ATR_RAMO;
        todos[1] = ATR_DIAS;
        todos[2] = ATR_DIAS_INT;
        System.arraycopy(c.params(), 0, todos, 3, c.params().length);

        return cruce(c.sql() + " GROUP BY e.ANIO, e.MES, r.VALOR ORDER BY e.ANIO, e.MES", todos);
    }

    /** Los dos nombres del costo total, que difieren entre la hoja del IMSS y la interna. */
    public double costoIncapacidades(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return sumaAtributoIncapacidad(desde, hasta, predio, cuenta, ATR_COSTO_IMSS, ATR_COSTO_INT);
    }

    /** Los dos nombres de los dias. */
    public double diasIncapacidades(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return sumaAtributoIncapacidad(desde, hasta, predio, cuenta, ATR_DIAS, ATR_DIAS_INT);
    }

    // ---------------------------------------------------------------------------------------
    //  Accidentabilidad
    // ---------------------------------------------------------------------------------------

    private static final String[] FAMILIA_ACCIDENTE = {"ACCIDENTE"};

    private static final String ATR_TIPO_RIESGO = "TIPO_RIESGO";

    /**
     * El costo vive en dos atributos, uno por via de dictamen.
     *
     * <p>El bloque {@code COSTO} del Excel abre en {@code CALIFICADO} e {@code IMPROCEDENTE}, y
     * cada uno es una formula que ya suma los costos de las incapacidades de esa via. El costo del
     * accidente es la suma de los dos: un mismo caso puede tener dias calificados y dias que el
     * IMSS declaro improcedentes y paga la empresa.
     */
    private static final String[] ATR_COSTO_ACCIDENTE = {"COSTO.CALIFICADO", "COSTO.IMPROCEDENTE"};

    /**
     * Prefijo de los atributos de causa.
     *
     * <p>{@code CAUSA DE RT} es un grupo <b>mixto</b> en el Excel: su primera columna es la
     * descripcion en texto libre y las siete siguientes son las causas con marca. Se cargo como
     * grupo de campos, asi que la causa quedo en el <i>nombre</i> del atributo
     * ({@code CAUSA_DE_RT.CAIDA}) y no en su valor, y aqui se recupera del nombre.
     */
    private static final String PREFIJO_CAUSA_RT = "CAUSA_DE_RT.";
    private static final String ATR_DESCRIPCION_RT = "CAUSA_DE_RT.DESCRIPCION_BREVE_DEL_ACCIDENTE";

    /** Si hay accidentes historicos cargados. */
    public boolean hayAccidentes() {
        try {
            Consulta c = new Consulta(
                    "SELECT COUNT(*) FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_ACCIDENTE);
            c.and("ROWNUM = 1");
            Long n = jdbc.queryForObject(c.sql(), Long.class, c.params());
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Un grupo de accidentes con su conteo y su costo. */
    public record AgrupadoAccidente(String clave, long accidentes, double costo) {
    }

    /** Total de accidentes y costo, sin agrupar. */
    public AgrupadoAccidente totalesAccidentes(LocalDate desde, LocalDate hasta,
                                               String predio, String cuenta) {
        Consulta c = new Consulta("""
                SELECT 'TOTAL' clave,
                       COUNT(DISTINCT e.ID) accidentes,
                       NVL(SUM(k.VALOR_NUM), 0) costo
                  FROM SERV_MED_BITACORA_EVENTO e
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO k
                         ON k.EVENTO_ID = e.ID AND k.NOMBRE IN (?, ?)""", FAMILIA_ACCIDENTE);
        c.filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(c.sql(), (rs, i) -> new AgrupadoAccidente(
                rs.getString("clave"), rs.getLong("accidentes"), rs.getDouble("costo")),
                conCosto(c));
    }

    /** Accidentes y costo por una columna de EVENTO: {@code PREDIO} o {@code ESTATUS}. */
    public List<AgrupadoAccidente> accidentesPorColumna(String columna, LocalDate desde,
                                                        LocalDate hasta, String predio,
                                                        String cuenta) {
        String col = "NVL(e." + columnaValida(columna) + ", '" + SIN_DATO + "')";
        Consulta c = new Consulta("""
                SELECT %s clave,
                       COUNT(DISTINCT e.ID) accidentes,
                       NVL(SUM(k.VALOR_NUM), 0) costo
                  FROM SERV_MED_BITACORA_EVENTO e
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO k
                         ON k.EVENTO_ID = e.ID AND k.NOMBRE IN (?, ?)""".formatted(col),
                FAMILIA_ACCIDENTE);
        c.filtros(desde, hasta, predio, cuenta);
        return agrupadoAccidente(c.sql() + " GROUP BY " + col + " ORDER BY 2 DESC", conCosto(c));
    }

    /** Accidentes y costo por tipo de riesgo (laboral o trayecto). */
    public List<AgrupadoAccidente> accidentesPorTipoRiesgo(LocalDate desde, LocalDate hasta,
                                                           String predio, String cuenta) {
        Consulta c = new Consulta("""
                SELECT t.VALOR clave,
                       COUNT(DISTINCT e.ID) accidentes,
                       NVL(SUM(k.VALOR_NUM), 0) costo
                  FROM SERV_MED_BITACORA_EVENTO e
                  JOIN SERV_MED_BITACORA_ATRIBUTO t
                         ON t.EVENTO_ID = e.ID AND t.NOMBRE = ?
                  LEFT JOIN SERV_MED_BITACORA_ATRIBUTO k
                         ON k.EVENTO_ID = e.ID AND k.NOMBRE IN (?, ?)""", FAMILIA_ACCIDENTE);
        c.and("t.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);

        Object[] todos = new Object[c.params().length + 3];
        todos[0] = ATR_TIPO_RIESGO;
        todos[1] = ATR_COSTO_ACCIDENTE[0];
        todos[2] = ATR_COSTO_ACCIDENTE[1];
        System.arraycopy(c.params(), 0, todos, 3, c.params().length);
        return agrupadoAccidente(c.sql() + " GROUP BY t.VALOR ORDER BY 2 DESC", todos);
    }

    /**
     * Accidentes por causa de riesgo de trabajo.
     *
     * <p>La causa sale del <b>nombre</b> del atributo, no de su valor, por como se cargo el grupo
     * mixto. Se excluye la descripcion en texto libre, que comparte el prefijo pero no es una causa.
     */
    public List<ConteoSimpleDto> accidentesPorCausaRt(LocalDate desde, LocalDate hasta,
                                                      String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT REPLACE(SUBSTR(a.NOMBRE, INSTR(a.NOMBRE, '.') + 1), '_', ' ') clave, "
                + "COUNT(DISTINCT e.ID) cantidad FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO a ON a.EVENTO_ID = e.ID", FAMILIA_ACCIDENTE);
        c.and("a.NOMBRE LIKE ?", PREFIJO_CAUSA_RT + "%");
        c.and("a.NOMBRE <> ?", ATR_DESCRIPCION_RT);
        c.filtros(desde, hasta, predio, cuenta);
        return conteo(c.sql() + " GROUP BY REPLACE(SUBSTR(a.NOMBRE, INSTR(a.NOMBRE, '.') + 1), '_', ' ') "
                + "ORDER BY COUNT(DISTINCT e.ID) DESC", c.params(), 0);
    }

    /** Serie mensual de accidentes. */
    public List<PuntoMensualDto> accidentesTendencia(LocalDate desde, LocalDate hasta,
                                                     String predio, String cuenta) {
        Consulta c = new Consulta(
                "SELECT TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0') mes, COUNT(*) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e", FAMILIA_ACCIDENTE);
        c.filtros(desde, hasta, predio, cuenta);
        c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY e.ANIO, e.MES ORDER BY e.ANIO, e.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    /** Cruce de accidentes por (mes o predio) y tipo de riesgo. */
    public List<Cruce> accidentesPorTipo(String dimension, LocalDate desde, LocalDate hasta,
                                         String predio, String cuenta) {
        boolean porMes = "MES".equalsIgnoreCase(dimension);
        String dim = porMes
                ? "TO_CHAR(e.ANIO) || '-' || LPAD(TO_CHAR(e.MES), 2, '0')"
                : "NVL(e.PREDIO, '" + SIN_DATO + "')";
        Consulta c = new Consulta(
                "SELECT " + dim + " clave, t.VALOR valor, COUNT(DISTINCT e.ID) cantidad "
                + "FROM SERV_MED_BITACORA_EVENTO e "
                + "JOIN SERV_MED_BITACORA_ATRIBUTO t ON t.EVENTO_ID = e.ID", FAMILIA_ACCIDENTE);
        c.and("t.NOMBRE = ?", ATR_TIPO_RIESGO);
        c.and("t.VALOR IS NOT NULL");
        c.filtros(desde, hasta, predio, cuenta);
        if (porMes) {
            c.and("e.ANIO IS NOT NULL AND e.MES IS NOT NULL");
        }
        return cruce(c.sql() + " GROUP BY " + dim + ", t.VALOR ORDER BY 1", c.params());
    }

    /** Los dos ? del costo van antes de los del WHERE: en JDBC el orden es posicional. */
    private Object[] conCosto(Consulta c) {
        Object[] todos = new Object[c.params().length + 2];
        todos[0] = ATR_COSTO_ACCIDENTE[0];
        todos[1] = ATR_COSTO_ACCIDENTE[1];
        System.arraycopy(c.params(), 0, todos, 2, c.params().length);
        return todos;
    }

    private List<AgrupadoAccidente> agrupadoAccidente(String sql, Object[] params) {
        return jdbc.query(sql, (rs, i) -> new AgrupadoAccidente(
                rs.getString("clave"), rs.getLong("accidentes"), rs.getDouble("costo")), params);
    }

    // ---------------------------------------------------------------------------------------
    //  Consumibles
    //
    //  La unica familia que vive en SERV_MED_BITACORA_METRICA y no en _EVENTO: su renglon no es
    //  una persona sino un medicamento, y lo que se mide es cuantas piezas se consumieron.
    // ---------------------------------------------------------------------------------------

    private static final String FAMILIA_CONSUMIBLE = "CONSUMIBLE";

    /**
     * La medida que cuenta como "consumo del mes".
     *
     * <p>El Excel trae tres: el desglose por semana, el {@code TOTAL x LOTE} y el
     * {@code CONSUMO ACUMULADO}. Para sumar hay que usar <b>una sola</b>, o el total sale al triple:
     * las semanas ya estan dentro del total, y el acumulado lo repite otra vez.
     *
     * <p>Se usa el total por lote porque es el que el propio archivo calcula y el que cuadra contra
     * la suma de sus semanas. Las otras dos se guardaron y se pueden consultar, pero no se suman
     * aqui.
     */
    private static final String MEDIDA_CONSUMO = "CONSUMO SEMANAL TOTAL X LOTE";

    /** El origen de la hoja de inventario de equipo, dentro de la misma familia. */
    private static final String ORIGEN_EQUIPO = "EQUIPO";
    private static final String ORIGEN_DETALLE = "DETALLE";

    /** Si hay consumo historico cargado. */
    public boolean hayConsumo() {
        try {
            Integer n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM SERV_MED_BITACORA_METRICA "
                    + "WHERE FAMILIA = ? AND ROWNUM = 1", Integer.class, FAMILIA_CONSUMIBLE);
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Piezas totales y medicamentos distintos con consumo. */
    public long[] totalesConsumo(LocalDate desde, LocalDate hasta, String predio) {
        ConsultaMetrica c = new ConsultaMetrica(
                "SELECT NVL(SUM(m.VALOR), 0) piezas, COUNT(DISTINCT m.CONCEPTO) medicamentos "
                + "FROM SERV_MED_BITACORA_METRICA m", ORIGEN_DETALLE, MEDIDA_CONSUMO);
        c.filtros(desde, hasta, predio);
        return jdbc.queryForObject(c.sql(),
                (rs, i) -> new long[]{rs.getLong("piezas"), rs.getLong("medicamentos")},
                c.params());
    }

    /** Consumo por medicamento, de mayor a menor. */
    public List<ConteoSimpleDto> consumoPorMedicamento(LocalDate desde, LocalDate hasta,
                                                       String predio, int limite) {
        ConsultaMetrica c = new ConsultaMetrica(
                "SELECT m.CONCEPTO clave, NVL(SUM(m.VALOR), 0) cantidad "
                + "FROM SERV_MED_BITACORA_METRICA m", ORIGEN_DETALLE, MEDIDA_CONSUMO);
        c.filtros(desde, hasta, predio);
        return conteo(c.sql() + " GROUP BY m.CONCEPTO HAVING SUM(m.VALOR) > 0 "
                + "ORDER BY SUM(m.VALOR) DESC", c.params(), limite);
    }

    /** Consumo por predio. */
    public List<ConteoSimpleDto> consumoPorPredio(LocalDate desde, LocalDate hasta, String predio) {
        ConsultaMetrica c = new ConsultaMetrica(
                "SELECT NVL(m.PREDIO, '" + SIN_DATO + "') clave, NVL(SUM(m.VALOR), 0) cantidad "
                + "FROM SERV_MED_BITACORA_METRICA m", ORIGEN_DETALLE, MEDIDA_CONSUMO);
        c.filtros(desde, hasta, predio);
        return conteo(c.sql() + " GROUP BY NVL(m.PREDIO, '" + SIN_DATO + "') "
                + "ORDER BY SUM(m.VALOR) DESC", c.params(), 0);
    }

    /** Piezas por mes. */
    public List<PuntoMensualDto> consumoTendencia(LocalDate desde, LocalDate hasta, String predio) {
        ConsultaMetrica c = new ConsultaMetrica(
                "SELECT TO_CHAR(m.ANIO) || '-' || LPAD(TO_CHAR(m.MES), 2, '0') mes, "
                + "NVL(SUM(m.VALOR), 0) cantidad FROM SERV_MED_BITACORA_METRICA m",
                ORIGEN_DETALLE, MEDIDA_CONSUMO);
        c.filtros(desde, hasta, predio);
        c.and("m.MES IS NOT NULL");
        return jdbc.query(c.sql() + " GROUP BY m.ANIO, m.MES ORDER BY m.ANIO, m.MES",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")),
                c.params());
    }

    /**
     * Inventario de equipo por predio, de la hoja {@code MATERIAL FIJO}.
     *
     * <p>No es consumo: un baumanometro no se gasta. Va aparte en la pantalla por eso, y no suma
     * con las piezas de medicamento.
     *
     * <p>Se filtra por {@code MES IS NULL}, que es como se cargo: el inventario es el estado del
     * anio, no un hecho de un mes.
     */
    public List<ConteoSimpleDto> equipoPorPredio(String predio) {
        ConsultaMetrica c = new ConsultaMetrica(
                "SELECT NVL(m.PREDIO, '" + SIN_DATO + "') clave, NVL(SUM(m.VALOR), 0) cantidad "
                + "FROM SERV_MED_BITACORA_METRICA m", ORIGEN_EQUIPO, "CANTIDAD TOTAL");
        if (predio != null && !predio.isBlank()) {
            c.and("UPPER(m.PREDIO) = UPPER(?)", predio);
        }
        return conteo(c.sql() + " GROUP BY NVL(m.PREDIO, '" + SIN_DATO + "') "
                + "ORDER BY SUM(m.VALOR) DESC", c.params(), 0);
    }

    /**
     * El WHERE de las consultas de metrica.
     *
     * <p>Aparte de {@link Consulta} porque {@code _METRICA} no se une con {@code _EVENTO}: tiene
     * su propio {@code FAMILIA}, {@code PREDIO}, {@code ANIO} y {@code MES}, y no tiene
     * {@code FECHA}. Por eso el corte por periodo se hace con anio y mes y no con un rango de
     * fechas: en esta familia no hay un dia al que acotar.
     */
    private static final class ConsultaMetrica {
        private final StringBuilder sql;
        private final List<Object> params = new ArrayList<>();
        private boolean conWhere;

        ConsultaMetrica(String base, String origen, String subconcepto) {
            this.sql = new StringBuilder(base);
            and("m.FAMILIA = ?", FAMILIA_CONSUMIBLE);
            and("m.ORIGEN = ?", origen);
            and("m.SUBCONCEPTO = ?", subconcepto);
        }

        void filtros(LocalDate desde, LocalDate hasta, String predio) {
            if (desde != null) {
                and("(m.ANIO > ? OR (m.ANIO = ? AND NVL(m.MES, 12) >= ?))",
                        desde.getYear(), desde.getYear(), desde.getMonthValue());
            }
            if (hasta != null) {
                and("(m.ANIO < ? OR (m.ANIO = ? AND NVL(m.MES, 1) <= ?))",
                        hasta.getYear(), hasta.getYear(), hasta.getMonthValue());
            }
            if (predio != null && !predio.isBlank()) {
                and("UPPER(m.PREDIO) = UPPER(?)", predio);
            }
        }

        void and(String condicion, Object... valores) {
            sql.append(conWhere ? " AND " : " WHERE ").append(condicion);
            conWhere = true;
            params.addAll(List.of(valores));
        }

        String sql() {
            return sql.toString();
        }

        Object[] params() {
            return params.toArray();
        }
    }

    private List<Cruce> cruce(String sql, Object[] params) {
        return jdbc.query(sql,
                (rs, i) -> new Cruce(rs.getString("clave"), rs.getString("valor"),
                        rs.getLong("cantidad")),
                params);
    }

    private List<ConteoSimpleDto> conteo(String sql, Object[] params, int limite) {
        List<ConteoSimpleDto> todos = jdbc.query(sql,
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")),
                params);
        return (limite <= 0 || todos.size() <= limite) ? todos : todos.subList(0, limite);
    }

    /**
     * Lista blanca de columnas. Los nombres de columna no se pueden parametrizar en JDBC, asi
     * que se validan contra un conjunto fijo en vez de concatenar lo que llegue.
     */
    private String columnaValida(String columna) {
        return switch (columna == null ? "" : columna.toUpperCase()) {
            case "PREDIO", "CUENTA", "PUESTO", "AREA", "AGENCIA", "GENERO",
                 "RANGO_EDAD", "ESTATUS" -> columna.toUpperCase();
            default -> throw new IllegalArgumentException(
                    "Columna no permitida para agrupar: " + columna);
        };
    }

    /** Arma el WHERE con sus parametros, para no repetirlo en cada consulta. */
    private static final class Consulta {
        private final StringBuilder sql;
        private final List<Object> params = new ArrayList<>();
        private boolean conWhere;

        /** Por omision, la familia de las tres pantallas de Morbilidad. */
        Consulta(String base) {
            this(base, FAMILIA_ATENCION);
        }

        /**
         * Acotada a una o varias familias.
         *
         * <p><b>Siempre acota por familia, nunca se consulta la tabla entera.</b> Todas las
         * familias comparten {@code SERV_MED_BITACORA_EVENTO} y sus atributos comparten nombre
         * cuando el concepto es el mismo, asi que sin el filtro los numeros mezclan cosas
         * distintas. El 30-sep-2026 el script de verificacion lo hizo por descuido y las causas
         * de consulta salieron contaminadas con el ramo de las incapacidades, dejando {@code EG}
         * como el segundo motivo de consulta del anio.
         */
        Consulta(String base, String... familias) {
            this.sql = new StringBuilder(base);
            if (familias.length == 1) {
                and("e.FAMILIA = ?", (Object) familias[0]);
            } else {
                String huecos = String.join(",", java.util.Collections.nCopies(familias.length, "?"));
                and("e.FAMILIA IN (" + huecos + ")", (Object[]) familias);
            }
        }

        void filtros(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
            if (desde != null) {
                and("e.FECHA >= ?", java.sql.Date.valueOf(desde));
            }
            if (hasta != null) {
                and("e.FECHA <= ?", java.sql.Date.valueOf(hasta));
            }
            if (predio != null && !predio.isBlank()) {
                and("UPPER(e.PREDIO) = UPPER(?)", predio);
            }
            if (cuenta != null && !cuenta.isBlank()) {
                and("UPPER(e.CUENTA) = UPPER(?)", cuenta);
            }
        }

        void and(String condicion, Object... valores) {
            sql.append(conWhere ? " AND " : " WHERE ").append(condicion);
            conWhere = true;
            params.addAll(List.of(valores));
        }

        String sql() {
            return sql.toString();
        }

        Object[] params() {
            return params.toArray();
        }
    }
}
