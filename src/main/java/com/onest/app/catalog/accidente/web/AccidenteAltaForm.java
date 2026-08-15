package com.onest.app.catalog.accidente.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form del alta de accidente de trabajo.
 * Nombres alineados con el formulario (fragments/accidentes-form.html).
 */
@Getter
@Setter
public class AccidenteAltaForm {

    private String nss;
    private String fechaAccidente;
    private String tipoRiesgo;
    private String causaRt;
    private String diagnostico;
    private String sdi;
    private String statusCalificacion;
    private String costo;
    private String observaciones;
}
