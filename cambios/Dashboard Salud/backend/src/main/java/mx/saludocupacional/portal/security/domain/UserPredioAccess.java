package mx.saludocupacional.portal.security.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Autorización de un usuario sobre un predio concreto.
 *
 * <p>Cuando existen registros para un usuario, toda consulta de datos operativos
 * se filtra por esos predios. Un usuario sin registros ve la organización
 * completa.
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "user_predio_access")
public class UserPredioAccess {

    @EmbeddedId
    private UserPredioAccessId id = new UserPredioAccessId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("predioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private mx.saludocupacional.portal.catalog.domain.Predio predio;

    /** Clave compuesta por usuario y predio. */
    @Getter
    @Setter
    @Embeddable
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class UserPredioAccessId implements Serializable {
        private Long userId;
        private Long predioId;
    }
}
