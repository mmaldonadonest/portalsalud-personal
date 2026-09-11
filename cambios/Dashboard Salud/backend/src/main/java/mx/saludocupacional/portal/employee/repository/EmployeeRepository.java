package mx.saludocupacional.portal.employee.repository;

import mx.saludocupacional.portal.employee.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

/** Acceso a datos de colaboradores. */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @EntityGraph(attributePaths = {"predio", "cuenta", "area", "puesto", "agencia"})
    Optional<Employee> findByIdAndDeletedAtIsNull(Long id);

    Optional<Employee> findByNumeroEmpleadoAndDeletedAtIsNull(String numeroEmpleado);

    boolean existsByNumeroEmpleadoAndDeletedAtIsNull(String numeroEmpleado);

    /**
     * Búsqueda paginada con filtros opcionales.
     *
     * <p>Cada parámetro nulo se ignora, de modo que un solo método atiende
     * todas las combinaciones de la pantalla. El parámetro {@code prediosPermitidos}
     * acota el resultado para los usuarios con alcance limitado; cuando es nulo,
     * el usuario ve la organización completa.
     */
    @Query("""
           SELECT e FROM Employee e
           WHERE e.deletedAt IS NULL
             AND (:texto IS NULL
                  OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
                  OR LOWER(e.numeroEmpleado) LIKE LOWER(CONCAT('%', :texto, '%')))
             AND (:predioId IS NULL OR e.predio.id = :predioId)
             AND (:cuentaId IS NULL OR e.cuenta.id = :cuentaId)
             AND (:activo IS NULL OR e.activo = :activo)
             AND (:prediosPermitidos IS NULL OR e.predio.id IN :prediosPermitidos)
           ORDER BY e.nombre
           """)
    @EntityGraph(attributePaths = {"predio", "cuenta", "area", "puesto"})
    Page<Employee> buscar(@Param("texto") String texto,
                          @Param("predioId") Long predioId,
                          @Param("cuentaId") Long cuentaId,
                          @Param("activo") Boolean activo,
                          @Param("prediosPermitidos") Collection<Long> prediosPermitidos,
                          Pageable pageable);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.deletedAt IS NULL AND e.activo = true")
    long contarActivos();

    @Query("""
           SELECT COUNT(e) FROM Employee e
           WHERE e.deletedAt IS NULL AND e.activo = true AND e.predio.id = :predioId
           """)
    long contarActivosPorPredio(@Param("predioId") Long predioId);
}
