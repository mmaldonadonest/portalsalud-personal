package mx.saludocupacional.portal.security.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mx.saludocupacional.portal.catalog.domain.Predio;
import mx.saludocupacional.portal.shared.domain.SoftDeletableEntity;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuario del portal.
 *
 * <p>La contraseña se guarda siempre como hash BCrypt; el texto plano no existe
 * en la base ni en las bitácoras. Los usuarios con rol de gerencia de predio
 * ven únicamente los predios listados en {@code predioAccess}.
 */
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends SoftDeletableEntity {

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 160)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "ultimo_acceso")
    private OffsetDateTime ultimoAcceso;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserPredioAccess> predioAccess = new HashSet<>();

    /** Códigos de permiso efectivos del usuario, derivados de su rol. */
    public Set<String> getPermisos() {
        return role.getPermissions().stream()
                .map(Permission::getCodigo)
                .collect(Collectors.toSet());
    }

    public boolean tienePermiso(String codigo) {
        return role.tienePermiso(codigo);
    }

    /** Verdadero cuando el usuario ve todos los predios sin restricción. */
    public boolean tieneAlcanceGlobal() {
        return predioAccess.isEmpty();
    }

    public Set<Long> getPrediosPermitidos() {
        return predioAccess.stream()
                .map(a -> a.getPredio().getId())
                .collect(Collectors.toSet());
    }

    public void otorgarAcceso(Predio predio) {
        UserPredioAccess acceso = new UserPredioAccess();
        acceso.setUser(this);
        acceso.setPredio(predio);
        predioAccess.add(acceso);
    }
}
