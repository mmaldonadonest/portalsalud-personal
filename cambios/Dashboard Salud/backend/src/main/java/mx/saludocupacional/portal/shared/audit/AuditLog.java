package mx.saludocupacional.portal.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Asiento de la bitácora.
 *
 * <p>Una sola tabla registra los cambios de todos los módulos. Cuando el
 * registro afectado contiene información clínica, se marca
 * {@code contieneDatosSensibles} y se omite el detalle del texto libre: la
 * bitácora prueba que algo cambió sin replicar el diagnóstico.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction accion;

    @Column(nullable = false, length = 60)
    private String modulo;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior")
    private String valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @Column(name = "contiene_datos_sensibles", nullable = false)
    private boolean contieneDatosSensibles = false;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora = OffsetDateTime.now();
}
