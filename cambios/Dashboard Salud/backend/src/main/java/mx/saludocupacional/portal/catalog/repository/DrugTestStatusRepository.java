package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.DrugTestStatus;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de estados de seguimiento de prueba. */
@Repository
public interface DrugTestStatusRepository extends CatalogRepository<DrugTestStatus> {
}
