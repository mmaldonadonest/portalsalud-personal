package com.onest.app.catalog.incapacidad.repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Incapacidades de un NSS tal como las guardo el WS (TBL_SERV_INCAPACIDAD_MEDICA, base del portal dentro de
 * BIOMETRICO), con lo que hace falta para armar la cadena inicial -> subsecuentes (C-050..C-054, 18-nov-2026).
 * Solo lectura. Se lee por JDBC y no por el WS de consulta porque ese no regresa FECHA_PRIMERA_INCAPACIDAD,
 * ALTA ni URL_ARCHIVOS en la lista.
 */
@Repository
public class CadenaIncapacidadRepository {

    /** Un registro de la tabla del WS. Fechas ya convertidas (null si vienen vacias o con basura). */
    public record Fila(long regId, String folio, String rubro, String ramo, String tipo, LocalDate inicio,
                       LocalDate termino, Integer dias, Double costo, LocalDate primera, String alta, String rel) {

        public boolean subsecuente() {
            return tipo != null && tipo.trim().equalsIgnoreCase("subsecuente");
        }

        /** Alta = Si cierra la cadena (C-053). El PHP viejo dejo «No_aplica» en ALTA (E-043), eso no cierra. */
        public boolean conAlta() {
            return alta != null && alta.trim().equalsIgnoreCase("Si");
        }
    }

    private final JdbcTemplate jdbc;

    public CadenaIncapacidadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Fila> porNss(String nss) {
        return jdbc.query(
                "SELECT REG_ID, FOLIO_INCAPACIDAD, RUBRO, RAMO, TIPO_INCAPACIDAD, FECHA_INICIO, FECHA_TERMINO,"
                        + " DIAS_AUTORIZADOS, COSTO, FECHA_PRIMERA_INCAPACIDAD, ALTA, URL_ARCHIVOS"
                        + "  FROM TBL_SERV_INCAPACIDAD_MEDICA WHERE TRIM(NSS) = ? ORDER BY REG_ID",
                (rs, i) -> new Fila(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        fecha(rs.getString(6)), fecha(rs.getString(7)),
                        rs.getObject(8) == null ? null : rs.getInt(8),
                        rs.getObject(9) == null ? null : rs.getDouble(9),
                        fecha(rs.getString(10)), rs.getString(11), rs.getString(12)),
                nss.trim());
    }

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Las fechas son texto: casi todas AAAA-MM-DD (lo que manda el input date) y 5 viejas DD/MM/AAAA
     * (diagnostico del 17-nov, seccion 8). Lo demas (vacio o basura) se toma como sin fecha.
     */
    static LocalDate fecha(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            if (t.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                return LocalDate.parse(t.substring(0, 10));
            }
            if (t.matches("\\d{2}/\\d{2}/\\d{4}.*")) {
                return LocalDate.parse(t.substring(0, 10), DMY);
            }
        } catch (DateTimeParseException ex) {
            return null;
        }
        return null;
    }
}
