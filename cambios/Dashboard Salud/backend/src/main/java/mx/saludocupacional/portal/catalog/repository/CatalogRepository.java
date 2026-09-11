package mx.saludocupacional.portal.catalog.repository;

import mx.saludocupacional.portal.catalog.domain.CatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones comunes a todos los catálogos.
 *
 * <p>Declararlas una vez evita repetir los mismos tres métodos en diecisiete
 * repositorios. Spring Data genera la implementación a partir del nombre de
 * cada método.
 *
 * @param <T> entidad de catálogo concreta
 */
@NoRepositoryBean
public interface CatalogRepository<T extends CatalogEntity> extends JpaRepository<T, Long> {

    /** Catálogo visible en formularios, en el orden definido para la interfaz. */
    List<T> findByActivoTrueOrderByOrdenAscNombreAsc();

    List<T> findAllByOrderByOrdenAscNombreAsc();

    Optional<T> findByNombreIgnoreCase(String nombre);

    Optional<T> findByCodigo(String codigo);

    boolean existsByNombreIgnoreCase(String nombre);
}
