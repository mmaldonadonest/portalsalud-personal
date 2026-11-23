package com.onest.app.catalog.costo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * De donde sale el SDI (salario diario integrado) de un trabajador a una fecha (P-035, P-036).
 *
 * <p>23-nov-2026: todavia no se sabe la fuente oficial. El diagnostico del 19-nov
 * (C:\miguel\diagnostico-sdi-salida.txt) mostro que en BIOMETRICO no existe; puede llegar por un WS de RH o
 * Nominas o por una vista. Mientras tanto la unica implementacion es {@link SdiCapturado}, que no tiene dato y
 * deja que el portal use el SDI que se teclea en el formulario.
 *
 * <p>Cuando se conozca la fuente se agrega otra implementacion marcada {@code @Primary} (o se reemplaza
 * {@link SdiCapturado}); {@link ReglasCosto} y los servicios de alta no cambian.
 */
public interface FuenteSdi {

    /**
     * SDI vigente del trabajador en {@code fecha}: la de inicio de la incapacidad o la del accidente (P-036).
     *
     * @return vacio si la fuente no lo tiene; entonces se usa el capturado en el formulario
     */
    Optional<BigDecimal> sdi(String nss, LocalDate fecha);

    /** Texto corto para el usuario y la bitacora: «capturado», «RH»... */
    String nombre();
}
