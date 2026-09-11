package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.MedicalExamType;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de tipos de examen médico. */
@Repository
public interface MedicalExamTypeRepository extends CatalogRepository<MedicalExamType> {
}
