package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.Cuenta;
import org.springframework.stereotype.Repository;

/** Acceso a datos del catálogo de cuentas o clientes. */
@Repository
public interface CuentaRepository extends CatalogRepository<Cuenta> {
}
