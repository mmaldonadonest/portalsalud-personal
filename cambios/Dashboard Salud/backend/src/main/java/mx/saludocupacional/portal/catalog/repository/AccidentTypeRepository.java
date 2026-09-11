package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.AccidentType;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de tipos de riesgo de accidente. */
@Repository
public interface AccidentTypeRepository extends CatalogRepository<AccidentType> {
}
