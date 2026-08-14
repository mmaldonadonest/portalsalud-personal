package com.onest.app.catalog.expediente.dto;

/**
 * Consulta medica del historial de un NSS.
 * Equivale a cada elemento de Datos de showConsults (conuslta_medica_usuario)
 * usado por expedienteShowConsults.php?type=expedienteConsulta.
 */
public record ConsultaDto(
        String idConsulta,
        String fechaConsulta,
        String tipoConsulta,
        String areaAccidente,
        String causa
) {
    /**
     * true si la consulta es de tipo accidente/emergencia. No compara contra el catalogo
     * exacto del select de consulta-form.html ("incidente_accidente") porque los datos
     * reales de TBL_SERV_CONSULTA_MEDICA no lo respetan (verificado 2026-08-11: existen
     * "EMERGENCIA" y "ACCIDENTE" en mayusculas, "incidente_accidente" no aparece ni una vez
     * en produccion) - se busca la palabra en cualquier variante de mayusculas/minusculas.
     */
    public boolean esAccidente() {
        if (tipoConsulta == null) {
            return false;
        }
        String t = tipoConsulta.toLowerCase();
        return t.contains("accidente") || t.contains("emergencia");
    }
}
