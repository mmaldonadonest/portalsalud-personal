package mx.saludocupacional.portal.medicalexam.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Objetos de entrada y salida del módulo de exámenes médicos. */
public final class MedicalExamDtos {

    private MedicalExamDtos() {
    }

    /** Examen tal como aparece en listados y en el expediente. */
    public record MedicalExamItem(
            Long id,
            Long empleadoId,
            String empleado,
            String numeroEmpleado,
            String predio,
            Long predioId,
            LocalDate fechaExamen,
            String tipo,
            Long tipoId,
            String resultado,
            Long resultadoId,
            String periodo
    ) {
    }

    /** Alta o edición de un examen médico. */
    public record MedicalExamRequest(
            @NotNull(message = "El colaborador es obligatorio")
            Long empleadoId,

            @NotNull(message = "La fecha del examen es obligatoria")
            LocalDate fechaExamen,

            @NotNull(message = "El tipo de examen es obligatorio")
            Long tipoId,

            @NotNull(message = "El resultado es obligatorio")
            Long resultadoId,

            Long cuentaId,
            String observaciones
    ) {
    }
}
