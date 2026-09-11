package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Tipo de examen médico: nuevo ingreso, periódico, pos incapacidad o pretest. */
@Entity
@Table(name = "medical_exam_types")
public class MedicalExamType extends CatalogEntity {
}
