package com.onest.excel.layout;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.onest.excel.inventario.ArchivoInventariado;
import com.onest.excel.inventario.Familia;
import com.onest.excel.lectura.Hoja;
import com.onest.excel.normalizacion.Normalizador;
import com.onest.excel.transposicion.Transpositor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Corre el detector de encabezado y el transpositor contra los archivos reales y reporta que
 * salio, <b>sin escribir una sola fila en ninguna base</b>.
 *
 * <p>Existe para responder la pregunta que decide si el plan se sostiene: ¿el mismo detector
 * sirve para las nueve familias, o cada una va a necesitar su caso especial? Se contesta con los
 * archivos delante, no con una muestra de treinta.
 */
public final class AnalisisLayout {

    private AnalisisLayout() {
    }

    /** Hojas que no son tablas de detalle. Misma lista que {@code CargaService}; ver ahi el porque. */
    private static final List<String> HOJAS_IGNORADAS =
            List.of("ACUMULADO", "ACUM", "GRAF", "CHART", "COMPARATIVO", "REPORT RH",
                    "REP RH", "NOMENCLATURA", "CONCENTRADO", "GENERAL", "CONTROL GRAL",
                    "INFOR", "REVISIONES", "ST7", "ST-7", "PEND ", "HOJA1");

    /**
     * Hojas que NO son detalle de un predio.
     *
     * <p>Las primeras son resumenes y graficas. Las de MES son la trampa que destapo la
     * correccion del multi-hoja el 29-sep-2026: los archivos de incapacidades traen la hoja del
     * predio con el anio completo <b>y ademas</b> doce hojas mensuales con los mismos datos
     * partidos. Procesar las dos cosas mete cada renglon dos veces.
     *
     * <p>La hoja del predio manda; los meses se ignoran. Si alguna vez hiciera falta el corte
     * mensual, ya esta en las columnas ANIO y MES de SERV_MED_BITACORA_EVENTO.
     */
    private static final java.util.Set<String> HOJAS_MES = java.util.Set.of(
            "EN", "FB", "MZ", "AB", "MY", "JN", "JL", "AG", "SP", "NV", "DC",
            "ENE", "FEB", "MAR", "MZO", "ABR", "MAY", "MYO", "JUN", "JUL", "AGO",
            "SEP", "OCT", "NOV", "DIC");

    public static String generar(List<ArchivoInventariado> archivos, int anio) {
        StringBuilder sb = new StringBuilder();
        String linea = "=".repeat(100);
        sb.append('\n').append(linea).append('\n');
        sb.append(" ANALISIS DE LAYOUT  -  detector de encabezado y transpositor contra archivos reales\n");
        sb.append(" No escribe en ninguna base.\n");
        sb.append(linea).append('\n');

        Map<Familia, List<Resultado>> porFamilia = new LinkedHashMap<>();
        for (ArchivoInventariado a : archivos) {
            if (!a.abrio() || !a.esDelAnio(anio) || !a.familia().enAlcance()) {
                continue;
            }
            // Las familias de matriz mensual no usan este detector, asi que analizarlas aqui
            // reportaria un fallo que no existe: la carga real las lee con otro lector.
            if (com.onest.excel.carga.MapeoFamilia.de(a.familia())
                    .map(com.onest.excel.carga.MapeoFamilia::matrizMensual).orElse(false)) {
                continue;
            }
            porFamilia.computeIfAbsent(a.familia(), k -> new ArrayList<>()).addAll(analizar(a));
        }

        porFamilia.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().ordinal()))
                .forEach(e -> reportarFamilia(sb, e.getKey(), e.getValue()));

        resumen(sb, porFamilia, linea);
        return sb.toString();
    }

    private record Resultado(String archivo, String predio, String hoja, Integer filaEncabezado,
                             int ancho, int bloques, int columnasSimples, int filasDatos,
                             int filasConAviso, List<String> gruposPrincipales, String error) {
    }

    /**
     * Un resultado por HOJA de detalle, no por archivo: un archivo puede traer varios predios.
     * Tiene que hacer lo mismo que {@code CargaService} o el modo de prueba en seco mentiria
     * sobre lo que va a pasar en la carga real.
     */
    private static List<Resultado> analizar(ArchivoInventariado a) {
        try (Workbook libro = WorkbookFactory.create(new File(a.ruta().toString()), null, true)) {
            List<String> hojas = hojasDeDetalle(libro);
            if (hojas.isEmpty()) {
                return List.of(fallo(a, "no se hallo hoja de detalle"));
            }
            List<Resultado> rs = new ArrayList<>(hojas.size());
            for (String h : hojas) {
                rs.add(analizarHoja(libro, a, h));
            }
            return rs;
        } catch (Exception ex) {
            return List.of(fallo(a, ex.getClass().getSimpleName() + ": " + ex.getMessage()));
        }
    }

    private static Resultado analizarHoja(Workbook libro, ArchivoInventariado a, String nombreHoja) {
        try {
            Hoja hoja = new Hoja(libro.getSheet(nombreHoja));
            Optional<Encabezado> enc = DetectorEncabezado.detectar(hoja);
            if (enc.isEmpty()) {
                return fallo(a, "no se detecto encabezado");
            }
            Encabezado e = enc.get();

            Optional<Encabezado.Columna> colNombre =
                    e.columnaPorGrupo("NOMBRE COMPLETO", "NOMBRE");
            if (colNombre.isEmpty()) {
                return new Resultado(a.nombre(), a.carpetaPredio(), nombreHoja, e.filaGrupo() + 1,
                        hoja.ancho(), e.bloques().size(), e.columnasSimples().size(), 0, 0,
                        gruposDe(e), "sin columna de NOMBRE");
            }

            // Con los MISMOS grupos de campos que usara la carga. Sin esto el modo analizar
            // trataba DATOS INCAPACIDAD como one-hot y reportaba "4 marcas" en cada renglon:
            // los 16 archivos de pos incapacidad salian con avisos = filas (76 de 76, 45 de 45),
            // como si el layout estuviera roto, cuando la carga real no levanta ni uno.
            var mapeo = com.onest.excel.carga.MapeoFamilia.de(a.familia());
            var grupos = mapeo.map(com.onest.excel.carga.MapeoFamilia::gruposDeCampos)
                    .orElse(java.util.Set.of());
            var multi = mapeo.map(m -> m.gruposMultimarca().keySet())
                    .orElse(java.util.Set.of());
            var filas = Transpositor.transponer(hoja, e, colNombre.get().indice(), grupos, multi);
            long conAviso = filas.stream().filter(f -> !f.limpia()).count();

            return new Resultado(a.nombre(), a.carpetaPredio(), nombreHoja, e.filaGrupo() + 1,
                    hoja.ancho(), e.bloques().size(), e.columnasSimples().size(),
                    filas.size(), (int) conAviso, gruposDe(e), null);
        } catch (Exception ex) {
            return fallo(a, ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static List<String> gruposDe(Encabezado e) {
        return e.bloques().stream().map(b -> b.grupo() + "(" + b.ancho() + ")").limit(12).toList();
    }

    private static Resultado fallo(ArchivoInventariado a, String error) {
        return new Resultado(a.nombre(), a.carpetaPredio(), "-", null, 0, 0, 0, 0, 0,
                List.of(), error);
    }

    /**
     * La hoja de detalle: la primera que no sea resumen, grafica ni nomenclatura.
     *
     * <p>En 2026 casi siempre se llama como el predio, pero no se puede confiar en eso: los
     * siete archivos de seguimiento medico especial tienen su hoja de datos llamada {@code AIFA}
     * en todos los predios.
     */
    /**
     * TODAS las hojas de detalle. Misma regla que {@code CargaService.hojasDeDetalle}: si las
     * dos no coinciden, el modo de prueba en seco miente sobre lo que hara la carga real.
     */
    private static List<String> hojasDeDetalle(Workbook libro) {
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

    private static void reportarFamilia(StringBuilder sb, Familia familia, List<Resultado> rs) {
        sb.append("\n-- ").append(familia.name()).append(' ')
          .append("-".repeat(Math.max(0, 94 - familia.name().length()))).append('\n');
        sb.append(String.format("   %-22s %-12s %4s %5s %5s %7s %7s  %s%n",
                "PREDIO", "HOJA", "hdr", "ancho", "bloq", "filas", "avisos", "ESTADO"));
        for (Resultado r : rs) {
            sb.append(String.format("   %-22s %-12s %4s %5d %5d %7d %7d  %s%n",
                    recortar(r.predio(), 22), recortar(r.hoja(), 12),
                    r.filaEncabezado() == null ? "-" : r.filaEncabezado(),
                    r.ancho(), r.bloques(), r.filasDatos(), r.filasConAviso(),
                    r.error() == null ? "ok" : "*** " + r.error()));
        }
        rs.stream().filter(r -> r.error() == null).findFirst().ifPresent(r ->
                sb.append("   bloques one-hot: ").append(String.join(" · ", r.gruposPrincipales()))
                  .append('\n'));
    }

    private static void resumen(StringBuilder sb, Map<Familia, List<Resultado>> porFamilia, String linea) {
        List<Resultado> todos = porFamilia.values().stream().flatMap(List::stream).toList();
        long ok = todos.stream().filter(r -> r.error() == null).count();
        long filas = todos.stream().mapToLong(Resultado::filasDatos).sum();
        long avisos = todos.stream().mapToLong(Resultado::filasConAviso).sum();

        sb.append('\n').append(linea).append('\n');
        sb.append(String.format(" archivos analizados : %d%n", todos.size()));
        sb.append(String.format(" con layout resuelto : %d%s%n", ok,
                ok == todos.size() ? "  <- el mismo detector sirve para todas" : ""));
        sb.append(String.format(" renglones de datos  : %,d%n", filas));
        sb.append(String.format(" renglones con aviso : %,d  (%.2f%%)%n", avisos,
                filas == 0 ? 0.0 : 100.0 * avisos / filas));
        sb.append(linea).append('\n');
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
