package com.onest.app.catalog.dashboard.service;

import com.onest.app.catalog.dashboard.repository.VistaAccidenteRepository.FilaReporte;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * C-044 (P-050, SM 20-nov-2026): reporte anual de accidentes con la forma de la hoja GENERAL del
 * Excel «REPORTE ACCIDENTABILIDAD» de Servicio Medico: un renglon por predio y, en columnas, mes,
 * tipo de riesgo, genero, cuenta, area, puesto, dias y costo de calificado e improcedente, causa de
 * RT, costos y status.
 *
 * <p>Solo agrupa (sin base ni POI) para poder probarlo; el Excel lo escribe ReporteAnualAccidentesService.
 *
 * <p>Reglas acordadas en C-044 para que cuadre con el Excel de SM:
 * <ul>
 *   <li>El grupo calificado / improcedente sale del <b>status de calificacion</b>, no de la columna
 *       donde se tecleaban las fechas del ST7/ST2 (en el portal no hay esas columnas).</li>
 *   <li>Dias y costo son los guardados en cada accidente (DIAS y COSTO): el reporte no recalcula.</li>
 *   <li>Lo que no trae dato no se reparte ni se inventa: va en una columna «Sin dato» que solo
 *       aparece si hay al menos un caso asi.</li>
 * </ul>
 */
public final class ReporteAnualAccidentes {

    public static final String SIN_DATO = "Sin dato";
    public static final String SIN_PREDIO = "Sin asignar";

    public static final List<String> TIPOS = List.of("LABORAL", "TRAYECTO");
    public static final List<String> GENEROS = List.of("FEM", "MASC");
    /** Las 7 causas del Excel de SM, con su encabezado corto de la hoja GENERAL. */
    public static final Map<String, String> CAUSAS = causas();
    public static final List<String> STATUS = List.of("PENDIENTE", "CALIFICADO", "IMPROCEDENTE", "BAJA INCONCLUSO");

    private ReporteAnualAccidentes() {
    }

    private static Map<String, String> causas() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ACCID VIAL", "ACCID VIAL");
        m.put("AGRESION / ASALTO", "AGRESION");
        m.put("CAIDA", "CAIDA");
        m.put("GOLPE DIRECTO CON O POR OBJETO", "GOLPE");
        m.put("HERIDA", "HERIDA");
        m.put("LESION ERGONOMICA", "ERGONOMICA");
        m.put("OTRO (IMPROCEDENTE)", "IMPROCEDENTE");
        return m;
    }

    /** Lo de un predio (o el total). Los mapas por texto libre (cuenta, area, puesto) van ordenados. */
    public static final class Renglon {
        public final String predio;
        public final long[] meses = new long[12];
        public long sinMes;
        public final Map<String, Long> tipo = new LinkedHashMap<>();
        public final Map<String, Long> genero = new LinkedHashMap<>();
        public final Map<String, Long> cuenta = new TreeMap<>();
        public final Map<String, Long> area = new TreeMap<>();
        public final Map<String, Long> puesto = new TreeMap<>();
        /** Llave: "CALIFICADO|LABORAL", "IMPROCEDENTE|TRAYECTO"... */
        public final Map<String, Long> dias = new LinkedHashMap<>();
        public final Map<String, BigDecimal> costo = new LinkedHashMap<>();
        public final Map<String, Long> causa = new LinkedHashMap<>();
        public final Map<String, Long> status = new LinkedHashMap<>();
        /** Costo de todo el predio por status (incluye Pendiente y Baja / inconcluso). */
        public final Map<String, BigDecimal> costoStatus = new LinkedHashMap<>();
        public long total;

        Renglon(String predio) {
            this.predio = predio;
        }

        public long n(Map<String, Long> m, String k) {
            return m.getOrDefault(k, 0L);
        }

        public BigDecimal d(Map<String, BigDecimal> m, String k) {
            return m.getOrDefault(k, BigDecimal.ZERO);
        }

        /** Suma de una dimension sin contar «Sin dato» (las causas solo cuentan los casos que la traen). */
        public long suma(Map<String, Long> m) {
            return m.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    /** Resultado: renglones por predio (alfabetico, «Sin asignar» al final) y el total. */
    public record Reporte(int anio, List<Renglon> predios, Renglon total,
                          List<String> cuentas, List<String> areas, List<String> puestos,
                          boolean tipoSinDato, boolean generoSinDato, boolean statusSinDato) {
    }

    public static Reporte armar(int anio, Collection<FilaReporte> filas) {
        Map<String, Renglon> porPredio = new TreeMap<>((a, b) -> {
            // «Sin asignar» al final: es lo que falta por mapear, no un predio
            if (a.equals(b)) {
                return 0;
            }
            if (SIN_PREDIO.equals(a)) {
                return 1;
            }
            if (SIN_PREDIO.equals(b)) {
                return -1;
            }
            return a.compareToIgnoreCase(b);
        });
        Renglon total = new Renglon("TOTAL");
        TreeSet<String> cuentas = new TreeSet<>();
        TreeSet<String> areas = new TreeSet<>();
        TreeSet<String> puestos = new TreeSet<>();

        for (FilaReporte f : filas) {
            String predio = texto(f.predio(), SIN_PREDIO);
            Renglon r = porPredio.computeIfAbsent(predio, Renglon::new);
            String cuenta = texto(f.cuenta(), SIN_DATO);
            String area = texto(f.area(), SIN_DATO);
            String puesto = texto(f.puesto(), SIN_DATO);
            cuentas.add(cuenta);
            areas.add(area);
            puestos.add(puesto);
            for (Renglon x : List.of(r, total)) {
                sumar(x, f, cuenta, area, puesto);
            }
        }

        List<Renglon> lista = new ArrayList<>(porPredio.values());
        return new Reporte(anio, lista, total, alFinal(cuentas), alFinal(areas), alFinal(puestos),
                total.tipo.containsKey(SIN_DATO), total.genero.containsKey(SIN_DATO), total.status.containsKey(SIN_DATO));
    }

    private static void sumar(Renglon r, FilaReporte f, String cuenta, String area, String puesto) {
        r.total++;
        if (f.fecha() != null) {
            r.meses[f.fecha().getMonthValue() - 1]++;
        } else {
            r.sinMes++;
        }
        String tipo = tipo(f.tipoRiesgo());
        String status = status(f.statusCalificacion());
        mas(r.tipo, tipo);
        mas(r.genero, genero(f.genero()));
        mas(r.cuenta, cuenta);
        mas(r.area, area);
        mas(r.puesto, puesto);
        mas(r.status, status);
        String causa = causa(f.causaRt());
        if (causa != null) {
            mas(r.causa, causa);
        }
        BigDecimal costo = f.costo() == null ? BigDecimal.ZERO : f.costo();
        r.costoStatus.merge(status, costo, BigDecimal::add);
        // Bloque CALIFICADO / IMPROCEDENTE de la hoja: solo esos dos status y con tipo conocido
        if (("CALIFICADO".equals(status) || "IMPROCEDENTE".equals(status)) && TIPOS.contains(tipo)) {
            String k = status + "|" + tipo;
            r.dias.merge(k, f.dias() == null ? 0L : f.dias().longValue(), Long::sum);
            r.costo.merge(k, costo, BigDecimal::add);
        }
    }

    private static void mas(Map<String, Long> m, String k) {
        m.merge(k, 1L, Long::sum);
    }

    /** Orden alfabetico con «Sin dato» al final. */
    private static List<String> alFinal(TreeSet<String> valores) {
        List<String> l = new ArrayList<>(valores);
        if (l.remove(SIN_DATO)) {
            l.add(SIN_DATO);
        }
        return l;
    }

    static String texto(String v, String siVacio) {
        return v == null || v.isBlank() ? siVacio : v.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    static String tipo(String v) {
        String t = texto(v, SIN_DATO);
        if (t.contains("LABORAL")) {
            return "LABORAL";
        }
        if (t.contains("TRAYECTO")) {
            return "TRAYECTO";
        }
        return SIN_DATO;
    }

    static String genero(String v) {
        String g = texto(v, SIN_DATO);
        return GENEROS.contains(g) ? g : SIN_DATO;
    }

    /** El portal guarda BAJA_INCONCLUSO y la vista lo deja con espacio; el Excel trae el texto del catalogo. */
    static String status(String v) {
        String s = texto(v, SIN_DATO).replace('_', ' ');
        if (s.startsWith("BAJA")) {
            return "BAJA INCONCLUSO";
        }
        return STATUS.contains(s) ? s : SIN_DATO;
    }

    /** null si no trae causa: la hoja de SM solo cuenta los casos que la tienen. */
    static String causa(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String c = texto(v, SIN_DATO);
        if (CAUSAS.containsKey(c)) {
            return c;
        }
        // El portal guardaba la clave con guion bajo (ACCID_VIAL...); la vista ya la traduce, esto es por si acaso
        String letras = c.replaceAll("[^A-Z]", "");
        if (letras.length() < 4) {
            return null;
        }
        for (String k : CAUSAS.keySet()) {
            if (k.replaceAll("[^A-Z]", "").startsWith(letras)) {
                return k;
            }
        }
        return null;
    }
}
