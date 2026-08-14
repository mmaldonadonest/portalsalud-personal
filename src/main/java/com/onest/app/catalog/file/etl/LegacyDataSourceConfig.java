package com.onest.app.catalog.file.etl;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Segundo DataSource (perfil "etl"): el origen legacy MariaDB de U09.
 *
 * <p>OJO: declarar aqui un bean {@code DataSource} (o {@code JdbcTemplate}) desactiva
 * la autoconfiguracion de Spring Boot para el bean primario correspondiente
 * ({@code @ConditionalOnMissingBean(DataSource.class)} / {@code (JdbcOperations.class)}
 * se satisfacen con CUALQUIER bean de ese tipo, no solo el que declaramos). Sin
 * ambos beans primarios explicitos aqui, JPA Y {@code FsFileRepository} (que
 * inyecta {@code JdbcTemplate} sin qualifier) terminarian conectados a MariaDB
 * en vez de Oracle. Por eso replicamos aqui, SOLO bajo el perfil "etl",
 * exactamente lo que Spring Boot haria por defecto desde {@code spring.datasource.*}
 * (patron documentado de "dos DataSource").</p>
 */
@Configuration
@Profile("etl")
public class LegacyDataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Qualifier("legacy")
    public DataSource legacyDataSource(
            @Value("${etl.legacy.datasource.url}") String url,
            @Value("${etl.legacy.datasource.username}") String username,
            @Value("${etl.legacy.datasource.password}") String password,
            @Value("${etl.legacy.datasource.driver-class-name}") String driverClassName) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    @Bean
    @Qualifier("legacy")
    public JdbcTemplate legacyJdbcTemplate(@Qualifier("legacy") DataSource legacyDataSource) {
        return new JdbcTemplate(legacyDataSource);
    }
}
