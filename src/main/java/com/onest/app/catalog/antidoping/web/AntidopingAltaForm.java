package com.onest.app.catalog.antidoping.web;

import java.util.List;
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

    // Cadena de custodia FT-SO-14 (20-oct-2026): el WS no tiene estos campos; van a SERV_MED_TAG
    // ligados al REG_ID de la prueba (CustodiaService).
    private String identTipo;
    private String identEmitida;
    private String identNumero;
    private String razon;
    private String razonOtro;
    private List<String> elementos;
    private String bac;
    private String tratamiento;
    private String diagnostico;
    private String medicamento;
    private String recetadoPor;
    private String horaToma;
    private String lugar;
    private String responsable;
}
