package mx.saludocupacional.portal.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Rol asignado a un usuario.
 *
 * <p>Los seis roles iniciales son SUPER_ADMIN, SALUD_OCUPACIONAL, GERENTE_SALUD,
 * GERENTE_PREDIO, DIRECCION y CONSULTA. La combinación de permisos de cada uno
 * se administra desde la interfaz, sin tocar el código.
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 240)
    private String descripcion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    public boolean tienePermiso(String codigo) {
        return permissions.stream().anyMatch(p -> p.getCodigo().equals(codigo));
    }
}
