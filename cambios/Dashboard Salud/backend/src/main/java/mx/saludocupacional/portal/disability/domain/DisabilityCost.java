package mx.saludocupacional.portal.disability.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Costo económico de una incapacidad.
 *
 * <p>Vive en una tabla aparte para poder otorgar permisos distintos sobre el
 * dato monetario y sobre la información clínica: la dirección puede necesitar el
 * costo agregado sin acceder al diagnóstico de la persona.
 */
@Getter
@Setter
@Entity
@Table(name = "disability_costs")
public class DisabilityCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "disability_id", nullable = false, unique = true)
    private Disability disability;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda = "MXN";

    @Column(name = "fecha_registro", nullable = false)
    private OffsetDateTime fechaRegistro = OffsetDateTime.now();
}
