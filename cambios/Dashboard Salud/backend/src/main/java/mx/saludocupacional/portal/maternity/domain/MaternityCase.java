package mx.saludocupacional.portal.maternity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.disability.domain.Disability;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.LocalDate;

/**
 * Caso de maternidad de una colaboradora.
 *
 * <p>Conserva atributos que solo tienen sentido aquí, como la fecha probable de
 * parto y el seguimiento del caso. La incapacidad correspondiente se crea en la
 * misma transacción y queda enlazada, de modo que los días de maternidad se
 * cuentan una sola vez, junto con el resto de incapacidades.
 */
@Getter
@Setter
@Entity
@Table(name = "maternity_cases")
public class MaternityCase extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @Column(name = "fecha_inicio_incapacidad", nullable = false)
    private LocalDate fechaInicioIncapacidad;

    @Column(name = "fecha_probable_parto")
    private LocalDate fechaProbableParto;

    @Column(name = "dias_incapacidad", nullable = false)
    private Integer diasIncapacidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estatus estatus = Estatus.ACTIVO;

    /** Incapacidad generada por este caso. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disability_id", unique = true)
    private Disability disability;

    /** Situación del caso. */
    public enum Estatus {
        ACTIVO,
        CONCLUIDO
    }
}
