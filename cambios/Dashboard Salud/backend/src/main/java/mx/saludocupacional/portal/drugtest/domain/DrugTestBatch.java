package mx.saludocupacional.portal.drugtest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.shared.domain.BaseEntity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Lote de pruebas de antidoping.
 *
 * <p>La existencia disponible no se guarda: se calcula sumando los movimientos
 * de entrada y restando los de consumo. Así el inventario siempre cuadra con su
 * historia y nadie puede ajustar el stock sin dejar rastro.
 */
@Getter
@Setter
@Entity
@Table(name = "drug_test_batches")
public class DrugTestBatch extends BaseEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @Column(name = "fecha_caducidad", nullable = false)
    private LocalDate fechaCaducidad;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(nullable = false)
    private boolean activo = true;

    /** Días que faltan para caducar; negativo si el lote ya venció. */
    public long diasParaCaducar() {
        return ChronoUnit.DAYS.between(LocalDate.now(), fechaCaducidad);
    }

    public boolean estaVencido() {
        return fechaCaducidad.isBefore(LocalDate.now());
    }
}
