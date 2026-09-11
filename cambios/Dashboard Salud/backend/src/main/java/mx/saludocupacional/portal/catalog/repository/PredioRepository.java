package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.Predio;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos del catálogo de predios. */
@Repository
public interface PredioRepository extends CatalogRepository<Predio> {

    /**
     * Busca un predio por su nombre oficial o por cualquiera de sus alias.
     *
     * <p>El importador de Excel lo usa para resolver las abreviaturas de la hoja
     * ABAST ANTDP (M1, M2, TULTI, UT, WP, Z VALL, FOR) sin crear predios duplicados.
     */
    @Query("""
           SELECT DISTINCT p FROM Predio p
           LEFT JOIN p.aliases a
           WHERE UPPER(p.nombre) = UPPER(:nombre)
              OR UPPER(a.alias) = UPPER(:nombre)
           """)
    Optional<Predio> findByNombreOrAlias(@Param("nombre") String nombre);

    /** Predios a los que un usuario tiene acceso; para el rol Gerencia de Predio. */
    @Query("""
           SELECT p FROM Predio p
           WHERE p.id IN (
               SELECT upa.predio.id FROM UserPredioAccess upa WHERE upa.user.id = :userId
           )
           ORDER BY p.orden, p.nombre
           """)
    List<Predio> findAccesiblesPorUsuario(@Param("userId") Long userId);
}
