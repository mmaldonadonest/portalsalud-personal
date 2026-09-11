package mx.saludocupacional.portal.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Permiso concreto sobre un módulo, por ejemplo {@code disability.create}.
 *
 * <p>El nivel de sensibilidad separa lo que puede ver la dirección de lo que
 * solo corresponde al personal de salud ocupacional: un permiso AGREGADO expone
 * totales, mientras que uno OPERATIVO permite consultar registros individuales
 * con datos médicos identificables.
 */
@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String codigo;

    @Column(length = 240)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_sensibilidad", nullable = false, length = 20)
    private SensitivityLevel nivelSensibilidad = SensitivityLevel.OPERATIVO;

    public enum SensitivityLevel {
        /** Permite consultar registros individuales, incluidos datos médicos. */
        OPERATIVO,
        /** Solo expone cifras consolidadas, sin identificar personas. */
        AGREGADO
    }
}
