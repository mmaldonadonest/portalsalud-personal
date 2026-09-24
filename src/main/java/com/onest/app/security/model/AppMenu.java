package com.onest.app.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalogo local de modulos del submenu (espejo de los 14 id_menu que ya usa
 * fragments/nss-modules.html, ver docs/plan-rbac-local.md). CODE es la llave
 * estable que reemplaza al id_menu numerico de ORDS - independiente a proposito,
 * no se reutiliza la numeracion 1-14 de ORDS.
 */
@Entity
@Table(name = "SERV_MED_MENU")
public class AppMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODE", nullable = false)
    private String code;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "ICON")
    private String icon;

    @Column(name = "ORDER_NO", nullable = false)
    private Integer orderNo;

    @Column(name = "ACTIVE", nullable = false, columnDefinition = "CHAR(1)", length = 1)
    private String active = "Y";

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public String getActive() {
        return active;
    }
}
