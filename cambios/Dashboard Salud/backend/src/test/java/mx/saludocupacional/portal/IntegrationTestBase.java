package mx.saludocupacional.portal;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de las pruebas de integración.
 *
 * <p>Levanta un PostgreSQL real en contenedor y ejecuta las migraciones de
 * Flyway sobre él. Probar contra la base verdadera y no contra una en memoria
 * evita sorpresas con tipos como {@code JSONB} o con las restricciones que solo
 * existen en PostgreSQL.
 */
@Tag("integration")
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("salud_test")
            .withUsername("test")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("portal.jwt.secret", () -> "secreto-de-pruebas-con-mas-de-treinta-y-dos-caracteres");
    }
}
