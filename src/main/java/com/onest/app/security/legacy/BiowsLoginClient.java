package com.onest.app.security.legacy;

import com.onest.app.config.BiowsProperties;
import com.onest.app.security.legacy.dto.BiowsLoginRequest;
import com.onest.app.security.legacy.dto.BiowsLoginResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter de login legacy contra el WS ORDS .../login/registro_app (app::loginApiRest).
 * 25-nov-2026: usa su propio RestClient (loginRestClient) para poder apuntar a los servicios de la base de SSO
 * (portal.biows.login-base-url) sin mover los demas WS de biows. Mismo contrato en los dos lados.
 */
@Component
public class BiowsLoginClient {

    private static final Logger log = LoggerFactory.getLogger(BiowsLoginClient.class);
    private final RestClient biowsRestClient;
    private final BiowsProperties properties;

    public BiowsLoginClient(RestClient loginRestClient, BiowsProperties properties) {
        this.biowsRestClient = loginRestClient;
        this.properties = properties;
    }

    public boolean autenticar(String usuario, String password) {
        try {
            BiowsLoginResponse response = biowsRestClient.post()
                    .uri(properties.loginPathEfectivo())
                    .body(new BiowsLoginRequest(usuario, password, properties.appId()))
                    .retrieve()
                    .body(BiowsLoginResponse.class);

            boolean ok = esExitoso(response);
            log.info("[biows] POST {}{} usuario={} -> {}", properties.loginBaseUrlEfectiva(), properties.loginPathEfectivo(), usuario,
                    ok ? "OK" : "RECHAZADO");
            return ok;
        } catch (RestClientException ex) {
            log.warn("[biows] error en login legacy usuario={}: {}", usuario, ex.getMessage());
            return false;
        }
    }

    private static final String LOGIN_OK = "Login correcto";

    /**
     * Regla de exito fiel a checklogApi.php: recorre Datos y se queda con el
     * ultimo Mensaje; hay login si ese Mensaje es "Login correcto".
     */
    private static boolean esExitoso(BiowsLoginResponse response) {
        if (response == null || response.datos() == null || response.datos().isEmpty()) {
            return false;
        }
        String ultimoMensaje = null;
        for (BiowsLoginResponse.Dato dato : response.datos()) {
            if (dato != null) {
                ultimoMensaje = dato.mensaje();
            }
        }
        return ultimoMensaje != null && LOGIN_OK.equalsIgnoreCase(ultimoMensaje.trim());
    }
}
