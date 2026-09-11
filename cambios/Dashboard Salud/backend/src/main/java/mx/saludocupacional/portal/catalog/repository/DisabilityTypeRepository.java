package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.DisabilityType;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de tipos de incapacidad. */
@Repository
public interface DisabilityTypeRepository extends CatalogRepository<DisabilityType> {
}
