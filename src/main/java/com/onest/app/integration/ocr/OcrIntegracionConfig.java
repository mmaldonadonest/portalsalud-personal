package com.onest.app.integration.ocr;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cableado de la integracion OCR: propiedades, filtro de API key (como filtro de servlet, antes
 * de Spring Security, para que una llave mala conteste 401 JSON y no redirija al login) y el
 * scheduler de reintentos/caducidad.
 */
@Configuration
@EnableConfigurationProperties(OcrProperties.class)
@EnableScheduling
public class OcrIntegracionConfig {

    @Bean
    public FilterRegistrationBean<OcrApiKeyFilter> ocrApiKeyFilter(OcrProperties props) {
        FilterRegistrationBean<OcrApiKeyFilter> reg = new FilterRegistrationBean<>(new OcrApiKeyFilter(props));
        reg.addUrlPatterns(OcrApiKeyFilter.PREFIJO + "*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        reg.setName("ocrApiKeyFilter");
        return reg;
    }
}
