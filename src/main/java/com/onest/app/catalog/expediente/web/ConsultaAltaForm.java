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

    // Pase de salida FT-SO-02 (20-oct-2026), solo con tipo "pase_salida": el WS no tiene estos campos;
    // van a SERV_MED_TAG ligados al idArchivoRel de la consulta (PaseSalidaService).
    private String paseMotivo;
    private String paseFechaCita;
    private String paseSalida;
    private String paseEntrada;
    private String paseRegresa;
    private String paseIncapDias;
    private String paseIncapInicia;
    private String paseIncapTermina;
    private String paseObservaciones;

    // Pase de medicamento FT-SO-29 (20-oct-2026), opcional en cualquier consulta (casilla medGenerar=SI).
    // Varios medicamentos por pase (respuesta de SM 20-oct): un renglon por medicamento, mismas posiciones
    // en las cuatro listas. Vigencia = fecha autorizada para ingresar el medicamento, o permanente.
    private String medGenerar;
    private java.util.List<String> medMedicamento;
    private java.util.List<String> medGramaje;
    private java.util.List<String> medDuracion;
    private java.util.List<String> medFrecuencia;
    private String medVigencia;
    private String medPermanente;
    private String medObservaciones;

    // Carta de consentimiento para pruebas especiales FT-SO-23 (20-oct-2026): sale con el tipo de consulta
    // "pruebas_especiales" (respuesta de SM 20-oct: tipo de consulta en el menu, no casilla).
    private String pruebaAnticuerpos;
    private String pruebaAutoriza;
    private String pruebaFecha;
    private String pruebaLugar;
    private String pruebaResultado;
}
