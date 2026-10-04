package com.onest.excel.carga;

import java.util.List;
import java.util.Map;

import com.onest.excel.inventario.Familia;

/**
 * Como se traduce cada familia del Excel a las columnas del portal.
 *
 * <p>Es lo unico que hay que escribir por familia: <b>el lector, el detector, el transpositor y
 * el repositorio son los mismos para todas</b>. Por eso el plan estima 2 h de configuracion por
 * familia y no 13.
 *
 * <p>Las etiquetas de la izquierda son los <b>grupos del encabezado</b> tal como vienen en el
 * archivo, y se buscan con coincidencia parcial: por eso {@code "FECHA"} encuentra igual
 * {@code FECHA} que {@code FECHA ATENCION}, y {@code "NOMBRE"} encuentra
 * {@code NOMBRE (INICIAR POR APELLIDO Y NOMBRE COMPLETO)}.
 *
 * @param familia      a quien aplica
 * @param claveNombre  grupos candidatos para la columna que identifica el renglon
 * @param claveFecha   grupos candidatos para la fecha del evento
 * @param aColumna     grupo del Excel &rarr; columna de SERV_MED_BITACORA_EVENTO
 * @param aAtributo    grupo del Excel &rarr; NOMBRE en SERV_MED_BITACORA_ATRIBUTO
 * @param gruposDeCampos grupos que NO son "marca con 1": sus columnas son campos
 *                     independientes que conviven. Cada una se guarda como su propio atributo
 *                     con su tipo real. Lo destapo el ensayo del 29-sep-2026: el grupo
 *                     DATOS INCAPACIDAD trae DIAS, INICIA INCAPACIDAD, ULTIMO DIA e INICIA
 *                     LABORES, los cuatro llenos a la vez; tratarlo como one-hot se quedaba
 *                     con el primero y tiraba tres fechas reales por renglon.
 */
public record MapeoFamilia(
        Familia familia,
        List<String> claveNombre,
        List<String> claveFecha,
        Map<String, String> aColumna,
        Map<String, String> aAtributo,
        java.util.Set<String> gruposDeCampos,
        Map<String, Multimarca> gruposMultimarca,
        boolean matrizMensual) {

    /**
     * Un grupo donde <b>varias marcas son correctas</b>, y a que atributo va cada una.
     *
     * <p>El tercer tipo de grupo, despues del one-hot y los grupos de campos. Lo destapo
     * antidoping el 30-sep-2026: su bloque {@code RESULTADO} de 15 columnas lleva dos marcas en
     * los renglones positivos &mdash;{@code POSITIVO} y la sustancia&mdash; porque son dos datos
     * del mismo resultado.
     *
     * <p><b>Por que hay ruteo y no un solo atributo.</b> Guardar las dos marcas bajo el mismo
     * nombre repetiria exactamente el error que se corrigio esa misma manana con el bloque
     * {@code CAUSA} del examen pos incapacidad: una grafica "por resultado" saldria con
     * {@code NEGATIVO}, {@code POSITIVO}, {@code AMF} y {@code THC} como barras hermanas, cuando
     * las dos primeras son el veredicto y las otras la sustancia. Son preguntas distintas.
     *
     * @param porSubetiqueta   subetiqueta &rarr; atributo, para las que tienen destino propio
     * @param destinoPorDefecto a donde van las demas. <b>Es lo que hace la configuracion
     *                          duradera:</b> si el origen agrega una sustancia nueva, cae aqui
     *                          sola y aparece en la grafica, en vez de perderse por no estar en
     *                          una lista
     */
    public record Multimarca(Map<String, String> porSubetiqueta, String destinoPorDefecto) {

        /** El atributo donde se guarda una subetiqueta marcada. */
        public String destinoDe(String subetiqueta) {
            String s = com.onest.excel.normalizacion.Normalizador.texto(subetiqueta);
            return porSubetiqueta.getOrDefault(s, destinoPorDefecto);
        }
    }

    /** Constructor corto para las familias sin grupos de campos ni multimarca. */
    public MapeoFamilia(Familia f, List<String> n, List<String> fe,
                        Map<String, String> c, Map<String, String> a) {
        this(f, n, fe, c, a, java.util.Set.of(), Map.of(), false);
    }

    /** Constructor corto para las familias con grupos de campos y sin multimarca. */
    public MapeoFamilia(Familia f, List<String> n, List<String> fe,
                        Map<String, String> c, Map<String, String> a,
                        java.util.Set<String> campos) {
        this(f, n, fe, c, a, campos, Map.of(), false);
    }

    /** Constructor corto para las familias con multimarca. */
    public MapeoFamilia(Familia f, List<String> n, List<String> fe,
                        Map<String, String> c, Map<String, String> a,
                        java.util.Set<String> campos, Map<String, Multimarca> multi) {
        this(f, n, fe, c, a, campos, multi, false);
    }

    /**
     * Una familia de <b>matriz mensual</b>: el renglon es una entidad y las columnas son el tiempo.
     *
     * <p>No pasa por el detector generico ni produce eventos; la lee {@code LectorMatrizMensual} y
     * escribe metricas. Hoy solo consumibles.
     */
    public static MapeoFamilia matriz(Familia f) {
        return new MapeoFamilia(f, List.of(), List.of(), Map.of(), Map.of(),
                java.util.Set.of(), Map.of(), true);
    }

    /** Columnas fijas de EVENTO que un mapeo puede alimentar. */
    public static final String CUENTA = "CUENTA";
    public static final String AREA = "AREA";
    public static final String PUESTO = "PUESTO";
    public static final String AGENCIA = "AGENCIA";
    public static final String RANGO_EDAD = "RANGO_EDAD";
    public static final String GENERO = "GENERO";
    public static final String ESTATUS = "ESTATUS";

    private static final List<String> NOMBRE_ESTANDAR =
            List.of("NOMBRE COMPLETO", "NOMBRE");
    private static final List<String> FECHA_ESTANDAR =
            List.of("FECHA ATENCION", "FECHA DE PRIMERA INCAPACIDAD", "FECHA");

    /**
     * ATENCIONES DIARIAS. Verificado contra los 12 archivos de 2026: encabezado en la fila 3,
     * 187 columnas, 10 bloques one-hot.
     *
     * <p>Notas de negocio que se aplican aqui:
     * <ul>
     *   <li>El <b>PREDIO no sale de la cuenta</b> sino de la hoja del archivo; lo pone
     *       {@code CargaService}, no este mapeo.</li>
     *   <li>{@code PUESTO} es donde se distingue al cliente: cuando dice "cliente",
     *       {@code CUENTA} indica de que cuenta es (regla de servicio medico, 29-sep-2026).</li>
     *   <li>{@code CAUSAS MUSCULO ESQUELETICAS} viene vacio en la mayoria de los renglones y
     *       eso es correcto: solo se llena en riesgo de trabajo. Ahi vive
     *       {@code POLICONTUNDIDO}, que en 2026 ya no esta en el catalogo de causas, asi que no
     *       hay doble conteo posible.</li>
     * </ul>
     */
    public static final MapeoFamilia ATENCION = new MapeoFamilia(
            Familia.ATENCION,
            NOMBRE_ESTANDAR,
            FECHA_ESTANDAR,
            Map.of("CUENTA", CUENTA,
                   "AREA", AREA,
                   "PUESTO", PUESTO,
                   "AGENCIA", AGENCIA,
                   "RANGO DE EDAD", RANGO_EDAD,
                   "GENERO", GENERO,
                   "SEXO", GENERO),
            Map.of("CAUSA", "CAUSA",
                   "RAMO", "RAMO",
                   "CAUSAS MUSCULO ESQUELETICAS", "LESION_ME",
                   "DIAGNOSTICO", "DIAGNOSTICO",
                   "TRATAMIENTO", "TRATAMIENTO"));

    /**
     * EXAMEN DE NUEVO INGRESO. 111 columnas, 7 bloques.
     *
     * <p>{@code STATUS} es el dictamen, y servicio medico confirmo sus cuatro valores el
     * 29-sep-2026: <b>15 apto, 45 apto condicionado, 60 apto restringido (= inclusion) y 30 no
     * apto</b>. <b>Solo el 30 descalifica</b>: el 60 es apto. La lista de no aptos se filtra por
     * {@code = 30}, nunca por "distinto de 15".
     */
    public static final MapeoFamilia EXAMEN_INGRESO = new MapeoFamilia(
            Familia.EXAMEN_INGRESO, NOMBRE_ESTANDAR, FECHA_ESTANDAR,
            Map.of("CUENTA", CUENTA, "AREA", AREA, "PUESTO", PUESTO,
                   "RANGO DE EDAD", RANGO_EDAD, "GENERO", GENERO, "SEXO", GENERO),
            Map.of("STATUS", "DICTAMEN", "TIPO DE EXAMEN", "TIPO_EXAMEN"));

    public static final MapeoFamilia EXAMEN_PERIODICO = new MapeoFamilia(
            Familia.EXAMEN_PERIODICO, NOMBRE_ESTANDAR, FECHA_ESTANDAR,
            Map.of("CUENTA", CUENTA, "AREA", AREA, "PUESTO", PUESTO,
                   "RANGO DE EDAD", RANGO_EDAD, "GENERO", GENERO, "SEXO", GENERO),
            Map.of("STATUS", "DICTAMEN"));

    /**
     * EXAMEN POS INCAPACIDAD. 116 columnas, 8 bloques.
     *
     * <p><b>Su grupo {@code CAUSA} no son causas de consulta.</b> Sus cinco columnas son
     * {@code EG · MAT · RT 1 · RT 2 · INTERNA}, o sea el ramo de la incapacidad de la que vuelve
     * la persona. Guardarlo como atributo {@code CAUSA} lo metia en la misma bolsa que las 26
     * causas de las atenciones diarias, y la pantalla de Causas lo mostraba como si
     * {@code EG} fuera un motivo de consulta con 105 casos &mdash;el segundo mas frecuente&mdash;.
     *
     * <p>Hoy no llega a la grafica porque {@code BitacoraHistoricoRepository} filtra
     * {@code FAMILIA = 'ATENCION'} en toda consulta, asi que el sintoma solo se vio en la
     * verificacion. Se separa igual: el dia que alguien quiera las causas de todas las familias
     * juntas, el nombre ya no miente.
     */
    public static final MapeoFamilia EXAMEN_POS_INCAP = new MapeoFamilia(
            Familia.EXAMEN_POS_INCAP, NOMBRE_ESTANDAR, FECHA_ESTANDAR,
            Map.of("CUENTA", CUENTA, "AREA", AREA, "PUESTO", PUESTO,
                   "RANGO DE EDAD", RANGO_EDAD, "GENERO", GENERO, "SEXO", GENERO),
            Map.of("STATUS", "DICTAMEN", "CAUSA", "RAMO_INCAPACIDAD"),
            java.util.Set.of("DATOS INCAPACIDAD"));

    /**
     * ANTIDOPING. 164 columnas, 11 bloques, <b>encabezado de tres filas</b>.
     *
     * <p>Es la primera familia con encabezado de tres niveles, y por eso hizo falta cambiar el
     * detector: grupo en la fila 3, un nivel intermedio en la 4 que solo existe bajo
     * {@code RESULTADO} ({@code NEGATIVO}, {@code POSITIVO}, {@code DOPING}, {@code ALCOHOLEMIA}),
     * y las hojas en la 5, donde viven las doce sustancias.
     *
     * <p>Notas de negocio:
     * <ul>
     *   <li>{@code TIPO DE PRUEBA} distingue {@code ANTIDOPING} de {@code ALCOHOLEMIA}. Son dos
     *       pruebas distintas en el mismo reporte.</li>
     *   <li>{@code CONCLUSION} es el desenlace administrativo: {@code N/A}, {@code CAPA},
     *       {@code NO INTERESADO / BAJA}, {@code NO CONTRATADO}, {@code RECAIDA}.
     *       <b>{@code CAPA} es la liga con la familia de adicciones</b> de la fase 3: quien sale
     *       positivo y acepta tratamiento aparece alla.</li>
     *   <li>{@code STATUS} dice si la persona sigue laborando.</li>
     *   <li>Los doce archivos traen ademas una hoja {@code ANTIDOPING ACUMULADO} que es control de
     *       <b>inventario de kits</b> &mdash;existencias, lote, caducidad&mdash;, no personas. Se
     *       ignora por contener "ACUMULADO"; si algun dia se quiere, es otra familia.</li>
     * </ul>
     */
    public static final MapeoFamilia ANTIDOPING = new MapeoFamilia(
            Familia.ANTIDOPING,
            NOMBRE_ESTANDAR,
            FECHA_ESTANDAR,
            Map.of("CUENTA", CUENTA,
                   "AREA", AREA,
                   "PUESTO", PUESTO,
                   "AGENCIA", AGENCIA,
                   "EDAD", RANGO_EDAD,
                   "GENERO", GENERO,
                   "STATUS", ESTATUS),
            Map.of("TIPO DE PRUEBA", "TIPO_PRUEBA",
                   "CONCLUSION", "CONCLUSION"),
            java.util.Set.of(),
            // RESULTADO lleva dos marcas en los positivos: el veredicto y la sustancia.
            Map.of("RESULTADO", new Multimarca(
                    Map.of("NEGATIVO", "RESULTADO",
                           "POSITIVO", "RESULTADO",
                           "%", "ALCOHOLEMIA"),
                    "SUSTANCIA")));

    /**
     * MATERNIDAD. 153 columnas, 9 bloques.
     *
     * <p><b>Es la primera familia sin fecha del evento, y hay que entender por que.</b> Un caso de
     * maternidad no es un hecho de un dia: es un expediente que se sigue durante meses. Sus fechas
     * son todas de otra cosa:
     * <ul>
     *   <li>{@code FECHA DE INGRESO} es cuando la persona entro a la empresa, anios antes.</li>
     *   <li>{@code FUM} es la ultima menstruacion, y en los archivos de 2026 hay valores de
     *       <b>2024</b>. Es correcto: el embarazo cruza el anio.</li>
     *   <li>{@code FECHA PROBABLE DE PARTO} y las de incapacidad y lactancia son <b>formulas</b>
     *       sobre FUM, y proyectan al futuro.</li>
     * </ul>
     *
     * <p>Por eso {@code claveFecha} va <b>vacia a proposito</b>. Con la lista estandar, la busqueda
     * parcial por {@code "FECHA"} habria agarrado {@code FECHA DE INGRESO} &mdash;la primera
     * columna cuyo grupo contiene esa palabra&mdash; y la grafica habria salido por fecha de
     * contratacion. Peor: con FUM de 2024 en un archivo de 2026, la correccion de anio imposible
     * la habria "arreglado" a 2026, destruyendo un dato bueno.
     *
     * <p><b>El propio archivo dice como leerse:</b> su primer bloque es {@code MES REPORTADO} y su
     * primera casilla es {@code ANTES 2025}. El origen piensa en <i>cuando se reporta el caso</i>,
     * no en cuando ocurrio. De ahi sale el mes, y el anio del archivo. Las fechas clinicas se
     * guardan todas como atributos, con su tipo, para que no se pierda ninguna.
     *
     * <p>Los renglones marcados en {@code ANTES 2025} quedan sin mes: son expedientes heredados y
     * no pertenecen a ningun mes de 2026.
     */
    public static final MapeoFamilia MATERNIDAD = new MapeoFamilia(
            Familia.MATERNIDAD,
            NOMBRE_ESTANDAR,
            List.of(),                       // sin fecha del evento; ver el javadoc
            Map.of("CUENTA", CUENTA,
                   "AREA", AREA,
                   "PUESTO", PUESTO,
                   "EDAD", RANGO_EDAD,
                   "STATUS LABORAL", ESTATUS),
            Map.of("FUM", "FUM",
                   "FECHA PROBABLE DE PARTO", "FECHA_PROBABLE_PARTO",
                   "FECHA PROBABLE DE INCAP PRENATAL", "FECHA_PROBABLE_INCAP",
                   "FECHA PROBABLE REINICIO DE LABORES", "FECHA_PROBABLE_REINICIO",
                   "REINICIO DE LABORES", "REINICIO_REAL"),
            // Los cuatro grupos de campos: sus columnas conviven, no compiten por una marca.
            //   EDAD GESTACIONAL            -> SEMANA, MES
            //   INFORMACION DE INCAPACIDADES-> INICIO, TERMINO, FOLIO
            //   PERIODO DE LACTANCIA        -> INICIA, TERMINA, REANUDA ACTIVIDADES
            //   CORREO DE AVISO             -> EMBARAZO, LACTANCIA, TERMINO DE LACTANCIA,
            //                                  OBSERVACIONES
            java.util.Set.of("EDAD GESTACIONAL", "INFORMACION DE INCAPACIDADES",
                             "PERIODO DE LACTANCIA", "CORREO DE AVISO"));

    /**
     * INCAPACIDADES. <b>Dos hojas por archivo, con layouts distintos y la misma configuracion.</b>
     *
     * <p>Cada archivo trae la hoja del predio con las incapacidades del <b>IMSS</b> (173 columnas,
     * encabezado de tres filas) y una hoja {@code <predio> INT} con las <b>internas</b>, las que
     * paga la empresa (43 columnas). Servicio medico confirmo el 29-sep-2026 que
     * <b>las internas si cuentan y su costo si entra</b>, asi que van las dos.
     *
     * <p>Que las dos entren con un solo mapeo es la prueba de que el diseno sirve: la busqueda es
     * por <b>etiqueta y parcial</b>, asi que basta declarar las dos redacciones de cada concepto.
     * Donde el IMSS dice {@code AREA} la interna dice {@code DEPARTAMENTO}; donde una dice
     * {@code DIAGNOSTICO} la otra dice {@code DIAGNOSTICO Y/ O LESION}; donde una tiene
     * {@code SDI} la otra tiene {@code SALARIO}.
     *
     * <p><b>La fecha del evento.</b> Servicio medico definio que la unidad es el <b>episodio</b>,
     * de la primera incapacidad al alta. En la hoja del IMSS eso es
     * {@code FECHA DE PRIMERA INCAPACIDAD}; en la interna, {@code INICIO DE INCAPACIDAD}. El orden
     * de la lista importa: en la hoja del IMSS existen las dos &mdash;la segunda como subetiqueta
     * de {@code DETALLE DE INCAPACIDAD}&mdash; y tiene que ganar la primera.
     *
     * <p><b>Los cuatro bloques de 15 columnas no son "marca con 1".</b>
     * {@code ENFERMEDAD GENERAL}, {@code MATERNIDAD}, {@code ACCIDENTE LABORAL} y
     * {@code ACCIDENTE TRAYECTO} son <i>dias subsidiados por mes</i> para su ramo: numeros que
     * conviven, no opciones que compiten. Van como grupos de campos, y el nombre del grupo los
     * mantiene separados ({@code ENFERMEDAD_GENERAL.MAR} contra {@code MATERNIDAD.MAR}).
     *
     * <p>{@code DIAS ACUMULADOS} viene calculado en el archivo (columna 100) y se guarda tal cual,
     * dentro de {@code DETALLE DE INCAPACIDAD}. <b>Se guarda, no se hereda</b>: la instruccion fue
     * validarlo, y con el inicio y el termino tambien guardados se puede recalcular sin releer los
     * 132 archivos.
     */
    public static final MapeoFamilia INCAPACIDAD = new MapeoFamilia(
            Familia.INCAPACIDAD,
            NOMBRE_ESTANDAR,
            // El orden manda: en la hoja del IMSS existen las dos y gana la primera
            List.of("FECHA DE PRIMERA INCAPACIDAD", "INICIO DE INCAPACIDAD"),
            Map.ofEntries(
                    Map.entry("CUENTA", CUENTA),
                    Map.entry("AREA", AREA),
                    // La hoja interna llama DEPARTAMENTO a lo que en la del IMSS es una CUENTA:
                    // el valor observado, ECOMMERCE, esta en el catalogo de cuentas y no en el de
                    // areas. Va a CUENTA por la evidencia; queda como pregunta para confirmar.
                    Map.entry("DEPARTAMENTO", CUENTA),
                    Map.entry("PUESTO", PUESTO)),
            Map.ofEntries(
                    Map.entry("DIAGNOSTICO", "DIAGNOSTICO"),
                    Map.entry("CAUSA", "CAUSA"),
                    Map.entry("SDI", "SDI"),
                    Map.entry("SALARIO", "SALARIO"),
                    Map.entry("PAGO", "PAGO_60"),
                    Map.entry("ST7", "ST7"),
                    Map.entry("ALTA INGRESO", "ALTA_OPERACION"),
                    Map.entry("TOTAL DE DIAS", "DIAS_TOTAL"),
                    Map.entry("COSTO TOTAL", "COSTO_TOTAL"),
                    Map.entry("OBSERVACIONES", "OBSERVACIONES")),
            java.util.Set.of(
                    "DETALLE DE INCAPACIDAD",          // folio, ramo, tipo, inicio, termino, dias
                    "COSTOS",                          // por ramo y total
                    "ENFERMEDAD GENERAL", "MATERNIDAD",
                    "ACCIDENTE LABORAL", "ACCIDENTE TRAYECTO",
                    "DIAS SUBSIDIADOS POR LA EMPRESA", // el equivalente en la hoja interna
                    "COLOCAR"));                       // EVR / HX / OTRO, solo en la interna

    /**
     * ACCIDENTABILIDAD. 172 columnas, encabezado de tres filas, datos desde la fila 6.
     *
     * <p><b>Tampoco tiene fecha del accidente.</b> Sus columnas de fecha son
     * {@code FECHA PARA CALIFICACION} &mdash;cuando se dictamina, no cuando paso&mdash; y los
     * {@code ST2 (FECHA INICIO Y ALTA)} de cada incapacidad derivada. El anclaje temporal es el
     * bloque {@code MES REPORTADO}, igual que en maternidad, y con los mismos buckets de borde:
     * {@code PENDIENTES 2025} y {@code PENDIENTES PARA 2027}.
     *
     * <p><b>Tres grupos van como grupos de campos y no como "marca con 1", cada uno por su razon:</b>
     * <ul>
     *   <li>{@code CALIFICADO} e {@code IMPROCEDENTE} abren en {@code LABORAL} y {@code TRAYECTO},
     *       y debajo de cada uno hay fechas, dias y costo. Son datos que conviven.</li>
     *   <li>{@code CAUSA DE RT} es <b>mixto</b>: su primera columna es la descripcion en texto
     *       libre ("DERRAPE EN MOTO") y las siete siguientes son las causas con marca. Tratado como
     *       one-hot, {@code Normalizador.marcado} daria por marcada la descripcion &mdash;es texto
     *       no vacio&mdash; y la causa real se perderia. Como grupo de campos se guardan las dos
     *       cosas: la descripcion con su texto y la causa marcada en su propio atributo.</li>
     * </ul>
     *
     * <p>El precio de esa decision es que la causa queda en el <i>nombre</i> del atributo
     * ({@code CAUSA_DE_RT.CAIDA}) y no en su valor. Se puede consultar, pero una forma mas limpia
     * necesitaria un tipo de grupo nuevo &mdash;mixto&mdash; y eso es trabajo aparte.
     */
    public static final MapeoFamilia ACCIDENTE = new MapeoFamilia(
            Familia.ACCIDENTE,
            NOMBRE_ESTANDAR,
            List.of(),                       // sin fecha del accidente; ver el javadoc
            Map.of("CUENTA", CUENTA,
                   "AREA", AREA,
                   "PUESTO", PUESTO,
                   "GENERO", GENERO,
                   "STATUS", ESTATUS),
            Map.ofEntries(
                    Map.entry("TIPO DE RIESGO", "TIPO_RIESGO"),
                    Map.entry("DX IMSS", "DIAGNOSTICO"),
                    Map.entry("FECHA PARA CALIFICACION", "FECHA_CALIFICACION"),
                    Map.entry("OBSERVACIONES", "OBSERVACIONES"),
                    Map.entry("DOCUMENTO SUBIDO", "DOCUMENTO_FZ")),
            // SDI abarca DOS columnas -el salario diario integrado y el PAGO 60%- y las dos van
            // llenas en cada renglon. Como one-hot levantaba un aviso por renglon: 42 de 42 en
            // AIFA, 28 de 28 en Z Vallejo.
            java.util.Set.of("SDI", "CALIFICADO", "IMPROCEDENTE", "CAUSA DE RT", "COSTO"));

    /**
     * CONSUMIBLES. La unica familia que <b>no tiene personas</b>.
     *
     * <p>Su hoja {@code MEDICAMENTOS} es una matriz de <i>medicamento &times; mes</i>: 12 meses de
     * 14 columnas, con el consumo por semana, el total por lote y el acumulado. Se lee con
     * {@code LectorMatrizMensual} y se guarda en {@code SERV_MED_BITACORA_METRICA}, que existia
     * desde el DDL de la fase 1 con el comentario "conteos sin persona" y nadie habia usado.
     *
     * <p>Las tres medidas se guardan por decision de servicio medico del 30-sep-2026.
     *
     * <p><b>Su otra hoja, {@code MATERIAL FIJO}, es otra cosa y todavia no entra.</b> Se titula
     * "CONTROL DE EQUIPO" y no mide consumo sino patrimonio: hasta tres ciclos de recepcion,
     * cantidad y baja por equipo. Un baumanometro no se gasta. Esa hoja encaja en el camino normal
     * &mdash;un evento por equipo&mdash; y queda pendiente de una respuesta de servicio medico
     * sobre que significan los equipos sin fecha ni cantidad.
     */
    public static final MapeoFamilia CONSUMIBLE = matriz(Familia.CONSUMIBLE);

    private static final List<MapeoFamilia> TODOS = List.of(
            ATENCION, EXAMEN_INGRESO, EXAMEN_PERIODICO, EXAMEN_POS_INCAP, ANTIDOPING, MATERNIDAD,
            INCAPACIDAD, ACCIDENTE, CONSUMIBLE);

    /**
     * El mapeo de una familia, si ya esta configurada.
     *
     * <p>Las familias sin mapeo no fallan: se saltan y se reportan. Incapacidades,
     * accidentabilidad, antidoping, maternidad y consumibles entran en la fase 2, y los cuatro
     * de seguimiento en la fase 3.
     */
    public static java.util.Optional<MapeoFamilia> de(Familia familia) {
        return TODOS.stream().filter(m -> m.familia() == familia).findFirst();
    }

    public static List<Familia> configuradas() {
        return TODOS.stream().map(MapeoFamilia::familia).toList();
    }
}
