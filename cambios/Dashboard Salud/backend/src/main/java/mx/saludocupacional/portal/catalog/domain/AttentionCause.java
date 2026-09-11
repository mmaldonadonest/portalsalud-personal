package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Causa médica de una atención.
 *
 * <p>Corresponde a las veintiséis causas de la hoja CAUSAS del archivo origen.
 * La categoría agrupa causas afines para el dashboard, de modo que
 * «respiratorio» y «digestivo» puedan presentarse como bloques comparables.
 */
@Getter
@Setter
@Entity
@Table(name = "attention_causes")
public class AttentionCause extends CatalogEntity {

    /** Agrupación para el dashboard: PREVENTIVO, DIGESTIVO, RESPIRATORIO, MUSCULOESQUELETICO, NEUROLOGICO, CARDIOVASCULAR, OTROS. */
    @Column(length = 60)
    private String categoria;
}
