package com.onest.app.catalog.dashboard.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * WHERE con parametros sobre las vistas SERV_MED_V_* (alias "e"): fechas sobre FECHA, predio y
 * cuenta por igualdad sin mayusculas. Compartido por los repositorios de la fase 2 para que el
 * mismo filtro de pantalla corte igual en todas las tarjetas.
 */
final class VistaFiltro {

    /** Mismo texto que DashboardPredioFiltro.SIN_ASIGNAR: el filtro de pantalla lo manda tal cual. */
    static final String SIN_ASIGNAR = "Sin asignar";
    static final String SIN_DATO = "Sin dato";
    static final String MES = "TO_CHAR(e.FECHA, 'YYYY-MM')";
    static final String PREDIO = "NVL(e.PREDIO, '" + SIN_ASIGNAR + "')";

    private final StringBuilder sql;
    private final List<Object> params = new ArrayList<>();
    private boolean conWhere;

    VistaFiltro(String base) {
        this.sql = new StringBuilder(base);
    }

    VistaFiltro filtros(LocalDate desde, LocalDate hasta, String predio, String cuenta) {
        if (desde != null) {
            and("e.FECHA >= ?", java.sql.Date.valueOf(desde));
        }
        if (hasta != null) {
            and("e.FECHA <= ?", java.sql.Date.valueOf(hasta));
        }
        if (predio != null && !predio.isBlank()) {
            // "Sin asignar" es seleccionable a proposito: muestra lo que falta por mapear
            if (SIN_ASIGNAR.equalsIgnoreCase(predio.trim())) {
                and("e.PREDIO IS NULL");
            } else {
                and("UPPER(e.PREDIO) = UPPER(?)", predio.trim());
            }
        }
        if (cuenta != null && !cuenta.isBlank()) {
            and("UPPER(e.CUENTA) = UPPER(?)", cuenta.trim());
        }
        return this;
    }

    VistaFiltro and(String condicion, Object... valores) {
        sql.append(conWhere ? " AND " : " WHERE ").append(condicion);
        conWhere = true;
        params.addAll(List.of(valores));
        return this;
    }

    String sql() {
        return sql.toString();
    }

    Object[] params() {
        return params.toArray();
    }
}
