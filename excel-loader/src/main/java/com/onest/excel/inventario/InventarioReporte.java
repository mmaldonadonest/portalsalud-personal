package com.onest.excel.inventario;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Imprime el inventario de forma que se pueda leer y decidir.
 *
 * <p>No es adorno: el inventario existe para destapar las sorpresas del origen antes de
 * escribir el transpositor, y una sorpresa que no se ve no sirve de nada. Por eso este reporte
 * saca a la superficie, con nombre y apellido, cada cosa que va a estorbar: los archivos que no
 * se pudieron clasificar, los que estan en la carpeta de otro anio, los duplicados por
 * contenido, y las familias que traen mas de una estructura.
 */
public final class InventarioReporte {

    private InventarioReporte() {
    }

    private static final String LINEA = "=".repeat(96);

    public static String generar(List<ArchivoInventariado> archivos, int anioObjetivo) {
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(LINEA).append('\n');
        sb.append(" INVENTARIO DEL ORIGEN  -  ").append(archivos.size()).append(" archivos de Excel\n");
        sb.append(LINEA).append("\n");

        resumenPorFamilia(sb, archivos, anioObjetivo);
        problemas(sb, archivos);
        conflictoDeAnio(sb, archivos);
        duplicados(sb, archivos);
        formatos(sb, archivos);
        alcance(sb, archivos, anioObjetivo);

        return sb.toString();
    }

    private static void resumenPorFamilia(StringBuilder sb, List<ArchivoInventariado> archivos, int anio) {
        sb.append("\n-- POR FAMILIA ").append("-".repeat(80)).append('\n');
        sb.append(String.format("   %-18s %5s %6s  %-16s %-9s %s%n",
                "FAMILIA", "total", "del" + anio, "HOJAS (min-max)", "SEGUIM.", "ALCANCE"));

        Map<Familia, List<ArchivoInventariado>> porFamilia = archivos.stream()
                .collect(Collectors.groupingBy(ArchivoInventariado::familia,
                        () -> new LinkedHashMap<>(), Collectors.toList()));

        porFamilia.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))
                .forEach(e -> {
                    List<ArchivoInventariado> g = e.getValue();
                    long delAnio = g.stream().filter(a -> a.esDelAnio(anio)).count();
                    // Rango de hojas por libro. NO es una medida de variantes de estructura:
                    // dos libros con distinto numero de hojas pueden tener el mismo layout de
                    // datos. Decir "3 formas" por esto era enganoso, porque las 12 atenciones
                    // de 2026 son estructuralmente identicas (187 columnas, encabezado fila 3).
                    var conteos = g.stream().filter(ArchivoInventariado::abrio)
                            .mapToInt(a -> a.hojas().size()).summaryStatistics();
                    String hojas = conteos.getCount() == 0 ? "-"
                            : conteos.getMin() == conteos.getMax() ? String.valueOf(conteos.getMin())
                            : conteos.getMin() + "-" + conteos.getMax();
                    sb.append(String.format("   %-18s %5d %6d  %-16s %-9s %s%n",
                            e.getKey().name(), g.size(), delAnio, hojas,
                            e.getKey().esSeguimiento() ? "persona" : "evento",
                            e.getKey().enAlcance() ? "SI" : "no"));
                });
    }

    /**
     * El nombre dice un anio y el contenido dice otro. Es la seccion que faltaba y sin ella el
     * inventario descartaba archivos en silencio: los siete de NOM-035 se llaman 2026 y su hoja
     * GENERAL dice 2025, asi que al dejar ganar al contenido quedaban todos fuera de una carga
     * de 2026 sin que nadie lo notara.
     */
    private static void conflictoDeAnio(StringBuilder sb, List<ArchivoInventariado> archivos) {
        List<ArchivoInventariado> conflicto = archivos.stream()
                .filter(ArchivoInventariado::anioEnConflicto).toList();
        sb.append("\n-- EL NOMBRE DICE UN ANIO Y EL CONTENIDO DICE OTRO ").append("-".repeat(45)).append('\n');
        if (conflicto.isEmpty()) {
            sb.append("   ninguno\n");
            return;
        }
        sb.append("   Hay que decidir cual manda (--excel.anio.fuente=contenido|nombre).\n");
        sb.append("   Con 'contenido' estos archivos NO entran a una carga del anio del nombre:\n");
        conflicto.forEach(a -> sb.append(String.format("   nombre %-6s  contenido %-6s  %-14s %s%n",
                a.anioSegunNombre(), a.anioSegunContenido(),
                recortar(a.familia().name(), 14), recortar(a.nombre(), 48))));
    }

    private static void problemas(StringBuilder sb, List<ArchivoInventariado> archivos) {
        List<ArchivoInventariado> conError = archivos.stream().filter(a -> !a.abrio()).toList();
        sb.append("\n-- NO SE PUDIERON ABRIR ").append("-".repeat(71)).append('\n');
        if (conError.isEmpty()) {
            sb.append("   ninguno\n");
        } else {
            conError.forEach(a -> sb.append(String.format("   %-58s %s%n",
                    recortar(a.nombre(), 58), a.error())));
        }

        List<ArchivoInventariado> sinClasificar = archivos.stream()
                .filter(a -> a.familia() == Familia.DESCONOCIDA).toList();
        sb.append("\n-- SIN CLASIFICAR ").append("-".repeat(77)).append('\n');
        if (sinClasificar.isEmpty()) {
            sb.append("   ninguno: los patrones de Familia cubren todo el origen\n");
        } else {
            sb.append("   Hay que agregarles un patron en Familia o decidir que quedan fuera:\n");
            sinClasificar.forEach(a -> sb.append(String.format("   %-16s %s%n",
                    recortar(a.carpetaPredio(), 16), recortar(a.nombre(), 70))));
        }

        List<ArchivoInventariado> mienten = archivos.stream()
                .filter(ArchivoInventariado::extensionMiente).toList();
        sb.append("\n-- LA EXTENSION MIENTE ").append("-".repeat(72)).append('\n');
        if (mienten.isEmpty()) {
            sb.append("   ninguno: todas las extensiones coinciden con el contenido real\n");
        } else {
            mienten.forEach(a -> sb.append(String.format("   %-58s dice %s, es %s%n",
                    recortar(a.nombre(), 58), a.formatoExtension(), a.formatoReal())));
        }

        List<ArchivoInventariado> otroAnio = archivos.stream()
                .filter(ArchivoInventariado::carpetaDeOtroAnio).toList();
        sb.append("\n-- GUARDADOS EN LA CARPETA DE OTRO ANIO ").append("-".repeat(55)).append('\n');
        if (otroAnio.isEmpty()) {
            sb.append("   ninguno\n");
        } else {
            sb.append("   El anio se toma del contenido, asi que esto no rompe nada; es para saberlo:\n");
            otroAnio.forEach(a -> sb.append(String.format("   carpeta %-6s pero es de %-6s  %s%n",
                    a.anioSegunCarpeta(), a.anio(), recortar(a.nombre(), 56))));
        }
    }

    private static void duplicados(StringBuilder sb, List<ArchivoInventariado> archivos) {
        sb.append("\n-- COPIAS CON NOMBRE DISTINTO (mismo checksum) ").append("-".repeat(48)).append('\n');
        Map<String, List<ArchivoInventariado>> porChecksum = archivos.stream()
                .filter(a -> a.checksumSha256() != null)
                .collect(Collectors.groupingBy(ArchivoInventariado::checksumSha256));
        List<List<ArchivoInventariado>> grupos = porChecksum.values().stream()
                .filter(g -> g.size() > 1).toList();
        if (grupos.isEmpty()) {
            sb.append("   ninguna: los archivos son todos distintos entre si\n");
        } else {
            sb.append("   Cargar las dos seria contar lo mismo dos veces. Elegir una:\n");
            grupos.forEach(g -> {
                sb.append("   * ").append(g.size()).append(" copias identicas:\n");
                g.forEach(a -> sb.append("       ").append(a.carpetaPredio())
                        .append("/").append(a.nombre()).append('\n'));
            });
        }
    }

    private static void formatos(StringBuilder sb, List<ArchivoInventariado> archivos) {
        sb.append("\n-- FORMATO REAL ").append("-".repeat(79)).append('\n');
        Map<FormatoArchivo, Long> conteo = archivos.stream()
                .collect(Collectors.groupingBy(ArchivoInventariado::formatoReal,
                        TreeMap::new, Collectors.counting()));
        conteo.forEach((f, n) -> sb.append(String.format("   %-12s %4d%n", f, n)));
    }

    private static void alcance(StringBuilder sb, List<ArchivoInventariado> archivos, int anio) {
        long enAlcance = archivos.stream()
                .filter(a -> a.abrio() && a.esDelAnio(anio) && a.familia().enAlcance())
                .count();
        long fueraPorAnio = archivos.stream()
                .filter(a -> a.abrio() && !a.esDelAnio(anio)).count();
        long fueraPorFamilia = archivos.stream()
                .filter(a -> a.abrio() && a.esDelAnio(anio) && !a.familia().enAlcance())
                .count();

        sb.append("\n").append(LINEA).append('\n');
        sb.append(String.format(" A CARGAR (anio %d, familias en alcance) : %d archivos%n", anio, enAlcance));
        sb.append(String.format("   fuera porque son de otro anio         : %d%n", fueraPorAnio));
        sb.append(String.format("   fuera porque la familia no esta        : %d%n", fueraPorFamilia));
        sb.append(LINEA).append('\n');
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
