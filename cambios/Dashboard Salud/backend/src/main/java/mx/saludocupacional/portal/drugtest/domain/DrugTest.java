package mx.saludocupacional.portal.drugtest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Agencia;
import mx.saludocupacional.portal.catalog.domain.Area;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.DrugTestResult;
import mx.saludocupacional.portal.catalog.domain.DrugTestStatus;
import mx.saludocupacional.portal.catalog.domain.DrugTestType;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.catalog.domain.Puesto;
import mx.saludocupacional.portal.employee.domain.Employee;
import mx.saludocupacional.portal.shared.domain.Period;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.LocalDate;

/**
 * Prueba de antidoping o alcoholimetría aplicada a un colaborador.
 *
 * <p>Cuando la prueba se toma de un lote, el sistema descuenta una unidad del
 * inventario en la misma transacción: la existencia refleja la operación sin
 * que nadie tenga que ajustarla después.
 */
@Getter
@Setter
@Entity
@Table(name = "drug_tests")
public class DrugTest extends SoftDeletableEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agencia_id")
    private Agencia agencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @Column(name = "fecha_prueba", nullable = false)
    private LocalDate fechaPrueba;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drug_test_type_id", nullable = false)
    private DrugTestType testType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_test_result_id")
    private DrugTestResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_test_status_id")
    private DrugTestStatus status;

    /** Lote del que se tomó la prueba, si procede de inventario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private DrugTestBatch batch;
}
