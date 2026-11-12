package com.onest.app.importer.nss;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carga de los layouts por NSS (paso 4 del plan, docs/plan-carga-excel-layout.html).
 *
 * <p>Por archivo: reconoce el layout por sus encabezados, valida cada renglon, completa los datos
 * del empleado desde el biometrico, escribe un CSV con los renglones rechazados (para regresarlo
 * al predio) y, en modo cargar, lo guarda en SERV_MED_XLS_* en una sola transaccion.
 *
 * <p>Sin conexion (modo validar sin destino) hace todo menos el cruce con el biometrico y la
 * escritura: sirve para revisar los archivos antes de tocar la base.
 */
public class CargaNssService {

    private static final Logger log = LoggerFactory.getLogger(CargaNssService.class);

    /** NSS y fecha del renglon de ejemplo que trae la plantilla: si alguien lo deja, no se carga. */
    private static final String NSS_EJEMPLO = "30048315698";
    private static final LocalDate FECHA_EJEMPLO = LocalDate.of(2026, 3, 15);
    private static final int MAX_ERRORES_EN_LOTE = 500;
    /** Filas con texto (notas) que puede haber arriba del encabezado antes de darlo por no reconocido. */
    private static final int MAX_FILAS_ANTES_DEL_ENCABEZADO = 5;
    /** Debajo de esto (2 o mas ceros a la izquierda) el NSS es de relleno: 00000000001, 00000000002... */
    private static final long NSS_MINIMO_REAL = 1_000_000_000L;
    /** Marca de los avisos que piden accion antes de cargar: la pantalla los separa de los informativos. */
    public static final String REVISAR = "[revisar] ";
    /** Separa el texto del aviso de su lista de renglones (la pantalla la pone en un desplegable). */
    public static final String RENGLONES = "\nRenglones: ";
    /**
     * Motivo del aviso de un NSS que no esta en el biometrico pero entra marcado (EN_BIOMETRICO = 'N').
     * Regla del 3-nov-2026: cuenta en los totales, sin datos de la persona.
     */
    public static final String SIN_BIOMETRICO = "no existe en el biométrico: entra marcado";

    /**
     * Lo que paso con un archivo. {@code avisosFila} = renglones que SI entran pero hay que revisar
     * (la pantalla los lista y los pone en el Excel de errores con TIPO = AVISO).
     */
    public record ResultadoArchivo(Path archivo, LayoutNss layout, String motivoRechazo, int leidas,
                                   int validas, List<ValidadorNss.Error> errores, List<String> avisos,
                                   List<ValidadorNss.Error> avisosFila, Path csvErrores,
                                   XlsNssRepository.Escritura escritura, boolean omitido,
                                   List<XlsNssRepository.Reemplazo> reemplazos, Map<Integer, String> bajas) {
    }

    private final BiometricoNss biometrico;      // null = sin conexion
    private final XlsNssRepository repo;         // null = no se escribe
    private final Path salida;
    private final CorreccionesNss correcciones;  // null = sin Excel de NSS corregidos
    private LayoutNss soloLayout;                // null = se reconoce solo (consola); pantalla: el que eligio el usuario

    public CargaNssService(BiometricoNss biometrico, XlsNssRepository repo, Path salida) {
        this(biometrico, repo, salida, null);
    }

    public CargaNssService(BiometricoNss biometrico, XlsNssRepository repo, Path salida, CorreccionesNss correcciones) {
        this.biometrico = biometrico;
        this.repo = repo;
        this.salida = salida;
        this.correcciones = correcciones;
    }

    /**
     * La pantalla obliga a elegir el tipo de archivo: si los encabezados son de otro layout (o de
     * ninguno) se rechaza sin validar renglones, para no cargar un archivo como si fuera de otro tipo.
     */
    public CargaNssService soloLayout(LayoutNss layout) {
        this.soloLayout = layout;
        return this;
    }

    /** Todos los .xlsx de la carpeta (y subcarpetas), sin los temporales de Excel (~$...). */
    public static List<Path> archivos(Path raiz) throws IOException {
        try (Stream<Path> s = Files.walk(raiz)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xlsx"))
                    .filter(p -> !p.getFileName().toString().startsWith("~$"))
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains(".errores"))
                    .sorted().toList();
        }
    }

    public ResultadoArchivo procesar(Path archivo, boolean escribir, boolean forzar) {
        return procesar(archivo, escribir, forzar, null);
    }

    /** @param usuario quien carga desde la pantalla (SERV_MED_XLS_LOTE.CREATED_BY); null = la consola */
    public ResultadoArchivo procesar(Path archivo, boolean escribir, boolean forzar, String usuario) {
        log.info("[nss] {}", archivo.getFileName());
        List<String> avisos = new ArrayList<>();
        List<ValidadorNss.Registro> registros = new ArrayList<>();
        List<ValidadorNss.Error> errores = new ArrayList<>();
        // Renglones que SI entran pero el predio debe revisar (hoy: costo negativo de accidentes)
        List<ValidadorNss.Error> avisosFila = new ArrayList<>();
        ValidadorNss[] validador = new ValidadorNss[1];
        LayoutNss[] reconocido = new LayoutNss[1];   // layout del archivo cuando no es el elegido
        List<String>[] encabezados = new List[1];
        int[] leidas = {0};
        Set<Integer> corregidas = new java.util.HashSet<>();
        Map<Integer, String> nssPorFila = new java.util.HashMap<>();   // para la columna Baja de la pantalla
        List<List<String>> notas = new ArrayList<>();                  // filas con texto antes del encabezado
        int[] encabezadoRepetido = {0};
        Map<Integer, String> predioPorFila = new java.util.HashMap<>(); // para decir en que predios hay columnas cruzadas

        try {
            LectorXlsx.leer(archivo, null, fila -> {
                if (encabezados[0] == null) {
                    if (fila.vacia()) {
                        return;              // renglones en blanco antes del encabezado
                    }
                    // SM deja notas arriba del encabezado ("1 ATENCIONES NOV", 3-nov-2026: nota en la fila 1,
                    // encabezado en la 2). Se busca el encabezado en las primeras filas con texto.
                    ValidadorNss v = ValidadorNss.paraEncabezados(fila.valores()).orElse(null);
                    if (v == null && notas.size() < MAX_FILAS_ANTES_DEL_ENCABEZADO) {
                        notas.add(fila.valores());
                        return;
                    }
                    encabezados[0] = v != null || notas.isEmpty() ? fila.valores() : notas.get(0);
                    if (v != null && !notas.isEmpty()) {
                        avisos.add("El encabezado está en la fila " + fila.numero() + ": se ignoraron " + notas.size()
                                + " fila(s) de arriba (notas). Qué hacer: nada.");
                    }
                    validador[0] = v;
                    if (soloLayout != null && validador[0] != null && validador[0].layout() != soloLayout) {
                        reconocido[0] = validador[0].layout();
                        validador[0] = null;     // es de otro tipo: no se validan sus renglones
                    }
                    return;
                }
                if (validador[0] == null || fila.vacia()) {
                    return;
                }
                // El encabezado otra vez (archivos pegados uno debajo de otro, o dos filas de encabezado)
                if (mismoEncabezado(fila.valores(), encabezados[0])) {
                    encabezadoRepetido[0]++;
                    return;
                }
                // El renglon 2 de la plantilla es de ejemplo (NSS 30048315698). Se reconoce antes de
                // validar: el de incapacidades, por ejemplo, ni siquiera es valido (sin folio).
                if (fila.numero() == 2 && fila.valores().contains(NSS_EJEMPLO)) {
                    avisos.add("renglon 2: es el renglon de ejemplo de la plantilla; no se carga");
                    return;
                }
                leidas[0]++;
                LectorXlsx.Fila ajustada = ajustarNss(fila, validador[0], errores, avisosFila, corregidas);
                if (ajustada == null) {
                    return;              // la correccion no cuadra con el renglon: ya quedo el error
                }
                int iPredio = validador[0].indice("PREDIO");
                if (iPredio >= 0 && ajustada.valor(iPredio) != null) {
                    predioPorFila.put(fila.numero(), ajustada.valor(iPredio).trim());
                }
                int iNss = validador[0].indice("NSS");
                if (iNss >= 0 && ajustada.valor(iNss) != null) {
                    try {
                        nssPorFila.put(fila.numero(), ValidadorNss.nss(ajustada.valor(iNss).trim()));
                    } catch (IllegalArgumentException mal) {
                        // NSS mal escrito: ya sale como error del renglon, no hay baja que buscar
                    }
                }
                ValidadorNss.Resultado r = validador[0].validar(ajustada);
                if (r.valido()) {
                    // Por NSS + fecha del ejemplo; en consumibles (sin NSS) basta el renglon 2 con la
                    // fecha del ejemplo: la plantilla de material fijo lo trae completo y pasaba como dato.
                    boolean sinNss = !validador[0].layout().empleado() && r.registro().texto("NSS") == null;
                    if (FECHA_EJEMPLO.equals(r.registro().fecha("FECHA"))
                            && (NSS_EJEMPLO.equals(r.registro().texto("NSS")) || (sinNss && fila.numero() == 2))) {
                        avisos.add("renglon " + fila.numero() + ": es el renglon de ejemplo de la plantilla; no se carga");
                        leidas[0]--;
                        return;
                    }
                    registros.add(r.registro());
                    avisosFila.addAll(r.avisos());
                } else {
                    errores.addAll(r.errores());
                }
            });
        } catch (Exception ex) {
            return rechazo(archivo, null, "no se pudo leer: " + ex.getMessage());
        }

        if (encabezados[0] == null && !notas.isEmpty()) {
            encabezados[0] = notas.get(0);   // ninguna fila parecio encabezado: el rechazo dice que falta
        }
        if (encabezados[0] == null) {
            return rechazo(archivo, null, "la primera hoja esta vacia");
        }
        if (validador[0] == null && soloLayout != null) {
            return rechazo(archivo, null, reconocido[0] != null
                    ? "Elegiste " + soloLayout.hoja() + ", pero el archivo es de " + reconocido[0].hoja()
                      + ". Qué hacer: revisa que subiste el archivo correcto o elige el tipo " + reconocido[0].hoja() + "."
                    : "El archivo no tiene las columnas de " + soloLayout.hoja() + ". Faltan: "
                      + String.join(", ", ValidadorNss.faltantes(encabezados[0], soloLayout))
                      + ". Qué hacer: revisa que subiste el archivo correcto y que los encabezados sean los de la plantilla.");
        }
        if (validador[0] == null) {
            return rechazo(archivo, null, "los encabezados no corresponden a ningun layout:"
                    + ValidadorNss.faltantes(encabezados[0]));
        }
        LayoutNss layout = validador[0].layout();
        if (encabezadoRepetido[0] > 0) {
            avisos.add("El encabezado viene repetido " + encabezadoRepetido[0] + " vez(es) más abajo: esas filas se "
                    + "ignoraron. Qué hacer: nada.");
        }
        columnasCruzadas(validador[0], errores, predioPorFila, avisos);
        List<String> ignoradas = validador[0].ignoradas(encabezados[0]);
        if (!ignoradas.isEmpty()) {
            avisos.add("El archivo trae columnas que no son del layout " + layout.hoja() + " y no se guardan: "
                    + String.join(", ", ignoradas) + ". Qué hacer: nada, salvo que esa información deba cargarse.");
        }
        if (correcciones != null) {
            avisos.add(corregidas.size() + " NSS tomados del Excel de correcciones (" + correcciones.archivo().getFileName()
                    + "). Qué hacer: nada.");
            correcciones.todas().stream().filter(c -> !corregidas.contains(c.fila()) && errores.stream()
                    .noneMatch(e -> e.fila() == c.fila())).forEach(c -> avisos.add(REVISAR + "La corrección del renglón "
                    + c.fila() + " no se aplicó: ese renglón no existe o no es de este layout. Qué hacer: revisa el número de renglón en el Excel de correcciones."));
        }
        if (layout.aceptaNssCeros()) {
            long ceros = registros.stream().filter(CargaNssService::sinPersona).count();
            if (ceros > 0) {
                avisos.add(ceros + " renglones sin NSS (" + ValidadorNss.NSS_CEROS + "): se cargan y cuentan en los totales, "
                        + "pero no aparecen en el expediente de nadie. Qué hacer: nada; si deben contar por persona, "
                        + "pide el NSS al predio o ponlo en el Excel de correcciones.");
            }
        }

        // Datos del empleado desde el biometrico (solo layouts de empleados)
        if (layout.empleado()) {
            // 5-nov-2026 (decision del usuario): ya no se detectan NSS "arrastrados" en Excel. Si no estan en el
            // biometrico entran marcados con el resumen de siempre; corregirlos es responsabilidad de quien genera el archivo.
            if (biometrico == null) {
                avisos.add("sin conexion: no se verifico que los NSS existan en el biometrico");
            } else {
                completarDelBiometrico(layout, registros, errores, avisosFila);
                resumirBiometrico(registros, avisos);
            }
        }

        // Un aviso solo cuenta si su renglon sigue entrando: el biometrico pudo rechazarlo despues
        Set<Integer> filasValidas = new java.util.HashSet<>();
        registros.forEach(r -> filasValidas.add(r.fila()));
        avisosFila.removeIf(a -> !filasValidas.contains(a.fila()));

        Map<Integer, String> bajas = bajasDeLoListado(layout, errores, avisosFila, nssPorFila);

        Path csv = escribirErrores(archivo, errores, avisosFila);
        if (errores.size() > 0) {
            avisos.add(REVISAR + errores.stream().map(ValidadorNss.Error::fila).distinct().count()
                    + " renglones NO se van a cargar (en la tabla: «No entra»). Qué hacer: corrígelos en tu archivo "
                    + "y vuelve a validar; si cargas así, esos renglones se quedan fuera."
                    + RENGLONES + rangos(errores.stream().map(ValidadorNss.Error::fila).toList()));
        }
        // Un aviso por motivo (costo negativo, interna sin area/ramo, positivo sin sustancia...): antes
        // todos se anunciaban como "costo negativo", aunque fueran de antidoping (visto el 14-oct-2026)
        avisosFila.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.encabezado() + ": " + a.motivo().split(";")[0],
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .forEach((motivo, n) -> avisos.add(motivo.contains(": " + ValidadorNss.ENTRO_COMO)
                        ? n + " renglones entraron sin problema. " + motivo + ". Qué hacer: nada."
                        : motivo.contains(": " + SIN_BIOMETRICO)
                        ? n + " renglones traen un NSS que no existe en el biométrico: se cargan marcados y cuentan en "
                        + "los totales, pero sin nombre, género, edad, cuenta ni puesto, y no aparecen en el expediente "
                        + "de nadie. Qué hacer: si el NSS está mal, corrígelo en tu archivo y vuelve a cargar el mes; "
                        + "si está bien, nada (pestaña «Entran con ajuste»)."
                        : n + " renglones se cargan con un ajuste en " + motivo
                        + ". Qué hacer: revisa que el ajuste sea correcto (pestaña «Entran con ajuste»); "
                        + "si no, corrígelo en tu archivo y vuelve a validar."));

        // Antes de escribir: lo que el reemplazo por predio y mes va a sustituir (para avisar en pantalla)
        List<XlsNssRepository.Reemplazo> reemplazos = repo == null || registros.isEmpty()
                ? List.of() : repo.reemplazos(layout, registros);
        XlsNssRepository.Escritura escritura = null;
        boolean omitido = false;
        if (escribir) {
            if (registros.isEmpty()) {
                avisos.add("ningun renglon valido: no se escribio nada");
            } else {
                String checksum = checksum(archivo);
                Long previo = repo.loteConMismoContenido(layout, checksum);
                if (previo != null && !forzar) {
                    avisos.add("este mismo archivo ya se cargo (lote " + previo + "); se omite. "
                            + "Para recargarlo: «Cargar de nuevo»");
                    omitido = true;
                } else {
                    // FILAS_ERROR = renglones rechazados, no cantidad de errores: un renglon puede
                    // traer varios (lote 2 del 10-oct decia 4,800 para 3,926 renglones)
                    int renglonesConError = (int) errores.stream().map(ValidadorNss.Error::fila).distinct().count();
                    // Desde la pantalla el archivo vive en una carpeta temporal: se guarda solo su nombre
                    escritura = repo.escribir(layout, usuario == null ? archivo.toString()
                                    : archivo.getFileName().toString(), checksum, registros,
                            leidas[0], renglonesConError, reporteErrores(errores, avisosFila), usuario);
                    if (escritura.avisoDuplicados() != null) {
                        avisos.add(escritura.avisoDuplicados());
                    }
                }
            }
        }
        return new ResultadoArchivo(archivo, layout, null, leidas[0], registros.size(), errores, avisos,
                avisosFila, errores.isEmpty() && avisosFila.isEmpty() ? null : csv, escritura, omitido, reemplazos, bajas);
    }

    /**
     * Fecha y tipo de baja (BIO_EMPLEADO.FECHA_BAJA / TIPOBAJA) de los renglones que la pantalla lista
     * (errores y avisos), por numero de renglon. Solo informativo: no filtra ni rechaza nada (3-nov-2026).
     */
    private Map<Integer, String> bajasDeLoListado(LayoutNss layout, List<ValidadorNss.Error> errores,
                                                  List<ValidadorNss.Error> avisosFila, Map<Integer, String> nssPorFila) {
        if (biometrico == null || !layout.empleado()) {
            return Map.of();
        }
        Set<Integer> filas = new java.util.TreeSet<>();
        errores.forEach(e -> filas.add(e.fila()));
        avisosFila.forEach(a -> filas.add(a.fila()));
        Set<String> nss = new LinkedHashSet<>();
        filas.forEach(f -> {
            String n = nssPorFila.get(f);
            if (n != null && !ValidadorNss.NSS_CEROS.equals(n)) {
                nss.add(n);
            }
        });
        Map<String, BiometricoNss.Empleado> empleados = biometrico.buscar(nss);
        java.time.format.DateTimeFormatter ddmmaaaa = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Map<Integer, String> res = new java.util.HashMap<>();
        for (Integer f : filas) {
            BiometricoNss.Empleado e = nssPorFila.get(f) == null ? null : empleados.get(nssPorFila.get(f));
            if (e != null && e.fechaBaja() != null) {
                res.put(f, e.fechaBaja().format(ddmmaaaa) + (e.tipoBaja().isEmpty() ? "" : " " + e.tipoBaja()));
            }
        }
        return res;
    }

    /**
     * Agrega NOMBRE, GENERO, EDAD, CUENTA y PUESTO congelados a la fecha de carga, y EN_BIOMETRICO.
     * Un NSS que no esta en el biometrico entra marcado EN_BIOMETRICO = 'N' (regla del 3-nov-2026:
     * la atencion ocurrio y debe contar), salvo los NSS de relleno (00000000001...), que se rechazan.
     */
    private void completarDelBiometrico(LayoutNss layout, List<ValidadorNss.Registro> registros,
                                        List<ValidadorNss.Error> errores, List<ValidadorNss.Error> avisosFila) {
        // Sin persona: no hay a quien buscar; nombre, genero, edad, cuenta y puesto se quedan vacios
        boolean ceros = layout.aceptaNssCeros();
        Set<String> buscados = new LinkedHashSet<>();
        registros.stream().filter(r -> !(ceros && sinPersona(r))).forEach(r -> buscados.add(r.texto("NSS")));
        Map<String, BiometricoNss.Empleado> empleados = biometrico.buscar(buscados);
        registros.removeIf(r -> {
            if (ceros && sinPersona(r)) {
                return false;
            }
            BiometricoNss.Empleado e = empleados.get(r.texto("NSS"));
            if (e == null) {
                String nss = r.texto("NSS");
                if (Long.parseLong(nss) < NSS_MINIMO_REAL) {
                    errores.add(new ValidadorNss.Error(r.fila(), "NSS", nss, "no existe en el biometrico"));
                    return true;
                }
                r.valores().put("EN_BIOMETRICO", "N");
                avisosFila.add(new ValidadorNss.Error(r.fila(), "NSS", nss, SIN_BIOMETRICO
                        + " (sin nombre, género, edad, cuenta ni puesto)"));
                return false;
            }
            if (e.dePrueba()) {
                // 4-nov-2026 (decision del usuario): entra igual, es responsabilidad de quien manda el archivo.
                // Antes (10-oct) se rechazaba como si no existiera; ahora solo se avisa en "Entran con ajuste".
                avisosFila.add(new ValidadorNss.Error(r.fila(), "NSS", r.texto("NSS"),
                        "es un registro de prueba en el biometrico (EMP_STATUS 99); entra como lo manda el archivo"));
            }
            r.valores().put("EN_BIOMETRICO", "S");
            r.valores().put("NOMBRE", e.nombre());
            // 12-nov-2026: si el layout trae genero, edad o cuenta (atenciones), GANA el Excel; el biometrico
            // solo llena lo que el archivo no trae
            r.valores().putIfAbsent("GENERO", e.genero());
            r.valores().putIfAbsent("EDAD", e.edadAl(r.fecha("FECHA")));
            r.valores().putIfAbsent("CUENTA", e.cuenta());
            r.valores().put("PUESTO", e.puesto());
            return false;
        });
        errores.sort(java.util.Comparator.comparingInt(ValidadorNss.Error::fila));
    }

    /**
     * Resumen de lo que trajo el biometrico, para poder revisarlo sin abrir la base: cuantos
     * renglones quedaron sin cuenta, puesto, genero o edad, y una muestra de los primeros.
     */
    private static void resumirBiometrico(List<ValidadorNss.Registro> todos, List<String> avisos) {
        // Los de NSS en ceros no se buscaron: contarlos aqui inflaria "sin genero", "sin edad"...
        // Ni los que no estan en el biometrico: esos ya tienen su propio aviso
        List<ValidadorNss.Registro> registros = todos.stream()
                .filter(r -> !sinPersona(r) && !"N".equals(r.valores().get("EN_BIOMETRICO"))).toList();
        if (registros.isEmpty()) {
            return;
        }
        long sinCuenta = registros.stream().filter(r -> r.valores().get("CUENTA") == null).count();
        long sinPuesto = registros.stream().filter(r -> r.valores().get("PUESTO") == null).count();
        long sinGenero = registros.stream().filter(r -> r.valores().get("GENERO") == null).count();
        long sinEdad = registros.stream().filter(r -> r.valores().get("EDAD") == null).count();
        avisos.add(registros.size() + " renglones completados con los datos del empleado del biométrico. Al biométrico le falta: "
                + "cuenta en " + sinCuenta + ", puesto en " + sinPuesto + ", género en " + sinGenero + " y edad en " + sinEdad
                + ". Qué hacer: nada, se cargan igual.");
    }

    /** La fila trae los mismos encabezados que el encabezado del archivo (sin importar mayusculas ni espacios). */
    private static boolean mismoEncabezado(List<String> fila, List<String> encabezado) {
        List<String> a = fila.stream().filter(v -> v != null && !v.isBlank()).map(CatalogosNss::clave).toList();
        List<String> b = encabezado.stream().filter(v -> v != null && !v.isBlank()).map(CatalogosNss::clave).toList();
        return !a.isEmpty() && a.equals(b);
    }

    /**
     * Columnas cruzadas (3-nov-2026, "1 ATENCIONES NOV": bajo FECHA venia el area y bajo AREA la fecha en
     * 13,292 renglones). Sin esto la pantalla solo dice "fecha invalida" y "area fuera de catalogo" en cada
     * renglon. Un par cuenta si, en el mismo renglon, el valor de cada columna seria valido en la otra.
     * Solo avisa: los renglones siguen sin entrar.
     */
    private static void columnasCruzadas(ValidadorNss validador, List<ValidadorNss.Error> errores,
                                         Map<Integer, String> predioPorFila, List<String> avisos) {
        Map<Integer, List<ValidadorNss.Error>> porFila = new java.util.TreeMap<>();
        errores.forEach(e -> porFila.computeIfAbsent(e.fila(), k -> new ArrayList<>()).add(e));
        Map<String, Set<Integer>> pares = new java.util.LinkedHashMap<>();
        porFila.forEach((fila, es) -> {
            for (int i = 0; i < es.size(); i++) {
                for (int j = i + 1; j < es.size(); j++) {
                    ValidadorNss.Error a = es.get(i), b = es.get(j);
                    if (a.valor() == null || a.valor().isBlank() || b.valor() == null || b.valor().isBlank()) {
                        continue;
                    }
                    if (validador.valeComo(b.encabezado(), a.valor()) && validador.valeComo(a.encabezado(), b.valor())) {
                        pares.computeIfAbsent(a.encabezado() + " y " + b.encabezado(), k -> new java.util.TreeSet<>()).add(fila);
                    }
                }
            }
        });
        pares.forEach((par, filas) -> {
            Set<String> predios = new java.util.TreeSet<>();
            filas.forEach(f -> predios.add(predioPorFila.getOrDefault(f, "(sin predio)")));
            avisos.add(REVISAR + "Las columnas " + par + " vienen cruzadas en " + String.format("%,d", filas.size())
                    + " renglones (predios: " + String.join(", ", predios) + "): lo de una columna está en la otra. "
                    + "Qué hacer: intercambia el contenido de esas dos columnas en tu archivo y vuelve a validar."
                    + RENGLONES + rangos(filas));
        });
    }

    /** Renglones como rangos: 3-480, 482, 490-512 (para el aviso; la pantalla los muestra con un boton). */
    static String rangos(java.util.Collection<Integer> filas) {
        List<Integer> f = filas.stream().distinct().sorted().toList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < f.size(); i++) {
            int desde = f.get(i);
            while (i + 1 < f.size() && f.get(i + 1) == f.get(i) + 1) {
                i++;
            }
            sb.append(sb.length() == 0 ? "" : ", ").append(desde == f.get(i) ? String.valueOf(desde) : desde + "-" + f.get(i));
        }
        return sb.toString();
    }

    private static boolean sinPersona(ValidadorNss.Registro r) {
        return ValidadorNss.NSS_CEROS.equals(r.texto("NSS"));
    }

    /**
     * El NSS del renglon antes de validarlo:
     * <ul>
     *   <li>si el Excel de correcciones trae ese renglon, se pone su NSS. Solo sobre un renglon que
     *       llego sin NSS y con el mismo PREDIO y FECHA; si no, el renglon se rechaza (regresa null)
     *       para no darle a una atencion el NSS de otra persona;</li>
     *   <li>"(en blanco)" en un layout que acepta ceros (decision 30-oct-2026): es como Excel muestra
     *       una celda vacia al filtrar, y se pego asi; entra como 00000000000, sin persona.</li>
     * </ul>
     */
    private LectorXlsx.Fila ajustarNss(LectorXlsx.Fila fila, ValidadorNss validador, List<ValidadorNss.Error> errores,
                                       List<ValidadorNss.Error> avisosFila, Set<Integer> corregidas) {
        int iNss = validador.indice("NSS");
        if (iNss < 0) {
            return fila;
        }
        String crudo = fila.valor(iNss) == null ? "" : fila.valor(iNss).trim();
        boolean enBlanco = "(EN BLANCO)".equals(CatalogosNss.clave(crudo));
        boolean sinNss = crudo.isEmpty() || enBlanco || crudo.matches("0+(\\.0+)?");
        CorreccionesNss.Correccion c = correcciones == null ? null : correcciones.para(fila.numero()).orElse(null);
        if (c != null) {
            String motivo = null;
            if (!sinNss) {
                motivo = "el Excel de correcciones trae NSS " + c.nss() + " pero el renglon ya tiene NSS";
            } else if (c.predio() != null && !CatalogosNss.clave(c.predio())
                    .equals(CatalogosNss.clave(String.valueOf(fila.valor(validador.indice("PREDIO")))))) {
                motivo = "la correccion es de PREDIO " + c.predio() + ": no es este renglon";
            } else if (c.fecha() != null && !c.fecha().equals(fechaO(fila.valor(validador.indice("FECHA"))))) {
                motivo = "la correccion es de FECHA " + c.fecha() + ": no es este renglon";
            }
            if (motivo != null) {
                errores.add(new ValidadorNss.Error(fila.numero(), "NSS", crudo, motivo));
                return null;
            }
            corregidas.add(fila.numero());
            avisosFila.add(new ValidadorNss.Error(fila.numero(), "NSS", crudo.isEmpty() ? "(vacio)" : crudo,
                    "se tomo " + c.nss() + " de " + correcciones.archivo().getFileName()));
            return conValor(fila, iNss, c.nss());
        }
        if (enBlanco && validador.layout().aceptaNssCeros()) {
            avisosFila.add(new ValidadorNss.Error(fila.numero(), "NSS", crudo,
                    "entra como " + ValidadorNss.NSS_CEROS + " (sin persona)"));
            return conValor(fila, iNss, ValidadorNss.NSS_CEROS);
        }
        return fila;
    }

    private static LocalDate fechaO(String v) {
        try {
            return v == null ? null : ValidadorNss.fecha(v.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static LectorXlsx.Fila conValor(LectorXlsx.Fila fila, int indice, String valor) {
        List<String> v = new ArrayList<>(fila.valores());
        while (v.size() <= indice) {
            v.add(null);
        }
        v.set(indice, valor);
        return new LectorXlsx.Fila(fila.numero(), v);
    }

    /**
     * CSV para el predio: renglon, tipo, columna, lo que traia y el motivo. ERROR = el renglon no
     * entro y hay que corregirlo; AVISO = si entro, pero hay que revisarlo. Con BOM para Excel.
     */
    private Path escribirErrores(Path archivo, List<ValidadorNss.Error> errores, List<ValidadorNss.Error> avisosFila) {
        String nombre = archivo.getFileName().toString().replaceFirst("(?i)\\.xlsx$", "") + ".errores.csv";
        Path destino = (salida != null ? salida : archivo.getParent()).resolve(nombre);
        if (errores.isEmpty() && avisosFila.isEmpty()) {
            try {
                Files.deleteIfExists(destino);   // de una corrida anterior que si tenia errores
            } catch (IOException ignorada) {
                // no estorba
            }
            return destino;
        }
        List<String[]> lineas = new ArrayList<>();
        errores.forEach(e -> lineas.add(linea(e, "ERROR")));
        avisosFila.forEach(e -> lineas.add(linea(e, "AVISO")));
        lineas.sort(java.util.Comparator.comparingInt(l -> Integer.parseInt(l[0])));
        StringBuilder sb = new StringBuilder("﻿RENGLON;TIPO;COLUMNA;VALOR;MOTIVO\n");
        for (String[] l : lineas) {
            sb.append(String.join(";", l)).append('\n');
        }
        try {
            Files.writeString(destino, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.warn("[nss] no se pudo escribir {}: {}", destino, ex.getMessage());
        }
        return destino;
    }

    private static String[] linea(ValidadorNss.Error e, String tipo) {
        return new String[]{String.valueOf(e.fila()), tipo, csv(e.encabezado()), csv(e.valor()), csv(e.motivo())};
    }

    private static String csv(String s) {
        String t = s == null ? "" : s;
        return t.contains(";") || t.contains("\"") || t.contains("\n") ? "\"" + t.replace("\"", "\"\"") + "\"" : t;
    }

    /**
     * Lo que queda en SERV_MED_XLS_LOTE.REPORTE (los primeros, para no hacer un CLOB enorme): los
     * errores y, aparte, los avisos de renglones que si entraron.
     */
    private static String reporteErrores(List<ValidadorNss.Error> errores, List<ValidadorNss.Error> avisosFila) {
        StringBuilder sb = new StringBuilder();
        errores.stream().limit(MAX_ERRORES_EN_LOTE).forEach(e -> sb.append("renglon ").append(e.fila())
                .append(" | ").append(e.encabezado()).append(" | ").append(e.valor()).append(" | ")
                .append(e.motivo()).append('\n'));
        if (errores.size() > MAX_ERRORES_EN_LOTE) {
            sb.append("... y ").append(errores.size() - MAX_ERRORES_EN_LOTE).append(" mas (ver el CSV)\n");
        }
        avisosFila.stream().limit(MAX_ERRORES_EN_LOTE).forEach(e -> sb.append("AVISO renglon ").append(e.fila())
                .append(" | ").append(e.encabezado()).append(" | ").append(e.valor()).append(" | ")
                .append(e.motivo()).append('\n'));
        if (avisosFila.size() > MAX_ERRORES_EN_LOTE) {
            sb.append("... y ").append(avisosFila.size() - MAX_ERRORES_EN_LOTE).append(" avisos mas (ver el CSV)\n");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String checksum(Path archivo) {
        try (InputStream in = Files.newInputStream(archivo)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("no se pudo calcular el checksum de " + archivo, ex);
        }
    }

    private static ResultadoArchivo rechazo(Path archivo, LayoutNss layout, String motivo) {
        log.warn("[nss] {} rechazado: {}", archivo.getFileName(), motivo);
        return new ResultadoArchivo(archivo, layout, motivo, 0, 0, List.of(), List.of(), List.of(), null, null, false, List.of(), Map.of());
    }
}
