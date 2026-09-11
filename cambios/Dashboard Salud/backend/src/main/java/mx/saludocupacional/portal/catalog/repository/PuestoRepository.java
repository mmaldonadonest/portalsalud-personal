package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.Puesto;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de puestos. */
@Repository
public interface PuestoRepository extends CatalogRepository<Puesto> {
}
