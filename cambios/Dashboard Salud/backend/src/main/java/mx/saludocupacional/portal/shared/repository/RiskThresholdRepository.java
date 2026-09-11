package mx.saludocupacional.portal.shared.repository;

import mx.saludocupacional.portal.shared.domain.RiskThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso a los umbrales configurables de riesgo y alerta. */
@Repository
public interface RiskThresholdRepository extends JpaRepository<RiskThreshold, Long> {

    Optional<RiskThreshold> findByClave(String clave);
}
