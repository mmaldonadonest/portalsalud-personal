package com.onest.app.ayuda;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centro de ayuda. Meilisearch es opcional: sin URL o sin respuesta se busca en Oracle.
 *
 * @param visible false = los usuarios no ven el boton "?" ni el menu Ayuda (el admin si ve /admin/ayuda)
 * @param meili   url, llave maestra e indice de Meilisearch (docker/ayuda/)
 */
@ConfigurationProperties(prefix = "portal.ayuda")
public record AyudaProperties(Boolean visible, Meili meili) {

    public boolean esVisible() {
        return Boolean.TRUE.equals(visible);
    }

    public record Meili(String url, String key, String indice) {
        public String indiceONombre() {
            return indice == null || indice.isBlank() ? "ayuda" : indice.trim();
        }
    }

    public boolean meiliConfigurado() {
        return meili != null && meili.url() != null && !meili.url().isBlank();
    }
}
