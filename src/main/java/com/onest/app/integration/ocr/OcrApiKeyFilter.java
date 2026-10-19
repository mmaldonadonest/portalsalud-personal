package com.onest.app.integration.ocr;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticacion de /api/integracion/**: encabezado X-Api-Key con la llave que el portal emitio
 * para OCR. No usa la sesion de usuarios (OCR es un sistema) ni CSRF; SecurityConfiguration deja
 * pasar la ruta y este filtro decide. Sin llave configurada (WAR sin -Pqa/-Pprod) responde 503:
 * la API esta deshabilitada, no "mal autenticada".
 */
public class OcrApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OcrApiKeyFilter.class);
    public static final String PREFIJO = "/api/integracion/";
    public static final String HEADER = "X-Api-Key";

    private final OcrProperties props;

    public OcrApiKeyFilter(OcrProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(PREFIJO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!props.habilitada()) {
            log.warn("[ocr] llamada a {} con la integracion deshabilitada (sin api-key en el WAR)", request.getServletPath());
            responder(response, 503, "{\"error\":\"NO_DISPONIBLE\",\"reintentarEn\":300}");
            return;
        }
        String llave = request.getHeader(HEADER);
        if (llave == null || !MessageDigest.isEqual(llave.trim().getBytes(StandardCharsets.UTF_8),
                props.apiKey().trim().getBytes(StandardCharsets.UTF_8))) {
            log.warn("[ocr] X-Api-Key invalida o ausente desde {} a {}", request.getRemoteAddr(), request.getServletPath());
            responder(response, 401, "{\"error\":\"NO_AUTORIZADO\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void responder(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }
}
