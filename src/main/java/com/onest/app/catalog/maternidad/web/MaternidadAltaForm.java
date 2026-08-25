package com.onest.app.catalog.maternidad.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de un chequeo de seguimiento de maternidad.
 * Nombres alineados con el formulario (fragments/maternidad-form.html).
 */
@Getter
@Setter
public class MaternidadAltaForm {

    private String nss;
    private String semanasGestacion;
    private String fechaProbableParto;
    private String restriccionesLaborales;
    private String proximaRevision;
    private String observaciones;
    private String estatus;
    private String incapacidad;
    private String reincorporacion;
}
