package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Mecanismo del accidente: caída, golpe, accidente vial, ergonómica, entre otros. */
@Entity
@Table(name = "accident_causes")
public class AccidentCause extends CatalogEntity {
}
