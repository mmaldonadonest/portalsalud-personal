package com.onest.app.security.service;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class PortalUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String displayName;
    private volatile String avatar;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    /** Email con el que se pidio la foto a la intranet; null si no se conoce (sin reintento). */
    private final String emailAvatar;
    private volatile boolean avatarReintentado;

    public PortalUserPrincipal(
            Long id,
            String username,
            String displayName,
            String avatar,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this(id, username, displayName, avatar, password, authorities, null);
    }

    public PortalUserPrincipal(
            Long id,
            String username,
            String displayName,
            String avatar,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            String emailAvatar
    ) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatar = avatar;
        this.password = password;
        this.authorities = authorities;
        this.emailAvatar = emailAvatar;
    }

    /**
     * Si la foto fallo al entrar, la pide UNA vez mas (10-oct-2026: la intranet respondio 404 una
     * vez al login y la sesion completa se quedo con el avatar generico, aunque segundos despues
     * respondia bien). Solo un reintento por sesion: si la persona no tiene foto en la intranet,
     * no se vuelve a preguntar en cada pantalla.
     */
    public void reintentarAvatarSiFallo(java.util.function.Function<String, java.util.Optional<String>> resolver) {
        if (avatarReintentado || emailAvatar == null || !AvatarDefaults.DEFAULT_AVATAR.equals(avatar)) {
            return;
        }
        avatarReintentado = true;
        try {
            resolver.apply(emailAvatar).ifPresent(a -> this.avatar = a);
        } catch (RuntimeException ignorada) {
            // se queda el generico; ya no se reintenta
        }
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatar() {
        return avatar;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
