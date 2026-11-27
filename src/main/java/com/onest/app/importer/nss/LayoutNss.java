package com.onest.app.importer.nss;

import static com.onest.app.importer.nss.CampoNss.catalogo;
import static com.onest.app.importer.nss.CampoNss.decimal;
import static com.onest.app.importer.nss.CampoNss.decimalConSigno;
import static com.onest.app.importer.nss.CampoNss.entero;
import static com.onest.app.importer.nss.CampoNss.enteroNuevo;
import static com.onest.app.importer.nss.CampoNss.fecha;
import static com.onest.app.importer.nss.CampoNss.marca;
import static com.onest.app.importer.nss.CampoNss.nss;
import static com.onest.app.importer.nss.CampoNss.texto;
import static com.onest.app.importer.nss.CampoNss.textoObligatorio;

import java.util.List;

/**
 * Los 5 layouts con visto bueno del 10-oct-2026 (docs/09102026/) y los 4 del 12-oct
 * (docs/09102026/corregidos/: pos incapacidad, antidoping, medicamentos y material fijo). Los
 * encabezados son el contrato: si cambia una columna, se cambia aqui y en su DDL (excel-nss-01,
 * excel-nss-06 o excel-nss-07).
 *
 * <p>{@code empleado}: si las personas son empleados, nombre/genero/edad/cuenta/puesto se copian
 * del biometrico por NSS al cargar y el NSS tiene que existir alla. En examen de nuevo ingreso son
 * candidatos: esos datos vienen capturados en el propio layout.
 */
public enum LayoutNss {

    ATENCIONES("ATENCIONES", "SERV_MED_XLS_ATENCION", true, List.of(
            nss(),
            // «N°» y «MUSCULO ESQUELETICAS»: asi titula esas columnas el formato 2026 de SM
            // (ACUMULADO MODIFICAR 2026, 12-nov-2026); se aceptan las dos formas
            catalogo("PREDIO", "N°|Nº", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA", "FECHA", true),
            catalogo("RAMO", "RAMO", "RAMO_ATENCIONES", true),
            // «CAUSA DE CONSULTA»: asi viene en ATENCIONES 2026 ultimo.xlsx (12-nov-2026)
            catalogo("CAUSA", "CAUSA DE CONSULTA", "CAUSA", "CAUSA_CONSULTA", false),
            catalogo("CAUSAS MUSCULO ESQUELETICAS", "MUSCULO ESQUELETICAS", "LESION_ME", "LESION_ME", false),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            texto("TRATAMIENTO", "TRATAMIENTO", 1000),
            // 12-nov-2026 (decision del usuario): cuenta, edad y genero vienen en el Excel, son obligatorios y
            // GANAN sobre el biometrico (que muchas veces no los trae: EMP_FECHAN casi siempre vacia). Se leen
            // por nombre de encabezado, asi que el orden de las columnas no importa.
            textoObligatorio("CUENTA", "CUENTA", 160),
            // EDAD viene como RANGO (18 - 25 ... 55 +), igual que en nuevo ingreso: va a RANGO_EDAD
            // (excel-nss-18-atencion-rango-edad.sql). La columna EDAD queda para la del biometrico.
            catalogo("EDAD", "RANGO DE EDAD", "RANGO_EDAD", "RANGO_EDAD", true),
            catalogo("GENERO", "SEXO", "GENERO", "GENERO", true))),

    // 19-nov-2026: el formato 2026 de SM («2 EXAMEN NUEVO INGRESO 2026.xlsx», hoja EMNI) titula distinto cuatro
    // columnas (FECHA DE ATENCION, TIPO EXAMEN, PUESTO, EDAD): se aceptan las dos formas. El nombre viene dividido
    // (APELLIDO PATERNO, APELLIDO MATERNO, NOMBRE (S)) y ademas una columna NOMBRE con el nombre completo: esa NO se
    // carga (es la union de las otras tres; decision del usuario 19-nov), por eso NOMBRE (S) va primero. Un archivo
    // sin NOMBRE (S) (plantilla anterior) sigue tomando NOMBRE. Ya no trae ESTADO CIVIL ni ESCOLARIDAD (opcionales).
    EXAMEN_INGRESO("EXAMEN NUEVO INGRESO", "SERV_MED_XLS_EXAMEN_ING", false, List.of(
            nss(),
            texto("NOMBRE (S)", "NOMBRE(S)|NOMBRES|NOMBRE", "NOMBRE", 120),
            CampoNss.textoQuePuedeFaltar("APELLIDO_PATERNO", "APELLIDO PATERNO", "APELLIDO_PATERNO", 120),
            CampoNss.textoQuePuedeFaltar("APELLIDO_MATERNO", "APELLIDO MATERNO", "APELLIDO_MATERNO", 120),
            texto("PUESTO SOLICITADO", "PUESTO", "PUESTO_SOLICITADO", 160),
            catalogo("GENERO", "GENERO", "GENERO", false),
            // Rango y no edad (14-oct-2026): los Excel del servicio medico solo marcan el rango;
            // la edad exacta del candidato llega despues por OCR (fecha de nacimiento)
            catalogo("RANGO DE EDAD", "EDAD", "RANGO_EDAD", "RANGO_EDAD", false),
            texto("CUENTA", "CUENTA", 160),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA EXAMEN", "FECHA DE ATENCION", "FECHA", true),
            catalogo("TIPO DE EXAMEN", "TIPO EXAMEN", "TIPO_EXAMEN", "TIPO_EXAMEN_INGRESO", true),
            catalogo("STATUS", "DICTAMEN", "DICTAMEN", false),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            CampoNss.textoQuePuedeFaltar("ESTADO CIVIL", null, "ESTADO_CIVIL", 40),
            CampoNss.textoQuePuedeFaltar("ESCOLARIDAD", null, "ESCOLARIDAD", 60),
            // 19-nov-2026: SI = contratado, NO = no contratado (SM: 4,680 / 5,015 en 2026). Columna nueva:
            // docs/script-prod/excel-nss-19-status-contratacion.sql (base del portal) ANTES de cargar con este WAR
            CampoNss.catalogoQuePuedeFaltar("STATUS DE CONTRATACION", "STATUS_CONTRATACION", "SI_NO"),
            // 13-oct-2026: el Excel original las trae y el layout no (DDL excel-nss-12)
            texto("RESULTADOS PENDIENTES", "RESULTADOS_PENDIENTES", 500),
            texto("OBSERVACIONES", "OBSERVACIONES", 1000))),

    EXAMEN_PERIODICO("EXAMEN PERIODICO", "SERV_MED_XLS_EXAMEN_PER", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            // 24-nov-2026: SM pide «FECHA DE ATENCION» (asi viene el periodico 2026); «FECHA EXAMEN» de la
            // plantilla anterior se sigue aceptando
            fecha("FECHA DE ATENCION", "FECHA EXAMEN", "FECHA", true),
            catalogo("STATUS", "DICTAMEN", "DICTAMEN", false),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            texto("RESULTADOS PENDIENTES", "RESULTADOS_PENDIENTES", 500),
            texto("OBSERVACIONES", "OBSERVACIONES", 1000))),   // 13-oct-2026, DDL excel-nss-12

    INCAPACIDADES("INCAPACIDADES", "SERV_MED_XLS_INCAPACIDAD", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            texto("FOLIO", "FOLIO", 20),
            catalogo("RUBRO", "RUBRO", "RUBRO_INCAP", true),
            catalogo("RAMO", "RAMO", "RAMO_INCAPACIDADES", true),
            catalogo("TIPO", "TIPO", "TIPO_INCAP", false),
            fecha("INICIO DE INCAPACIDAD", "FECHA", true),
            fecha("TERMINO DE INCAPACIDAD", "FECHA_TERMINO", false),
            entero("DIAS DE INCAPACIDAD", "DIAS"),
            fecha("FECHA DE PRIMERA INCAPACIDAD", "FECHA_PRIMERA", false),
            decimal("SDI", "SDI"),
            decimal("PAGO (60%)", "PAGO_60"),
            decimal("COSTO ENF GRAL", "COSTO_ENF_GRAL"),
            decimal("COSTO MATERNIDAD", "COSTO_MATERNIDAD"),
            decimal("COSTO LABORAL", "COSTO_LABORAL"),
            decimal("COSTO TRAYECTO", "COSTO_TRAYECTO"),
            decimal("COSTO TOTAL", "COSTO_TOTAL"),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            texto("CAUSA", "CAUSA", 200),
            catalogo("ALTA INGRESO A LA OPERACION", "ALTA_OPERACION", "SI_NO", false),
            catalogo("ST7", "ST7", "SI_NO", false))),

    ACCIDENTES("ACCIDENTES", "SERV_MED_XLS_ACCIDENTE", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA", "FECHA", true),
            catalogo("TIPO DE RIESGO", "TIPO_RIESGO", "TIPO_RIESGO", true),
            texto("DX IMSS", "DX_IMSS", 1000),
            decimal("SDI", "SDI"),
            decimal("PAGO 60%", "PAGO_60"),
            fecha("ST7 FECHA AVISO ACCIDENTE", "FECHA_AVISO_ST7", false),
            fecha("ST2 FECHA ALTA ACCIDENTE", "FECHA_ALTA_ST2", false),
            entero("DIAS", "DIAS"),
            // Puede venir negativo: el bloque IMPROCEDENTE de los Excel del servicio medico da
            // -3 dias x pago 60% cuando faltan las fechas. Decision 12-oct-2026: entra y se avisa.
            decimalConSigno("COSTO", "COSTO"),
            catalogo("STATUS DE CALIFICACION", "STATUS_CALIFICACION", "ESTATUS_ACCIDENTE", false),
            texto("DESCRIPCION BREVE DEL ACCIDENTE", "DESCRIPCION", 1000),
            catalogo("CAUSA DE RT", "CAUSA_RT", "CAUSA_RT", false),
            catalogo("STATUS", "STATUS", "STATUS_CASO", false),
            fecha("FECHA PARA CALIFICACION", "FECHA_CALIFICACION", false),
            texto("OBSERVACIONES", "OBSERVACIONES", 1000))),

    // --- 12-oct-2026: layouts de docs/09102026/corregidos/ (DDL en excel-nss-06-ddl-pos-antidoping.sql)

    // Trae todas las columnas del periodico mas CAUSA: el reconocimiento por encabezados lo distingue
    // porque gana el layout con mas columnas presentes.
    // SIN FOLIO INCAPACIDAD (13-oct-2026, el usuario lo quito de la plantilla): los Excel historicos
    // no lo traen. La columna FOLIO_INCAPACIDAD de SERV_MED_XLS_EXAMEN_POS se queda (acepta nulo) y no
    // se llena. En el PORTAL si debe capturarse el folio (P12 del plan).
    EXAMEN_POS_INCAP("EXAMEN POS INCAPACIDAD", "SERV_MED_XLS_EXAMEN_POS", true, List.of(
            nss(),
            // 24-nov-2026: mismo sinonimo que el periodico. Si solo el periodico lo aceptara, un archivo de pos
            // incapacidad con «FECHA DE ATENCION» se reconoceria como PERIODICO (le sobra CAUSA) y no como el suyo
            fecha("FECHA DE ATENCION", "FECHA EXAMEN", "FECHA", true),
            catalogo("CAUSA", "CAUSA", "CAUSA_EXAMEN_POS_INCAP", false),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            catalogo("STATUS", "DICTAMEN", "DICTAMEN", false),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            texto("RESULTADOS PENDIENTES", "RESULTADOS_PENDIENTES", 500),
            texto("OBSERVACIONES", "OBSERVACIONES", 1000))),   // 13-oct-2026, DDL excel-nss-12

    // Solo empleados (decision 12-oct): el antidoping de candidatos se captura en el examen inicial
    ANTIDOPING("ANTIDOPING", "SERV_MED_XLS_ANTIDOPING", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA", "FECHA", true),
            catalogo("TIPO DE PRUEBA", "TIPO_PRUEBA", "TIPO_PRUEBA", true),
            catalogo("RESULTADO", "RESULTADO", "RESULTADO_PRUEBA", true),
            // 13-oct-2026: una columna por sustancia con 1 (en vez de una sola DOPING): una prueba
            // puede salir positiva a varias (hasta 6 en los Excel de 2025 y 2026). Se guardan juntas
            // en SUSTANCIA ("AMF, MET, THC"), ver ValidadorNss y columnasDerivadas().
            marca("AMF"), marca("BARB"), marca("BENZO"), marca("COC"), marca("LSD"), marca("MET"),
            marca("OPI"), marca("PVC"), marca("THC"), marca("SOLVENTE"), marca("OTRO"),
            decimal("ALCOHOLEMIA %", "ALCOHOLEMIA"),
            catalogo("CONCLUSION", "CONCLUSION", "CONCLUSION_ANTIDOPING", false),
            catalogo("STATUS", "STATUS", "STATUS_ANTIDOPING", false),
            texto("OBSERVACIONES", "OBSERVACIONES", 1000))),

    // 27-nov-2026: layout de SM «8 MATERNIDAD 2026» (un renglon por caso, encabezado en dos filas que junta
    // CargaNssService.combinarEncabezados). FECHA ya no viene en el archivo: es el dia 1 del MES DETECCION
    // (lo arma ValidadorNss), y con esa FECHA se cuentan los casos en las graficas y se reemplaza por predio y
    // mes. El embarazo = NSS + FUM (o + FECHA PROBABLE DE PARTO si falta la FUM), en CLAVE_EMBARAZO. El folio
    // NO identifica el embarazo. DDL excel-nss-08, -16 y -20.
    // Antes (12-oct a 26-nov) FECHA era la fecha de corte y se llamo FECHA, FECHA DE CORTE o FECHA DE INGRESO;
    // en el formato de SM «FECHA DE INGRESO» es el ingreso a la empresa (2015-2026) y por eso cambio.
    MATERNIDAD("MATERNIDAD", "SERV_MED_XLS_MATERNIDAD", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            // ENE..DIC (abreviado o completo) sin anio: el anio sale del titulo o del nombre del archivo
            textoObligatorio("MES DETECCION", "MES_DETECCION", 20),
            fecha("FECHA DE INGRESO", "FECHA_INGRESO", false),
            // «SATAUS LABORAL»: asi viene en el formato de SM (error de dedo); se aceptan las dos
            catalogo("STATUS LABORAL", "SATAUS LABORAL", "STATUS_LABORAL", "ESTATUS_LABORAL", true),
            // EDAD (rango «26 - 35»), CUENTA y PUESTO del archivo van en columnas propias: la de las graficas
            // (EDAD, CUENTA, PUESTO) sigue siendo la del biometrico hasta que SM conteste cual se usa
            CampoNss.catalogoQuePuedeFaltar("EDAD", "RANGO DE EDAD", "RANGO_EDAD", "RANGO_EDAD"),
            CampoNss.textoQuePuedeFaltar("CUENTA", null, "CUENTA_ARCHIVO", 160),
            CampoNss.textoQuePuedeFaltar("PUESTO", null, "PUESTO_ARCHIVO", 160),
            fecha("FUM", "FUM", false),
            // 15-oct-2026: SEMANA y MES GESTACION de la plantilla anterior. El formato de SM trae «EDAD GESTACIONAL
            // SEMANAS / MESES» calculadas al dia en que se abre el archivo (106 semanas en un caso ya terminado):
            // no se leen
            enteroNuevo("SEMANA GESTACION", "SEMANA_GESTACION"),
            enteroNuevo("MES GESTACION", "MES_GESTACION"),
            // Los alias son los encabezados de dos filas ya juntados («PROBABLE» + «FECHA DE PARTO»)
            fecha("FECHA PROBABLE DE PARTO", "PROBABLE FECHA DE PARTO", "FECHA_PROB_PARTO", false),
            fecha("FECHA PROBABLE DE INCAP PRENATAL", "PROBABLE FECHA DE INCAP PRENATAL", "FECHA_PROB_INCAP_PRENATAL", false),
            fecha("FECHA PROBABLE REINICIO DE LABORES", "PROBABLE REINICIO DE LABORES", "FECHA_PROB_REINICIO", false),
            // SM usa una sola columna para el reinicio real y el inicio de la lactancia: llena las dos
            fecha("FECHA REAL DE REINICIO DE LABORES", "REINICIO DE LABORES (FECHA REAL) E INICIO DE LACTANCIA",
                    "FECHA_REAL_REINICIO", false),
            fecha("FECHA DE INICIO DE INCAPACIDAD", "INCAPACIDAD INICIO", "FECHA_INICIO_INCAP", false),
            // La plantilla trae "INCAOACIDAD" (error de dedo); se acepta tambien bien escrito
            fecha("FECHA DE TERMINO DE INCAOACIDAD", "FECHA DE TERMINO DE INCAPACIDAD|INCAPACIDAD TERMINO",
                    "FECHA_TERMINO_INCAP", false),
            texto("FOLIO DE INCAPACIDAD", "INCAPACIDAD FOLIO", "FOLIO_INCAPACIDAD", 20),
            fecha("FECHA INICIO DE LACTANCIA", "REINICIO DE LABORES (FECHA REAL) E INICIO DE LACTANCIA",
                    "FECHA_INICIO_LACTANCIA", false),
            fecha("FECHA TERMINO DE LACTANCIA", "LACTANCIA TERMINA", "FECHA_TERMINO_LACTANCIA", false),
            fecha("FECHA DE REANUDACION DE ACTIVIDADES EN HORARIO NORMAL",
                    "LACTANCIA REANUDA ACTIVIDADES EN HORARIO NORMAL", "FECHA_REANUDA_HORARIO", false),
            // El formato de SM ya no los trae
            CampoNss.fechaQuePuedeFaltar("CORREO DE AVISO EMBARAZO", "CORREO_AVISO_EMBARAZO"),
            CampoNss.fechaQuePuedeFaltar("CORREO DE AVISO LACTANCIA", "CORREO_AVISO_LACTANCIA"),
            CampoNss.fechaQuePuedeFaltar("CORREO DE AVISO TERMINO DE LACTANCIA", "CORREO_AVISO_TERM_LACT"))),

    // Consumibles: inventario del consultorio, sin NSS ni biometrico (empleado = false) y sin
    // contraparte en el portal. DDL en excel-nss-07-ddl-consumibles.sql.
    //
    // Medicamentos: un renglon por lote x predio x mes. La FECHA no se captura: se arma con MES y
    // AÑO (dia 1), ver ValidadorNss. "DIAS PARA CADUCAR" y "ORDEN FIFO" no se leen: son formulas
    // que cambian segun el dia en que se abre el archivo.
    MEDICAMENTOS("CONSUMIBLES MEDICAMENTOS", "SERV_MED_XLS_MEDICAMENTO", false, List.of(
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("MES", "MES", "MES", true),
            entero("AÑO", "ANIO", true),
            catalogo("CATEGORIA", "CATEGORIA", "CATEGORIA_MEDICAMENTO", false),
            textoObligatorio("DESCRIPCION", "DESCRIPCION", 200),
            texto("LOTE", "LOTE_MEDICAMENTO", 60),
            fecha("FECHA DE INGRESO DEL LOTE", "FECHA_INGRESO", false),
            fecha("FECHA DE CADUCIDAD", "FECHA_CADUCIDAD", false),
            decimal("CANTIDAD MES ANTERIOR", "CANTIDAD_MES_ANT"),
            decimal("RECIBIDO EN EL MES", "RECIBIDO_MES"),
            decimal("CONSUMO SEMANA 1", "CONSUMO_S1"),
            decimal("CONSUMO SEMANA 2", "CONSUMO_S2"),
            decimal("CONSUMO SEMANA 3", "CONSUMO_S3"),
            decimal("CONSUMO SEMANA 4", "CONSUMO_S4"),
            decimal("CONSUMO SEMANA 5", "CONSUMO_S5"),
            decimal("TOTAL x LOTE", "TOTAL_LOTE"),
            decimal("CONSUMO ACUMULADO", "CONSUMO_ACUMULADO"),
            decimal("RESTANTE PARA USO x LOTE", "RESTANTE_LOTE"),
            decimal("TOTAL EN EXISTENCIA PARA PROXIMO MES", "EXISTENCIA_PROX_MES"))),

    // Material fijo: un renglon por recepcion de equipo. FECHA = FECHA DE RECEPCION.
    // TIPO DE MATERIAL sin catalogo por ahora (texto libre).
    MATERIAL_FIJO("CONSUMIBLES MATERIAL FIJO", "SERV_MED_XLS_MATERIAL_FIJO", false, List.of(
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("EQUIPO", "EQUIPO", "EQUIPO", true),
            texto("TIPO DE MATERIAL", "TIPO_MATERIAL", 200),
            fecha("FECHA DE RECEPCION", "FECHA", true),
            catalogo("MOTIVO DE RECEPCION", "MOTIVO", "MOTIVO_RECEPCION", false),
            decimal("CANTIDAD SURTIDA", "CANTIDAD_SURTIDA"),
            fecha("FECHA DE BAJA", "FECHA_BAJA", false),
            decimal("CANTIDAD TOTAL SURTIDA", "CANTIDAD_TOTAL")));

    private final String hoja;
    private final String tabla;
    private final boolean empleado;
    private final List<CampoNss> campos;

    LayoutNss(String hoja, String tabla, boolean empleado, List<CampoNss> campos) {
        this.hoja = hoja;
        this.tabla = tabla;
        this.empleado = empleado;
        this.campos = campos;
    }

    /** Nombre de la pestana de captura en el layout. */
    public String hoja() {
        return hoja;
    }

    public String tabla() {
        return tabla;
    }

    public boolean empleado() {
        return empleado;
    }

    public List<CampoNss> campos() {
        return campos;
    }

    /**
     * Si acepta NSS 00000000000 = renglon "sin persona". Decision 30-oct-2026 para ATENCIONES (el 40%
     * viene asi y el servicio medico no tiene esos NSS); el 13-nov-2026 el usuario lo extendio a TODOS
     * los layouts con NSS: el evento ocurrio y debe contar. Cuenta en totales y graficas; no se cruza
     * con el biometrico ni aparece en el expediente de nadie, y se identifica despues por el propio NSS
     * en ceros. Un NSS real que no esta en el biometrico tambien entra, marcado EN_BIOMETRICO = 'N'
     * (regla del 3-nov-2026, CargaNssService.completarDelBiometrico). Consumibles no llevan NSS.
     */
    public boolean aceptaNssCeros() {
        return campos.stream().anyMatch(c -> c.tipo() == CampoNss.Tipo.NSS);
    }

    /**
     * Columnas que no vienen en el Excel pero se guardan, calculadas por ValidadorNss: la FECHA de
     * medicamentos (dia 1 de MES / AÑO, la necesitan el reemplazo por predio y mes y las graficas)
     * y la CLAVE_EMBARAZO de maternidad (agrupa el historial de cada embarazo).
     */
    public List<String> columnasDerivadas() {
        return switch (this) {
            case MEDICAMENTOS -> List.of("FECHA");
            case MATERNIDAD -> List.of("FECHA", "CLAVE_EMBARAZO");   // FECHA = dia 1 del MES DETECCION
            case ANTIDOPING -> List.of("SUSTANCIA");
            default -> List.of();
        };
    }
}
