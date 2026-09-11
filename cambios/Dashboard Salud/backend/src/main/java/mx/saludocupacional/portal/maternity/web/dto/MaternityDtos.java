package mx.saludocupacional.portal.maternity.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import mx.saludocupacional.portal.maternity.domain.MaternityCase.Estatus;

import java.time.LocalDate;

/** Objetos de entrada y salida del módulo de maternidad. */
public final class MaternityDtos {

    private MaternityDtos() {
    }

    /** Caso tal como aparece en listados. */
    public record MaternityItem(
            Long id,
            Long empleadaId,
            String empleada,
            String predio,
            Long predioId,
            LocalDate fechaInicioIncapacidad,
            LocalDate fechaProbableParto,
            Integer diasIncapacidad,
            Estatus estatus,
            Long incapacidadId,
            String periodo
    ) {
    }

    /** Alta o edición de un caso; genera su incapacidad enlazada. */
    public record MaternityRequest(
            @NotNull(message = "La colaboradora es obligatoria")
            Long empleadaId,

            @NotNull(message = "La fecha de inicio es obligatoria")
            LocalDate fechaInicioIncapacidad,

            LocalDate fechaProbableParto,

            @NotNull(message = "Los días de incapacidad son obligatorios")
            @PositiveOrZero(message = "Los días no pueden ser negativos")
            Integer diasIncapacidad,

            Estatus estatus
    ) {
    }
}
