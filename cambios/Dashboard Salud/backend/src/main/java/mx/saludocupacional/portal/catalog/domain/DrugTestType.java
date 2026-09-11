package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Tipo de prueba: antidoping o alcoholemia. */
@Entity
@Table(name = "drug_test_types")
public class DrugTestType extends CatalogEntity {
}
