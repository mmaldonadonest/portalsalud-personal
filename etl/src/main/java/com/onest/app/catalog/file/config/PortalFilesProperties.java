package com.onest.app.catalog.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del almacenamiento de archivos (contrato fileManagementContract).
 * {@code root}: raiz del filesystem donde se guarda el binario (por entorno).
 */
@ConfigurationProperties(prefix = "portal.files")
public record PortalFilesProperties(String root) {
}
