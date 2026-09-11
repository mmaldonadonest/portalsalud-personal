package mx.saludocupacional.portal.importer.repository;

import mx.saludocupacional.portal.importer.domain.ImportBatchRow;
import mx.saludocupacional.portal.importer.domain.ImportBatchRow.Estado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a las filas detectadas en un lote. */
@Repository
public interface ImportBatchRowRepository extends JpaRepository<ImportBatchRow, Long> {

    List<ImportBatchRow> findByImportBatchIdOrderByFilaOrigenAsc(Long importBatchId);

    List<ImportBatchRow> findByImportBatchIdAndEstado(Long importBatchId, Estado estado);

    long countByImportBatchIdAndEstado(Long importBatchId, Estado estado);
}
