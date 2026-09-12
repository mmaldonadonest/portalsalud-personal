package com.onest.app.importer.service;

import com.onest.app.importer.dto.ImportacionDto.Incidencia;
import com.onest.app.importer.service.ExcelLector.ArchivoLeido;
import com.onest.app.importer.service.ExcelLector.FilaLeida;
import com.onest.app.importer.service.ExcelLector.HojaLeida;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/**
 * Pasos 2 y 3 del importador: valida formato y catalogos, detecta duplicados y arma las
 * incidencias por hoja. Es GENERICO a proposito: no hay layouts destino todavia, asi que
 * las reglas son las que se pueden afirmar de cualquier hoja tabular mas las que documenta
 * el analisis del reporte gerencial real (cambios/Dashboard Salud/docs/01_ANALISIS_ACTUAL.md):
 * meses abreviados inconsistentes, predios abreviados, columnas con tipos mezclados, costos
 * negativos, vinculos a libros externos.
 *
 * <p>Estado por fila: RECHAZADO (error de formula / celda ilegible) > ADVERTENCIA (duplicada,
 * costo negativo, predio no reconocido) > OK. Las incidencias son por hoja y llevan impacto
 * y resolucion propuesta, como en el prototipo.
 */
@Component
public class ImportValidador {

    /** Las 13 hojas del reporte gerencial "10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm". */
    static final Set<String> HOJAS_CONOCIDAS = Set.of(
            "ACUMULADO", "ATEN", "CAUSAS", "MUSC-ESQU", "EM", "INCAP", "INC IMSS", "INC INTER",
            "ACCID", "ANTDPG", "ABAST ANTDP", "ANTALCOH", "MATER");

    /** Abreviaturas de mes que aparecen en el reporte y su forma estandar. */
    static final Map<String, String> MESES_ALIAS = Map.of("MZO", "MAR", "MYO", "MAY", "AGS", "AGO", "SEPT", "SEP", "SET", "SEP");
    static final Set<String> MESES = Set.of("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC");

    /** Predios abreviados de la hoja ABAST ANTDP (documentados; el alias real se confirma con negocio). */
    static final Map<String, String> PREDIOS_ALIAS = Map.of(
            "M1", "MACRO 1", "M2", "MACRO 2", "TULTI", "TULTITLAN", "UT", "UT", "WP", "WP");

    public record Evaluacion(String estado, List<String> mensajes) {
    }

    public record ResultadoValidacion(Map<String, List<Evaluacion>> porHoja, List<Incidencia> incidencias,
                                      Map<String, Boolean> reconocida) {
    }

    /**
     * @param prediosCatalogo nombres del catalogo SERV_MED_PREDIO (puede venir vacio si ORDS no responde)
     */
    public ResultadoValidacion validar(ArchivoLeido archivo, List<String> prediosCatalogo) {
        Map<String, List<Evaluacion>> porHoja = new LinkedHashMap<>();
        List<Incidencia> incidencias = new ArrayList<>();
        Map<String, Boolean> reconocida = new LinkedHashMap<>();
        Set<String> catalogo = new HashSet<>();
        for (String p : prediosCatalogo == null ? List.<String>of() : prediosCatalogo) {
            catalogo.add(normaliza(p));
        }
        int hojasConExternas = 0;
        int externasTotal = 0;

        for (HojaLeida hoja : archivo.hojas()) {
            boolean conocida = HOJAS_CONOCIDAS.contains(hoja.nombre().toUpperCase(Locale.ROOT));
            reconocida.put(hoja.nombre(), conocida);
            List<Evaluacion> evals = new ArrayList<>();

            // --- reglas de encabezado (nivel hoja)
            List<String> mesesRaros = new ArrayList<>();
            for (String enc : hoja.encabezados()) {
                String u = enc.toUpperCase(Locale.ROOT).trim();
                if (MESES_ALIAS.containsKey(u)) {
                    mesesRaros.add(u + "/" + MESES_ALIAS.get(u));
                }
            }
            if (!mesesRaros.isEmpty()) {
                incidencias.add(new Incidencia(hoja.nombre(), "Meses inconsistentes: " + String.join(", ", mesesRaros),
                        "Bajo", "Normalizar a catálogo único de periodos"));
            }
            if (!conocida) {
                incidencias.add(new Incidencia(hoja.nombre(), "Hoja no reconocida del reporte gerencial (se carga igual)",
                        "Bajo", "Confirmar con negocio a qué tabla corresponde"));
            }
            if (hoja.truncada()) {
                incidencias.add(new Incidencia(hoja.nombre(), "Más de " + ExcelLector.MAX_FILAS_HOJA + " filas: se cargaron solo las primeras",
                        "Alto", "Partir el archivo o cargar por periodos"));
            }
            if (hoja.columnasIgnoradas() > 0) {
                incidencias.add(new Incidencia(hoja.nombre(), hoja.columnasIgnoradas() + " columnas fuera del encabezado (sin título en la fila "
                        + hoja.filaEncabezado() + ") no se cargaron", "Medio", "Dar título a esas columnas o quitarlas"));
            }
            if (hoja.formulasExternas() > 0) {
                hojasConExternas++;
                externasTotal += hoja.formulasExternas();
            }

            // --- perfil de tipos por columna y columnas de costo
            Map<String, Map<String, Integer>> tipos = new LinkedHashMap<>();
            for (String enc : hoja.encabezados()) {
                tipos.put(enc, new TreeMap<>());
            }
            List<String> columnasCosto = hoja.encabezados().stream()
                    .filter(e -> e.toUpperCase(Locale.ROOT).matches(".*(COSTO|IMPORTE|MONTO|SALARIO|\\$).*")).toList();
            // Columna de predio: la que se llame asi, o la primera si no tiene titulo (en el reporte
            // real la columna de predios suele ir sin encabezado). Con otro titulo (ACTIVIDAD en
            // ACUMULADO) no se valida contra el catalogo.
            String columnaPredio = hoja.encabezados().stream()
                    .filter(e -> e.toUpperCase(Locale.ROOT).matches(".*(PREDIO|SITIO|PLANTA|CEDIS).*")).findFirst()
                    .orElse(!hoja.encabezados().isEmpty() && hoja.encabezados().get(0).startsWith("COL_") ? hoja.encabezados().get(0) : null);

            // --- filas
            Map<String, Integer> huellas = new HashMap<>();
            Set<String> prediosNoReconocidos = new LinkedHashSet<>();
            Set<String> prediosAbreviados = new LinkedHashSet<>();
            int negativos = 0;
            for (FilaLeida fila : hoja.filas()) {
                List<String> msgs = new ArrayList<>();
                String estado = "OK";
                if (!fila.erroresFormula().isEmpty()) {
                    estado = "RECHAZADO";
                    msgs.add("Error de fórmula: " + String.join(", ", fila.erroresFormula()));
                }
                // Renglon de una sola celda de texto = titulo, nota al pie ("Fuente: ...") o
                // subtotal: no es un registro. Se carga (para no perder nada) pero marcado.
                boolean nota = fila.celdasConDato() == 1
                        && fila.valores().values().stream().filter(v -> v != null).allMatch(v -> v instanceof String);
                if (nota) {
                    estado = peor(estado, "ADVERTENCIA");
                    msgs.add("Renglón de nota o título, no es un registro");
                }
                // duplicado exacto dentro de la hoja
                String huella = fila.valores().values().toString();
                Integer primera = huellas.putIfAbsent(huella, fila.numero());
                if (primera != null && fila.celdasConDato() > 1) {
                    estado = peor(estado, "ADVERTENCIA");
                    msgs.add("Duplicada de la fila " + primera);
                }
                for (Map.Entry<String, Object> e : fila.valores().entrySet()) {
                    Object v = e.getValue();
                    if (v == null) {
                        continue;
                    }
                    tipos.get(e.getKey()).merge(tipoDe(v), 1, Integer::sum);
                    if (v instanceof Number n && n.doubleValue() < 0 && columnasCosto.contains(e.getKey())) {
                        negativos++;
                        estado = peor(estado, "ADVERTENCIA");
                        msgs.add(e.getKey() + " negativo (" + v + ")");
                    }
                }
                if (columnaPredio != null && !catalogo.isEmpty() && !nota) {
                    Object p = fila.valores().get(columnaPredio);
                    if (p instanceof String ps && !ps.isBlank()) {
                        String n = normaliza(ps);
                        if (PREDIOS_ALIAS.containsKey(ps.trim().toUpperCase(Locale.ROOT))) {
                            prediosAbreviados.add(ps.trim().toUpperCase(Locale.ROOT));
                        } else if (!catalogo.contains(n) && catalogo.stream().noneMatch(c -> c.contains(n) || n.contains(c))
                                && !n.matches("^(TOTAL|ACUMULADO|GENERAL|SUMA).*")) {
                            prediosNoReconocidos.add(ps.trim());
                        }
                    }
                }
                if (fila.formulasExternas() > 0) {
                    msgs.add(fila.formulasExternas() + " celda(s) con valor cacheado de libro externo");
                }
                evals.add(new Evaluacion(estado, msgs));
            }

            // --- incidencias derivadas de las filas
            List<String> mixtas = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> e : tipos.entrySet()) {
                Map<String, Integer> t = e.getValue();
                int total = t.values().stream().mapToInt(Integer::intValue).sum();
                // numero+texto con al menos 4 celdas ya es mezcla (el caso real: cantidad, folio de
                // lote y fecha en la misma columna de ABAST ANTDP); logico se ignora.
                t.remove("lógico");
                if (t.size() >= 2 && total >= 4) {
                    mixtas.add(e.getKey() + " (" + String.join("/", t.keySet()) + ")");
                }
            }
            if (!mixtas.isEmpty()) {
                incidencias.add(new Incidencia(hoja.nombre(), "Tipos mixtos en una misma columna: "
                        + String.join(", ", mixtas.size() > 4 ? mixtas.subList(0, 4) : mixtas) + (mixtas.size() > 4 ? " y " + (mixtas.size() - 4) + " más" : ""),
                        "Medio", "Separar en columnas tipadas"));
            }
            if (negativos > 0) {
                incidencias.add(new Incidencia(hoja.nombre(), "Costos negativos (" + negativos + " celdas)",
                        "Bajo", "Confirmar con negocio si son ajustes"));
            }
            if (!prediosAbreviados.isEmpty()) {
                incidencias.add(new Incidencia(hoja.nombre(), "Predios abreviados (" + String.join(", ", prediosAbreviados) + ")",
                        "Bajo", "Tabla de alias en catálogo de predios"));
            }
            if (!prediosNoReconocidos.isEmpty()) {
                List<String> l = new ArrayList<>(prediosNoReconocidos);
                incidencias.add(new Incidencia(hoja.nombre(), "Predios fuera del catálogo: "
                        + String.join(", ", l.size() > 5 ? l.subList(0, 5) : l) + (l.size() > 5 ? " y " + (l.size() - 5) + " más" : ""),
                        "Medio", "Dar de alta en SERV_MED_PREDIO o mapear como alias"));
            }
            long rechazadas = evals.stream().filter(ev -> "RECHAZADO".equals(ev.estado())).count();
            if (rechazadas > 0) {
                incidencias.add(new Incidencia(hoja.nombre(), rechazadas + " filas con error de fórmula (#REF!, #N/A, #DIV/0!)",
                        "Alto", "Corregir en el origen y volver a cargar"));
            }
            porHoja.put(hoja.nombre(), evals);
        }

        if (hojasConExternas > 0) {
            incidencias.add(new Incidencia("Todas", "Vínculos a libros externos en " + hojasConExternas
                    + (hojasConExternas == 1 ? " hoja" : " hojas") + " (" + externasTotal + " celdas): se usó el valor cacheado",
                    "Alto", "Cargar los libros satélite o consolidar en una sola fuente"));
        }
        for (String omitida : archivo.hojasOmitidas()) {
            incidencias.add(new Incidencia(omitida.replaceAll(" \\(.*\\)$", ""), "Hoja omitida: " + omitida.replaceAll("^.*\\((.*)\\)$", "$1"),
                    "Bajo", "Sin acción si es intencional"));
        }
        incidencias = consolidar(incidencias);
        incidencias.sort((a, b) -> Integer.compare(peso(b.impacto()), peso(a.impacto())));
        return new ResultadoValidacion(porHoja, incidencias, reconocida);
    }

    /**
     * La misma incidencia en 3+ hojas (tipico: los mismos predios fuera de catalogo en las 12
     * hojas de detalle) se reporta una sola vez como "Varias hojas (A, B, C...)" para que la
     * tabla no se llene de renglones identicos.
     */
    private static List<Incidencia> consolidar(List<Incidencia> lista) {
        Map<String, List<Incidencia>> grupos = new LinkedHashMap<>();
        for (Incidencia i : lista) {
            grupos.computeIfAbsent(i.incidencia() + "|" + i.impacto() + "|" + i.resolucion(), k -> new ArrayList<>()).add(i);
        }
        List<Incidencia> out = new ArrayList<>();
        for (List<Incidencia> g : grupos.values()) {
            if (g.size() >= 3) {
                List<String> hojas = g.stream().map(Incidencia::hoja).toList();
                String etiqueta = hojas.size() > 6
                        ? String.join(", ", hojas.subList(0, 6)) + " y " + (hojas.size() - 6) + " más"
                        : String.join(", ", hojas);
                out.add(new Incidencia("Varias hojas (" + hojas.size() + ")", g.get(0).incidencia() + " · en " + etiqueta,
                        g.get(0).impacto(), g.get(0).resolucion()));
            } else {
                out.addAll(g);
            }
        }
        return out;
    }

    private static int peso(String impacto) {
        return switch (impacto) {
            case "Alto" -> 3;
            case "Medio" -> 2;
            default -> 1;
        };
    }

    private static String peor(String actual, String nuevo) {
        return peso2(nuevo) > peso2(actual) ? nuevo : actual;
    }

    private static int peso2(String estado) {
        return switch (estado) {
            case "RECHAZADO" -> 2;
            case "ADVERTENCIA" -> 1;
            default -> 0;
        };
    }

    private static String tipoDe(Object v) {
        if (v instanceof Boolean) {
            return "lógico";
        }
        if (v instanceof Number) {
            return "número";
        }
        if (ExcelLector.esFechaIso(v)) {
            return "fecha";
        }
        return "texto";
    }

    static String normaliza(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT).replaceAll("[ÁÀÄ]", "A").replaceAll("[ÉÈË]", "E")
                .replaceAll("[ÍÌÏ]", "I").replaceAll("[ÓÒÖ]", "O").replaceAll("[ÚÙÜ]", "U").replaceAll("[^A-Z0-9]", "");
    }
}
