package mx.saludocupacional.portal.drugtest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Predio;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Movimiento de inventario sobre un lote de pruebas.
 *
 * <p>Es el único modo de alterar la existencia. Registrar una prueba de
 * antidoping genera automáticamente un consumo, de forma que el inventario
 * refleja la operación real sin captura adicional.
 */
@Getter
@Setter
@Entity
@Table(name = "drug_test_inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private DrugTestBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 160)
    private String referencia;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "created_by")
    private Long createdBy;

    /** Naturaleza del movimiento. */
    public enum Tipo {
        /** Recepción de unidades nuevas. */
        ENTRADA,
        /** Aplicación de una prueba. */
        CONSUMO,
        /** Corrección tras un conteo físico. */
        AJUSTE
    }
}
