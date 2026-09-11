package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Estado del trámite del accidente ante el IMSS. */
@Entity
@Table(name = "accident_statuses")
public class AccidentStatus extends CatalogEntity {
}
