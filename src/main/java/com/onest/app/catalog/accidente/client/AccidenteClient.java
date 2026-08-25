package com.onest.app.catalog.accidente.client;

import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteAltaRequest;
import com.onest.app.catalog.accidente.client.dto.BiowsAccidenteSeguimientoAltaRequest;
import com.onest.app.catalog.accidente.dto.AccidenteDto;
import com.onest.app.catalog.accidente.dto.AccidenteReporteDto;
import com.onest.app.catalog.accidente.dto.AccidenteSeguimientoDto;
import java.util.List;

/**
 * Gateway hacia el WS ORDS de accidentes de trabajo (por NSS).
 * Backend aplicado y verificado 2026-08-14, ver docs/ords-accidentes.sql.
 */
public interface AccidenteClient {

    /** POST .../Servcio/consulta_accidente. */
    List<AccidenteDto> findAccidentes(String nss);

    /** POST .../Servcio/accidente. Devuelve el mensaje Proceso. */
    String crearAccidente(BiowsAccidenteAltaRequest request);

    /**
     * Reporte administrativo por rango de fechas (todas las NSS), para el dashboard.
     * Backend aplicado y verificado 2026-08-17. fechaInicial/fechaFinal en formato "dd/MM/yy".
     */
    List<AccidenteReporteDto> reportePorFecha(String fechaInicial, String fechaFinal);

    /**
     * POST .../Servcio/accidente_seguimiento. Alta de una entrada de seguimiento sobre un
     * caso ya registrado (docs/ords-accidentes-seguimiento.sql).
     */
    String registrarSeguimiento(BiowsAccidenteSeguimientoAltaRequest request);

    /** POST .../Servcio/consulta_accidente_seguimiento. Historial de un caso especifico. */
    List<AccidenteSeguimientoDto> findSeguimientos(long accidenteRegId);
}
