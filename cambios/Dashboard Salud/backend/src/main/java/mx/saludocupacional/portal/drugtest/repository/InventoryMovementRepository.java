package mx.saludocupacional.portal.drugtest.repository;

import mx.saludocupacional.portal.drugtest.domain.InventoryMovement;
import mx.saludocupacional.portal.drugtest.domain.InventoryMovement.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a los movimientos de inventario. */
@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findByBatchIdOrderByFechaDescIdDesc(Long batchId);

    /**
     * Suma de las cantidades de un lote para un tipo de movimiento.
     *
     * <p>Recibir el tipo como parámetro evita nombrar el enumerado dentro de la
     * consulta, que resulta frágil cuando está anidado en otra clase.
     */
    @Query("""
           SELECT COALESCE(SUM(m.cantidad), 0) FROM InventoryMovement m
           WHERE m.batch.id = :batchId AND m.tipo = :tipo
           """)
    int sumarPorTipo(@Param("batchId") Long batchId, @Param("tipo") Tipo tipo);

    /**
     * Saldo neto de movimientos de un lote.
     *
     * <p>Las entradas y los ajustes suman; los consumos restan. La existencia
     * final del lote es su cantidad inicial más este saldo.
     */
    default int saldoDeLote(Long batchId) {
        return sumarPorTipo(batchId, Tipo.ENTRADA)
             + sumarPorTipo(batchId, Tipo.AJUSTE)
             - sumarPorTipo(batchId, Tipo.CONSUMO);
    }

    /** Unidades consumidas del lote. */
    default int consumoDeLote(Long batchId) {
        return sumarPorTipo(batchId, Tipo.CONSUMO);
    }
}
