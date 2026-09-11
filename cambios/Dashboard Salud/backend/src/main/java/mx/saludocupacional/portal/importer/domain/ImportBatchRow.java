package mx.saludocupacional.portal.importer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fila detectada dentro de un lote de importación.
 *
 * <p>Conserva el contenido original junto con el resultado de su validación, de
 * modo que el usuario pueda ver exactamente qué fila del archivo se rechazó y
 * por qué.
 */
@Getter
@Setter
@Entity
@Table(name = "import_batch_rows")
public class ImportBatchRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id", nullable = false)
    private ImportBatch importBatch;

    @Column(name = "fila_origen", nullable = false)
    private Integer filaOrigen;

    @Column(name = "hoja_origen", length = 60)
    private String hojaOrigen;

    @Column(name = "entidad_destino", nullable = false, length = 60)
    private String entidadDestino;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 400)
    private String motivoRechazo;

    /** Resultado de la validación de la fila. */
    public enum Estado {
        PENDIENTE,
        IMPORTADO,
        RECHAZADO,
        DUPLICADO
    }
}
