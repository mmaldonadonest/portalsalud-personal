package com.onest.app.catalog.nss.client;

import java.util.Optional;

/**
 * WS nuevo y aislado (Servcio/usuario_email, ver docs/ords-usuario-email-nuevo.sql) -
 * unico proposito: resolver el email real de un NSS para la foto de perfil (intranet
 * Onest resuelve por email, Catalogo/usuario no lo trae). Deliberadamente separado
 * de NssSearchClient para no acoplar esto a la busqueda por NSS.
 */
public interface EmailLookupClient {

    /** Nunca lanza excepcion - Optional.empty() ante cualquier fallo (NSS sin email, red, etc.). */
    Optional<String> emailPorNss(String nss);
}