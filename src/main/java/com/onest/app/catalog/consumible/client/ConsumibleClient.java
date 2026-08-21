package com.onest.app.catalog.consumible.client;

import com.onest.app.catalog.consumible.client.dto.BiowsConsumibleAltaRequest;
import com.onest.app.catalog.consumible.dto.ConsumibleDto;
import java.util.List;

/**
 * Gateway hacia el WS ORDS de consumibles de antidoping (inventario por PREDIO/mes).
 * Backend aplicado 2026-08-18, ver docs/ords-antidoping-consumibles.sql. NO esta
 * asociado a NSS/empleado.
 */
public interface ConsumibleClient {

    /** POST .../Servcio/consulta_consumibles. predio null/vacio regresa todos los registros. */
    List<ConsumibleDto> listar(String predio);

    /** POST .../Servcio/consumibles. Devuelve el mensaje Proceso. */
    String crear(BiowsConsumibleAltaRequest request);
}
