package com.onest.app.catalog.costo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Fuente provisional del SDI (23-nov-2026): no consulta nada. El costo se calcula con el SDI que el usuario
 * teclea en Incapacidades («Salario diario») y en Accidentes («SDI»), que hoy es el unico SDI real (P-035).
 * Ver {@link FuenteSdi} para cambiarla por el WS o la vista cuando exista.
 */
@Component
public class SdiCapturado implements FuenteSdi {

    @Override
    public Optional<BigDecimal> sdi(String nss, LocalDate fecha) {
        return Optional.empty();
    }

    @Override
    public String nombre() {
        return "capturado";
    }
}
