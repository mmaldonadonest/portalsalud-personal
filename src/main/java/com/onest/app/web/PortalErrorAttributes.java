package com.onest.app.web;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Agrega el trace-id de la peticion (TraceIdFilter / MDC) al modelo de la pagina de error
 * (templates/error.html) como "folio" para que el usuario lo pueda reportar y se busque en
 * el log. Lo demas (status, message, path, timestamp) es el modelo estandar de Spring Boot.
 */
@Component
public class PortalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);
        attrs.put("traceId", MDC.get("traceId"));
        return attrs;
    }
}
