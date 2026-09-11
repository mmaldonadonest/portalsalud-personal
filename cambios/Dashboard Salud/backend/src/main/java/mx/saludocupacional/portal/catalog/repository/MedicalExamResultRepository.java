package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.MedicalExamResult;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de resultados de examen médico. */
@Repository
public interface MedicalExamResultRepository extends CatalogRepository<MedicalExamResult> {
}
