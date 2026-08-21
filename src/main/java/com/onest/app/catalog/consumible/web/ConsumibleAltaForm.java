package com.onest.app.catalog.consumible.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de consumibles de antidoping (inventario por PREDIO/mes).
 * Nombres alineados con pages/consumibles.html.
 */
@Getter
@Setter
public class ConsumibleAltaForm {

    private String predio;
    private Integer anio;
    private Integer mes;
    private Integer cantidadInicial;
    private Integer entregaMensual;
    private Integer consumoMensual;
    private String observaciones;
}
