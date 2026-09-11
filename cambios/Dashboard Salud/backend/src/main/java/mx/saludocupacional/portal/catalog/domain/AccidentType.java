package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Tipo de riesgo del accidente: laboral o de trayecto. */
@Entity
@Table(name = "accident_types")
public class AccidentType extends CatalogEntity {
}
