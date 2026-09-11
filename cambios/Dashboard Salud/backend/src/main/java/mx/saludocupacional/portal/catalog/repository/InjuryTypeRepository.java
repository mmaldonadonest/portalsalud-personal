package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.InjuryType;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de tipos de lesión musculoesquelética. */
@Repository
public interface InjuryTypeRepository extends CatalogRepository<InjuryType> {
}
