package mx.saludocupacional.portal.disability.repository;

import mx.saludocupacional.portal.disability.domain.Disability;
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

/** Acceso a datos de incapacidades. */
@Repository
public interface DisabilityRepository extends JpaRepository<Disability, Long> {

    @EntityGraph(attributePaths = {"employee", "predio", "disabilityType", "period", "cost"})
    Optional<Disability> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT d FROM Disability d
           WHERE d.deletedAt IS NULL
             AND (:anio IS NULL OR d.period.anio = :anio)
             AND (:mes IS NULL OR d.period.mes = :mes)
             AND (:predioId IS NULL OR d.predio.id = :predioId)
             AND (:tipoId IS NULL OR d.disabilityType.id = :tipoId)
             AND (:prediosPermitidos IS NULL OR d.predio.id IN :prediosPermitidos)
           ORDER BY d.fechaInicio DESC
           """)
    @EntityGraph(attributePaths = {"employee", "predio", "disabilityType", "period"})
    Page<Disability> buscar(@Param("anio") Integer anio,
                            @Param("mes") Integer mes,
                            @Param("predioId") Long predioId,
                            @Param("tipoId") Long tipoId,
                            @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                            Pageable pageable);

    @EntityGraph(attributePaths = {"disabilityType", "period"})
    List<Disability> findByEmployeeIdAndDeletedAtIsNullOrderByFechaInicioDesc(Long employeeId);

    /** Días acumulados del periodo, base del indicador de días perdidos. */
    @Query("""
           SELECT COALESCE(SUM(d.diasIncapacidad), 0) FROM Disability d
           WHERE d.deletedAt IS NULL
             AND d.period.anio = :anio
             AND (:mes IS NULL OR d.period.mes = :mes)
             AND (:predioId IS NULL OR d.predio.id = :predioId)
           """)
    long sumarDias(@Param("anio") Integer anio,
                   @Param("mes") Integer mes,
                   @Param("predioId") Long predioId);

    /** Personas distintas con al menos una incapacidad en el periodo. */
    @Query("""
           SELECT COUNT(DISTINCT d.employee.id) FROM Disability d
           WHERE d.deletedAt IS NULL
             AND d.period.anio = :anio
             AND (:mes IS NULL OR d.period.mes = :mes)
             AND (:predioId IS NULL OR d.predio.id = :predioId)
           """)
    long contarPersonas(@Param("anio") Integer anio,
                        @Param("mes") Integer mes,
                        @Param("predioId") Long predioId);

    long countByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}
