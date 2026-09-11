package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Agencia externa que provee personal o servicios. */
@Entity
@Table(name = "agencias")
public class Agencia extends CatalogEntity {
}
