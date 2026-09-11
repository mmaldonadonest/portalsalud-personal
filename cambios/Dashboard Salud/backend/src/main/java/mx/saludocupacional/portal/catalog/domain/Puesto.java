package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Puesto de trabajo del colaborador. */
@Entity
@Table(name = "puestos")
public class Puesto extends CatalogEntity {
}
