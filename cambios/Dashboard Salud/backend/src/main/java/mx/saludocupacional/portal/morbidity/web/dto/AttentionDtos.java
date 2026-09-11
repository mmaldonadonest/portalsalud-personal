package mx.saludocupacional.portal.morbidity.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Objetos de entrada y salida del módulo de atenciones médicas. */
public final class AttentionDtos {

    private AttentionDtos() {
    }

    /** Atención tal como aparece en listados y en el expediente. */
    public record AttentionItem(
            Long id,
            Long empleadoId,
            String empleado,
            String predio,
            Long predioId,
            LocalDate fechaAtencion,
            String causa,
            Long causaId,
            String tipoLesion,
            Long tipoLesionId,
            boolean esPersonalInclusion,
            String periodo
    ) {
    }

    /**
     * Alta o edición de una atención.
     *
     * <p>Debe indicarse la causa médica, el tipo de lesión, o ambos; una
     * atención sin clasificar no aportaría nada a la morbilidad.
     */
    public record AttentionRequest(
            Long empleadoId,

            @NotNull(message = "El predio es obligatorio")
            Long predioId,

            @NotNull(message = "La fecha de atención es obligatoria")
            LocalDate fechaAtencion,

            Long causaId,
            Long tipoLesionId,
            Long cuentaId,
            Boolean esPersonalInclusion,
            String observaciones
    ) {
    }
}
