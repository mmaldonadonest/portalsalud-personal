package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.DrugTestType;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de tipos de prueba. */
@Repository
public interface DrugTestTypeRepository extends CatalogRepository<DrugTestType> {
}
