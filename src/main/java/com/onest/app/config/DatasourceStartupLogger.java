package com.onest.app.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Al terminar el arranque imprime un banner claro con la base de datos a la que
 * la aplicacion esta REALMENTE conectada. Los datos se leen del {@link DataSource}
 * (metadata de la conexion viva), no de application.properties, por lo que reflejan
 * la verdad aunque el perfil se sobreescriba por -Dspring.profiles.active o variable
 * de entorno.
 */
@Component
public class DatasourceStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(DatasourceStartupLogger.class);

    private final DataSource dataSource;
    private final Environment env;

    public DatasourceStartupLogger(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseTarget() {
        String perfiles = String.join(", ", env.getActiveProfiles());
        if (perfiles.isBlank()) {
            perfiles = "(ninguno; usando default: "
                    + String.join(", ", env.getDefaultProfiles()) + ")";
        }

        String url = "(desconocida)";
        String usuarioConexion = "(desconocido)";
        String producto = "(desconocido)";
        String dbName = "-";
        String pdb = "-";
        String host = "-";
        String usuarioSesion = "-";

        try (Connection cn = dataSource.getConnection()) {
            DatabaseMetaData md = cn.getMetaData();
            url = md.getURL();
            usuarioConexion = md.getUserName();
            producto = md.getDatabaseProductName() + " " + md.getDatabaseProductVersion()
                    .replaceAll("\\s+", " ").trim();

            // Contexto de Oracle: nombre de BD, PDB, host y usuario efectivo de la sesion
            try (var st = cn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT SYS_CONTEXT('USERENV','DB_NAME') db, "
                       + "SYS_CONTEXT('USERENV','CON_NAME') pdb, "
                       + "SYS_CONTEXT('USERENV','SERVER_HOST') host, "
                       + "SYS_CONTEXT('USERENV','CURRENT_USER') usr FROM dual")) {
                if (rs.next()) {
                    dbName = valorODefecto(rs.getString("db"));
                    pdb = valorODefecto(rs.getString("pdb"));
                    host = valorODefecto(rs.getString("host"));
                    usuarioSesion = valorODefecto(rs.getString("usr"));
                }
            } catch (SQLException ex) {
                log.debug("[db] no se pudo leer SYS_CONTEXT (¿no es Oracle?): {}", ex.getMessage());
            }
        } catch (SQLException ex) {
            log.error("[db] NO se pudo abrir conexion para diagnostico: {}", ex.getMessage(), ex);
        }

        log.info("\n"
                + "================ BASE DE DATOS ACTIVA ================\n"
                + "  Perfil(es) Spring : {}\n"
                + "  JDBC URL          : {}\n"
                + "  Usuario (conexion): {}\n"
                + "  Usuario (sesion)  : {}\n"
                + "  Producto          : {}\n"
                + "  DB_NAME / PDB      : {} / {}\n"
                + "  Server host        : {}\n"
                + "=====================================================",
                perfiles, url, usuarioConexion, usuarioSesion, producto, dbName, pdb, host);
    }

    private static String valorODefecto(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
