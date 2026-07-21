package com.onest.app.catalog.expediente.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Respuesta de .../Servcio/conuslta_medica_usuario. php-old recorre $data->Datos
 * (fecha_consulta, tipo_consulta, area_accidente, id_consulta).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiowsConsultasResponse(
        @JsonProperty("Datos") List<Dato> datos
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dato(
            @JsonProperty("id_consulta") String idConsulta,
            @JsonProperty("fecha_consulta") String fechaConsulta,
            @JsonProperty("tipo_consulta") String tipoConsulta,
            @JsonProperty("area_accidente") String areaAccidente,
            // Campos adicionales usados por el detalle (viewConsultaDetails.php)
            @JsonProperty("area_involucrada") String areaInvolucrada,
            @JsonProperty("causa") String causa,
            @JsonProperty("peso") String peso,
            @JsonProperty("talla") String talla,
            @JsonProperty("imc") String imc,
            @JsonProperty("fc") String fc,
            @JsonProperty("fr") String fr,
            @JsonProperty("ta") String ta,
            @JsonProperty("temperatura") String temperatura,
            @JsonProperty("motivo") String motivo,
            @JsonProperty("exploracion") String exploracion,
            @JsonProperty("diagnostico") String diagnostico,
            @JsonProperty("tratamiento") String tratamiento,
            @JsonProperty("consulta_relacionada") String consultaRelacionada,
            @JsonProperty("firma_digital") String firmaDigital
    ) {
    }
}
