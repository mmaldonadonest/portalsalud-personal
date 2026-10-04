package com.onest.excel.carga;

import java.util.List;
import java.util.Map;

/**
 * Tarea 13: el reporte de la corrida.
 *
 * <p>Su trabajo es que nadie tenga que confiar en que salio bien: por cada archivo dice cuantos
 * renglones leyo, cuantos inserto, cuantos descarto y <b>por que</b>. Un cargador que solo
 * imprime "listo" obliga a ir a la base a ver si de verdad quedo.
 */
public final class CargaReporte {

    private CargaReporte() {
    }

    private static final String LINEA = "=".repeat(104);

    public static String generar(List<CargaService.Resultado> resultados,
                                 Map<String, Integer> conteos, boolean muestra) {
        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(LINEA).append('\n');
        sb.append(muestra
                ? " CARGA DE ENSAYO (sample)  -  maximo " + CargaService.FILAS_MUESTRA + " renglones por archivo\n"
                : " CARGA COMPLETA (full)\n");
        sb.append(LINEA).append('\n');

        alertaNoHizoNada(sb, resultados);
        detalle(sb, resultados);
        problemas(sb, resultados);
        avisos(sb, resultados);
        resumen(sb, resultados, conteos, muestra);
        return sb.toString();
    }

    /**
     * Una corrida que no inserto nada porque todo estaba ya cargado.
     *
     * <p>Paso de verdad el 30-sep-2026: se corrio el {@code full} sin haber vaciado QA, las 66
     * hojas se saltaron con "ya cargada antes", no entro un solo renglon, y el reporte remato con
     * <b>"cuadre: todas las hojas comparables cuadran"</b> y el bat con <b>"[ok] Termino"</b>.
     * Tecnicamente cierto &mdash;cero hojas comparables, cero que no cuadran&mdash; y por completo
     * enganoso. El "0 de 138" estaba, pero doscientas lineas mas abajo.
     *
     * <p>Por eso esto va ARRIBA y no en el resumen: un aviso que hay que ir a buscar despues de
     * scrollear la lista de saltados no sirve de nada.
     */
    /**
     * Cuantas hojas fallaron. Una corrida con hojas caidas no es un exito.
     *
     * <p>El 30-sep-2026 la hoja MERCURIO de antidoping murio con un
     * {@code NullPointerException}, se perdieron sus 462 renglones &mdash;la hoja mas grande de la
     * familia&mdash; y el {@code .bat} remato con <b>{@code [ok] Termino}</b>. El error estaba en
     * el reporte, en su seccion, pero el codigo de salida decia lo contrario.
     */
    public static long hojasConError(List<CargaService.Resultado> rs) {
        return rs.stream().filter(r -> r.error() != null && !r.saltado()).count();
    }

    public static boolean noHizoNada(List<CargaService.Resultado> rs) {
        long insertadas = rs.stream().mapToLong(CargaService.Resultado::insertadas).sum();
        return insertadas == 0 && rs.stream().anyMatch(CargaReporte::yaCargada);
    }

    private static boolean yaCargada(CargaService.Resultado r) {
        return r.saltado() && r.error() != null && r.error().contains("ya cargada antes");
    }

    private static void alertaNoHizoNada(StringBuilder sb, List<CargaService.Resultado> rs) {
        if (!noHizoNada(rs)) {
            return;
        }
        long yaCargadas = rs.stream().filter(CargaReporte::yaCargada).count();
        sb.append('\n');
        sb.append(" ***\n");
        sb.append(" *** NO SE CARGO NADA. La base quedo exactamente como estaba.\n");
        sb.append(" ***\n");
        sb.append(String.format(" *** %d hojas se saltaron porque ya estaban registradas. El indice unico%n",
                yaCargadas));
        sb.append(" *** (ARCHIVO, HOJA) impide cargarlas dos veces, asi que no se duplico nada,\n");
        sb.append(" *** pero tampoco entro un solo renglon nuevo.\n");
        sb.append(" ***\n");
        sb.append(" *** Hay que VACIAR las tablas antes de volver a correr:\n");
        sb.append(" ***   docs/script-prod/excel-99-reversa-bitacora.sql   con v_forzar := 'S'\n");
        sb.append(" ***   y confirmar que la comprobacion del final sale en 0 / 0 / 0.\n");
        sb.append(" ***\n");
    }

    private static void detalle(StringBuilder sb, List<CargaService.Resultado> rs) {
        sb.append("\n-- POR ARCHIVO ").append("-".repeat(88)).append('\n');
        sb.append(String.format("   %-16s %-14s %7s %7s %-7s %8s %8s  %s%n",
                "PREDIO", "FAMILIA", "leidas", "el XLS", "cuadra", "insert", "atrib", "ESTADO"));
        for (CargaService.Resultado r : rs) {
            if (r.saltado()) {
                continue;
            }
            // El cuadre compara lo LEIDO contra lo que el propio Excel declara en su hoja
            // ACUMULADO. Se ponen las dos cifras juntas para que la comparacion se vea.
            String esperado = r.acumuladoEsperado() == null ? "-"
                    : String.valueOf(r.acumuladoEsperado());
            String marca = switch (r.cuadra()) {
                case "SI" -> "si";
                case "NO" -> "*** NO";
                // El archivo trae hoja ACUMULADO pero en ceros: no hay contra que cuadrar.
                // No es lo mismo que no cuadrar y no debe leerse igual.
                case "cero" -> "cero";
                default -> "-";
            };
            sb.append(String.format("   %-16s %-14s %7d %7s %-7s %8d %8d  %s%n",
                    recortar(r.predio(), 16), recortar(r.familia(), 14),
                    r.leidas(), esperado, marca, r.insertadas(), r.atributos(),
                    r.error() == null ? "ok" : "*** " + recortar(r.error(), 34)));
        }
    }

    private static void problemas(StringBuilder sb, List<CargaService.Resultado> rs) {
        List<CargaService.Resultado> saltados = rs.stream().filter(CargaService.Resultado::saltado).toList();
        sb.append("\n-- SALTADOS ").append("-".repeat(91)).append('\n');
        if (saltados.isEmpty()) {
            sb.append("   ninguno\n");
        } else {
            saltados.forEach(r -> sb.append(String.format("   %-46s %s%n",
                    recortar(r.archivo(), 46), r.error())));
        }

        List<CargaService.Resultado> fallidos = rs.stream()
                .filter(r -> r.error() != null && !r.saltado()).toList();
        sb.append("\n-- CON ERROR ").append("-".repeat(90)).append('\n');
        if (fallidos.isEmpty()) {
            sb.append("   ninguno\n");
        } else {
            fallidos.forEach(r -> sb.append(String.format("   %-46s %s%n",
                    recortar(r.archivo(), 46), r.error())));
        }
    }

    private static void avisos(StringBuilder sb, List<CargaService.Resultado> rs) {
        long total = rs.stream().mapToLong(r -> r.avisos().size()).sum();
        sb.append("\n-- AVISOS ").append("-".repeat(93)).append('\n');
        if (total == 0) {
            sb.append("   ninguno\n");
            return;
        }
        sb.append("   ").append(total).append(" en total. Quedan completos en "
                + "SERV_MED_BITACORA_LOTE.MENSAJES. Primeros:\n");
        rs.stream().filter(r -> !r.avisos().isEmpty()).limit(4).forEach(r -> {
            sb.append("   * ").append(r.predio()).append(" / ").append(r.familia())
              .append("  (").append(r.avisos().size()).append(")\n");
            r.avisos().stream().limit(3).forEach(a -> sb.append("       ").append(a).append('\n'));
        });
    }

    private static void resumen(StringBuilder sb, List<CargaService.Resultado> rs,
                                Map<String, Integer> conteos, boolean muestra) {
        long ok = rs.stream().filter(CargaService.Resultado::ok).count();
        long insertadas = rs.stream().mapToLong(CargaService.Resultado::insertadas).sum();
        long atributos = rs.stream().mapToLong(CargaService.Resultado::atributos).sum();
        long noCuadran = rs.stream().filter(r -> "NO".equals(r.cuadra())).count();
        long cuadran = rs.stream().filter(r -> "SI".equals(r.cuadra())).count();
        long acumuladoEnCero = rs.stream().filter(r -> "cero".equals(r.cuadra())).count();

        sb.append('\n').append(LINEA).append('\n');
        sb.append(String.format(" archivos procesados : %d de %d%n", ok, rs.size()));
        sb.append(String.format(" eventos insertados  : %,d%n", insertadas));
        sb.append(String.format(" atributos           : %,d%n", atributos));
        long sinCuadre = rs.stream().filter(r -> !r.saltado() && r.error() == null
                && "-".equals(r.cuadra())).count();
        if (noCuadran > 0) {
            sb.append(String.format(" *** NO CUADRAN      : %d hojas contra lo que declara su ACUMULADO%n", noCuadran));
        } else if (cuadran > 0) {
            sb.append(String.format(" cuadre              : las %d hojas comparables cuadran%n", cuadran));
        } else {
            // Sin esta rama, cero hojas comparables imprimia "todas las hojas comparables
            // cuadran", que es cierto y no significa nada.
            sb.append(" cuadre              : NO SE COMPARO NINGUNA HOJA\n");
        }
        if (acumuladoEnCero > 0) {
            sb.append(String.format(" acumulado en cero   : %d (el archivo no lleva su total; no es falla)%n",
                    acumuladoEnCero));
        }
        if (sinCuadre > 0) {
            sb.append(String.format(" sin cuadre posible  : %d (sin hoja ACUMULADO o el predio no empareja)%n", sinCuadre));
        }
        sb.append("\n En la base ahora:\n");
        conteos.forEach((t, n) -> sb.append(String.format("   SERV_MED_BITACORA_%-10s %,10d%n", t, n)));
        if (muestra) {
            sb.append("\n Esto fue un ENSAYO. Para revertirlo:\n");
            sb.append("   docs/script-prod/excel-99-reversa-bitacora.sql  (v_forzar := 'S')\n");
            sb.append(" Si los numeros se ven bien, correr de nuevo con --excel.modo=full\n");
        }
        sb.append(LINEA).append('\n');
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
