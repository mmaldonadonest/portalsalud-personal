package mx.saludocupacional.portal.morbidity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.AttentionCause;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.InjuryType;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.LocalDate;

/**
 * Atención médica prestada a un colaborador.
 *
 * <p>Reúne en una sola tabla lo que el archivo origen repartía entre las hojas
 * de atenciones, causas y lesiones musculoesqueléticas. Cada atención se
 * clasifica por causa médica o por tipo de lesión, de modo que la morbilidad es
 * una lectura de estos registros y no un módulo de captura aparte.
 */
@Getter
@Setter
@Entity
@Table(name = "medical_attentions")
public class MedicalAttention extends SoftDeletableEntity {

    /**
     * Colaborador atendido.
     *
     * <p>Admite valor nulo únicamente para los datos históricos migrados desde
     * el Excel, donde la atención se registraba de forma agregada sin
     * identificar a la persona.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
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

    @Column(name = "fecha_atencion", nullable = false)
    private LocalDate fechaAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attention_cause_id")
    private AttentionCause attentionCause;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "injury_type_id")
    private InjuryType injuryType;

    @Column(name = "es_personal_inclusion", nullable = false)
    private boolean esPersonalInclusion = false;

    @Column(columnDefinition = "text")
    private String observaciones;

    public boolean esMusculoesqueletica() {
        return injuryType != null;
    }
}
