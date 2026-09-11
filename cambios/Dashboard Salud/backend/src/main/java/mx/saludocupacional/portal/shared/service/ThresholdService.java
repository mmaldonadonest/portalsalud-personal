package mx.saludocupacional.portal.shared.service;

import lombok.RequiredArgsConstructor;
import mx.saludocupacional.portal.shared.domain.RiskThreshold;
import mx.saludocupacional.portal.shared.exception.ResourceNotFoundException;
import mx.saludocupacional.portal.shared.repository.RiskThresholdRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lectura de los umbrales que gobiernan el semáforo de riesgo y las alertas.
 *
 * <p>Cualquier componente que necesite un límite lo pide aquí. Así, cuando la
 * gerencia decide que el riesgo crítico empieza en trescientos días en lugar de
 * doscientos ochenta, basta editar el valor en la interfaz.
 */
@Service
@RequiredArgsConstructor
public class ThresholdService {

    /** Claves conocidas por la aplicación; el valor de cada una vive en la base. */
    public static final String MORBILIDAD_CRITICO = "morbilidad_critico";
    public static final String MORBILIDAD_ALTO = "morbilidad_alto";
    public static final String MORBILIDAD_MEDIO = "morbilidad_medio";
    public static final String CADUCIDAD_CRITICO = "antidoping_cad_critico";
    public static final String CADUCIDAD_ALERTA = "antidoping_cad_alerta";
    public static final String CADUCIDAD_VIGILAR = "antidoping_cad_vigilar";
    public static final String STOCK_MINIMO = "antidoping_stock_min";
    public static final String VARIACION_RELEVANTE = "variacion_relevante";

    private final RiskThresholdRepository repository;

    @Cacheable("umbrales")
    @Transactional(readOnly = true)
    public BigDecimal valor(String clave) {
        return repository.findByClave(clave)
                .map(RiskThreshold::getValorNumero)
                .orElseThrow(() -> new ResourceNotFoundException("umbral", clave));
    }

    public int valorEntero(String clave) {
        return valor(clave).intValue();
    }

    @Transactional(readOnly = true)
    public List<RiskThreshold> listar() {
        return repository.findAll();
    }

    @CacheEvict(value = "umbrales", allEntries = true)
    @Transactional
    public RiskThreshold actualizar(Long id, BigDecimal nuevoValor) {
        RiskThreshold umbral = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("umbral", id));
        umbral.setValorNumero(nuevoValor);
        return repository.save(umbral);
    }
}
