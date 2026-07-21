package com.onest.app.catalog.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita la configuracion del modulo de archivos (raiz del filesystem, etc.).
 */
@Configuration
@EnableConfigurationProperties(PortalFilesProperties.class)
public class FileStorageConfig {
}
