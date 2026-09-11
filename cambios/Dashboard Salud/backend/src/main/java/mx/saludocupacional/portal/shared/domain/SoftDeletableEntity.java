package mx.saludocupacional.portal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entidad con borrado lógico.
 *
 * <p>Los registros con relevancia legal o auditable nunca se eliminan de la base:
 * se marcan con {@code deletedAt}. Los repositorios filtran por
 * {@code deletedAt IS NULL} para que el borrado sea transparente a los servicios.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }
}
