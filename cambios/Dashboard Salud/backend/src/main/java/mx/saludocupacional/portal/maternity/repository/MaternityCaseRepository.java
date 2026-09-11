package mx.saludocupacional.portal.maternity.repository;

import mx.saludocupacional.portal.maternity.domain.MaternityCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/** Acceso a datos de casos de maternidad. */
@Repository
public interface MaternityCaseRepository extends JpaRepository<MaternityCase, Long> {

    @EntityGraph(attributePaths = {"employee", "predio", "period", "disability"})
    Optional<MaternityCase> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT m FROM MaternityCase m
           WHERE m.deletedAt IS NULL
             AND (:anio IS NULL OR m.period.anio = :anio)
             AND (:mes IS NULL OR m.period.mes = :mes)
             AND (:predioId IS NULL OR m.predio.id = :predioId)
             AND (:prediosPermitidos IS NULL OR m.predio.id IN :prediosPermitidos)
           ORDER BY m.fechaInicioIncapacidad DESC
           """)
    @EntityGraph(attributePaths = {"employee", "predio", "period"})
    Page<MaternityCase> buscar(@Param("anio") Integer anio,
                               @Param("mes") Integer mes,
                               @Param("predioId") Long predioId,
                               @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                               Pageable pageable);
}
