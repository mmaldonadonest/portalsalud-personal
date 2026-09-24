package com.onest.app.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Join menu<->rol (espejo de TBL_APPS_ROL_MENU de ORDS). Entidad propia (no
 * @ManyToMany implicito) para poder consultarla/administrarla directo desde
 * la pantalla /admin/roles/{id}/menus y desde LocalModulePermissionClient.
 */
@Entity
@Table(name = "SERV_MED_MENU_ROLE")
public class AppMenuRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MENU_ID", nullable = false)
    private AppMenu menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROLE_ID", nullable = false)
    private AppSecRole role;

    protected AppMenuRole() {
    }

    public AppMenuRole(AppMenu menu, AppSecRole role) {
        this.menu = menu;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public AppMenu getMenu() {
        return menu;
    }

    public AppSecRole getRole() {
        return role;
    }
}
