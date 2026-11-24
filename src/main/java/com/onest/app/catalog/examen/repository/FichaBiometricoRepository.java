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
                            // 0 = activo y 1 = baja (usuario, 24-nov-2026): con reingresos manda el registro vigente
                            + "   ORDER BY CASE d.EMP_STATUS WHEN 0 THEN 0 WHEN 1 THEN 1 ELSE 2 END,"
                            + "            CASE WHEN dv.DIVISION_NOMBRE IS NOT NULL THEN 0 ELSE 1 END"
                            + ") WHERE ROWNUM = 1",
                    (rs, i) -> new Extra(limpio(rs.getString(1)), limpio(rs.getString(2))), nss.trim());
            return filas.isEmpty() ? Extra.VACIO : filas.get(0);
        } catch (DataAccessException ex) {
            log.warn("[ficha] no se pudo leer escolaridad/departamento de {}: {}", nss, ex.getMostSpecificCause().getMessage());
            return Extra.VACIO;
        }
    }

    /**
     * Puesto del empleado en el biometrico (17-nov-2026): el WS del examen no lo trae para el empleado y el impreso
     * salia con «Puesto» en blanco (hojas 1 y 3 y FT-SO-05). Misma union que la vista SERV_MED_V_EMPLEADO
     * (datos laborales -> BIOMETRICO_PUESTO). Consulta aparte de {@link #extra} para que si falla no se pierdan
     * la escolaridad ni el departamento. Vacio si no hay.
     */
    public String puesto(String nss) {
        if (nss == null || nss.isBlank()) {
            return "";
        }
        try {
            List<String> filas = jdbc.query(
                    "SELECT MAX(TRIM(pu.PUESTO_NOMBRE)) FROM BIO_DATOS_LABORALES_EMPLEADOS c"
                            + " JOIN BIOMETRICO_PUESTO pu ON pu.PUESTO_ID = c.ID_PUESTO WHERE c.EMP_NSS = ?",
                    (rs, i) -> limpio(rs.getString(1)), nss.trim());
            return filas.isEmpty() ? "" : filas.get(0);
        } catch (DataAccessException ex) {
            log.warn("[ficha] no se pudo leer el puesto de {}: {}", nss, ex.getMostSpecificCause().getMessage());
            return "";
        }
    }

    /**
     * Baja del empleado para el distintivo de la ficha NSS (24-nov-2026). El usuario confirmo que EMP_STATUS = 1
     * es BAJA (caso 90099119373: 1, TIPOBAJA 'Z4' y FECHA_BAJA vacia, por eso la fecha no se usa). Multipredio:
     * solo es baja si tiene algun renglon en 1 y ninguno en 0 (activo); 99 y 100 no cuentan para ningun lado.
     * Devuelve null si no es baja o si no se pudo leer (la ficha no debe caerse por esto).
     */
    public Baja baja(String nss) {
        if (nss == null || nss.isBlank()) {
            return null;
        }
        try {
            // Motivo: catalogo BIO_MOTIVO_BAJA (MOT_ID_SAP = clave de SAP, p. ej. Z4 = Bajo Desempeño). Si la
            // clave no esta en el catalogo, MOTIVO queda vacio y se muestra la clave tal cual.
            List<Baja> filas = jdbc.query(
                    "SELECT SUM(CASE WHEN TRIM(TO_CHAR(e.EMP_STATUS)) = '1' THEN 1 ELSE 0 END) BAJAS,"
                            + "     SUM(CASE WHEN TRIM(TO_CHAR(e.EMP_STATUS)) = '0' THEN 1 ELSE 0 END) ACTIVOS,"
                            + "     MAX(CASE WHEN TRIM(TO_CHAR(e.EMP_STATUS)) = '1' THEN TRIM(e.TIPOBAJA) END) TIPO,"
                            + "     MAX(CASE WHEN TRIM(TO_CHAR(e.EMP_STATUS)) = '1' THEN TRIM(m.MOT_DESCRIPCION) END) MOTIVO"
                            + "  FROM BIO_EMPLEADO e"
                            + "  LEFT JOIN BIO_MOTIVO_BAJA m ON TRIM(m.MOT_ID_SAP) = TRIM(e.TIPOBAJA)"
                            + " WHERE e.EMP_NSS = ?",
                    (rs, i) -> rs.getInt("BAJAS") > 0 && rs.getInt("ACTIVOS") == 0
                            ? new Baja(limpio(rs.getString("TIPO")), limpio(rs.getString("MOTIVO"))) : null,
                    nss.trim());
            return filas.isEmpty() ? null : filas.get(0);
        } catch (DataAccessException ex) {
            log.warn("[ficha] no se pudo leer la baja de {}: {}", nss, ex.getMostSpecificCause().getMessage());
            return null;
        }
    }

    /**
     * tipo = BIO_EMPLEADO.TIPOBAJA tal cual (p. ej. "Z4"); motivo = su descripcion en BIO_MOTIVO_BAJA
     * ("Bajo Desempeño"). Cualquiera puede venir vacio.
     */
    public record Baja(String tipo, String motivo) {
        /** Lo que se lee en la ficha: el motivo; si no hay, la clave; si tampoco, nada. */
        public String texto() {
            return !motivo.isEmpty() ? motivo : tipo;
        }
    }

    /** Sin espacios de relleno ("E.Secundaria      AR" -> "E.Secundaria AR"). */
    private static String limpio(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
