package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Resultado del examen médico: apto, no apto, condicionado o inclusión. */
@Entity
@Table(name = "medical_exam_results")
public class MedicalExamResult extends CatalogEntity {
}
