package mx.saludocupacional.portal.accident.domain;

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
import mx.saludocupacional.portal.catalog.domain.AccidentCause;
import mx.saludocupacional.portal.catalog.domain.AccidentStatus;
import mx.saludocupacional.portal.catalog.domain.AccidentType;
import mx.saludocupacional.portal.catalog.domain.Area;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.catalog.domain.Puesto;
import mx.saludocupacional.portal.disability.domain.Disability;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.employee.domain.Employee.Genero;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Accidente de trabajo o de trayecto.
 *
 * <p>Cuando el accidente incapacita, la incapacidad correspondiente se crea en
 * la misma transacción y queda enlazada aquí. Los días perdidos se leen de ese
 * registro enlazado, nunca se duplican en esta tabla: así el dashboard y el
 * módulo de incapacidades siempre coinciden.
 */
@Getter
@Setter
@Entity
@Table(name = "accidents")
public class Accident extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puesto_id")
    private Puesto puesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @Column(name = "fecha_accidente", nullable = false)
    private LocalDate fechaAccidente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accident_type_id", nullable = false)
    private AccidentType accidentType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accident_cause_id", nullable = false)
    private AccidentCause accidentCause;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accident_status_id", nullable = false)
    private AccidentStatus accidentStatus;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Genero genero;

    @Column(name = "genera_incapacidad", nullable = false)
    private boolean generaIncapacidad = false;

    /** Incapacidad originada por este accidente, si la hubo. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disability_id", unique = true)
    private Disability disability;

    @Column(name = "costo_calificado", precision = 14, scale = 2)
    private BigDecimal costoCalificado;

    @Column(name = "costo_improcedente", precision = 14, scale = 2)
    private BigDecimal costoImprocedente;

    @Column(columnDefinition = "text")
    private String descripcion;

    /** Días perdidos según la incapacidad enlazada; cero si no incapacitó. */
    public int getDiasPerdidos() {
        return disability == null ? 0 : disability.getDiasIncapacidad();
    }
}
