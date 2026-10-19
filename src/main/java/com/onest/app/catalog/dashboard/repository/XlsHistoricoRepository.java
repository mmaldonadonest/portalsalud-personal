package com.onest.app.catalog.dashboard.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.onest.app.catalog.dashboard.dto.ConteoSimpleDto;
import com.onest.app.catalog.dashboard.dto.PuntoMensualDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Historico de los Excel leido de las tablas NUEVAS por NSS (SERV_MED_XLS_*), para los 5 layouts
 * con visto bueno del 10-oct-2026: atenciones, examen de nuevo ingreso, examen periodico,
 * incapacidades y accidentes.
 *
 * <p><b>Interruptor.</b> Solo existe con {@code portal.historico.fuente=XLS}. Sin esa propiedad
 * (el default) el portal sigue con {@link BitacoraHistoricoRepository} tal cual: esto se prepara
 * en el paso 5 del plan y se enciende en la activacion (paso 9), en la misma ventana en que se
 * carga la foto inicial.
 *
 * <p><b>Extiende al repositorio viejo a proposito.</b> Los servicios del dashboard lo inyectan
 * por ese tipo; con {@code @Primary} reciben este. Lo que no se sobreescribe aqui -antidoping,
 * maternidad, consumibles- se sigue leyendo de la carga vieja, que es lo acordado: esas familias
 * no tienen layout nuevo todavia.
 *
 * <p>Diferencias con la carga vieja que se notan en pantalla:
 * <ul>
 *   <li>Personas se cuentan por NSS, no por nombre normalizado.</li>
 *   <li>Solo cuentan los renglones VALIDO: los DUPLICADO ya estan en el portal y los trae ORDS.</li>
 *   <li>Mes = mes de FECHA; ya no hay renglones "sin mes" (la fecha es obligatoria en el layout).</li>
 * </ul>
 */
@Repository
@Primary
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "XLS")
public class XlsHistoricoRepository extends BitacoraHistoricoRepository {

    private static final String SIN_DATO = "Sin dato";
    private static final String MES = "TO_CHAR(e.FECHA, 'YYYY-MM')";

    private static final String XLS_ATENCION =
            "(SELECT * FROM SERV_MED_XLS_ATENCION WHERE ESTADO_REGISTRO = 'VALIDO') e";
    private static final String XLS_INCAPACIDAD =
            "(SELECT * FROM SERV_MED_XLS_INCAPACIDAD WHERE ESTADO_REGISTRO = 'VALIDO') e";
    private static final String XLS_ACCIDENTE =
            "(SELECT * FROM SERV_MED_XLS_ACCIDENTE WHERE ESTADO_REGISTRO = 'VALIDO') e";

    /**
     * Examenes = nuevo ingreso + periodico + pos incapacidad (tablas nuevas; sin carga vieja
     * desde el 14-oct-2026). El dictamen nuevo (APTO_CONDICIONADO...) se traduce a la etiqueta con
     * codigo que reconoce DictamenExamen.deEtiquetaExcel, la misma de la carga vieja: "(15)",
     * "(30)", "(45)", "(60)". Sin eso "NO_APTO" contenia "APTO" y caia en el bucket equivocado.
     */
    static final String DICTAMEN_NUEVO = "DECODE(DICTAMEN, 'APTO', 'APTO (15)', 'NO_APTO', 'NO APTO (30)', "
            + "'APTO_CONDICIONADO', 'APTO CONDICIONADO (45)', 'APTO_RESTRINGIDO', 'APTO RESTRINGIDO (60)', DICTAMEN)";
    private static final String XLS_EXAMEN = "(SELECT FECHA, PREDIO, CUENTA, " + DICTAMEN_NUEVO + " DICTAMEN "
            + "FROM SERV_MED_XLS_EXAMEN_ING WHERE ESTADO_REGISTRO = 'VALIDO' "
            + "UNION ALL SELECT FECHA, PREDIO, CUENTA, " + DICTAMEN_NUEVO + " DICTAMEN "
            + "FROM SERV_MED_XLS_EXAMEN_PER WHERE ESTADO_REGISTRO = 'VALIDO' "
            + "UNION ALL SELECT FECHA, PREDIO, CUENTA, " + DICTAMEN_NUEVO + " DICTAMEN "
            + "FROM SERV_MED_XLS_EXAMEN_POS WHERE ESTADO_REGISTRO = 'VALIDO') e";

    private final JdbcTemplate jdbc;

    // De donde se lee cada familia. Todas las consultas de abajo trabajan sobre el alias "e" con
    // las mismas columnas, asi que otra fuente (las vistas unificadas) solo cambia estas cuatro.
    private final String atencion;
    private final String examen;
    private final String incapacidad;
    private final String accidente;

    public XlsHistoricoRepository(JdbcTemplate jdbc) {
        this(jdbc, XLS_ATENCION, XLS_EXAMEN, XLS_INCAPACIDAD, XLS_ACCIDENTE);
    }

    protected XlsHistoricoRepository(JdbcTemplate jdbc, String atencion, String examen, String incapacidad,
                                     String accidente) {
        super(jdbc);
        this.jdbc = jdbc;
        this.atencion = atencion;
        this.examen = examen;
        this.incapacidad = incapacidad;
        this.accidente = accidente;
    }

    // ---------------------------------------------------------------------------------------
    //  Atenciones (Consultas, Causas, Musculoesqueletico)
    // ---------------------------------------------------------------------------------------

    @Override
    public boolean hayDatos() {
        return hay(atencion);
    }

    @Override
    public long totalAtenciones(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return numero(new Filtro("SELECT COUNT(*) FROM " + atencion).filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public long totalPersonas(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return numero(new Filtro("SELECT COUNT(DISTINCT e.NSS) FROM " + atencion).filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<ConteoSimpleDto> porColumna(String columna, LocalDate desde, LocalDate hasta,
                                            String predio, String cuenta, int limite) {
        String expr = switch (columna == null ? "" : columna.toUpperCase()) {
            case "PREDIO", "CUENTA", "PUESTO", "AREA", "GENERO" -> "NVL(e." + columna.toUpperCase() + ", '" + SIN_DATO + "')";
            // Mismos cortes que DashboardConsultaService.rangoEdad, para que las barras se sumen
            case "RANGO_EDAD" -> "CASE WHEN e.EDAD IS NULL OR e.EDAD <= 0 THEN '" + SIN_DATO + "' "
                    + "WHEN e.EDAD <= 25 THEN '18-25' WHEN e.EDAD <= 35 THEN '26-35' "
                    + "WHEN e.EDAD <= 45 THEN '36-45' WHEN e.EDAD <= 55 THEN '46-55' ELSE '55+' END";
            default -> throw new IllegalArgumentException("Columna no permitida para agrupar: " + columna);
        };
        Filtro f = new Filtro("SELECT " + expr + " clave, COUNT(*) cantidad FROM " + atencion)
                .filtros(desde, hasta, predio, cuenta);
        return conteo(f.sql() + " GROUP BY " + expr + " ORDER BY COUNT(*) DESC", f.params(), limite);
    }

    /**
     * Los atributos de la carga vieja ahora son columnas. Solo se cuentan los renglones que traen
     * el dato: en la carga vieja un atributo ausente no generaba renglon, y asi la grafica de
     * causas no gana una barra "Sin dato" del tamano de todas las atenciones sin causa.
     */
    @Override
    public List<ConteoSimpleDto> porAtributo(String nombreAtributo, LocalDate desde, LocalDate hasta,
                                             String predio, String cuenta, int limite) {
        String col = switch (nombreAtributo == null ? "" : nombreAtributo.toUpperCase()) {
            case "CAUSA", "RAMO", "LESION_ME", "DIAGNOSTICO" -> nombreAtributo.toUpperCase();
            default -> throw new IllegalArgumentException("Atributo no disponible en atenciones: " + nombreAtributo);
        };
        Filtro f = new Filtro("SELECT e." + col + " clave, COUNT(*) cantidad FROM " + atencion)
                .and("e." + col + " IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return conteo(f.sql() + " GROUP BY e." + col + " ORDER BY COUNT(*) DESC", f.params(), limite);
    }

    @Override
    public List<PuntoMensualDto> tendenciaMensual(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return mensual(new Filtro("SELECT " + MES + " mes, COUNT(*) cantidad FROM " + atencion)
                .filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<Object[]> tendenciaPorPredio(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT NVL(e.PREDIO, '" + SIN_DATO + "') predio, " + MES + " mes, COUNT(*) cantidad FROM "
                + atencion).filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "'), " + MES + " ORDER BY 1, 2",
                (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getLong(3)}, f.params());
    }

    @Override
    public List<ConteoSimpleDto> lesionesMusculoesqueleticas(LocalDate desde, LocalDate hasta, String predio,
                                                             String cuenta) {
        return porAtributo("LESION_ME", desde, hasta, predio, cuenta, 0);
    }

    @Override
    public long personasConLesion(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return numero(new Filtro("SELECT COUNT(DISTINCT e.NSS) FROM " + atencion)
                .and("e.LESION_ME IS NOT NULL").filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<ConteoSimpleDto> lesionesPorPredio(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, COUNT(*) cantidad FROM " + atencion)
                .and("e.LESION_ME IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return conteo(f.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "') ORDER BY COUNT(*) DESC", f.params(), 0);
    }

    @Override
    public List<PuntoMensualDto> tendenciaLesiones(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return mensual(new Filtro("SELECT " + MES + " mes, COUNT(*) cantidad FROM " + atencion)
                .and("e.LESION_ME IS NOT NULL").filtros(desde, hasta, predio, cuenta));
    }

    // ---------------------------------------------------------------------------------------
    //  Examenes (ingreso + periodico nuevos; pos incapacidad de la carga vieja)
    // ---------------------------------------------------------------------------------------

    @Override
    public boolean hayExamenes() {
        return hay(examen);
    }

    @Override
    public long totalExamenes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return numero(new Filtro("SELECT COUNT(*) FROM " + examen).filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<ConteoSimpleDto> examenesPorDictamen(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT e.DICTAMEN clave, COUNT(*) cantidad FROM " + examen)
                .and("e.DICTAMEN IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return conteo(f.sql() + " GROUP BY e.DICTAMEN ORDER BY COUNT(*) DESC", f.params(), 0);
    }

    @Override
    public List<Cruce> examenesPorPredioYDictamen(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, e.DICTAMEN valor, COUNT(*) cantidad FROM "
                + examen).and("e.DICTAMEN IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return cruce(f.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "'), e.DICTAMEN ORDER BY COUNT(*) DESC", f.params());
    }

    @Override
    public List<Cruce> examenesPorMesYDictamen(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT " + MES + " clave, e.DICTAMEN valor, COUNT(*) cantidad FROM " + examen)
                .and("e.DICTAMEN IS NOT NULL").and("e.FECHA IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return cruce(f.sql() + " GROUP BY " + MES + ", e.DICTAMEN ORDER BY 1", f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Incapacidades
    // ---------------------------------------------------------------------------------------

    /** Misma etiqueta que la carga vieja ("Interna"), para que se sume con lo del portal. */
    private static final String RUBRO = "CASE WHEN e.RUBRO = 'INTERNA' THEN 'Interna' ELSE 'IMSS' END";

    @Override
    public boolean hayIncapacidades() {
        return hay(incapacidad);
    }

    @Override
    public TotalesIncapacidad totalesIncapacidad(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT COUNT(*) episodios, COUNT(DISTINCT e.NSS) personas FROM " + incapacidad)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new TotalesIncapacidad(rs.getLong(1), rs.getLong(2)), f.params());
    }

    @Override
    public double diasIncapacidades(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return decimal(new Filtro("SELECT NVL(SUM(e.DIAS), 0) FROM " + incapacidad).filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public double costoIncapacidades(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return decimal(new Filtro("SELECT NVL(SUM(e.COSTO_TOTAL), 0) FROM " + incapacidad)
                .filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<AgrupadoIncapacidad> incapacidadPorDimension(String dimension, LocalDate desde, LocalDate hasta,
                                                             String predio, String cuenta) {
        String dim = switch (dimension == null ? "" : dimension.toUpperCase()) {
            case "PREDIO", "CUENTA", "AREA" -> "NVL(e." + dimension.toUpperCase() + ", '" + SIN_DATO + "')";
            case "RUBRO" -> RUBRO;
            default -> throw new IllegalArgumentException("Dimension no permitida para incapacidades: " + dimension);
        };
        return agrupadoIncapacidad(dim, null, desde, hasta, predio, cuenta);
    }

    @Override
    public List<AgrupadoIncapacidad> incapacidadPorRamo(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return agrupadoIncapacidad("e.RAMO", "e.RAMO IS NOT NULL", desde, hasta, predio, cuenta);
    }

    private List<AgrupadoIncapacidad> agrupadoIncapacidad(String dim, String condicion, LocalDate desde,
                                                          LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT " + dim + " clave, COUNT(*) episodios, COUNT(DISTINCT e.NSS) personas, "
                + "NVL(SUM(e.DIAS), 0) dias, NVL(SUM(e.COSTO_TOTAL), 0) costo FROM " + incapacidad);
        if (condicion != null) {
            f.and(condicion);
        }
        f.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + dim + " ORDER BY 2 DESC", (rs, i) -> new AgrupadoIncapacidad(
                rs.getString("clave"), rs.getLong("episodios"), rs.getLong("personas"),
                rs.getLong("dias"), rs.getDouble("costo")), f.params());
    }

    /** {rubro, mes, NSS} distintos: con NSS ya se pueden juntar las personas del Excel y del portal. */
    @Override
    public List<String[]> incapacidadNssPorRubroYMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT DISTINCT " + RUBRO + " rubro, " + MES + " mes, e.NSS FROM " + incapacidad)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql(), (rs, i) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}, f.params());
    }

    @Override
    public List<PuntoMensualDto> incapacidadTendencia(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return mensual(new Filtro("SELECT " + MES + " mes, COUNT(*) cantidad FROM " + incapacidad)
                .filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<PuntoMensualDto> incapacidadDiasPorMes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return mensual(new Filtro("SELECT " + MES + " mes, NVL(SUM(e.DIAS), 0) cantidad FROM " + incapacidad)
                .filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<Cruce> incapacidadDiasPorMesYRamo(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT " + MES + " clave, e.RAMO valor, NVL(SUM(e.DIAS), 0) cantidad FROM " + incapacidad)
                .and("e.RAMO IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return cruce(f.sql() + " GROUP BY " + MES + ", e.RAMO ORDER BY 1", f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Accidentes
    // ---------------------------------------------------------------------------------------

    @Override
    public boolean hayAccidentes() {
        return hay(accidente);
    }

    @Override
    public AgrupadoAccidente totalesAccidentes(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT 'TOTAL' clave, COUNT(*) accidentes, NVL(SUM(e.COSTO), 0) costo FROM " + accidente)
                .filtros(desde, hasta, predio, cuenta);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new AgrupadoAccidente(rs.getString(1), rs.getLong(2),
                rs.getDouble(3)), f.params());
    }

    /** ESTATUS = el estatus de calificacion (calificado, improcedente...), igual que el lado del portal. */
    @Override
    public List<AgrupadoAccidente> accidentesPorColumna(String columna, LocalDate desde, LocalDate hasta,
                                                        String predio, String cuenta) {
        String dim = switch (columna == null ? "" : columna.toUpperCase()) {
            case "PREDIO" -> "NVL(e.PREDIO, '" + SIN_DATO + "')";
            case "ESTATUS" -> "NVL(e.STATUS_CALIFICACION, '" + SIN_DATO + "')";
            default -> throw new IllegalArgumentException("Columna no permitida para accidentes: " + columna);
        };
        return agrupadoAccidente(dim, null, desde, hasta, predio, cuenta);
    }

    @Override
    public List<AgrupadoAccidente> accidentesPorTipoRiesgo(LocalDate desde, LocalDate hasta, String predio,
                                                           String cuenta) {
        return agrupadoAccidente("e.TIPO_RIESGO", "e.TIPO_RIESGO IS NOT NULL", desde, hasta, predio, cuenta);
    }

    private List<AgrupadoAccidente> agrupadoAccidente(String dim, String condicion, LocalDate desde, LocalDate hasta,
                                                      String predio, String cuenta) {
        Filtro f = new Filtro("SELECT " + dim + " clave, COUNT(*) accidentes, NVL(SUM(e.COSTO), 0) costo FROM "
                + accidente);
        if (condicion != null) {
            f.and(condicion);
        }
        f.filtros(desde, hasta, predio, cuenta);
        return jdbc.query(f.sql() + " GROUP BY " + dim + " ORDER BY 2 DESC", (rs, i) -> new AgrupadoAccidente(
                rs.getString("clave"), rs.getLong("accidentes"), rs.getDouble("costo")), f.params());
    }

    @Override
    public List<ConteoSimpleDto> accidentesPorCausaRt(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        Filtro f = new Filtro("SELECT e.CAUSA_RT clave, COUNT(*) cantidad FROM " + accidente)
                .and("e.CAUSA_RT IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return conteo(f.sql() + " GROUP BY e.CAUSA_RT ORDER BY COUNT(*) DESC", f.params(), 0);
    }

    @Override
    public List<PuntoMensualDto> accidentesTendencia(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        return mensual(new Filtro("SELECT " + MES + " mes, COUNT(*) cantidad FROM " + accidente)
                .filtros(desde, hasta, predio, cuenta));
    }

    @Override
    public List<Cruce> accidentesPorTipo(String dimension, LocalDate desde, LocalDate hasta, String predio,
                                         String cuenta) {
        String dim = "MES".equalsIgnoreCase(dimension) ? MES : "NVL(e.PREDIO, '" + SIN_DATO + "')";
        Filtro f = new Filtro("SELECT " + dim + " clave, e.TIPO_RIESGO valor, COUNT(*) cantidad FROM " + accidente)
                .and("e.TIPO_RIESGO IS NOT NULL").filtros(desde, hasta, predio, cuenta);
        return cruce(f.sql() + " GROUP BY " + dim + ", e.TIPO_RIESGO ORDER BY 1", f.params());
    }

    // ---------------------------------------------------------------------------------------
    //  Consumibles (layout 9, 12-oct-2026): SERV_MED_XLS_MEDICAMENTO y SERV_MED_XLS_MATERIAL_FIJO.
    //  Sin portal con que unirlos, asi que no hay vista: se leen las tablas. Sin respaldo a la carga
    //  vieja (decision 14-oct-2026): si estan vacias, la pantalla queda vacia.
    // ---------------------------------------------------------------------------------------

    private static final String MEDICAMENTO = "(SELECT * FROM SERV_MED_XLS_MEDICAMENTO WHERE ESTADO_REGISTRO = 'VALIDO') e";
    /** Lo capturado manda: TOTAL x LOTE; si no lo trae, la suma de las semanas. */
    private static final String CONSUMO = "COALESCE(e.TOTAL_LOTE, NVL(e.CONSUMO_S1, 0) + NVL(e.CONSUMO_S2, 0) "
            + "+ NVL(e.CONSUMO_S3, 0) + NVL(e.CONSUMO_S4, 0) + NVL(e.CONSUMO_S5, 0))";

    @Override
    public boolean hayConsumo() {
        return hay(MEDICAMENTO) || hay("(SELECT * FROM SERV_MED_XLS_MATERIAL_FIJO) e");
    }

    @Override
    public long[] totalesConsumo(LocalDate desde, LocalDate hasta, String predio) {
        Filtro f = new Filtro("SELECT NVL(SUM(" + CONSUMO + "), 0), COUNT(DISTINCT CASE WHEN " + CONSUMO
                + " > 0 THEN e.DESCRIPCION END) FROM " + MEDICAMENTO).filtros(desde, hasta, predio, null);
        return jdbc.queryForObject(f.sql(), (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)}, f.params());
    }

    @Override
    public List<ConteoSimpleDto> consumoPorMedicamento(LocalDate desde, LocalDate hasta, String predio, int limite) {
        Filtro f = new Filtro("SELECT e.DESCRIPCION clave, NVL(SUM(" + CONSUMO + "), 0) cantidad FROM " + MEDICAMENTO)
                .filtros(desde, hasta, predio, null);
        return conteo(f.sql() + " GROUP BY e.DESCRIPCION HAVING SUM(" + CONSUMO + ") > 0 ORDER BY 2 DESC",
                f.params(), limite);
    }

    @Override
    public List<ConteoSimpleDto> consumoPorPredio(LocalDate desde, LocalDate hasta, String predio) {
        Filtro f = new Filtro("SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, NVL(SUM(" + CONSUMO + "), 0) cantidad FROM "
                + MEDICAMENTO).filtros(desde, hasta, predio, null);
        return conteo(f.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "') ORDER BY 2 DESC", f.params(), 0);
    }

    @Override
    public List<PuntoMensualDto> consumoTendencia(LocalDate desde, LocalDate hasta, String predio) {
        return mensual(new Filtro("SELECT " + MES + " mes, NVL(SUM(" + CONSUMO + "), 0) cantidad FROM " + MEDICAMENTO)
                .filtros(desde, hasta, predio, null));
    }

    /**
     * Equipo vigente por predio: lo recibido que no tiene fecha de baja. NO se acota por fecha: es el
     * estado del inventario, no un hecho del periodo.
     */
    @Override
    public List<ConteoSimpleDto> equipoPorPredio(String predio) {
        String fuente = "(SELECT * FROM SERV_MED_XLS_MATERIAL_FIJO WHERE ESTADO_REGISTRO = 'VALIDO') e";
        Filtro f = new Filtro("SELECT NVL(e.PREDIO, '" + SIN_DATO + "') clave, NVL(SUM(NVL(e.CANTIDAD_SURTIDA, 0)), 0) "
                + "cantidad FROM " + fuente).and("e.FECHA_BAJA IS NULL").filtros(null, null, predio, null);
        return conteo(f.sql() + " GROUP BY NVL(e.PREDIO, '" + SIN_DATO + "') ORDER BY 2 DESC", f.params(), 0);
    }

    // ---------------------------------------------------------------------------------------
    //  Utilerias
    // ---------------------------------------------------------------------------------------

    /**
     * Si la fuente (ya con su filtro de renglones validos y alias) trae al menos un renglon.
     * Sin la tabla o la vista (otro ambiente) = no hay datos.
     */
    private boolean hay(String fuente) {
        try {
            Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + fuente + " WHERE ROWNUM = 1", Long.class);
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private long numero(Filtro f) {
        Long n = jdbc.queryForObject(f.sql(), Long.class, f.params());
        return n == null ? 0 : n;
    }

    private double decimal(Filtro f) {
        Double n = jdbc.queryForObject(f.sql(), Double.class, f.params());
        return n == null ? 0 : n;
    }

    private List<PuntoMensualDto> mensual(Filtro f) {
        return jdbc.query(f.sql() + " GROUP BY " + MES + " ORDER BY 1",
                (rs, i) -> new PuntoMensualDto(rs.getString("mes"), rs.getLong("cantidad")), f.params());
    }

    private List<ConteoSimpleDto> conteo(String sql, Object[] params, int limite) {
        List<ConteoSimpleDto> todos = jdbc.query(sql,
                (rs, i) -> new ConteoSimpleDto(rs.getString("clave"), rs.getLong("cantidad")), params);
        return (limite <= 0 || todos.size() <= limite) ? todos : todos.subList(0, limite);
    }

    private List<Cruce> cruce(String sql, Object[] params) {
        return jdbc.query(sql, (rs, i) -> new Cruce(rs.getString("clave"), rs.getString("valor"),
                rs.getLong("cantidad")), params);
    }

    /** WHERE con parametros: fechas sobre FECHA, predio y cuenta por igualdad sin mayusculas. */
    private static final class Filtro {
        private final StringBuilder sql;
        private final List<Object> params = new ArrayList<>();
        private boolean conWhere;

        Filtro(String base) {
            this.sql = new StringBuilder(base);
        }

        Filtro filtros(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
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
            return this;
        }

        Filtro and(String condicion, Object... valores) {
            sql.append(conWhere ? " AND " : " WHERE ").append(condicion);
            conWhere = true;
            params.addAll(List.of(valores));
            return this;
        }

        String sql() {
            return sql.toString();
        }

        Object[] params() {
            return params.toArray();
        }
    }
}
