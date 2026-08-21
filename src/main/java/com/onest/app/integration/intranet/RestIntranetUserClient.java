package com.onest.app.integration.intranet;

import com.onest.app.integration.intranet.dto.IntranetUserResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementacion contra GET {baseUrl}/api/v1/user/mail/{email}. La imagen vive en
 * {baseUrl}/uploads/profile_image/{guid}.jpg (mismo host, ruta fija, confirmado
 * contra el codigo legacy que se esta reemplazando aqui).
 *
 * Deliberadamente NUNCA propaga una excepcion: esto se llama durante el login
 * (PortalUserDetailsService) y una intranet lenta/caida no debe impedir
 * autenticar - solo se pierde la foto de perfil, se cae al avatar default.
 */
@Component
public class RestIntranetUserClient implements IntranetUserClient {

    private static final Logger log = LoggerFactory.getLogger(RestIntranetUserClient.class);
    private static final String PATH_USER_BY_MAIL = "/api/v1/user/mail/{email}";
    private static final String PATH_PROFILE_IMAGE = "/uploads/profile_image/%s.jpg";

    private final RestClient intranetRestClient;
    private final IntranetProperties properties;

    public RestIntranetUserClient(RestClient intranetRestClient, IntranetProperties properties) {
        this.intranetRestClient = intranetRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<String> avatarUrlPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            IntranetUserResponse response = intranetRestClient.get()
                    .uri(PATH_USER_BY_MAIL, email.trim())
                    .retrieve()
                    .body(IntranetUserResponse.class);

            if (response == null || response.guid() == null || response.guid().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(properties.baseUrl() + PATH_PROFILE_IMAGE.formatted(response.guid()));
        } catch (Exception ex) {
            log.warn("[intranet] no se pudo resolver la foto de perfil de {}: {}", email, ex.getMessage());
            return Optional.empty();
        }
    }
}
