package com.onest.excel.nss;

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

    /** Lo que paso con un archivo. */
    public record ResultadoArchivo(Path archivo, LayoutNss layout, String motivoRechazo, int leidas,
                                   int validas, List<ValidadorNss.Error> errores, List<String> avisos,
                                   Path csvErrores, XlsNssRepository.Escritura escritura, boolean omitido) {
    }

    private final BiometricoNss biometrico;      // null = sin conexion
    private final XlsNssRepository repo;         // null = no se escribe
    private final Path salida;

    public CargaNssService(BiometricoNss biometrico, XlsNssRepository repo, Path salida) {
        this.biometrico = biometrico;
        this.repo = repo;
        this.salida = salida;
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
        log.info("[nss] {}", archivo.getFileName());
        List<String> avisos = new ArrayList<>();
        List<ValidadorNss.Registro> registros = new ArrayList<>();
        List<ValidadorNss.Error> errores = new ArrayList<>();
        // Renglones que SI entran pero el predio debe revisar (hoy: costo negativo de accidentes)
        List<ValidadorNss.Error> avisosFila = new ArrayList<>();
        ValidadorNss[] validador = new ValidadorNss[1];
        List<String>[] encabezados = new List[1];
        int[] leidas = {0};

        try {
            LectorXlsx.leer(archivo, null, fila -> {
                if (encabezados[0] == null) {
                    if (fila.vacia()) {
                        return;              // renglones en blanco antes del encabezado
                    }
                    encabezados[0] = fila.valores();
                    validador[0] = ValidadorNss.paraEncabezados(fila.valores()).orElse(null);
                    return;
                }
                if (validador[0] == null || fila.vacia()) {
                    return;
                }
                // El renglon 2 de la plantilla es de ejemplo (NSS 30048315698). Se reconoce antes de
                // validar: el de incapacidades, por ejemplo, ni siquiera es valido (sin folio).
                if (fila.numero() == 2 && fila.valores().contains(NSS_EJEMPLO)) {
                    avisos.add("renglon 2: es el renglon de ejemplo de la plantilla; no se carga");
                    return;
                }
                leidas[0]++;
                ValidadorNss.Resultado r = validador[0].validar(fila);
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

        if (encabezados[0] == null) {
            return rechazo(archivo, null, "la primera hoja esta vacia");
        }
        if (validador[0] == null) {
            return rechazo(archivo, null, "los encabezados no corresponden a ningun layout:"
                    + ValidadorNss.faltantes(encabezados[0]));
        }
        LayoutNss layout = validador[0].layout();

        // Datos del empleado desde el biometrico (solo layouts de empleados)
        if (layout.empleado()) {
            if (biometrico == null) {
                avisos.add("sin conexion: no se verifico que los NSS existan en el biometrico");
            } else {
                completarDelBiometrico(registros, errores);
                resumirBiometrico(registros, avisos);
            }
        }

        // Un aviso solo cuenta si su renglon sigue entrando: el biometrico pudo rechazarlo despues
        Set<Integer> filasValidas = new java.util.HashSet<>();
        registros.forEach(r -> filasValidas.add(r.fila()));
        avisosFila.removeIf(a -> !filasValidas.contains(a.fila()));

        Path csv = escribirErrores(archivo, errores, avisosFila);
        if (errores.size() > 0) {
            avisos.add(errores.size() + " error(es) en " + errores.stream().map(ValidadorNss.Error::fila)
                    .distinct().count() + " renglon(es): ver " + csv.getFileName());
        }
        // Un aviso por motivo (costo negativo, interna sin area/ramo, positivo sin sustancia...): antes
        // todos se anunciaban como "costo negativo", aunque fueran de antidoping (visto el 14-oct-2026)
        avisosFila.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.encabezado() + ": " + a.motivo().split(";")[0],
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .forEach((motivo, n) -> avisos.add(n + " aviso(s) " + motivo + " -> entran tal cual y el predio "
                        + "debe revisarlos (TIPO = AVISO en " + csv.getFileName() + ")"));

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
                            + "Para recargarlo: --excel.nss.forzar=true");
                    omitido = true;
                } else {
                    // FILAS_ERROR = renglones rechazados, no cantidad de errores: un renglon puede
                    // traer varios (lote 2 del 10-oct decia 4,800 para 3,926 renglones)
                    int renglonesConError = (int) errores.stream().map(ValidadorNss.Error::fila).distinct().count();
                    escritura = repo.escribir(layout, archivo.toString(), checksum, registros,
                            leidas[0], renglonesConError, reporteErrores(errores, avisosFila));
                    if (escritura.avisoDuplicados() != null) {
                        avisos.add(escritura.avisoDuplicados());
                    }
                }
            }
        }
        return new ResultadoArchivo(archivo, layout, null, leidas[0], registros.size(), errores, avisos,
                errores.isEmpty() && avisosFila.isEmpty() ? null : csv, escritura, omitido);
    }

    /**
     * Agrega NOMBRE, GENERO, EDAD, CUENTA y PUESTO congelados a la fecha de carga. Un NSS que no
     * esta en el biometrico pasa a error: sin eso el renglon no apareceria en el expediente de nadie.
     */
    private void completarDelBiometrico(List<ValidadorNss.Registro> registros, List<ValidadorNss.Error> errores) {
        Set<String> nss = new LinkedHashSet<>();
        registros.forEach(r -> nss.add(r.texto("NSS")));
        Map<String, BiometricoNss.Empleado> empleados = biometrico.buscar(nss);
        registros.removeIf(r -> {
            BiometricoNss.Empleado e = empleados.get(r.texto("NSS"));
            if (e == null) {
                errores.add(new ValidadorNss.Error(r.fila(), "NSS", r.texto("NSS"), "no existe en el biometrico"));
                return true;
            }
            if (e.dePrueba()) {
                errores.add(new ValidadorNss.Error(r.fila(), "NSS", r.texto("NSS"),
                        "es un registro de prueba en el biometrico (EMP_STATUS 99)"));
                return true;
            }
            r.valores().put("NOMBRE", e.nombre());
            r.valores().put("GENERO", e.genero());
            r.valores().put("EDAD", e.edadAl(r.fecha("FECHA")));
            r.valores().put("CUENTA", e.cuenta());
            r.valores().put("PUESTO", e.puesto());
            return false;
        });
        errores.sort(java.util.Comparator.comparingInt(ValidadorNss.Error::fila));
    }

    /**
     * Resumen de lo que trajo el biometrico, para poder revisarlo sin abrir la base: cuantos
     * renglones quedaron sin cuenta, puesto, genero o edad, y una muestra de los primeros.
     */
    private static void resumirBiometrico(List<ValidadorNss.Registro> registros, List<String> avisos) {
        if (registros.isEmpty()) {
            return;
        }
        long sinCuenta = registros.stream().filter(r -> r.valores().get("CUENTA") == null).count();
        long sinPuesto = registros.stream().filter(r -> r.valores().get("PUESTO") == null).count();
        long sinGenero = registros.stream().filter(r -> r.valores().get("GENERO") == null).count();
        long sinEdad = registros.stream().filter(r -> r.valores().get("EDAD") == null).count();
        avisos.add("biometrico: " + registros.size() + " completados | sin cuenta " + sinCuenta
                + " | sin puesto " + sinPuesto + " | sin genero " + sinGenero + " | sin edad " + sinEdad);
        registros.stream().limit(3).forEach(r -> avisos.add("  muestra renglon " + r.fila() + ": NSS " + r.texto("NSS")
                + " | genero " + r.valores().get("GENERO") + " | edad " + r.valores().get("EDAD")
                + " | cuenta " + r.valores().get("CUENTA") + " | puesto " + r.valores().get("PUESTO")));
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
        return new ResultadoArchivo(archivo, layout, motivo, 0, 0, List.of(), List.of(), null, null, false);
    }
}
