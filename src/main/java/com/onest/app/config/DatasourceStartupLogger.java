package com.onest.app.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
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
                + "  Perfil(es) Spring : {}  (de: {})\n"
                + "  JDBC URL          : {}\n"
                + "  URL configurada   : {}\n"
                + "  Usuario (conexion): {}\n"
                + "  Usuario (sesion)  : {}\n"
                + "  Usuario config.   : {}\n"
                + "  Producto          : {}\n"
                + "  DB_NAME / PDB      : {} / {}\n"
                + "  Server host        : {}\n"
                + "  TNS_ADMIN          : {}\n"
                + "  Archivos (root)    : {}\n"
                + "  --- Configuracion leida de (si una llave esta en varias, gana la primera) ---\n"
                + "{}"
                + "=====================================================",
                perfiles, origen("spring.profiles.active"), url,
                conOrigen("spring.datasource.url"), usuarioConexion, usuarioSesion,
                conOrigen("spring.datasource.username"), producto, dbName, pdb, host,
                conOrigen("portal.oracle.tns-admin"), conOrigen("portal.files.root"), fuentes());
    }

    /**
     * Valor configurado y de donde salio (19-oct-2026). Las rutas del servidor se pueden reescribir
     * sin rearmar el WAR: XML del contexto de Tomcat > setenv.sh > application-prod.properties; aqui
     * se comprueba en cualquier servidor que la reescritura funciono. Nunca se usa con la contrasena.
     */
    private String conOrigen(String llave) {
        String valor = env.getProperty(llave);
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor + "  (de: " + origen(llave) + ")";
    }

    /** Primera fuente (la que gana) que define la llave. */
    private String origen(String llave) {
        if (env instanceof ConfigurableEnvironment ce) {
            for (PropertySource<?> fuente : ce.getPropertySources()) {
                // "configurationProperties" es la envoltura de Spring sobre TODAS las fuentes: se
                // salta para nombrar la fuente real
                if (!"configurationProperties".equals(fuente.getName()) && fuente.containsProperty(llave)) {
                    return nombreFuente(fuente.getName());
                }
            }
        }
        return "?";
    }

    /**
     * Fuentes de configuracion en orden de prioridad. Del WAR se cuentan las llaves; de Tomcat y del
     * servidor se listan solo los NOMBRES de las llaves del portal (nunca valores: ahi podria venir
     * una contrasena). Asi un setenv viejo con SPRING_DATASOURCE_* se ve en el log al arrancar.
     */
    private String fuentes() {
        if (!(env instanceof ConfigurableEnvironment ce)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (PropertySource<?> fuente : ce.getPropertySources()) {
            String nombre = fuente.getName();
            boolean delWar = nombre.startsWith("Config resource");
            boolean delServidor = SERVIDOR.contains(nombre);
            if ((!delWar && !delServidor) || !(fuente instanceof EnumerablePropertySource<?> e)) {
                continue;
            }
            String detalle;
            if (delWar) {
                detalle = e.getPropertyNames().length + " llaves";
            } else {
                List<String> delPortal = Arrays.stream(e.getPropertyNames()).filter(DatasourceStartupLogger::esDelPortal).sorted().toList();
                detalle = delPortal.isEmpty() ? "nada del portal" : String.join(", ", delPortal);
            }
            sb.append(String.format("  %d. %-42s: %s%n", n++, nombreFuente(nombre), detalle));
        }
        return sb.toString();
    }

    /** Fuentes que no vienen del WAR: las pone Tomcat o el servidor. */
    private static final List<String> SERVIDOR = List.of(
            "servletConfigInitParams", "servletContextInitParams", "jndiProperties", "systemProperties", "systemEnvironment");

    // Sin "server.": Tomcat define server.loader (catalina.properties) y no es del portal
    private static boolean esDelPortal(String llave) {
        String l = llave.toLowerCase(Locale.ROOT);
        return l.startsWith("spring.") || l.startsWith("portal.")
                || l.startsWith("spring_") || l.startsWith("portal_") || l.startsWith("ocr_");
    }

    private static String nombreFuente(String fuente) {
        switch (fuente) {
            case "servletConfigInitParams":
                return "Tomcat, parametros del servlet";
            case "servletContextInitParams":
                return "XML del contexto de Tomcat";
            case "jndiProperties":
                return "Tomcat, JNDI";
            case "systemProperties":
                return "setenv, -D en CATALINA_OPTS";
            case "systemEnvironment":
                return "setenv, variable de entorno";
            default:
                break;
        }
        // "Config resource 'class path resource [application-prod.properties]' via location ..."
        int a = fuente.indexOf('[');
        int b = fuente.indexOf(']');
        String archivo = (a >= 0 && b > a) ? fuente.substring(a + 1, b) : fuente;
        return fuente.contains("class path resource") ? "WAR, " + archivo : "archivo externo, " + archivo;
    }

    private static String valorODefecto(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
