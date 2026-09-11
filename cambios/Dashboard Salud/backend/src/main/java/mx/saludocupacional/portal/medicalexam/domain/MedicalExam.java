package mx.saludocupacional.portal.medicalexam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.MedicalExamResult;
import mx.saludocupacional.portal.catalog.domain.MedicalExamType;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.LocalDate;

/**
 * Examen médico practicado a un colaborador.
 *
 * <p>Cubre los exámenes de nuevo ingreso, periódicos y posteriores a una
 * incapacidad. El resultado alimenta directamente los indicadores de aptitud
 * del dashboard.
 */
@Getter
@Setter
@Entity
@Table(name = "medical_exams")
public class MedicalExam extends SoftDeletableEntity {

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

    @Column(name = "fecha_examen", nullable = false)
    private LocalDate fechaExamen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_exam_type_id", nullable = false)
    private MedicalExamType examType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_exam_result_id", nullable = false)
    private MedicalExamResult examResult;

    @Column(columnDefinition = "text")
    private String observaciones;
}
