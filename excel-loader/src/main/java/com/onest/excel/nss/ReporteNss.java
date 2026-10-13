package com.onest.excel.nss;

import java.util.List;
import java.util.Map;

/** Reporte de consola de una corrida: por archivo, que entro, que no y por que. */
public final class ReporteNss {

    private static final int ERRORES_EN_PANTALLA = 15;

    private ReporteNss() {
    }

    public static String generar(List<CargaNssService.ResultadoArchivo> resultados, boolean escribio,
                                 Map<String, Integer> conteos) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================================\n");
        sb.append(escribio ? " CARGA POR NSS" : " VALIDACION POR NSS (no se escribio en la base)").append('\n');
        sb.append("================================================================\n");
        int leidas = 0, validas = 0, errores = 0, insertados = 0, reemplazados = 0, duplicados = 0, rechazados = 0;
        for (CargaNssService.ResultadoArchivo r : resultados) {
            sb.append("\n").append(r.archivo().getFileName());
            if (r.motivoRechazo() != null) {
                rechazados++;
                sb.append("\n  [RECHAZADO] ").append(r.motivoRechazo()).append('\n');
                continue;
            }
            int renglonesConError = (int) r.errores().stream().map(ValidadorNss.Error::fila).distinct().count();
            sb.append("  -> ").append(r.layout().hoja()).append('\n');
            sb.append("  renglones leidos ").append(r.leidas()).append(" | validos ").append(r.validas())
              .append(" | con error ").append(renglonesConError).append('\n');
            leidas += r.leidas();
            validas += r.validas();
            errores += renglonesConError;
            if (r.escritura() != null) {
                var e = r.escritura();
                sb.append("  lote ").append(e.loteId()).append(" | insertados ").append(e.insertados())
                  .append(" | reemplazados de cargas anteriores ").append(e.reemplazados())
                  .append(" | ya estaban en el portal ").append(e.duplicados()).append('\n');
                insertados += e.insertados();
                reemplazados += e.reemplazados();
                duplicados += e.duplicados();
            }
            r.avisos().forEach(a -> sb.append("  aviso: ").append(a).append('\n'));
            r.errores().stream().limit(ERRORES_EN_PANTALLA).forEach(e -> sb.append("    renglon ").append(e.fila())
                    .append(" | ").append(e.encabezado()).append(" = '").append(e.valor()).append("' | ")
                    .append(e.motivo()).append('\n'));
            if (r.errores().size() > ERRORES_EN_PANTALLA) {
                sb.append("    ... ").append(r.errores().size() - ERRORES_EN_PANTALLA).append(" mas en ")
                  .append(r.csvErrores()).append('\n');
            }
        }
        sb.append("\n----------------------------------------------------------------\n");
        sb.append(" archivos ").append(resultados.size()).append(" (rechazados ").append(rechazados).append(")\n");
        sb.append(" renglones leidos ").append(leidas).append(" | validos ").append(validas)
          .append(" | con error ").append(errores).append('\n');
        if (escribio) {
            sb.append(" insertados ").append(insertados).append(" | reemplazados ").append(reemplazados)
              .append(" | duplicados del portal ").append(duplicados).append('\n');
            if (conteos != null) {
                sb.append("\n Total en las tablas:\n");
                conteos.forEach((t, n) -> sb.append("   ").append(String.format("%-28s", t)).append(n).append('\n'));
            }
        }
        sb.append("================================================================\n");
        return sb.toString();
    }
}
