package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.shared.domain.BaseEntity;

/**
 * Base común de los catálogos maestros.
 *
 * <p>Los diecisiete catálogos del portal comparten la misma forma: código
 * opcional, nombre único, bandera de activo y orden de presentación. Declararla
 * una sola vez evita repetir esos campos en cada entidad.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class CatalogEntity extends BaseEntity {

    @Size(max = 40)
    @Column(length = 40, unique = true)
    private String codigo;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, unique = true, length = 160)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private Integer orden = 0;
}
