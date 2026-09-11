package mx.saludocupacional.portal.security.repository;

import mx.saludocupacional.portal.security.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de roles. */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNombre(String nombre);

    @EntityGraph(attributePaths = {"permissions"})
    List<Role> findAllByOrderByNombreAsc();
}
