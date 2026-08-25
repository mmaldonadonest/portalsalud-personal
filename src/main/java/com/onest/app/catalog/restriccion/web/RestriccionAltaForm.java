package com.onest.app.catalog.restriccion.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de una restriccion medica asignada a un NSS.
 * Nombres alineados con el formulario embebido en examen-shell.html.
 */
@Getter
@Setter
public class RestriccionAltaForm {

    private String nss;
    private String codigoRestriccion;
    private String descripcion;
    private String valorLimite;
    private String unidad;
    private String fechaInicio;
    private String fechaFin;
    private String fechaRevaloracion;
    private String temporalidad;
    private String observaciones;
    private String medicoResponsable;
    private String estatus;
}
