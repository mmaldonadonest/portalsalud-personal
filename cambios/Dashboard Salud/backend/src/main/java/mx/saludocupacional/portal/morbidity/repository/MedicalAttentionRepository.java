package mx.saludocupacional.portal.morbidity.repository;

import mx.saludocupacional.portal.morbidity.domain.MedicalAttention;
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

/** Acceso a datos de atenciones médicas. */
@Repository
public interface MedicalAttentionRepository extends JpaRepository<MedicalAttention, Long> {

    @EntityGraph(attributePaths = {"employee", "predio", "attentionCause", "injuryType", "period"})
    Optional<MedicalAttention> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT a FROM MedicalAttention a
           WHERE a.deletedAt IS NULL
             AND (:anio IS NULL OR a.period.anio = :anio)
             AND (:mes IS NULL OR a.period.mes = :mes)
             AND (:predioId IS NULL OR a.predio.id = :predioId)
             AND (:causaId IS NULL OR a.attentionCause.id = :causaId)
             AND (:prediosPermitidos IS NULL OR a.predio.id IN :prediosPermitidos)
           ORDER BY a.fechaAtencion DESC
           """)
    @EntityGraph(attributePaths = {"employee", "predio", "attentionCause", "injuryType", "period"})
    Page<MedicalAttention> buscar(@Param("anio") Integer anio,
                                  @Param("mes") Integer mes,
                                  @Param("predioId") Long predioId,
                                  @Param("causaId") Long causaId,
                                  @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"predio", "attentionCause", "injuryType", "period"})
    List<MedicalAttention> findByEmployeeIdAndDeletedAtIsNullOrderByFechaAtencionDesc(Long employeeId);

    long countByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}
