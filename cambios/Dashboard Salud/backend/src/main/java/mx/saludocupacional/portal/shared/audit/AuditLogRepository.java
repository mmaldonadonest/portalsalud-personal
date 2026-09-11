package mx.saludocupacional.portal.shared.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/** Consulta de la bitácora. */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Bitácora filtrada. Cada parámetro nulo se ignora, de modo que un solo
     * método atiende todas las combinaciones de la pantalla de auditoría.
     */
    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:userId IS NULL OR a.userId = :userId)
             AND (:modulo IS NULL OR a.modulo = :modulo)
             AND (:accion IS NULL OR a.accion = :accion)
             AND (:desde IS NULL OR a.fechaHora >= :desde)
             AND (:hasta IS NULL OR a.fechaHora <= :hasta)
           ORDER BY a.fechaHora DESC
           """)
    Page<AuditLog> buscar(@Param("userId") Long userId,
                          @Param("modulo") String modulo,
                          @Param("accion") AuditAction accion,
                          @Param("desde") OffsetDateTime desde,
                          @Param("hasta") OffsetDateTime hasta,
                          Pageable pageable);

    Page<AuditLog> findByEntidadAndEntidadIdOrderByFechaHoraDesc(String entidad, Long entidadId, Pageable pageable);
}
