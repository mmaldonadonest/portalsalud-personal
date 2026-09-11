package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Resultado de la prueba: negativo o positivo. */
@Entity
@Table(name = "drug_test_results")
public class DrugTestResult extends CatalogEntity {
}
