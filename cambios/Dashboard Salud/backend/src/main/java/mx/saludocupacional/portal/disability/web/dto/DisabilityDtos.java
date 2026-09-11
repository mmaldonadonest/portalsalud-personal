package mx.saludocupacional.portal.disability.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import mx.saludocupacional.portal.disability.domain.Disability.Origen;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Objetos de entrada y salida del módulo de incapacidades. */
public final class DisabilityDtos {

    private DisabilityDtos() {
    }

    /** Incapacidad tal como aparece en listados y en el expediente. */
    public record DisabilityItem(
            Long id,
            Long empleadoId,
            String empleado,
            String numeroEmpleado,
            String predio,
            Long predioId,
            String tipo,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer dias,
            int horasNoTrabajadas,
            String folioImss,
            boolean esInterna,
            Origen origen,
            Long origenId,
            BigDecimal costo,
            String periodo
    ) {
    }

    /**
     * Alta de una incapacidad capturada directamente.
     *
     * <p>No admite origen: las incapacidades derivadas de un accidente o de
     * maternidad se crean desde su módulo, no por esta vía.
     */
    public record DisabilityRequest(
            @NotNull(message = "El colaborador es obligatorio")
            Long empleadoId,

            @NotNull(message = "El tipo de incapacidad es obligatorio")
            Long tipoId,

            @NotNull(message = "La fecha de inicio es obligatoria")
            LocalDate fechaInicio,

            LocalDate fechaFin,

            @NotNull(message = "Los días de incapacidad son obligatorios")
            @PositiveOrZero(message = "Los días no pueden ser negativos")
            Integer dias,

            @Size(max = 60, message = "El folio admite hasta 60 caracteres")
            String folioImss,

            Boolean esInterna,

            Long cuentaId,
            Long areaId,

            @PositiveOrZero(message = "El costo no puede ser negativo")
            BigDecimal costo
    ) {
    }
}
