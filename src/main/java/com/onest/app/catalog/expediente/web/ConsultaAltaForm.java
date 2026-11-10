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

    // Nota de fisioterapia FT-SO-38 (C-065, 9-nov-2026): tipo de consulta "fisioterapia" (FisioterapiaService).
    // Motivo, 1a toma (signos + exploracion) y plan (tratamiento) salen de los campos de la consulta;
    // las listas fisioToma* traen la 2a y 3a toma en ese orden.
    private String fisioDxMedico;
    private String fisioEvolucion;
    private String fisioTratPrevio;
    private String fisioIncapTipo;
    private String fisioIncapDias;
    private String fisioIncapInicio;
    private String fisioDxFisio;
    private java.util.List<String> fisioTomaFecha;
    private java.util.List<String> fisioTomaPeso;
    private java.util.List<String> fisioTomaTalla;
    private java.util.List<String> fisioTomaImc;
    private java.util.List<String> fisioTomaFc;
    private java.util.List<String> fisioTomaFr;
    private java.util.List<String> fisioTomaTa;
    private java.util.List<String> fisioTomaNota;
}
