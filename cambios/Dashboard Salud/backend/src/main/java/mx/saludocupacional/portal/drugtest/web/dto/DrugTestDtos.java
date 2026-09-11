package mx.saludocupacional.portal.drugtest.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import mx.saludocupacional.portal.drugtest.domain.InventoryMovement.Tipo;

import java.time.LocalDate;

/** Objetos de entrada y salida de antidoping e inventario. */
public final class DrugTestDtos {

    private DrugTestDtos() {
    }

    /** Prueba tal como aparece en listados. */
    public record DrugTestItem(
            Long id,
            Long empleadoId,
            String empleado,
            String predio,
            Long predioId,
            LocalDate fechaPrueba,
            String tipo,
            String resultado,
            String estatus,
            String agencia,
            String lote,
            String periodo
    ) {
    }

    /** Alta de una prueba; si indica lote, descuenta una unidad del inventario. */
    public record DrugTestRequest(
            @NotNull(message = "El colaborador es obligatorio")
            Long empleadoId,

            @NotNull(message = "La fecha de la prueba es obligatoria")
            LocalDate fechaPrueba,

            @NotNull(message = "El tipo de prueba es obligatorio")
            Long tipoId,

            Long resultadoId,
            Long estatusId,
            Long agenciaId,
            Long loteId
    ) {
    }

    /** Lote con su existencia calculada y su estado de caducidad. */
    public record BatchItem(
            Long id,
            String lote,
            String predio,
            Long predioId,
            LocalDate fechaCaducidad,
            long diasParaCaducar,
            Integer cantidadInicial,
            int consumo,
            int disponible,
            int porcentajeDisponible,
            String estadoCaducidad,
            boolean activo
    ) {
    }

    /** Alta de un lote de pruebas. */
    public record BatchRequest(
            @NotBlank(message = "El número de lote es obligatorio")
            @Size(max = 60, message = "El lote admite hasta 60 caracteres")
            String lote,

            @NotNull(message = "El predio es obligatorio")
            Long predioId,

            @NotNull(message = "La fecha de caducidad es obligatoria")
            LocalDate fechaCaducidad,

            @NotNull(message = "La cantidad inicial es obligatoria")
            @Positive(message = "La cantidad inicial debe ser mayor que cero")
            Integer cantidadInicial
    ) {
    }

    /** Movimiento manual de inventario. */
    public record MovementRequest(
            @NotNull(message = "El lote es obligatorio")
            Long loteId,

            @NotNull(message = "El tipo de movimiento es obligatorio")
            Tipo tipo,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor que cero")
            Integer cantidad,

            @NotNull(message = "La fecha es obligatoria")
            LocalDate fecha,

            @Size(max = 160)
            String referencia
    ) {
    }

    /** Movimiento registrado sobre un lote. */
    public record MovementItem(
            Long id,
            String lote,
            Tipo tipo,
            Integer cantidad,
            LocalDate fecha,
            String referencia
    ) {
    }

    /** Alerta de inventario derivada de los umbrales configurados. */
    public record AlertaInventario(
            Long loteId,
            String lote,
            String predio,
            String severidad,
            String mensaje,
            long diasParaCaducar,
            int disponible
    ) {
    }
}
