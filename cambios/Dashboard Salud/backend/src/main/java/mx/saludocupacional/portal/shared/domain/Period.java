package mx.saludocupacional.portal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Mes calendario al que pertenece un registro operativo.
 *
 * <p>El año es un atributo del periodo, nunca una tabla aparte: agregar 2027 no
 * implica crear estructuras nuevas. Marcar un periodo como cerrado impide la
 * captura retroactiva salvo para perfiles autorizados.
 */
@Getter
@Setter
@Entity
@Table(name = "periods",
       uniqueConstraints = @UniqueConstraint(name = "uk_period", columnNames = {"anio", "mes"}))
public class Period {

    private static final String[] NOMBRES_MES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    /** Mes del uno al doce. */
    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private boolean cerrado = false;

    @Column(name = "fecha_cierre")
    private OffsetDateTime fechaCierre;

    /** Abreviatura del mes tal como se presenta en el dashboard. */
    public String getNombreMes() {
        return NOMBRES_MES[mes - 1];
    }

    /** Etiqueta completa, por ejemplo «JUN 2026». */
    public String getEtiqueta() {
        return getNombreMes() + " " + anio;
    }

    public boolean contiene(LocalDate fecha) {
        return fecha.getYear() == anio && fecha.getMonthValue() == mes;
    }

    public void cerrar() {
        this.cerrado = true;
        this.fechaCierre = OffsetDateTime.now();
    }
}
