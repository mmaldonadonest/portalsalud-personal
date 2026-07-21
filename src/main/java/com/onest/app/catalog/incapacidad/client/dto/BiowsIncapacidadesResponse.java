package com.onest.app.catalog.incapacidad.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/consulta_incapacidad. php recorre $data->Datos.
 * Incluye los campos que usan la lista (expedienteShowConsults) y el detalle (viewIncapDetails).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsIncapacidadesResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_consulta") String idConsulta,
            @JsonProperty("fecha_consulta") String fechaConsulta,
            @JsonProperty("folio_incapacidad") String folioIncapacidad,
            @JsonProperty("ramo") String ramo,
            @JsonProperty("tipo_incapacidad") String tipoIncapacidad,
            @JsonProperty("fecha_inicio") String fechaInicio,
            @JsonProperty("fecha_termino") String fechaTermino,
            @JsonProperty("dias_autorizados") String diasAutorizados,
            @JsonProperty("salario_integrado") String salarioIntegrado,
            @JsonProperty("costo") String costo,
            @JsonProperty("imputable") String imputable,
            @JsonProperty("estado_dictamen") String estadoDictamen,
            @JsonProperty("goce_sueldo") String goceSueldo,
            @JsonProperty("complemento_salarial") String complementoSalarial,
            @JsonProperty("rubro") String rubro,
            @JsonProperty("fecha_alta") String fechaAlta,
            @JsonProperty("st2") String st2,
            @JsonProperty("alta") String alta,
            @JsonProperty("salario_acumulado") String salarioAcumulado,
            @JsonProperty("url_archivos") String urlArchivos,
            @JsonProperty("firma_digital") String firmaDigital
    ) {
    }
}
