package com.onest.etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del ETL de migracion historica (U09): lee {@code servicioMedico.files} y
 * {@code servicioMedico.tags} de la MariaDB del legacy PHP y los carga en Oracle
 * ({@code SERV_MED_FS_FILE}, {@code SERV_MED_TAG}) y en el filesystem del portal.
 *
 * <p>Proyecto SEPARADO del portal desde el 23-sep-2026: antes vivia dentro del WAR bajo el
 * perfil "etl" y eso significaba llevar el driver de MariaDB a produccion y poder disparar una
 * carga masiva por error. El codigo de los runners y del almacenamiento se movio TAL CUAL (la
 * corrida de 2026-08-13 ya estaba verificada: 21,448 archivos y 550,560 tags), para no cambiar
 * las rutas de sharding ni los checksums con los que el portal localiza los binarios.</p>
 *
 * <p>Escanea {@code com.onest.app.catalog.file} porque las clases conservan su paquete original.
 * El perfil "etl" sigue activo por defecto (ver application.properties): los runners lo exigen.</p>
 *
 * <p>Uso: ver README.md. Nada corre solo — {@code etl.files.mode} y {@code etl.tags.mode}
 * valen {@code none} salvo que se indique {@code sample} o {@code full}.</p>
 */
@SpringBootApplication(scanBasePackages = {"com.onest.etl", "com.onest.app.catalog.file"})
@ConfigurationPropertiesScan("com.onest.app.catalog.file")
@EntityScan("com.onest.app.catalog.file")
public class EtlApplication {

    public static void main(String[] args) {
        // Sin servidor web: el proceso corre, hace su trabajo y termina.
        SpringApplication app = new SpringApplication(EtlApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }
}
