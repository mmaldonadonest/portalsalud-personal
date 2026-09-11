package mx.saludocupacional.portal.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Agencia;
import mx.saludocupacional.portal.catalog.domain.Area;
import mx.saludocupacional.portal.catalog.domain.Cuenta;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.catalog.domain.Puesto;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.LocalDate;
import java.time.Period;

/**
 * Colaborador de la organización.
 *
 * <p>Es la entidad central del portal: atenciones, exámenes, incapacidades,
 * accidentes, pruebas y casos de maternidad se relacionan con este registro. Al
 * consultar el expediente de una persona se obtiene su historia completa sin
 * cruzar información entre módulos.
 */
@Getter
@Setter
@Entity
@Table(name = "employees")
public class Employee extends SoftDeletableEntity {

    @Column(name = "numero_empleado", unique = true, length = 40)
    private String numeroEmpleado;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Genero genero;

    /**
     * Fecha de nacimiento.
     *
     * <p>La edad se calcula al consultar; almacenar un rango fijo dejaría al
     * colaborador congelado en un grupo etario que envejece con él.
     */
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

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

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(nullable = false)
    private boolean activo = true;

    /** Identificador en el sistema de Recursos Humanos, para la integración futura. */
    @Column(name = "codigo_externo_rh", length = 60)
    private String codigoExternoRh;

    /** Edad cumplida a la fecha indicada, o vacío si no se conoce el nacimiento. */
    public Integer edadA(LocalDate fecha) {
        if (fechaNacimiento == null) {
            return null;
        }
        return Period.between(fechaNacimiento, fecha).getYears();
    }

    public Integer getEdad() {
        return edadA(LocalDate.now());
    }

    /** Antigüedad en años cumplidos, o vacío si no se conoce el ingreso. */
    public Integer getAntiguedadAnios() {
        if (fechaIngreso == null) {
            return null;
        }
        return Period.between(fechaIngreso, LocalDate.now()).getYears();
    }

    public enum Genero {
        FEMENINO,
        MASCULINO
    }
}
