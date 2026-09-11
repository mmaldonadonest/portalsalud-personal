package mx.saludocupacional.portal.importer.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote de importación desde un archivo de Excel.
 *
 * <p>La carga ocurre en dos tiempos: primero se analiza el archivo y se guarda
 * lo detectado para que el usuario lo revise; solo al confirmar se escribe en
 * las tablas operativas. Así ningún archivo mal formado altera la información
 * sin que alguien lo apruebe.
 */
@Getter
@Setter
@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "archivo_nombre", nullable = false, length = 260)
    private String archivoNombre;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "fecha_importacion", nullable = false)
    private OffsetDateTime fechaImportacion = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PREVIEW;

    @Column(name = "registros_totales", nullable = false)
    private Integer registrosTotales = 0;

    @Column(name = "registros_correctos", nullable = false)
    private Integer registrosCorrectos = 0;

    @Column(name = "registros_advertencia", nullable = false)
    private Integer registrosAdvertencia = 0;

    @Column(name = "registros_rechazados", nullable = false)
    private Integer registrosRechazados = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_errores")
    private String detalleErrores;

    @OneToMany(mappedBy = "importBatch", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ImportBatchRow> filas = new ArrayList<>();

    /** Situación del lote. */
    public enum Estado {
        /** Analizado y a la espera de revisión. */
        PREVIEW,
        /** Aprobado y volcado a las tablas operativas. */
        CONFIRMADO,
        /** Descartado sin escribir nada. */
        RECHAZADO
    }
}
