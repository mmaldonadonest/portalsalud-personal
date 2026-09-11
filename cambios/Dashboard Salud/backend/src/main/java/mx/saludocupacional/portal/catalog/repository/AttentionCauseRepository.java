package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.AttentionCause;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a datos del catálogo de causas de atención. */
@Repository
public interface AttentionCauseRepository extends CatalogRepository<AttentionCause> {

    List<AttentionCause> findByCategoriaOrderByOrdenAsc(String categoria);

    /** Categorías existentes, para armar los filtros del dashboard. */
    @Query("SELECT DISTINCT c.categoria FROM AttentionCause c WHERE c.categoria IS NOT NULL ORDER BY c.categoria")
    List<String> findCategorias();
}
