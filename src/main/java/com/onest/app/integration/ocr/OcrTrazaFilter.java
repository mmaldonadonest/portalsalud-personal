package com.onest.app.integration.ocr;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Traza completa de lo que OCR manda al portal (/api/integracion/**): origen, cabeceras, JSON tal
 * cual llego, respuesta y tiempo. Va al logger {@link #LOGGER} ("ocr.integracion"), que
 * logback-spring.xml manda ademas a logs/portal-salud-ocr.log.
 *
 * <p>Corre ANTES del filtro de API key para que tambien quede registrada una llave mala o
 * ausente (401) con el cuerpo que traia. Pedido 5-oct-2026: las pruebas con OCR se hacen
 * directamente en produccion, asi que se registra TODO, incluida la X-Api-Key, hasta que la
 * integracion este estable; despues conviene enmascararla.
 */
public class OcrTrazaFilter extends OncePerRequestFilter {

    public static final String LOGGER = "ocr.integracion";
    private static final Logger log = LoggerFactory.getLogger(LOGGER);
    /** Tope por cuerpo para no reventar el log con un JSON absurdo; el contrato cabe de sobra. */
    private static final int MAX_CUERPO = 64 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(OcrApiKeyFilter.PREFIJO);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // El cuerpo se lee completo ANTES de seguir la cadena: asi queda en la traza aunque el
        // filtro de API key rechace con 401 sin llegar a leerlo, y el controlador lo recibe igual.
        CuerpoCacheado req = new CuerpoCacheado(request);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
        long inicio = System.currentTimeMillis();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = System.currentTimeMillis() - inicio;
            try {
                log.info("[ocr-http] {} {}{} desde {} (X-Forwarded-For={}) -> {} en {} ms\n  cabeceras: {}\n  recibido:  {}\n  respondido: {}",
                        req.getMethod(), req.getRequestURI(), req.getQueryString() == null ? "" : "?" + req.getQueryString(),
                        req.getRemoteAddr(), valor(req.getHeader("X-Forwarded-For")),
                        res.getStatus(), ms, cabeceras(req), cuerpo(req.cuerpo), cuerpo(res.getContentAsByteArray()));
            } catch (RuntimeException ex) {
                log.warn("[ocr-http] no se pudo trazar la peticion: {}", ex.getMessage());
            }
            // Imprescindible: el wrapper retuvo el cuerpo de la respuesta, hay que soltarlo al cliente
            res.copyBodyToResponse();
        }
    }

    /** Request con el cuerpo en memoria, releible las veces que haga falta. */
    private static final class CuerpoCacheado extends HttpServletRequestWrapper {
        private final byte[] cuerpo;

        CuerpoCacheado(HttpServletRequest request) throws IOException {
            super(request);
            this.cuerpo = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bytes = new ByteArrayInputStream(cuerpo);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bytes.read();
                }

                @Override
                public boolean isFinished() {
                    return bytes.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // lectura sincrona; no se usa async
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String enc = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    enc == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(enc)));
        }
    }

    private static String cabeceras(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        for (String nombre : Collections.list(req.getHeaderNames())) {
            // Cookie/Authorization no viajan en esta API; si llegaran, no aportan y podrian ser de un usuario
            if ("cookie".equalsIgnoreCase(nombre) || "authorization".equalsIgnoreCase(nombre)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(nombre).append('=').append(String.join(",", Collections.list(req.getHeaders(nombre))));
        }
        return sb.toString();
    }

    private static String cuerpo(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "(vacio)";
        }
        String s = new String(bytes, 0, Math.min(bytes.length, MAX_CUERPO), StandardCharsets.UTF_8);
        return bytes.length > MAX_CUERPO ? s + "…(" + bytes.length + " bytes)" : s;
    }

    private static String valor(String v) {
        return v == null ? "-" : v;
    }
}
