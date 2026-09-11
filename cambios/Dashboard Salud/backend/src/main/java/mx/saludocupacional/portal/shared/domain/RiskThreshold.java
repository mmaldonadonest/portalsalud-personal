package mx.saludocupacional.portal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Umbral configurable que alimenta el semáforo de riesgo y las alertas.
 *
 * <p>Ningún límite vive en el código: los días que definen un predio en riesgo
 * crítico, los días de caducidad que disparan una alerta de inventario y el
 * porcentaje de variación que genera un insight se administran desde la
 * interfaz. Cambiarlos no requiere volver a compilar ni desplegar.
 */
@Getter
@Setter
@Entity
@Table(name = "risk_thresholds")
public class RiskThreshold extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String clave;

    @Column(nullable = false, length = 240)
    private String descripcion;

    @Column(name = "valor_numero", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorNumero;
}
