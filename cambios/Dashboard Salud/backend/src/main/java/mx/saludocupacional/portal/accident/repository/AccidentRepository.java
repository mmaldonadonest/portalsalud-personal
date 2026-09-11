package mx.saludocupacional.portal.accident.repository;

import mx.saludocupacional.portal.accident.domain.Accident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de accidentes. */
@Repository
public interface AccidentRepository extends JpaRepository<Accident, Long> {

    @EntityGraph(attributePaths = {"employee", "predio", "accidentType", "accidentCause",
            "accidentStatus", "period", "disability"})
    Optional<Accident> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT a FROM Accident a
           WHERE a.deletedAt IS NULL
             AND (:anio IS NULL OR a.period.anio = :anio)
             AND (:mes IS NULL OR a.period.mes = :mes)
             AND (:predioId IS NULL OR a.predio.id = :predioId)
             AND (:tipoId IS NULL OR a.accidentType.id = :tipoId)
             AND (:prediosPermitidos IS NULL OR a.predio.id IN :prediosPermitidos)
           ORDER BY a.fechaAccidente DESC
           """)
    @EntityGraph(attributePaths = {"employee", "predio", "accidentType", "accidentCause",
            "accidentStatus", "period", "disability"})
    Page<Accident> buscar(@Param("anio") Integer anio,
                          @Param("mes") Integer mes,
                          @Param("predioId") Long predioId,
                          @Param("tipoId") Long tipoId,
                          @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                          Pageable pageable);

    @EntityGraph(attributePaths = {"predio", "accidentType", "accidentCause", "accidentStatus",
            "period", "disability"})
    List<Accident> findByEmployeeIdAndDeletedAtIsNullOrderByFechaAccidenteDesc(Long employeeId);

    long countByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}
