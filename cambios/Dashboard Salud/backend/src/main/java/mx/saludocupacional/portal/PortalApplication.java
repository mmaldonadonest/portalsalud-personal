package mx.saludocupacional.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Punto de entrada del Portal Integral de Salud Ocupacional.
 *
 * <p>El sistema sustituye el proceso basado en el archivo
 * {@code 10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm} y sus ocho libros satélite,
 * consolidando morbilidad, incapacidades, accidentabilidad, exámenes médicos,
 * antidoping y maternidad en una sola base de datos con una única capa analítica.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class PortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortalApplication.class, args);
    }
}
