package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Tipo de incapacidad: enfermedad general, maternidad, accidente laboral, accidente de trayecto o interna. */
@Entity
@Table(name = "disability_types")
public class DisabilityType extends CatalogEntity {
}
