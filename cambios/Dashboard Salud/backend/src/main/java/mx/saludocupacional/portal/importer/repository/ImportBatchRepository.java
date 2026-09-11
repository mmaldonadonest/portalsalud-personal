package mx.saludocupacional.portal.importer.repository;

import mx.saludocupacional.portal.importer.domain.ImportBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a los lotes de importación. */
@Repository
public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    Page<ImportBatch> findAllByOrderByFechaImportacionDesc(Pageable pageable);
}
