package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.Area;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de áreas. */
@Repository
public interface AreaRepository extends CatalogRepository<Area> {
}
