package com.onest.app.catalog.dashboard.dto;

import java.util.List;

/**
 * KPIs de consumo de medicamentos, a partir del historico de los Excel del servicio medico
 * (familia {@code CONSUMIBLE} en {@code SERV_MED_BITACORA_METRICA}).
 *
 * <p><b>Es el unico modulo que no tiene contraparte en ORDS.</b> Los otros ocho fusionan lo que se
 * captura en el portal con lo que venia en Excel; aqui el portal no captura consumo de
 * medicamentos, asi que todo lo que se ve viene del historico. Si algun dia se captura en el
 * portal, este DTO ya tiene la forma.
 *
 * @param totalPiezas      piezas consumidas en el periodo
 * @param medicamentos     cuantos medicamentos distintos tuvieron consumo
 * @param porMedicamento   ranking de consumo
 * @param porPredio        consumo por predio
 * @param tendenciaMensual piezas por mes
 * @param equipoPorPredio  inventario de equipo, de la hoja MATERIAL FIJO. <b>Es otra cosa</b>: no
 *                         es consumo sino patrimonio, y por eso va aparte y no suma con lo demas
 */
public record DashboardConsumoDto(
        String fechaInicial,
        String fechaFinal,
        long totalPiezas,
        long medicamentos,
        List<ConteoSimpleDto> porMedicamento,
        List<ConteoSimpleDto> porPredio,
        List<PuntoMensualDto> tendenciaMensual,
        List<ConteoSimpleDto> equipoPorPredio
) {
}
