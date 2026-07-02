package com.onest.app.catalog.nss.client;

import com.onest.app.catalog.nss.dto.CandidatoDto;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import java.util.Optional;

/**
 * Gateway hacia los WS ORDS legacy involucrados en la busqueda por NSS.
 * Equivale al flujo de searchEmploye.php + app::searchUser()/CheckEmploye()/searchProspecto()
 * de php-old/app/app.php.
 */
public interface NssSearchClient {

    /** searchUser -> POST .../Catalogo/usuario. */
    Optional<EmpleadoDto> findUsuario(String nss, String usuarioConsulta);

    /**
     * CheckEmploye -> POST .../Servcio/consulta_examen y, si Estado != 0,
     * dispara el ALTA en .../Servcio/Medico.
     */
    void checkEmploye(String nss, String tipoUsuario, String usuarioConsulta);

    /** searchProspecto -> POST .../Catalogo/Candidatos. */
    Optional<CandidatoDto> findProspecto(String termino);
}
