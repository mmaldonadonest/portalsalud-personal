package com.onest.app.catalog.incapacidad.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * C-046 (17-nov-2026): ¿un folio de incapacidad IMSS ya existe en el sistema? Solo lectura, en la misma base
 * del portal (BIOMETRICO): lo capturado por el WS (TBL_SERV_INCAPACIDAD_MEDICA) y lo cargado de Excel
 * (SERV_MED_XLS_INCAPACIDAD, solo VALIDO). El folio se compara sin espacios y en mayusculas, igual que la vista
 * SERV_MED_V_INCAPACIDAD empata portal con Excel.
 */
@Repository
public class FolioIncapacidadRepository {

    /** Donde ya esta el folio: NSS, fecha de inicio (o de registro) y fuente. */
    public record Existente(String nss, LocalDate fecha, String fuente) {
    }

    private final JdbcTemplate jdbc;

    public FolioIncapacidadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Existente> buscar(String folioNormalizado) {
        List<Existente> filas = jdbc.query(
                "SELECT NSS, FECHA, FUENTE FROM ("
                        + " SELECT TRIM(i.NSS) NSS, TRUNC(i.FECHA_REGISTRO) FECHA, 'PORTAL' FUENTE"
                        + "   FROM TBL_SERV_INCAPACIDAD_MEDICA i"
                        + "  WHERE UPPER(REPLACE(i.FOLIO_INCAPACIDAD, ' ')) = ?"
                        + " UNION ALL"
                        + " SELECT TRIM(x.NSS), x.FECHA, 'EXCEL'"
                        + "   FROM SERV_MED_XLS_INCAPACIDAD x"
                        + "  WHERE x.ESTADO_REGISTRO = 'VALIDO' AND UPPER(REPLACE(x.FOLIO, ' ')) = ?"
                        + ") ORDER BY FECHA DESC NULLS LAST FETCH FIRST 1 ROWS ONLY",
                (rs, i) -> new Existente(rs.getString(1),
                        rs.getDate(2) == null ? null : rs.getDate(2).toLocalDate(), rs.getString(3)),
                folioNormalizado, folioNormalizado);
        return filas.stream().findFirst();
    }
}
