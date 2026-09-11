package mx.saludocupacional.portal.security.repository;

import mx.saludocupacional.portal.security.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de permisos. */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCodigo(String codigo);

    List<Permission> findAllByOrderByCodigoAsc();
}
