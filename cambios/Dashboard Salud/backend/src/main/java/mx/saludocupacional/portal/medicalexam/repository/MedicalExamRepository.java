package mx.saludocupacional.portal.medicalexam.repository;

import mx.saludocupacional.portal.medicalexam.domain.MedicalExam;
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

/** Acceso a datos de exámenes médicos. */
@Repository
public interface MedicalExamRepository extends JpaRepository<MedicalExam, Long> {

    @EntityGraph(attributePaths = {"employee", "predio", "examType", "examResult", "period"})
    Optional<MedicalExam> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT e FROM MedicalExam e
           WHERE e.deletedAt IS NULL
             AND (:anio IS NULL OR e.period.anio = :anio)
             AND (:mes IS NULL OR e.period.mes = :mes)
             AND (:predioId IS NULL OR e.predio.id = :predioId)
             AND (:tipoId IS NULL OR e.examType.id = :tipoId)
             AND (:resultadoId IS NULL OR e.examResult.id = :resultadoId)
             AND (:prediosPermitidos IS NULL OR e.predio.id IN :prediosPermitidos)
           ORDER BY e.fechaExamen DESC
           """)
    @EntityGraph(attributePaths = {"employee", "predio", "examType", "examResult", "period"})
    Page<MedicalExam> buscar(@Param("anio") Integer anio,
                             @Param("mes") Integer mes,
                             @Param("predioId") Long predioId,
                             @Param("tipoId") Long tipoId,
                             @Param("resultadoId") Long resultadoId,
                             @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                             Pageable pageable);

    @EntityGraph(attributePaths = {"predio", "examType", "examResult", "period"})
    List<MedicalExam> findByEmployeeIdAndDeletedAtIsNullOrderByFechaExamenDesc(Long employeeId);

    long countByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}
