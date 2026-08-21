package com.onest.app.catalog.consumible.dto;

/**
 * Registro mensual de inventario de consumibles de antidoping (kits de prueba),
 * por PREDIO. Equivale a Datos de consulta_consumibles (docs/ords-antidoping-consumibles.sql
 * BLOQUE 2). NO esta asociado a NSS/empleado.
 */
public record ConsumibleDto(
        String idRegistro,
        String predio,
        int anio,
        int mes,
        int cantidadInicial,
        int entregaMensual,
        int consumoMensual,
        String observaciones,
        String fechaRegistro
) {
}
