package mx.saludocupacional.portal.drugtest.repository;

import mx.saludocupacional.portal.drugtest.domain.DrugTestBatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de lotes de pruebas. */
@Repository
public interface DrugTestBatchRepository extends JpaRepository<DrugTestBatch, Long> {

    Optional<DrugTestBatch> findByLote(String lote);

    @EntityGraph(attributePaths = {"predio"})
    List<DrugTestBatch> findByActivoTrueOrderByFechaCaducidadAsc();

    @EntityGraph(attributePaths = {"predio"})
    List<DrugTestBatch> findByPredioIdAndActivoTrueOrderByFechaCaducidadAsc(Long predioId);

    /** Lotes que caducan dentro del horizonte indicado, para las alertas. */
    @EntityGraph(attributePaths = {"predio"})
    @Query("""
           SELECT b FROM DrugTestBatch b
           WHERE b.activo = true AND b.fechaCaducidad <= :limite
           ORDER BY b.fechaCaducidad
           """)
    List<DrugTestBatch> findProximosACaducar(@Param("limite") LocalDate limite);
}
