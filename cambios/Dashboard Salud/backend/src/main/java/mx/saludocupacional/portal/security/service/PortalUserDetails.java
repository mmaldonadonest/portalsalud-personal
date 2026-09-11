package mx.saludocupacional.portal.security.service;

import lombok.Getter;
import mx.saludocupacional.portal.security.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Usuario autenticado tal como lo ve Spring Security.
 *
 * <p>Expone el rol como {@code ROLE_…} y cada permiso como autoridad propia, de
 * modo que las anotaciones {@code @PreAuthorize("hasAuthority('disability.create')")}
 * funcionen directamente sobre los códigos definidos en la base.
 */
@Getter
public class PortalUserDetails implements UserDetails {

    private final transient User usuario;
    private final Collection<GrantedAuthority> authorities;

    public PortalUserDetails(User usuario) {
        this.usuario = usuario;
        this.authorities = Stream.concat(
                        Stream.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().getNombre())),
                        usuario.getPermisos().stream().map(SimpleGrantedAuthority::new))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public Long getUsuarioId() {
        return usuario.getId();
    }

    /** Predios que el usuario puede consultar; vacío significa alcance global. */
    public Set<Long> getPrediosPermitidos() {
        return usuario.getPrediosPermitidos();
    }

    public boolean tieneAlcanceGlobal() {
        return usuario.tieneAlcanceGlobal();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo() && !usuario.isDeleted();
    }

    /** Lista inmutable para evitar que un consumidor altere las autoridades. */
    public List<GrantedAuthority> authoritiesInmutables() {
        return List.copyOf(authorities);
    }
}
