package com.onest.app.catalog.dashboard.repository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Historico leido de las VISTAS unificadas (docs/script-prod/excel-nss-05-vistas.sql) en lugar de
 * consultar directo las tablas SERV_MED_XLS_*. Mismas consultas que {@link XlsHistoricoRepository};
 * solo cambia de donde lee cada familia.
 *
 * <p><b>Interruptor.</b> Solo existe con {@code portal.historico.fuente=VISTA}. Pensado para
 * desarrollar en local contra produccion; en el servidor se deja en BITACORA hasta la activacion.
 *
 * <p><b>Por que solo FUENTE &lt;&gt; 'PORTAL'.</b> Los servicios del dashboard siguen sumando lo del
 * portal desde los WS de ORDS. Si aqui tambien entraran los renglones PORTAL de la vista, cada
 * captura del portal se contaria dos veces. Lo que si se gana desde ya: la regla "manda el portal"
 * se aplica al LEER (la vista descarta el renglon del Excel si el evento ya esta capturado), no
 * solo al cargar. Cuando los servicios dejen de llamar a ORDS para graficar, este filtro se quita.
 */
@Repository
@Primary
@ConditionalOnProperty(name = "portal.historico.fuente", havingValue = "VISTA")
public class VistaHistoricoRepository extends XlsHistoricoRepository {

    private static final String SIN_PORTAL = " WHERE FUENTE <> 'PORTAL'";

    public VistaHistoricoRepository(JdbcTemplate jdbc) {
        super(jdbc,
                "(SELECT * FROM SERV_MED_V_ATENCION" + SIN_PORTAL + ") e",
                // La vista trae el dictamen normalizado (APTO_CONDICIONADO...); las graficas esperan
                // la etiqueta con codigo, igual que con las tablas XLS.
                "(SELECT FECHA, PREDIO, CUENTA, " + DICTAMEN_NUEVO + " DICTAMEN FROM SERV_MED_V_EXAMEN"
                        + SIN_PORTAL + ") e",
                // COSTO de la vista = COSTO_TOTAL del layout; se expone con el nombre que usan las consultas.
                "(SELECT v.*, v.COSTO COSTO_TOTAL FROM SERV_MED_V_INCAPACIDAD v WHERE v.FUENTE <> 'PORTAL') e",
                "(SELECT * FROM SERV_MED_V_ACCIDENTE" + SIN_PORTAL + ") e");
    }
}
