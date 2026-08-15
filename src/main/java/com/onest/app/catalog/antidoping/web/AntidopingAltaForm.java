package com.onest.app.catalog.antidoping.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de antidoping/alcoholimetria.
 * Nombres alineados con el formulario (fragments/antidoping-form.html).
 */
@Getter
@Setter
public class AntidopingAltaForm {

    private String nss;
    private String folio;
    private String tipoPrueba;
    private String sustancia;
    private String resultado;
    private String statusConclusion;
    private String observaciones;
}
