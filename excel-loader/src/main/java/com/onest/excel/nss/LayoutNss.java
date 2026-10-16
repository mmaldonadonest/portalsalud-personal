package com.onest.excel.nss;

import static com.onest.excel.nss.CampoNss.catalogo;
import static com.onest.excel.nss.CampoNss.decimal;
import static com.onest.excel.nss.CampoNss.decimalConSigno;
import static com.onest.excel.nss.CampoNss.entero;
import static com.onest.excel.nss.CampoNss.enteroNuevo;
import static com.onest.excel.nss.CampoNss.fecha;
import static com.onest.excel.nss.CampoNss.marca;
import static com.onest.excel.nss.CampoNss.nss;
import static com.onest.excel.nss.CampoNss.texto;
import static com.onest.excel.nss.CampoNss.textoObligatorio;

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
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA", "FECHA", true),
            catalogo("RAMO", "RAMO", "RAMO_ATENCIONES", true),
            catalogo("CAUSA", "CAUSA", "CAUSA_CONSULTA", false),
            catalogo("CAUSAS MUSCULO ESQUELETICAS", "LESION_ME", "LESION_ME", false),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            texto("TRATAMIENTO", "TRATAMIENTO", 1000))),

    EXAMEN_INGRESO("EXAMEN NUEVO INGRESO", "SERV_MED_XLS_EXAMEN_ING", false, List.of(
            nss(),
            texto("NOMBRE", "NOMBRE", 120),
            texto("APELLIDO_PATERNO", "APELLIDO_PATERNO", 120),
            texto("APELLIDO_MATERNO", "APELLIDO_MATERNO", 120),
            texto("PUESTO SOLICITADO", "PUESTO_SOLICITADO", 160),
            catalogo("GENERO", "GENERO", "GENERO", false),
            // Rango y no edad (14-oct-2026): los Excel del servicio medico solo marcan el rango;
            // la edad exacta del candidato llega despues por OCR (fecha de nacimiento)
            catalogo("RANGO DE EDAD", "RANGO_EDAD", "RANGO_EDAD", false),
            texto("CUENTA", "CUENTA", 160),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA EXAMEN", "FECHA", true),
            catalogo("TIPO DE EXAMEN", "TIPO_EXAMEN", "TIPO_EXAMEN_INGRESO", true),
            catalogo("STATUS", "DICTAMEN", "DICTAMEN", false),
            texto("DIAGNOSTICO", "DIAGNOSTICO", 1000),
            texto("ESTADO CIVIL", "ESTADO_CIVIL", 40),
            texto("ESCOLARIDAD", "ESCOLARIDAD", 60),
            // 13-oct-2026: el Excel original las trae y el layout no (DDL excel-nss-12)
            texto("RESULTADOS PENDIENTES", "RESULTADOS_PENDIENTES", 500),
            texto("OBSERVACIONES", "OBSERVACIONES", 1000))),

    EXAMEN_PERIODICO("EXAMEN PERIODICO", "SERV_MED_XLS_EXAMEN_PER", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            fecha("FECHA EXAMEN", "FECHA", true),
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
            fecha("FECHA EXAMEN", "FECHA", true),
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

    // Seguimiento con historial (decision 12-oct): un renglon por caso por MES REPORTADO (FECHA =
    // fecha de corte). El embarazo = NSS + FUM (o + FECHA PROBABLE DE PARTO si falta la FUM), en
    // CLAVE_EMBARAZO, que arma ValidadorNss. El folio NO identifica el embarazo. DDL excel-nss-08.
    MATERNIDAD("MATERNIDAD", "SERV_MED_XLS_MATERNIDAD", true, List.of(
            nss(),
            catalogo("PREDIO", "PREDIO", "PREDIO", true),
            catalogo("AREA", "AREA", "AREA", true),
            // Es la fecha de corte / mes reportado. La plantilla la llamo FECHA (10-oct), FECHA DE CORTE
            // (12-oct) y FECHA DE INGRESO (15-oct, mismo significado, decision del usuario): se aceptan las tres
            fecha("FECHA DE INGRESO", "FECHA DE CORTE|FECHA", "FECHA", true),
            catalogo("STATUS LABORAL", "STATUS_LABORAL", "ESTATUS_LABORAL", true),
            fecha("FUM", "FUM", false),
            // 15-oct-2026: columnas nuevas de la plantilla (se capturan, no se calculan de la FUM).
            // Los archivos de antes no las traen y se siguen reconociendo (DDL excel-nss-16)
            enteroNuevo("SEMANA GESTACION", "SEMANA_GESTACION"),
            enteroNuevo("MES GESTACION", "MES_GESTACION"),
            fecha("FECHA PROBABLE DE PARTO", "FECHA_PROB_PARTO", false),
            fecha("FECHA PROBABLE DE INCAP PRENATAL", "FECHA_PROB_INCAP_PRENATAL", false),
            fecha("FECHA PROBABLE REINICIO DE LABORES", "FECHA_PROB_REINICIO", false),
            fecha("FECHA REAL DE REINICIO DE LABORES", "FECHA_REAL_REINICIO", false),
            fecha("FECHA DE INICIO DE INCAPACIDAD", "FECHA_INICIO_INCAP", false),
            // La plantilla trae "INCAOACIDAD" (error de dedo); se acepta tambien bien escrito
            fecha("FECHA DE TERMINO DE INCAOACIDAD", "FECHA DE TERMINO DE INCAPACIDAD", "FECHA_TERMINO_INCAP", false),
            texto("FOLIO DE INCAPACIDAD", "FOLIO_INCAPACIDAD", 20),
            fecha("FECHA INICIO DE LACTANCIA", "FECHA_INICIO_LACTANCIA", false),
            fecha("FECHA TERMINO DE LACTANCIA", "FECHA_TERMINO_LACTANCIA", false),
            fecha("FECHA DE REANUDACION DE ACTIVIDADES EN HORARIO NORMAL", "FECHA_REANUDA_HORARIO", false),
            fecha("CORREO DE AVISO EMBARAZO", "CORREO_AVISO_EMBARAZO", false),
            fecha("CORREO DE AVISO LACTANCIA", "CORREO_AVISO_LACTANCIA", false),
            fecha("CORREO DE AVISO TERMINO DE LACTANCIA", "CORREO_AVISO_TERM_LACT", false))),

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
     * Columnas que no vienen en el Excel pero se guardan, calculadas por ValidadorNss: la FECHA de
     * medicamentos (dia 1 de MES / AÑO, la necesitan el reemplazo por predio y mes y las graficas)
     * y la CLAVE_EMBARAZO de maternidad (agrupa el historial de cada embarazo).
     */
    public List<String> columnasDerivadas() {
        return switch (this) {
            case MEDICAMENTOS -> List.of("FECHA");
            case MATERNIDAD -> List.of("CLAVE_EMBARAZO");
            case ANTIDOPING -> List.of("SUSTANCIA");
            default -> List.of();
        };
    }
}
