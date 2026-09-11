package mx.saludocupacional.portal.disability.domain;

import jakarta.persistence.CascadeType;
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
import mx.saludocupacional.portal.catalog.domain.Area;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.DisabilityType;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.LocalDate;

/**
 * Incapacidad de un colaborador.
 *
 * <p>Es la fuente única de días perdidos y de su costo. Las incapacidades
 * derivadas de un accidente o de un caso de maternidad se crean desde el módulo
 * dueño del hecho y quedan enlazadas mediante {@code origenTipo} y
 * {@code origenId}; nunca se capturan dos veces.
 *
 * <p>Cualquier indicador de días perdidos se calcula sobre esta tabla, de modo
 * que accidentabilidad y maternidad no sumen por su cuenta y produzcan cifras
 * distintas a las del dashboard.
 */
@Getter
@Setter
@Entity
@Table(name = "disabilities")
public class Disability extends SoftDeletableEntity {

    /** Horas de una jornada laboral, para convertir días en horas no trabajadas. */
    public static final int HORAS_POR_JORNADA = 8;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disability_type_id", nullable = false)
    private DisabilityType disabilityType;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "dias_incapacidad", nullable = false)
    private Integer diasIncapacidad;

    @Column(name = "folio_imss", length = 60)
    private String folioImss;

    @Column(name = "es_interna", nullable = false)
    private boolean esInterna = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_tipo", nullable = false, length = 20)
    private Origen origenTipo = Origen.MANUAL;

    /** Identificador del accidente o del caso de maternidad que la originó. */
    @Column(name = "origen_id")
    private Long origenId;

    @OneToOne(mappedBy = "disability", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private DisabilityCost cost;

    /** Horas no trabajadas derivadas de los días autorizados. */
    public int getHorasNoTrabajadas() {
        return diasIncapacidad == null ? 0 : diasIncapacidad * HORAS_POR_JORNADA;
    }

    public boolean esGeneradaPorOtroModulo() {
        return origenTipo != Origen.MANUAL;
    }

    /** Procedencia del registro. */
    public enum Origen {
        /** Capturada directamente en el módulo de incapacidades. */
        MANUAL,
        /** Generada al registrar un accidente que incapacita. */
        ACCIDENTE,
        /** Generada al registrar un caso de maternidad. */
        MATERNIDAD
    }
}
