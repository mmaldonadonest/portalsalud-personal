package mx.saludocupacional.portal.accident.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import mx.saludocupacional.portal.employee.domain.Employee.Genero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Objetos de entrada y salida del módulo de accidentabilidad. */
public final class AccidentDtos {

    private AccidentDtos() {
    }

    /** Accidente tal como aparece en listados. */
    public record AccidentItem(
            Long id,
            Long empleadoId,
            String empleado,
            String predio,
            Long predioId,
            LocalDate fechaAccidente,
            String tipo,
            String causa,
            String estatus,
            Genero genero,
            boolean generaIncapacidad,
            Long incapacidadId,
            int diasPerdidos,
            BigDecimal costoCalificado,
            BigDecimal costoImprocedente,
            String periodo
    ) {
    }

    /**
     * Alta o edición de un accidente.
     *
     * <p>Cuando {@code diasIncapacidad} es mayor que cero, el sistema crea la
     * incapacidad correspondiente y la enlaza: no hay que capturarla aparte.
     */
    public record AccidentRequest(
            @NotNull(message = "El colaborador es obligatorio")
            Long empleadoId,

            @NotNull(message = "La fecha del accidente es obligatoria")
            LocalDate fechaAccidente,

            @NotNull(message = "El tipo de riesgo es obligatorio")
            Long tipoId,

            @NotNull(message = "La causa es obligatoria")
            Long causaId,

            @NotNull(message = "El estatus es obligatorio")
            Long estatusId,

            Genero genero,

            @PositiveOrZero(message = "Los días de incapacidad no pueden ser negativos")
            Integer diasIncapacidad,

            BigDecimal costoCalificado,
            BigDecimal costoImprocedente,
            String descripcion
    ) {
    }
}
