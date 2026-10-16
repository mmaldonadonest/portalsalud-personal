package com.onest.app.catalog.examen.repository;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Solo lectura del biometrico (mismo esquema BIOMETRICO del portal) para los datos de la ficha
 * de identificacion del FT-SO-17 que ningun WS de ORDS expone (15-oct-2026):
 * <ul>
 *   <li>ESCOLARIDAD: BIO_EMPLEADO.NIV_ESTUIDIOS (texto) y, si viene vacio, el catalogo
 *       BIO_NIVEL_ESTUDIOS por BIO_DATOS_LABORALES_EMPLEADOS.ID_ESTUDIOS.</li>
 *   <li>DEPARTAMENTO: BIO_DIVISION_SAP.DIVISION_NOMBRE por BIO_DATOS_LABORALES_EMPLEADOS.DIVISION.</li>
 * </ul>
 * Medido 15-oct en produccion: la mayoria de los activos los tiene vacios (~25 mil sin
 * escolaridad, ~22 mil sin division); vacio se muestra como vacio, no se inventa.
 */
@Repository
public class FichaBiometricoRepository {

    private static final Logger log = LoggerFactory.getLogger(FichaBiometricoRepository.class);

    public record Extra(String escolaridad, String departamento) {
        public static final Extra VACIO = new Extra("", "");
    }

    private final JdbcTemplate jdbc;

    public FichaBiometricoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Escolaridad y departamento del NSS (se prefiere el registro activo y con division). */
    public Extra extra(String nss) {
        if (nss == null || nss.isBlank()) {
            return Extra.VACIO;
        }
        try {
            List<Extra> filas = jdbc.query(
                    "SELECT ESCOLARIDAD, DEPARTAMENTO FROM ("
                            + "  SELECT NVL(TRIM(d.NIV_ESTUIDIOS), TRIM(ne.NOMBRE_ESTUDIOS)) ESCOLARIDAD,"
                            + "         TRIM(dv.DIVISION_NOMBRE) DEPARTAMENTO"
                            + "    FROM BIO_EMPLEADO d"
                            + "    LEFT JOIN BIO_DATOS_LABORALES_EMPLEADOS c ON c.EMP_NSS = d.EMP_NSS"
                            + "    LEFT JOIN BIO_NIVEL_ESTUDIOS ne ON ne.ID_ESTUDIOS = c.ID_ESTUDIOS"
                            + "    LEFT JOIN BIO_DIVISION_SAP dv ON dv.DIVISION = c.DIVISION"
                            + "   WHERE d.EMP_NSS = ?"
                            + "   ORDER BY CASE WHEN d.EMP_STATUS = 1 THEN 0 ELSE 1 END,"
                            + "            CASE WHEN dv.DIVISION_NOMBRE IS NOT NULL THEN 0 ELSE 1 END"
                            + ") WHERE ROWNUM = 1",
                    (rs, i) -> new Extra(limpio(rs.getString(1)), limpio(rs.getString(2))), nss.trim());
            return filas.isEmpty() ? Extra.VACIO : filas.get(0);
        } catch (DataAccessException ex) {
            log.warn("[ficha] no se pudo leer escolaridad/departamento de {}: {}", nss, ex.getMostSpecificCause().getMessage());
            return Extra.VACIO;
        }
    }

    /** Sin espacios de relleno ("E.Secundaria      AR" -> "E.Secundaria AR"). */
    private static String limpio(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
