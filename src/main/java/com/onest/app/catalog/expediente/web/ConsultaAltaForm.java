package com.onest.app.catalog.expediente.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de consulta medica (viewConsulMedic -> consultMedic.php).
 * Nombres de campo alineados con el formulario (fragments/consulta-form.html).
 */
@Getter
@Setter
public class ConsultaAltaForm {

    private String nss;
    private String idArchivoRel;
    private String tipoConsulta;
    private String areaAccidente;
    private String areaAnatomica;
    private String causa;
    private String peso;
    private String talla;
    private String imc;
    private String fc;
    private String fr;
    private String ta;
    private String temperatura;
    private String motivo;
    private String exploracion;
    private String diagnostico;
    private String tratamiento;
    private String firma;
}
