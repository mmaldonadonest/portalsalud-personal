package com.onest.excel.persistencia;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La conexion al destino, declarada a mano y <b>solo cuando hay a donde conectarse</b>.
 *
 * <p>{@code ExcelLoaderApplication} excluye {@code DataSourceAutoConfiguration} a proposito: sin
 * esta clase el programa es incapaz de tocar una base, y por eso los modos {@code inventario} y
 * {@code analizar} no pueden escribir aunque alguien se equivoque de parametro.
 *
 * <p>La condicion cierra el circulo: sin una {@code excel.destino.url} <b>con valor</b> no se
 * crea ni el DataSource. No hay default a ninguna base.
 *
 * <p>Se usa {@code @ConditionalOnExpression} y no {@code @ConditionalOnProperty} porque este
 * ultimo considera presente una propiedad <b>vacia</b>, y el {@code application.properties}
 * define {@code excel.destino.url=${EXCEL_TARGET_URL:}} con default vacio a proposito. Con
 * {@code ConditionalOnProperty} el programa intentaba conectarse hasta en modo {@code none}.
 *
 * <p><b>El pool se configura en corto a proposito.</b> El 27-sep-2026 un arranque con
 * credenciales malas bloqueo la cuenta BIOMETRICO en produccion: HikariCP precarga
 * {@code minimum-idle} conexiones al iniciar, y ese valor toma por defecto el de
 * {@code maximum-pool-size}, o sea diez. Diez logins fallidos de un golpe son exactamente el
 * limite del perfil DEFAULT de Oracle. Con {@code minimum-idle=0} y
 * {@code initialization-fail-timeout=1}, una credencial equivocada cuesta <b>un</b> intento.
 */
@Configuration
@ConditionalOnExpression("'${excel.destino.url:}' != ''")
public class DestinoConfig {

    private static final Logger log = LoggerFactory.getLogger(DestinoConfig.class);

    /**
     * Conexion al destino, con o sin wallet.
     *
     * <p><b>{@code tns} y {@code wallet} son cosas distintas y van por separado.</b> Juntarlas en
     * una sola propiedad fue el primer intento y estaba mal: apuntar {@code wallet_location} a una
     * carpeta que solo tiene {@code tnsnames.ora} no es inofensivo.
     * <ul>
     *   <li><b>{@code tns}</b> &rarr; la carpeta con {@code tnsnames.ora}. Sirve para que una URL
     *       corta como {@code jdbc:oracle:thin:@PDBPRD} encuentre el host. <b>Sigue haciendo falta
     *       usuario y contrasena.</b></li>
     *   <li><b>{@code wallet}</b> &rarr; la carpeta con {@code cwallet.sso}. Guarda las
     *       credenciales (SEPS), asi que <b>no van en ningun archivo</b>: se dejan vacias y las
     *       saca el driver.</li>
     * </ul>
     *
     * <p>Se pueden usar las dos juntas, una sola, o ninguna. Lo unico que no se vale es quedarse
     * sin forma de autenticar: <b>sin wallet, el usuario y la contrasena son obligatorios</b>.
     *
     * <p>Leer un {@code cwallet.sso} necesita {@code oraclepki} en el classpath. Esta en el pom;
     * sin el, el fallo es un error de PKI que no menciona que falta un jar.
     */
    @Bean
    public DataSource dataSource(@Value("${excel.destino.url}") String url,
                                 @Value("${excel.destino.usuario}") String usuario,
                                 @Value("${excel.destino.password}") String password,
                                 @Value("${excel.destino.tns:}") String tns,
                                 @Value("${excel.destino.wallet:}") String wallet) {
        boolean conTns = tns != null && !tns.isBlank();
        boolean conWallet = wallet != null && !wallet.isBlank();
        boolean hayCredenciales = usuario != null && !usuario.isBlank()
                && password != null && !password.isBlank();

        if (!conWallet && !hayCredenciales) {
            throw new IllegalStateException(
                    "Falta usuario o password del destino. Se pasan por variable de entorno "
                    + "(EXCEL_TARGET_USER / EXCEL_TARGET_PASSWORD), nunca en un archivo del "
                    + "repositorio. Solo se pueden omitir si hay un wallet con las credenciales "
                    + "(EXCEL_TARGET_WALLET).");
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        if (hayCredenciales) {
            ds.setUsername(usuario);
            ds.setPassword(password);
        }
        // Como propiedades de conexion y no como -D del proceso, para que queden acotadas a este
        // DataSource y se vean en el codigo.
        if (conTns) {
            ds.addDataSourceProperty("oracle.net.tns_admin", tns);
        }
        if (conWallet) {
            ds.addDataSourceProperty("oracle.net.wallet_location",
                    "(SOURCE=(METHOD=FILE)(METHOD_DATA=(DIRECTORY=" + wallet + ")))");
        }
        ds.setDriverClassName("oracle.jdbc.OracleDriver");
        ds.setMaximumPoolSize(4);
        ds.setMinimumIdle(0);                    // no precargar: un fallo cuesta un intento
        ds.setInitializationFailTimeout(1);      // abortar al primero, no reintentar
        ds.setConnectionTimeout(5000);
        ds.setPoolName("excel-loader");

        // Se registra la URL y el usuario, NUNCA la contrasena
        log.info("Destino: {} como {}{}{}", url,
                hayCredenciales ? usuario : "(usuario del wallet)",
                conTns ? " · tns en " + tns : "",
                conWallet ? " · wallet en " + wallet : "");
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate t = new JdbcTemplate(dataSource);
        t.setFetchSize(500);
        return t;
    }
}
