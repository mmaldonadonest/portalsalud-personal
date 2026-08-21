package com.onest.app.integration.intranet;

import java.util.Optional;

/**
 * Resuelve la URL de foto de perfil de un usuario contra la intranet Onest, a
 * partir de su email. Solo aplica al login JAVA (PortalUserDetailsService) - el
 * login LEGACY_PHP no tiene email disponible.
 */
public interface IntranetUserClient {

    /** Nunca lanza excepcion - Optional.empty() ante cualquier fallo (email vacio, red, 404, etc.). */
    Optional<String> avatarUrlPorEmail(String email);
}
