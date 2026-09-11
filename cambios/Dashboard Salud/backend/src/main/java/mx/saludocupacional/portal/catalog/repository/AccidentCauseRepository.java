package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.AccidentCause;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de causas de accidente. */
@Repository
public interface AccidentCauseRepository extends CatalogRepository<AccidentCause> {
}
