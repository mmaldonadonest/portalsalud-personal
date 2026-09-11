package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Predio o centro de trabajo.
 *
 * <p>Es la dimensión principal de todos los módulos operativos: atenciones,
 * incapacidades, accidentes, exámenes y pruebas se agrupan por predio. Nunca se
 * guarda el nombre del predio como texto en otra tabla; siempre se referencia
 * por identificador, de modo que renombrarlo se refleja en todo el portal.
 */
@Getter
@Setter
@Entity
@Table(name = "predios")
public class Predio extends CatalogEntity {

    @OneToMany(mappedBy = "predio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PredioAlias> aliases = new ArrayList<>();

    public void addAlias(String alias) {
        PredioAlias nuevo = new PredioAlias();
        nuevo.setPredio(this);
        nuevo.setAlias(alias);
        aliases.add(nuevo);
    }
}
