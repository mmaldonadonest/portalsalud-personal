package mx.saludocupacional.portal.security.repository;

import mx.saludocupacional.portal.security.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de usuarios. */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Carga el usuario con su rol, permisos y alcance de predios en una sola consulta. */
    @EntityGraph(attributePaths = {"role", "role.permissions", "predioAccess", "predioAccess.predio"})
    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = {"role", "role.permissions", "predioAccess", "predioAccess.predio"})
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    List<User> findByDeletedAtIsNullOrderByNombreAsc();

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);
}
