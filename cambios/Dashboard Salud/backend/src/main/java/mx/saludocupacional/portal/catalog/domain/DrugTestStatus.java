package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Seguimiento posterior a la prueba: CAPA, recaída, baja, entre otros. */
@Entity
@Table(name = "drug_test_statuses")
public class DrugTestStatus extends CatalogEntity {
}
