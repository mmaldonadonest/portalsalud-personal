package com.onest.app.catalog.incapacidad.client;

import com.onest.app.catalog.incapacidad.client.dto.BiowsIncapacidadAltaRequest;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDetalleDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadDto;
import com.onest.app.catalog.incapacidad.dto.IncapacidadReporteDto;
import java.util.List;
import java.util.Optional;

/**
 * Gateway hacia el WS ORDS de incapacidades (por NSS).
 * Equivale a app::showIncapacidades() de php-old/app/app.php.
 */
public interface IncapacidadClient {

    /** showIncapacidades -> POST .../Servcio/consulta_incapacidad. */
    List<IncapacidadDto> findIncapacidades(String nss);

    /** Detalle de una incapacidad (reconsulta y filtra por id_consulta, como viewIncapDetails.php). */
    Optional<IncapacidadDetalleDto> findIncapacidadDetail(String nss, String idConsulta);

    /** addIncapacidad -> POST .../Servcio/incapacidades. Devuelve el mensaje Proceso. */
    String crearIncapacidad(BiowsIncapacidadAltaRequest request);

    /**
     * Reporte administrativo por rango de fechas (todas las NSS), para RH/contabilidad.
     * Equivale a app::traerDatosGeneralIncap(). fechaInicial/fechaFinal en formato "dd/MM/yy".
     */
    List<IncapacidadReporteDto> reportePorFecha(String fechaInicial, String fechaFinal);
}
