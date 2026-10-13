package com.onest.excel.nss;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos del empleado por NSS, desde el biometrico. SOLO LECTURA.
 *
 * <p>Mismas tablas y joins que el WS de la ficha (docs/contextoWS.txt:664): BIO_EMPLEADO para
 * nombre, sexo y nacimiento; BIO_DATOS_LABORALES_EMPLEADOS para cuenta y puesto. Un NSS puede
 * tener mas de un renglon laboral (multipredio): se toma uno, el primero.
 */
public class BiometricoNss {

    /** Lo que se congela en el renglon al cargar. La edad se calcula a la fecha del evento. */
    public record Empleado(String nss, String nombre, String genero, LocalDate nacimiento,
                           String cuenta, String puesto, String estatus) {

        /**
         * EMP_STATUS 99 = registros viejos de prueba en el biometrico (p. ej. NSS 99999999999,
         * "AAAAAA"). Confirmado por el usuario el 10-oct-2026: se rechazan como si no existieran.
         */
        public boolean dePrueba() {
            return "99".equals(estatus);
        }

        public Integer edadAl(LocalDate fecha) {
            if (nacimiento == null || fecha == null || nacimiento.isAfter(fecha)) {
                return null;
            }
            int e = Period.between(nacimiento, fecha).getYears();
            return e > 100 ? null : e;    // 126 anios = fecha de nacimiento basura
        }
    }

    private static final int LOTE = 500;

    private final JdbcTemplate jdbc;

    public BiometricoNss(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Empleado> buscar(Collection<String> nssDistintos) {
        if (nssDistintos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Empleado> res = new HashMap<>();
        List<String> todos = new ArrayList<>(nssDistintos);
        for (int i = 0; i < todos.size(); i += LOTE) {
            List<String> parte = todos.subList(i, Math.min(i + LOTE, todos.size()));
            String huecos = String.join(",", Collections.nCopies(parte.size(), "?"));
            String sql = """
                    SELECT e.EMP_NSS, e.EMP_NOMBRE, e.EMP_APELLIDO, e.EMP_APELLIDO2, e.EMP_SEXO,
                           e.EMP_FECHAN, e.EMP_STATUS, cu.CUENTA_NOMBRE, pu.PUESTO_NOMBRE
                      FROM BIO_EMPLEADO e
                      LEFT JOIN (SELECT dl.EMP_NSS, dl.CUENTA_ID, dl.ID_PUESTO,
                                        ROW_NUMBER() OVER (PARTITION BY dl.EMP_NSS ORDER BY dl.ROWID) rn
                                   FROM BIO_DATOS_LABORALES_EMPLEADOS dl
                                  WHERE dl.EMP_NSS IN (%s)) d
                             ON d.EMP_NSS = e.EMP_NSS AND d.rn = 1
                      LEFT JOIN BIOMETRICO_CUENTA_SAP cu ON cu.CUENTA_ID = d.CUENTA_ID
                      LEFT JOIN BIOMETRICO_PUESTO pu ON pu.PUESTO_ID = d.ID_PUESTO
                     WHERE e.EMP_NSS IN (%s)""".formatted(huecos, huecos);
            Object[] params = new Object[parte.size() * 2];
            for (int k = 0; k < parte.size(); k++) {
                params[k] = parte.get(k);
                params[k + parte.size()] = parte.get(k);
            }
            jdbc.query(sql, rs -> {
                String nss = ValidadorNss.nss(String.valueOf(rs.getObject("EMP_NSS")).trim());
                if (res.containsKey(nss)) {
                    return;
                }
                String nombre = String.join(" ", List.of(
                        nvl(rs.getString("EMP_NOMBRE")), nvl(rs.getString("EMP_APELLIDO")),
                        nvl(rs.getString("EMP_APELLIDO2")))).replaceAll("\\s+", " ").trim();
                res.put(nss, new Empleado(nss, nombre.isEmpty() ? null : nombre,
                        genero(rs.getString("EMP_SEXO")), nacimiento(rs.getObject("EMP_FECHAN")),
                        rs.getString("CUENTA_NOMBRE"), rs.getString("PUESTO_NOMBRE"),
                        rs.getObject("EMP_STATUS") == null ? null : String.valueOf(rs.getObject("EMP_STATUS")).trim()));
            }, params);
        }
        return res;
    }

    private static String nvl(String s) {
        return s == null ? "" : s.trim();
    }

    /** El biometrico guarda el sexo como 1/2 (1 = hombre, 2 = mujer) o con letra: a FEM/MASC del layout. */
    static String genero(String sexo) {
        if (sexo == null) {
            return null;
        }
        String s = sexo.trim().toUpperCase();
        return switch (s) {
            case "1", "H", "HOMBRE", "MASCULINO", "MASC" -> "MASC";
            case "2", "F", "MUJER", "FEMENINO", "FEM" -> "FEM";
            default -> null;   // "M" es ambiguo (masculino o mujer): mejor vacio que equivocado
        };
    }

    /** EMP_FECHAN puede venir como fecha o como texto, segun el ambiente. */
    static LocalDate nacimiento(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        String t = v.toString().trim();
        for (java.time.format.DateTimeFormatter f : FORMATOS_NACIMIENTO) {
            try {
                return LocalDate.parse(t.length() >= 10 && t.charAt(4) == '-' ? t.substring(0, 10) : t, f);
            } catch (java.time.format.DateTimeParseException ignorada) {
                // siguiente formato
            }
        }
        return null;
    }

    /**
     * Formatos de nacimiento. Ademas del ISO y dd/mm/aaaa, el de Oracle por omision ("23-SEP-83"),
     * que es como lo muestra la ficha; el anio de dos digitos se toma como 1930-2029.
     */
    private static final List<java.time.format.DateTimeFormatter> FORMATOS_NACIMIENTO = List.of(
            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
            java.time.format.DateTimeFormatter.ofPattern("d/M/uuuu"),
            new java.time.format.DateTimeFormatterBuilder().parseCaseInsensitive()
                    .appendPattern("d-MMM-")
                    .appendValueReduced(java.time.temporal.ChronoField.YEAR, 2, 2, 1930)
                    .toFormatter(java.util.Locale.ENGLISH));
}
