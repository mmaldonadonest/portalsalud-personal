package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.Agencia;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de agencias. */
@Repository
public interface AgenciaRepository extends CatalogRepository<Agencia> {
}
