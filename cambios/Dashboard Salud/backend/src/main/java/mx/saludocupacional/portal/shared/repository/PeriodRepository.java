package mx.saludocupacional.portal.shared.repository;

import mx.saludocupacional.portal.shared.domain.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a los periodos mensuales. */
@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {

    Optional<Period> findByAnioAndMes(Integer anio, Integer mes);

    List<Period> findByAnioOrderByMesAsc(Integer anio);

    /** Años con periodos registrados, para poblar el filtro del dashboard. */
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.anio FROM Period p ORDER BY p.anio DESC")
    List<Integer> findAniosDisponibles();
}
