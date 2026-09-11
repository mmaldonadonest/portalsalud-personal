package mx.saludocupacional.portal.employee.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import mx.saludocupacional.portal.employee.domain.Employee.Genero;

import java.time.LocalDate;

/** Objetos de entrada y salida del módulo de colaboradores. */
public final class EmployeeDtos {

    private EmployeeDtos() {
    }

    /** Colaborador tal como aparece en listados. */
    public record EmployeeItem(
            Long id,
            String numeroEmpleado,
            String nombre,
            Genero genero,
            Integer edad,
            String predio,
            Long predioId,
            String cuenta,
            String area,
            String puesto,
            LocalDate fechaIngreso,
            Integer antiguedadAnios,
            boolean activo
    ) {
    }

    /** Alta o edición de un colaborador. */
    public record EmployeeRequest(
            @Size(max = 40, message = "El número de empleado admite hasta 40 caracteres")
            String numeroEmpleado,

            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 200, message = "El nombre admite hasta 200 caracteres")
            String nombre,

            Genero genero,

            @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
            LocalDate fechaNacimiento,

            @NotNull(message = "El predio es obligatorio")
            Long predioId,

            Long cuentaId,
            Long areaId,
            Long puestoId,
            Long agenciaId,
            LocalDate fechaIngreso,
            Boolean activo,

            @Size(max = 60)
            String codigoExternoRh
    ) {
    }

    /**
     * Un evento en la historia del colaborador.
     *
     * @param fecha  cuándo ocurrió
     * @param tipo   módulo que lo originó
     * @param titulo descripción breve para la línea de tiempo
     * @param detalle información complementaria, ausente si el usuario carece
     *                del permiso para ver datos clínicos
     */
    public record EventoTimeline(
            LocalDate fecha,
            String tipo,
            String titulo,
            String detalle,
            Long registroId
    ) {
    }

    /** Expediente unificado con la historia completa del colaborador. */
    public record ExpedienteResponse(
            EmployeeItem empleado,
            ResumenExpediente resumen,
            java.util.List<EventoTimeline> timeline
    ) {
    }

    /** Cifras acumuladas del colaborador en el año consultado. */
    public record ResumenExpediente(
            long atenciones,
            long examenes,
            long incapacidades,
            long diasIncapacidad,
            long accidentes,
            long pruebas
    ) {
    }
}
