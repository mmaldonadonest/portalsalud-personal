package com.onest.app.catalog.file.etl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Escritura en {@code SERV_MED_TAG} (Oracle, destino del ETL de {@code servicioMedico.tags}).
 * Landing EAV fiel: sin normalizar (ver tags-salud.sql). {@code SOURCE_ID} guarda el
 * id original de MariaDB para idempotencia/reconciliacion (no hay unique constraint
 * en la tabla - se verifica por consulta antes de insertar, igual que el ETL de files).
 *
 * <p><b>Todo aqui esta escrito para hacer POCOS viajes a la base, no para ser elegante.</b>
 * El destino de produccion esta a <b>99 ms de latencia</b> (medido el 1-oct-2026 contra
 * 10.249.249.35). A ese costo, lo que decide cuanto tarda la migracion no es el trabajo sino
 * el numero de roundtrips:
 *
 * <pre>
 *   fila por fila  : 551,044 x 2 viajes = 1,102,088 roundtrips = ~30 HORAS
 *   en lote de 500 : 1 consulta + 1,102 viajes                 = ~2 minutos
 * </pre>
 *
 * <p>La primera version hacia un {@code SELECT COUNT(*)} y un {@code INSERT} por cada tag. Se
 * midio en produccion el 1-oct-2026: avanzaba a ~5 filas por segundo y se detuvo la corrida.
 * El estimado de "~15 minutos" del runbook venia de la prueba de agosto contra un Oracle
 * <b>local</b>, donde el mismo millon de roundtrips cuesta dos minutos porque la latencia es
 * de 0.1 ms. Mismo codigo, otro escenario, tres ordenes de magnitud de diferencia.
 */
@Repository
@Profile("etl")
public class MedTagWriter {

    private static final Logger log = LoggerFactory.getLogger(MedTagWriter.class);

    private static final String INSERT_SQL =
            "INSERT INTO SERV_MED_TAG (NSS, TYPE, CONTENT, TAG_GROUP, SOURCE_ID, MIGRATED_AT, CREATED_AT, CREATED_BY) "
            + "VALUES (?, ?, ?, SERV_MED_FN_TAG_GROUP(?), ?, SYSTIMESTAMP, SYSTIMESTAMP, ?)";

    private final JdbcTemplate jdbc;

    public MedTagWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Todos los {@code SOURCE_ID} ya migrados, en <b>una sola consulta</b>.
     *
     * <p>Sustituye al {@code existsBySourceId} por fila. Medio millon de {@code Long} son unos
     * 20 MB en memoria; medio millon de viajes de red a 99 ms son quince horas. No hay duda.
     *
     * <p>El {@code fetchSize} importa: con el default de 10 filas por viaje, traer 551 mil ids
     * serian 55 mil roundtrips y volveriamos al mismo problema por otra puerta.
     */
    public Set<Long> sourceIdsExistentes() {
        Set<Long> ids = new HashSet<>(1 << 20);
        jdbc.setFetchSize(5000);
        try {
            jdbc.query("SELECT SOURCE_ID FROM SERV_MED_TAG WHERE SOURCE_ID IS NOT NULL",
                    rs -> { ids.add(rs.getLong(1)); });
        } finally {
            jdbc.setFetchSize(-1);
        }
        return ids;
    }

    /**
     * Inserta un lote completo en un viaje.
     *
     * <p>Si el lote falla, <b>se reintenta fila por fila</b>: Oracle aborta el batch entero por
     * una sola fila mala y sin esto se perderian las 499 buenas sin saber cual era el problema.
     * Devuelve cuantas entraron de verdad.
     */
    public int insertBatch(List<LegacyTagRow> filas, String createdBy) {
        if (filas == null || filas.isEmpty()) {
            return 0;
        }
        try {
            jdbc.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    LegacyTagRow r = filas.get(i);
                    ps.setString(1, r.nss());
                    ps.setString(2, r.type());
                    ps.setString(3, r.content());
                    ps.setString(4, r.type());
                    ps.setLong(5, r.id());
                    ps.setString(6, createdBy);
                }

                @Override
                public int getBatchSize() {
                    return filas.size();
                }
            });
            return filas.size();
        } catch (RuntimeException ex) {
            log.warn("[etl-tags] el lote de {} fallo ({}). Se reintenta fila por fila para aislar.",
                    filas.size(), ex.getMessage());
            int ok = 0;
            for (LegacyTagRow r : filas) {
                try {
                    jdbc.update(INSERT_SQL, r.nss(), r.type(), r.content(), r.type(), r.id(), createdBy);
                    ok++;
                } catch (RuntimeException fila) {
                    log.error("[etl-tags] id={} NO se pudo insertar: {}", r.id(), fila.getMessage());
                }
            }
            return ok;
        }
    }
}
