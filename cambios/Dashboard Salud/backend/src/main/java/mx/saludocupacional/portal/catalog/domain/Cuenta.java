package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Cuenta o cliente corporativo al que pertenece la operación. */
@Entity
@Table(name = "cuentas")
public class Cuenta extends CatalogEntity {
}
