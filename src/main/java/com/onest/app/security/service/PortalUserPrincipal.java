package com.onest.app.security.service;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class PortalUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String displayName;
    private final String avatar;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public PortalUserPrincipal(
            Long id,
            String username,
            String displayName,
            String avatar,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatar = avatar;
        this.password = password;
        this.authorities = authorities;
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
