package mx.saludocupacional.portal.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Rango de edad para segmentar indicadores.
 *
 * <p>La edad de cada colaborador se calcula desde su fecha de nacimiento en el
 * momento de la consulta; este catálogo solo define los cortes con los que se
 * agrupan los resultados, de modo que cambiarlos no obliga a recapturar nada.
 */
@Getter
@Setter
@Entity
@Table(name = "age_ranges")
public class AgeRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nombre;

    @Column(name = "edad_min", nullable = false)
    private Integer edadMin;

    /** Nulo en el último rango, que no tiene límite superior. */
    @Column(name = "edad_max")
    private Integer edadMax;

    @Column(nullable = false)
    private Integer orden = 0;

    public boolean contiene(int edad) {
        return edad >= edadMin && (edadMax == null || edad <= edadMax);
    }
}
