package mx.saludocupacional.portal.drugtest.repository;

import mx.saludocupacional.portal.drugtest.domain.DrugTest;
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

/** Acceso a datos de pruebas de antidoping y alcoholimetría. */
@Repository
public interface DrugTestRepository extends JpaRepository<DrugTest, Long> {

    @EntityGraph(attributePaths = {"employee", "predio", "testType", "result", "status", "period", "batch"})
    Optional<DrugTest> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
           SELECT t FROM DrugTest t
           WHERE t.deletedAt IS NULL
             AND (:anio IS NULL OR t.period.anio = :anio)
             AND (:mes IS NULL OR t.period.mes = :mes)
             AND (:predioId IS NULL OR t.predio.id = :predioId)
             AND (:tipoId IS NULL OR t.testType.id = :tipoId)
             AND (:prediosPermitidos IS NULL OR t.predio.id IN :prediosPermitidos)
           ORDER BY t.fechaPrueba DESC
           """)
    @EntityGraph(attributePaths = {"employee", "predio", "testType", "result", "status", "period"})
    Page<DrugTest> buscar(@Param("anio") Integer anio,
                          @Param("mes") Integer mes,
                          @Param("predioId") Long predioId,
                          @Param("tipoId") Long tipoId,
                          @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                          Pageable pageable);

    @EntityGraph(attributePaths = {"predio", "testType", "result", "period"})
    List<DrugTest> findByEmployeeIdAndDeletedAtIsNullOrderByFechaPruebaDesc(Long employeeId);

    long countByEmployeeIdAndDeletedAtIsNull(Long employeeId);
}
