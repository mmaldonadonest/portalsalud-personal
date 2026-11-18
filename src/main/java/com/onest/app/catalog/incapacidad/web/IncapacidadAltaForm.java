package com.onest.app.catalog.incapacidad.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de incapacidad (ViewIncap -> incap.php).
 * Nombres alineados con el formulario (fragments/incapacidad-form.html).
 */
@Getter
@Setter
public class IncapacidadAltaForm {

    private String nss;
    private String idArchivoRel;
    private String firma;

    private String folio;
    private String rubro;
    private String ramo;
    private String tipo;
    private String fechaPrimera;
    private String fechaInicio;
    private String fechaTermino;
    private String diasAutorizados;
    private String salarioIntegrado;
    private String costo;
    private String imputable;
    private String estadoDictamen;
    private String goceSueldo;
    private String complementoSalarial;
    private String salarioAcumulado;
    private String st2;
    private String alta;
    private String fechaAlta;

    // C-051 / C-052 (18-nov-2026): el WS no tiene columnas para esto; van a SERV_MED_TAG ligados al idArchivoRel
    /** REG_ID de la incapacidad que continua esta subsecuente (vacio en una inicial). */
    private String anterior;
    private String diagnostico;
}
