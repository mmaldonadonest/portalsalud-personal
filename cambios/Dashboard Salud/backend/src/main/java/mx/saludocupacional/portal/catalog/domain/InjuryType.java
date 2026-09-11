package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Tipo de lesión musculoesquelética.
 *
 * <p>Corresponde a la hoja MUSC-ESQU del archivo origen. La nomenclatura clínica
 * mexicana abrevia miembro torácico como MT y miembro pélvico como MP; la región
 * corporal se guarda por separado para poder agrupar sin interpretar el nombre.
 */
@Getter
@Setter
@Entity
@Table(name = "injury_types")
public class InjuryType extends CatalogEntity {

    /** MT, MP, COLUMNA, CABEZA, TRONCO o MULTIPLE. */
    @Column(name = "region_corporal", length = 40)
    private String regionCorporal;
}
