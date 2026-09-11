package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Área funcional dentro del predio. */
@Entity
@Table(name = "areas")
public class Area extends CatalogEntity {
}
