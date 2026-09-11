package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Nombre alternativo de un predio.
 *
 * <p>La hoja ABAST ANTDP del archivo origen abrevia los predios (M1, M2, TULTI,
 * UT, WP, Z VALL, FOR). El importador resuelve esas abreviaturas contra esta
 * tabla para evitar duplicar predios durante la migración.
 */
@Getter
@Setter
@Entity
@Table(name = "predio_aliases",
       uniqueConstraints = @UniqueConstraint(name = "uk_predio_alias", columnNames = {"predio_id", "alias"}))
public class PredioAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predio_id", nullable = false)
    private Predio predio;

    @Column(nullable = false, length = 60)
    private String alias;
}
