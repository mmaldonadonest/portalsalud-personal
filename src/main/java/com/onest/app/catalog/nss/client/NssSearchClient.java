package com.onest.app.catalog.nss.client;

import com.onest.app.catalog.nss.dto.CandidatoDto;
import com.onest.app.catalog.nss.dto.EmpleadoDto;
import java.util.List;
import java.util.Optional;

/**
 * Gateway hacia los WS ORDS legacy involucrados en la busqueda por NSS.
 * Equivale al flujo de searchEmploye.php + app::searchUser()/CheckEmploye()/searchProspecto()
 * de php-old/app/app.php.
 */
public interface NssSearchClient {

    /** searchUser -> POST .../Catalogo/usuario. Solo la PRIMERA fila (usado por login/email). */
    Optional<EmpleadoDto> findUsuario(String nss, String usuarioConsulta);

    /**
     * Igual que {@link #findUsuario} pero SIN truncar a la primera fila. El WS ya regresa
     * una fila por cada relacion laboral del NSS (join BIO_EMPLEADO -> BIO_DATOS_LABORALES_EMPLEADOS
     * -> biometrico_cuenta_SAP, ver docs/contextoWS.txt security/Catalogo/usuario) - si el NSS
     * esta asociado a varios predios/cuentas, esto regresa una fila por cada uno.
     */
    List<EmpleadoDto> findAsociaciones(String nss, String usuarioConsulta);

    /**
     * CheckEmploye -> POST .../Servcio/consulta_examen y, si Estado != 0,
     * dispara el ALTA en .../Servcio/Medico.
     */
    void checkEmploye(String nss, String tipoUsuario, String usuarioConsulta);

    /** searchProspecto -> POST .../Catalogo/Candidatos. */
    Optional<CandidatoDto> findProspecto(String termino);
}
