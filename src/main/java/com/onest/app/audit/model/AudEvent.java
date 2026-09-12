package com.onest.app.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Bitacora funcional (APP_AUD_EVENT, creada en db/sql/00_init_oracle21c.sql y vacia hasta el
 * 11-sep-2026: nadie escribia en ella). Una fila por escritura del portal y por login.
 *
 * <p>Mapeo de columnas al vocabulario del modulo Auditoria: EVENT_TYPE = modulo (Consultas,
 * Incapacidades, Sistema...), ACTION = create/update/delete/export/login, ENTITY_NAME = tipo
 * de registro, ENTITY_ID = clave del registro (NSS, id), DETAIL_JSON = detalle legible.
 */
@Entity
@Table(name = "APP_AUD_EVENT")
public class AudEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    private String eventType;

    @Column(name = "ENTITY_NAME", length = 120)
    private String entityName;

    @Column(name = "ENTITY_ID", length = 120)
    private String entityId;

    @Column(name = "ACTION", length = 80)
    private String action;

    @Column(name = "EVENT_TS", nullable = false)
    private LocalDateTime eventTs;

    @Column(name = "USERNAME", length = 120)
    private String username;

    @Lob
    @Column(name = "DETAIL_JSON")
    private String detailJson;

    @Column(name = "TRACE_ID", length = 120)
    private String traceId;

    @Column(name = "IP_ADDRESS", length = 80)
    private String ipAddress;

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getEventTs() {
        return eventTs;
    }

    public void setEventTs(LocalDateTime eventTs) {
        this.eventTs = eventTs;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
