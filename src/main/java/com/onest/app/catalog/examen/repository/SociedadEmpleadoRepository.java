package com.onest.app.catalog.examen.repository;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Solo lectura de BIO_EMPLEADO.SOCIEDAD (biometrico, mismo esquema BIOMETRICO del portal):
 * la razon social que contrata a la persona (GRUPO LOGISTICO INTEGRAL, SERVICIOS ORLAMEX,
 * GESTION EMPRESARIAL VALLE, ONELOGISTIC). Es la AGENCIA del FT-SO-04 rev. 05 (15-oct-2026).
 * Ningun WS de ORDS la expone (el legacy regresaba 'sin agencia' fijo); las vistas
 * SERV_MED_V_* ya leen BIO_EMPLEADO por JDBC, asi que no hace falta WS nuevo.
 */
@Repository
public class SociedadEmpleadoRepository {

    private static final Logger log = LoggerFactory.getLogger(SociedadEmpleadoRepository.class);

    private final JdbcTemplate jdbc;

    public SociedadEmpleadoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Sociedad del NSS; si el NSS se repite en BIO_EMPLEADO se prefiere el registro activo.
     * Vacio si no hay dato (al 9-oct-2026, ~21 mil activos la tienen vacia) o si falla la lectura.
     */
    public Optional<String> sociedad(String nss) {
        if (nss == null || nss.isBlank()) {
            return Optional.empty();
        }
        try {
            List<String> filas = jdbc.queryForList(
                    "SELECT SOCIEDAD FROM ("
                            + "  SELECT TRIM(SOCIEDAD) SOCIEDAD FROM BIO_EMPLEADO"
                            + "   WHERE EMP_NSS = ? AND TRIM(SOCIEDAD) IS NOT NULL"
                            // 0 = activo y 1 = baja (usuario, 24-nov-2026): con reingresos manda el registro vigente
                            + "   ORDER BY CASE EMP_STATUS WHEN 0 THEN 0 WHEN 1 THEN 1 ELSE 2 END"
                            + ") WHERE ROWNUM = 1",
                    String.class, nss.trim());
            return filas.stream().findFirst();
        } catch (DataAccessException ex) {
            log.warn("[agencia] no se pudo leer BIO_EMPLEADO.SOCIEDAD de {}: {}", nss, ex.getMostSpecificCause().getMessage());
            return Optional.empty();
        }
    }
}
