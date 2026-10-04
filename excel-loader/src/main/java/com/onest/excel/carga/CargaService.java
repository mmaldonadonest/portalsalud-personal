package com.onest.excel.carga;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.onest.excel.inventario.ArchivoInventariado;
import com.onest.excel.layout.DetectorEncabezado;
import com.onest.excel.layout.Encabezado;
import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;
import com.onest.excel.persistencia.BitacoraRepository;
import com.onest.excel.transposicion.Transpositor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Tarea 12: orquestar la carga de un archivo, de principio a fin.
 *
 * <p>Por archivo: abre, elige la hoja de detalle, detecta el encabezado, transpone, mapea y
 * escribe. Cada archivo es su propio lote y su propia transaccion logica: si uno falla, los
 * demas siguen y queda constancia de cual fue.
 *
 * <p><b>Nada se descarta en silencio.</b> Todo renglon que no entra deja su razon en
 * {@code SERV_MED_BITACORA_LOTE.MENSAJES}, con el numero de fila del Excel para poder ir a
 * verlo.
 */
@Service
@ConditionalOnExpression("'${excel.destino.url:}' != ''")
public class CargaService {

    private static final Logger log = LoggerFactory.getLogger(CargaService.class);

    /**
     * Hojas que no son tablas de detalle.
     *
     * <p>{@code INFOR} se agrego el 30-sep-2026: los doce archivos de antidoping la traen al
     * final, con 5 columnas y 20 renglones de instrucciones para quien captura. Sin ignorarla,
     * cada archivo dejaba un lote fallido con "sin columna que identifique el renglon".
     *
     * <p>{@code ST7} y {@code PEND } son de incapacidades y <b>no son incapacidades</b>: son
     * acuses de <i>entrega de documentos a nominas</i>, con mes de entrega y firma, sin un solo
     * renglon de persona. {@code "PEND "} lleva espacio para que atrape {@code PEND 2025} y
     * {@code PEND 2026} sin pegarle a una hoja que empiece por "PENDIENTE" y sea otra cosa.
     *
     * <p><b>{@code REVISIONES} es distinta: no es basura, es data que no cabe todavia.</b> La
     * traen los archivos de maternidad y es una matriz de <i>nombre &times; mes de revision
     * prenatal</i>, con observaciones del tipo "PULSERA ROJA". Un renglon por persona y una
     * columna por mes, o sea otra forma, no la de un evento por renglon. Se ignora para que no
     * entre como si fuera un predio llamado REVISIONES, y queda anotada en el plan como contenido
     * pendiente &mdash;no como hoja descartable&mdash;.
     */
    private static final List<String> HOJAS_IGNORADAS =
            List.of("ACUMULADO", "ACUM", "GRAF", "CHART", "COMPARATIVO", "REPORT RH",
                    "REP RH", "NOMENCLATURA", "CONCENTRADO", "GENERAL", "CONTROL GRAL",
                    "INFOR", "REVISIONES", "ST7", "ST-7", "PEND ", "HOJA1");

    /**
     * Hojas que llevan el nombre de un MES y no de un predio.
     *
     * <p>Es la trampa que destapo la correccion del multi-hoja, el 29-sep-2026. Los archivos de
     * incapacidades traen la hoja del predio con el anio completo <b>y ademas</b> doce hojas
     * mensuales con los mismos renglones partidos: en World Park, {@code WORLD PARK} tiene 124
     * renglones y entre {@code EN}, {@code FB}, {@code MZ}… hay otros 114 que son los mismos.
     * Procesar unas y otras mete cada atencion dos veces.
     *
     * <p><b>La hoja del predio manda y los meses se ignoran.</b> Si alguna vez hace falta el
     * corte mensual, ya esta en las columnas {@code ANIO} y {@code MES} de
     * {@code SERV_MED_BITACORA_EVENTO}, calculadas de la fecha de cada renglon.
     */
    private static final java.util.Set<String> HOJAS_MES = java.util.Set.of(
            "EN", "FB", "MZ", "AB", "MY", "JN", "JL", "AG", "SP", "NV", "DC",
            "ENE", "FEB", "MAR", "MZO", "ABR", "MAY", "MYO", "JUN", "JUL", "AGO",
            "SEP", "OCT", "NOV", "DIC");

    /** Cuantos renglones trae el modo de ensayo. */
    public static final int FILAS_MUESTRA = 25;

    private final BitacoraRepository repo;

    public CargaService(BitacoraRepository repo) {
        this.repo = repo;
    }

    /** Resultado de procesar un archivo. */
    public record Resultado(String archivo, String predio, String familia, String hoja,
                            int leidas, int insertadas, int descartadas, int atributos,
                            Integer acumuladoEsperado, String cuadra,
                            List<String> avisos, String error, boolean saltado) {

        public boolean ok() {
            return error == null && !saltado;
        }
    }

    /**
     * Procesa un archivo. Devuelve <b>un resultado por hoja de detalle</b>, no uno por archivo.
     *
     * <p>Un archivo puede traer varios predios, uno por hoja: {@code MKLS - UT - FLORA} tiene las
     * hojas {@code FLORA} (1 renglon), {@code MKLS} (60) y {@code UT} (55). Quedarse con la
     * primera hoja &mdash;como hacia la version del ensayo del 29-sep-2026&mdash; perdia 115
     * atenciones sin avisar, porque la hoja que agarraba estaba casi vacia y el reporte decia
     * "0 renglones" como si el archivo no tuviera datos.
     *
     * <p>El esquema ya lo soportaba: {@code SERV_MED_BITACORA_LOTE} es por archivo <b>y hoja</b>,
     * con indice unico {@code (ARCHIVO, HOJA)}. Solo el cargador no lo aprovechaba.
     */
    public List<Resultado> cargar(ArchivoInventariado archivo, boolean muestra) {
        Optional<MapeoFamilia> mapeoOpt = MapeoFamilia.de(archivo.familia());
        if (mapeoOpt.isEmpty()) {
            return List.of(saltado(archivo, "familia sin mapeo configurado todavia"));
        }
        MapeoFamilia mapeo = mapeoOpt.get();

        try (Workbook libro = WorkbookFactory.create(new File(archivo.ruta().toString()), null, true)) {
            List<String> hojas = hojasDeDetalle(libro);
            if (hojas.isEmpty()) {
                return List.of(error(archivo, "no se hallo hoja de detalle"));
            }
            List<Resultado> rs = new ArrayList<>(hojas.size());
            for (String h : hojas) {
                rs.add(cargarHoja(libro, archivo, mapeo, h, muestra));
            }
            return rs;
        } catch (Exception e) {
            log.warn("Fallo {}: {}", archivo.nombre(), e.toString());
            return List.of(error(archivo, e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    private Resultado cargarHoja(Workbook libro, ArchivoInventariado archivo, MapeoFamilia mapeo,
                                 String nombreHoja, boolean muestra) {
        try {
            if (repo.loteYaProcesado(archivo.nombre(), nombreHoja)) {
                return saltado(archivo, "hoja " + nombreHoja + " ya cargada antes");
            }

            Hoja hoja = new Hoja(libro.getSheet(nombreHoja));

            // Las hojas de matriz mensual no pasan por el detector generico: su renglon no es una
            // persona en una fecha sino una entidad con doce meses en las columnas. Ver
            // LectorMatrizMensual para el porque.
            if (mapeo.matrizMensual()) {
                return cargarMatriz(hoja, archivo, mapeo, nombreHoja, muestra);
            }

            Optional<Encabezado> encOpt = DetectorEncabezado.detectar(hoja);
            if (encOpt.isEmpty()) {
                return error(archivo, "no se detecto encabezado");
            }
            Encabezado enc = encOpt.get();

            Optional<Encabezado.Columna> colNombre =
                    enc.columnaPorGrupo(mapeo.claveNombre().toArray(String[]::new));
            if (colNombre.isEmpty()) {
                return error(archivo, "sin columna que identifique el renglon ("
                        + String.join(" / ", mapeo.claveNombre()) + ")");
            }

            List<Transpositor.FilaTranspuesta> filas = Transpositor.transponer(
                    hoja, enc, colNombre.get().indice(), mapeo.gruposDeCampos(),
                    mapeo.gruposMultimarca().keySet());
            int leidas = filas.size();
            int fueraDeMuestra = 0;
            if (muestra && filas.size() > FILAS_MUESTRA) {
                fueraDeMuestra = filas.size() - FILAS_MUESTRA;
                filas = filas.subList(0, FILAS_MUESTRA);
            }
            int procesadas = filas.size();

            // El predio sale de la HOJA, no del mapeo cuenta->predio del portal, y pasa por la
            // lista canonica: el mismo predio se escribe distinto entre archivos de la misma
            // carpeta (MKLS/MIKELS, FORANEO/FLORANEO, UT/U TEPALCAPA).
            //
            // Cuando el nombre de la hoja NO dice el predio -las hojas de incapacidades internas
            // llamadas solo INTERNA o INTERNAS- se cae a la carpeta, que es el unico lugar que
            // queda. Devolver null ahi seria dejar renglones sin predio y fuera de toda grafica.
            String predio = Normalizador.predio(nombreHoja);
            if (predio == null) {
                predio = Normalizador.predio(archivo.carpetaPredio());
            }
            // El cuadre se lee SIEMPRE, tambien en modo muestra: compara contra lo LEIDO, no
            // contra lo insertado, y el tope de la muestra solo limita lo segundo. Asi el
            // ensayo ya verifica que el transpositor lee bien el archivo completo.
            Integer acumulado = leerAcumulado(libro, nombreHoja);

            long loteId = repo.abrirLote(archivo.nombre(), archivo.ruta().toString(),
                    archivo.checksumSha256(), nombreHoja, archivo.familia().name(),
                    predio, archivo.anio());

            Construccion c = construir(filas, mapeo, enc, hoja, loteId, archivo, nombreHoja, predio);
            int insertados = repo.insertarEventos(c.eventos());
            Map<Integer, Long> ids = repo.idsPorFila(loteId);
            int atributos = repo.insertarAtributos(c.atributos(), ids);

            // FILAS_DESCARTADAS cuenta SOLO lo que se rechazo de verdad, no el tope del ensayo.
            // Antes era leidas - insertados y en modo muestra eso daba 19,718 "descartadas" que
            // en realidad eran renglones buenos que el tope no alcanzo a meter: el reporte
            // parecia una carga con dos tercios de basura cuando no habia ni un rechazo.
            int descartadas = procesadas - insertados;
            List<String> avisos = new ArrayList<>(c.avisos());
            if (fueraDeMuestra > 0) {
                avisos.add("ensayo: " + fueraDeMuestra + " renglones quedaron fuera por el tope de "
                        + FILAS_MUESTRA + " (no son descartes)");
            }
            String estado = c.avisos().isEmpty() ? "OK" : "CON_AVISOS";
            repo.cerrarLote(loteId, leidas, insertados, descartadas, acumulado,
                    resumirAvisos(avisos), estado);

            // Mismos tres estados que guarda el repositorio: "cero" es el archivo que no lleva
            // su acumulado, y no es lo mismo que no cuadrar.
            String cuadra;
            if (acumulado == null) {
                cuadra = "-";
            } else if (acumulado == 0 && leidas > 0) {
                cuadra = "cero";
            } else {
                cuadra = acumulado == leidas ? "SI" : "NO";
            }
            return new Resultado(archivo.nombre(), predio, archivo.familia().name(), nombreHoja,
                    leidas, insertados, descartadas, atributos, acumulado, cuadra,
                    c.avisos(), null, false);

        } catch (Exception e) {
            log.warn("Fallo {} hoja {}: {}", archivo.nombre(), nombreHoja, e.toString());
            return error(archivo, "hoja " + nombreHoja + " · "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Carga una hoja de matriz mensual en {@code SERV_MED_BITACORA_METRICA}.
     *
     * <p>Es el otro camino del cargador. Comparte el lote y la bitacora con el camino normal
     * &mdash;para que el reporte, la reversa y el rastreo funcionen igual&mdash; pero escribe en
     * otra tabla, porque lo que produce no son eventos de personas sino numeros.
     *
     * <p>En el reporte, <b>"leidas" son los conceptos</b> (medicamentos) e "insertadas" son las
     * metricas, que son muchas mas. No es un error de cuenta: son unidades distintas, y por eso
     * esta hoja nunca va a cuadrar contra un ACUMULADO de renglones.
     */
    private Resultado cargarMatriz(Hoja hoja, ArchivoInventariado archivo, MapeoFamilia mapeo,
                                   String nombreHoja, boolean muestra) {
        // El predio sale SIEMPRE de la carpeta, nunca de la hoja.
        //
        // En las otras ocho familias la hoja ES el predio, pero aqui las hojas se llaman
        // MEDICAMENTOS y MATERIAL FIJO: son el tipo de contenido. La primera version uso el
        // nombre de la hoja -con la carpeta solo como respaldo cuando venia nulo- y como
        // "MEDICAMENTOS" no es nulo, los 4,742 renglones de la carga del 30-sep-2026 quedaron con
        // predio "MEDICAMENTOS" y "MATERIAL FIJO". Cualquier reporte por predio habria sido
        // inservible, y nada en el reporte del cargador gritaba.
        //
        // Nota: en la carpeta MIKELS-UT-FLORA el archivo de consumibles cubre los tres predios
        // juntos, asi que ahi el predio queda con el nombre de la carpeta. Es la verdad: ese
        // archivo no los separa, y verlo como una barra rara es mejor que repartirlo a ojo.
        String predio = Normalizador.predio(archivo.carpetaPredio());
        long loteId = repo.abrirLote(archivo.nombre(), archivo.ruta().toString(),
                archivo.checksumSha256(), nombreHoja, archivo.familia().name(),
                predio, archivo.anio());

        int anio = archivo.anio() == null ? 0 : archivo.anio();
        // Consumibles trae DOS hojas con formas distintas bajo la misma familia: MEDICAMENTOS es
        // una matriz de medicamento x mes y MATERIAL FIJO es el inventario de equipo. Cada una
        // tiene su lector; las dos escriben metricas y se distinguen por la columna ORIGEN.
        var res = esHojaDeEquipo(nombreHoja)
                ? LectorEquipo.leer(hoja, loteId, mapeo.familia().name(), predio, anio)
                : LectorMatrizMensual.leer(hoja, loteId, mapeo.familia().name(), predio, anio);

        var metricas = res.metricas();
        if (muestra && metricas.size() > FILAS_MUESTRA) {
            metricas = metricas.subList(0, FILAS_MUESTRA);
        }
        int insertadas = repo.insertarMetricas(metricas);

        String estado = res.avisos().isEmpty() ? "OK" : "CON_AVISOS";
        repo.cerrarLote(loteId, res.conceptos(), insertadas, 0, null,
                resumirAvisos(res.avisos()), estado);

        return new Resultado(archivo.nombre(), predio, archivo.familia().name(), nombreHoja,
                res.conceptos(), insertadas, 0, insertadas, null, "-",
                res.avisos(), null, false);
    }

    /** La hoja de inventario de equipo dentro de consumibles. */
    private boolean esHojaDeEquipo(String nombreHoja) {
        String t = Normalizador.texto(nombreHoja);
        return t != null && (t.contains("MATERIAL FIJO") || t.contains("EQUIPO"));
    }

    private record Construccion(List<BitacoraRepository.EventoFila> eventos,
                                List<BitacoraRepository.AtributoFila> atributos,
                                List<String> avisos) {
    }

    private Construccion construir(List<Transpositor.FilaTranspuesta> filas, MapeoFamilia mapeo,
                                   Encabezado enc, Hoja hoja, long loteId,
                                   ArchivoInventariado archivo, String nombreHoja, String predio) {
        List<BitacoraRepository.EventoFila> eventos = new ArrayList<>(filas.size());
        List<BitacoraRepository.AtributoFila> atributos = new ArrayList<>(filas.size() * 4);
        List<String> avisos = new ArrayList<>();

        Optional<Encabezado.Columna> colFecha =
                enc.columnaPorGrupo(mapeo.claveFecha().toArray(String[]::new));

        for (Transpositor.FilaTranspuesta f : filas) {
            String nombre = primerValor(f, mapeo.claveNombre());
            LocalDate leida = colFecha
                    .map(c -> hoja.fecha(f.filaExcel() - 1, c.indice()))
                    .orElse(null);
            // El anio capturado puede ser imposible: se vieron 2029, 2626, 2002 y 2023 en
            // archivos de 2026. Se corrige conservando dia y mes, y queda el aviso con el valor
            // original para poder ir a ver la celda.
            LocalDate fecha = Normalizador.anioPlausible(leida, archivo.anio());
            if (leida != null && !leida.equals(fecha)) {
                avisos.add("fila " + f.filaExcel() + " · anio imposible " + leida.getYear()
                        + ", se tomo " + fecha);
            }

            Map<String, String> col = valoresPorDestino(f, mapeo.aColumna());

            // OJO con el ternario: si una rama es int y la otra Integer, Java DESEMPAQUETA la
            // Integer y truena con NullPointerException cuando viene nula. Asi se cayo la hoja
            // MERCURIO de antidoping el 30-sep-2026 -462 renglones, la mas grande de la familia-
            // en el primer renglon que no traia ni fecha legible ni marca en el bloque MES.
            // El defecto llevaba ahi desde el principio y no se habia disparado porque en las
            // cuatro familias de la fase 1 los 46 renglones sin fecha SI tenian marca de mes.
            Integer mes = fecha != null ? Integer.valueOf(fecha.getMonthValue()) : mesDelBloque(f);
            Integer anio = fecha != null ? Integer.valueOf(fecha.getYear())
                    : anioDelBloque(f, archivo.anio());

            // Sin fecha de celda pero con mes del bloque, se arma el dia 1 de ese mes.
            //
            // NO es inventar un dato: el mes y el anio los dice el origen -el bloque MES del
            // renglon y el anio del archivo-, y lo unico sintetico es el dia, que ningun reporte
            // usa. Lo que resuelve es que esos renglones DESAPARECIAN en cuanto la pantalla
            // aplicaba un filtro de fechas, porque toda consulta acota por FECHA y un nulo no
            // entra en un rango. Eran 46 en la carga de la fase 1, y en maternidad serian todos.
            //
            // Queda el aviso "sin fecha legible" del renglon, para que se sepa cual es sintetica.
            if (fecha == null && mes != null && anio != null) {
                fecha = LocalDate.of(anio, mes, 1);
            }

            eventos.add(new BitacoraRepository.EventoFila(
                    loteId, mapeo.familia().name(),
                    fecha,
                    anio,
                    mes,
                    predio,
                    col.get(MapeoFamilia.CUENTA),
                    col.get(MapeoFamilia.AREA),
                    col.get(MapeoFamilia.PUESTO),
                    col.get(MapeoFamilia.AGENCIA),
                    nombre,
                    Normalizador.nombrePersona(nombre),
                    Normalizador.rangoEdad(col.get(MapeoFamilia.RANGO_EDAD)),
                    Normalizador.genero(col.get(MapeoFamilia.GENERO)),
                    null, null, null,
                    col.get(MapeoFamilia.ESTATUS),
                    archivo.nombre(), nombreHoja, f.filaExcel()));

            mapeo.aAtributo().forEach((grupoExcel, nombreAtributo) -> {
                String v = buscarValor(f, grupoExcel);
                if (v != null && !v.isBlank()) {
                    atributos.add(new BitacoraRepository.AtributoFila(
                            f.filaExcel(), nombreAtributo, recortar(v, 300), null, null));
                }
            });

            // Los grupos de campos: cada columna es un dato con su propio tipo. Se lee de la
            // celda y no del texto transpuesto, para que una fecha entre como fecha y los dias
            // como numero. Antes estos cuatro valores se colapsaban en uno solo.
            f.campos().forEach((clave, columna) -> {
                int filaHoja = f.filaExcel() - 1;
                String nombreAtributo = Normalizador.nombreAtributo(clave);
                LocalDate fechaCampo = hoja.fecha(filaHoja, columna);
                if (fechaCampo != null) {
                    atributos.add(new BitacoraRepository.AtributoFila(
                            f.filaExcel(), nombreAtributo, null, null, fechaCampo));
                    return;
                }
                Double num = hoja.numero(filaHoja, columna);
                if (num != null) {
                    atributos.add(new BitacoraRepository.AtributoFila(
                            f.filaExcel(), nombreAtributo, null, num, null));
                    return;
                }
                String texto = hoja.texto(filaHoja, columna);
                if (texto != null && !texto.isBlank()) {
                    atributos.add(new BitacoraRepository.AtributoFila(
                            f.filaExcel(), nombreAtributo, recortar(texto, 300), null, null));
                }
            });

            // Los grupos multimarca: cada marca es su propio renglon de atributo. El nombre lo
            // decide el ruteo del mapeo, para que el veredicto y la sustancia no caigan en la
            // misma bolsa. El esquema lo soporta sin cambios: _ATRIBUTO no tiene unicidad por
            // (EVENTO_ID, NOMBRE), justo para esto.
            mapeo.gruposMultimarca().forEach((grupoExcel, ruteo) -> {
                // Por coincidencia parcial y no por igualdad: el transpositor guarda el nombre del
                // grupo TAL COMO viene en el archivo, y la redaccion cambia entre anios igual que
                // en todo lo demas. Declarar "RESULTADO" tiene que encontrar tambien
                // "RESULTADO DE LA PRUEBA".
                List<String> marcas = marcasDe(f, grupoExcel);
                if (marcas.isEmpty()) {
                    return;
                }
                for (String marca : marcas) {
                    atributos.add(new BitacoraRepository.AtributoFila(
                            f.filaExcel(), ruteo.destinoDe(marca), recortar(marca, 300), null, null));
                }
            });

            if (!f.limpia()) {
                f.avisos().forEach(a -> avisos.add("fila " + f.filaExcel() + " · " + a));
            }
            // El aviso tiene que decir la verdad de CADA caso, porque los dos se arreglan
            // distinto y uno de los dos no es un defecto del archivo.
            if (fecha == null) {
                if (colFecha.isPresent()) {
                    // Hay columna de fecha y la celda no se pudo leer: eso si es un dato malo
                    avisos.add("fila " + f.filaExcel() + " · sin fecha legible");
                } else {
                    // La familia no tiene fecha del evento -maternidad- y el renglon tampoco trae
                    // mes reportable: esta marcado en un bucket sin mes, tipo "ANTES 2025".
                    // No es una celda ilegible, es un caso heredado; decirle "sin fecha legible"
                    // manda a buscar un problema donde no hay ninguno.
                    avisos.add("fila " + f.filaExcel()
                            + " · sin mes reportado (caso heredado); no entra en cortes por fecha");
                }
            }
        }
        return new Construccion(eventos, atributos, avisos);
    }

    /**
     * El anio que dice el bloque MES, cuando la casilla marcada nombra <b>otro</b> anio.
     *
     * <p>El bloque MES no solo tiene los doce meses: en maternidad, incapacidades y
     * accidentabilidad tiene casillas de borde que nombran un anio distinto al del archivo &mdash;
     * {@code ANTES 2025}, {@code 2025 PENDIENTE}, {@code PENDIENTES 2025}, {@code 2027 ENERO},
     * {@code PENDIENTES PARA 2027}&mdash;. Son expedientes que vienen de antes o que siguen al
     * anio siguiente.
     *
     * <p><b>Servicio medico decidio el 30-sep-2026 que esos NO cuentan en el reporte de 2026.</b>
     * Hasta ese dia se les ponia el anio del archivo, asi que un total sin filtro los sumaba como
     * si fueran de 2026: exactamente lo contrario de lo que se pidio. Ahora se les pone el anio
     * que el propio origen escribio en la casilla.
     *
     * <p>El mes se queda nulo aunque la casilla diga {@code 2027 ENERO}: lo que esa casilla
     * significa es "se arrastra a 2027", no "ocurrio en enero de 2027". Sin mes, el renglon no
     * entra en ninguna serie mensual, que es lo correcto.
     *
     * @return el anio de la casilla si nombra uno distinto; si no, el del archivo
     */
    private Integer anioDelBloque(Transpositor.FilaTranspuesta f, Integer anioArchivo) {
        String marca = buscarValor(f, "MES");
        if (marca == null) {
            return anioArchivo;
        }
        var m = java.util.regex.Pattern.compile("(20\\d{2})").matcher(marca);
        if (!m.find()) {
            return anioArchivo;
        }
        int anio = Integer.parseInt(m.group(1));
        return anio == (anioArchivo == null ? -1 : anioArchivo) ? anioArchivo : anio;
    }

    /** El mes del bloque MES, para los archivos donde la fecha no se pudo leer. */
    private Integer mesDelBloque(Transpositor.FilaTranspuesta f) {
        String mes = buscarValor(f, "MES");
        if (mes == null) {
            return null;
        }
        List<String> meses = List.of("ENE", "FEB", "MZO", "ABR", "MYO", "JUN",
                                     "JUL", "AGO", "SEP", "OCT", "NOV", "DIC");
        String m = Normalizador.texto(mes);
        for (int i = 0; i < meses.size(); i++) {
            if (m != null && m.startsWith(meses.get(i).substring(0, 3))) {
                return i + 1;
            }
        }
        return null;
    }

    private Map<String, String> valoresPorDestino(Transpositor.FilaTranspuesta f,
                                                  Map<String, String> mapa) {
        Map<String, String> r = new java.util.HashMap<>();
        mapa.forEach((grupoExcel, destino) -> {
            String v = buscarValor(f, grupoExcel);
            if (v != null && !v.isBlank() && !r.containsKey(destino)) {
                r.put(destino, recortar(v, 160));
            }
        });
        return r;
    }

    /** Las marcas de un grupo multimarca, buscando el grupo por coincidencia parcial. */
    private List<String> marcasDe(Transpositor.FilaTranspuesta f, String grupoBuscado) {
        String buscado = Normalizador.texto(grupoBuscado);
        for (Map.Entry<String, List<String>> e : f.marcas().entrySet()) {
            String g = Normalizador.texto(e.getKey());
            if (g != null && buscado != null && g.contains(buscado)) {
                return e.getValue();
            }
        }
        return List.of();
    }

    /** Busca por coincidencia parcial: los grupos cambian de redaccion entre archivos. */
    private String buscarValor(Transpositor.FilaTranspuesta f, String grupoBuscado) {
        String buscado = Normalizador.texto(grupoBuscado);
        for (Map.Entry<String, String> e : f.valores().entrySet()) {
            String g = Normalizador.texto(e.getKey());
            if (g != null && buscado != null && g.contains(buscado)) {
                return e.getValue();
            }
        }
        return null;
    }

    private String primerValor(Transpositor.FilaTranspuesta f, List<String> candidatos) {
        for (String c : candidatos) {
            String v = buscarValor(f, c);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /**
     * El total que el propio Excel declara en su hoja ACUMULADO. Es el cuadre: se compara
     * contra lo que el cargador conto. Verificado en ENE 2017 de MACRO I, donde el detalle da
     * 103 renglones y el ACUMULADO dice 103.
     */
    private Integer leerAcumulado(Workbook libro, String nombreHoja) {
        return CuadreAcumulado.totalDe(libro, nombreHoja).orElse(null);
    }

    /**
     * TODAS las hojas de detalle del libro, no la primera.
     *
     * <p>Cada una es un predio distinto y su propio lote. El ensayo del 29-sep-2026 mostro por
     * que importa: {@code MKLS - UT - FLORA} tiene las hojas {@code FLORA} (1 renglon),
     * {@code MKLS} (60) y {@code UT} (55). Quedarse con la primera agarraba FLORA, la casi
     * vacia, y el reporte decia "0 renglones" como si el archivo no tuviera datos. Se perdian
     * 115 atenciones sin una sola senal de error.
     *
     * <p>Se descartan las de resumen, grafica y nomenclatura. Lo que queda son hojas de datos,
     * aunque alguna venga casi vacia: eso es legitimo y el reporte lo dira.
     */
    private List<String> hojasDeDetalle(Workbook libro) {
        List<String> hojas = new ArrayList<>();
        for (int i = 0; i < libro.getNumberOfSheets(); i++) {
            String n = libro.getSheetName(i);
            String t = Normalizador.texto(n);
            if (t == null) {
                continue;
            }
            boolean ignorar = HOJAS_IGNORADAS.stream().anyMatch(t::contains)
                    || HOJAS_MES.contains(t)
                    || t.matches("G[FR]?\\w{0,3}\\d*");
            if (!ignorar) {
                hojas.add(n);
            }
        }
        return hojas;
    }

    private String resumirAvisos(List<String> avisos) {
        if (avisos.isEmpty()) {
            return null;
        }
        String todo = String.join("\n", avisos);
        return todo.length() > 30000 ? todo.substring(0, 30000) + "\n... (truncado)" : todo;
    }

    private static String recortar(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }

    private Resultado error(ArchivoInventariado a, String error) {
        return new Resultado(a.nombre(), a.carpetaPredio(), a.familia().name(), "-",
                0, 0, 0, 0, null, "-", List.of(), error, false);
    }

    private Resultado saltado(ArchivoInventariado a, String razon) {
        return new Resultado(a.nombre(), a.carpetaPredio(), a.familia().name(), "-",
                0, 0, 0, 0, null, "-", List.of(), razon, true);
    }
}
