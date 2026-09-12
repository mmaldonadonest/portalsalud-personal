package com.onest.app.audit.dto;

import java.util.List;

/** Pagina de la bitacora para el modulo Auditoria (/api/auditoria). */
public record AuditoriaDto(
        long total,
        int pagina,
        int porPagina,
        List<Evento> eventos
) {
    /** {@code fecha} en ISO yyyy-MM-ddTHH:mm:ss; {@code accion} create/update/delete/export/login. */
    public record Evento(
            Long id,
            String fecha,
            String usuario,
            String accion,
            String modulo,
            String entidad,
            String registro,
            String detalle,
            String ip
    ) {
    }

    /** Valores distintos para los selects de filtro. */
    public record Filtros(List<String> usuarios, List<String> modulos, List<String> acciones) {
    }
}
