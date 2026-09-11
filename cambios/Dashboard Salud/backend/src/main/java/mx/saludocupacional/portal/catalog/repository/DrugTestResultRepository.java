package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.DrugTestResult;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de resultados de prueba. */
@Repository
public interface DrugTestResultRepository extends CatalogRepository<DrugTestResult> {
}
