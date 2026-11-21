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
    /** E-017: solo la CONCLUSION (va al WS en STATUS_CONCLUSION, como siempre). */
    private String statusConclusion;
    /** E-017 (Word SM 10-nov, punto 15): ESTATUS aparte, LABORANDO o BAJA_NO_CONTRATADO (base del portal). */
    private String estatus;
    private String observaciones;

    // C-034 / C-077 (10-nov, Word SM punto 15): la pantalla puede mandar LOS DOS tipos (casillas «tipos»), cada uno
    // con su resultado (resultado = antidoping, resultadoAlcohol = alcoholimetria), varias sustancias y la fecha de
    // aplicacion. Si «tipos» viene vacio se usa tipoPrueba/resultado/sustancia (seleccion aleatoria y pantallas viejas).
    private List<String> tipos;
    private String resultadoAlcohol;
    private List<String> sustancias;
    private String fechaAplicacion;

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
