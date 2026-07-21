package com.onest.app.catalog.examen.client;

import java.util.Map;

/**
 * Gateway hacia el WS ORDS del examen medico.
 * Equivale a app::getDatsNss() (/Servcio/consulta_examen) y
 * app::sendHeredoFamDats(CAMBIO) (/Servcio/Medico) de php-old.
 */
public interface ExamenClient {

    /**
     * Datos del examen para un NSS, APLANADOS a claves punteadas
     * (p.ej. "NEUROLOGIA.AVC", "SERV_MED_EXPLORACION_FISICA.PESO").
     * Mapa vacio si no hay examen.
     */
    Map<String, String> getExamenData(String nss);

    /**
     * Guarda el examen (CAMBIO) reconstruyendo el payload anidado desde las claves
     * punteadas. campos = name->value de las secciones cargadas; firma = data-url.
     * Devuelve el mensaje Proceso del WS.
     */
    String guardar(String nss, Map<String, String> campos, String firma);
}
