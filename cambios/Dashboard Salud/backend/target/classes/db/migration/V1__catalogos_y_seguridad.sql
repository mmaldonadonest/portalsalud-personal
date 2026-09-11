-- =============================================================================
-- V1 — Catálogos maestros, periodos y modelo de seguridad
-- Portal Integral de Salud Ocupacional
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Catálogos organizacionales
-- -----------------------------------------------------------------------------

CREATE TABLE predios (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);
COMMENT ON TABLE predios IS 'Catálogo maestro de predios; referenciado por todos los módulos operativos';

-- Resuelve las abreviaturas de la hoja ABAST ANTDP durante la importación
CREATE TABLE predio_aliases (
    id         BIGSERIAL PRIMARY KEY,
    predio_id  BIGINT NOT NULL REFERENCES predios(id) ON DELETE CASCADE,
    alias      VARCHAR(60) NOT NULL,
    CONSTRAINT uk_predio_alias UNIQUE (predio_id, alias)
);
COMMENT ON TABLE predio_aliases IS 'Nombres alternativos de un predio (M1, M2, TULTI, UT, WP, Z VALL, FOR)';

CREATE TABLE cuentas (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE agencias (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE areas (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE puestos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

-- -----------------------------------------------------------------------------
-- Catálogos clínicos
-- -----------------------------------------------------------------------------

CREATE TABLE attention_causes (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    categoria   VARCHAR(60),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);
COMMENT ON COLUMN attention_causes.categoria IS 'Agrupación para el dashboard: preventivo, digestivo, respiratorio, musculoesquelético…';

CREATE TABLE injury_types (
    id              BIGSERIAL PRIMARY KEY,
    codigo          VARCHAR(40) UNIQUE,
    nombre          VARCHAR(160) NOT NULL UNIQUE,
    region_corporal VARCHAR(40),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    orden           INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT,
    updated_by      BIGINT
);
COMMENT ON COLUMN injury_types.region_corporal IS 'MT (miembro torácico), MP (miembro pélvico), COLUMNA, CABEZA, TRONCO, MULTIPLE';

CREATE TABLE medical_exam_types (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE medical_exam_results (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE disability_types (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE accident_types (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE accident_causes (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE accident_statuses (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE drug_test_types (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE drug_test_results (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE drug_test_statuses (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40) UNIQUE,
    nombre      VARCHAR(160) NOT NULL UNIQUE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    orden       INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  BIGINT,
    updated_by  BIGINT
);

CREATE TABLE age_ranges (
    id        BIGSERIAL PRIMARY KEY,
    nombre    VARCHAR(40) NOT NULL UNIQUE,
    edad_min  INTEGER NOT NULL,
    edad_max  INTEGER,
    orden     INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_age_range CHECK (edad_max IS NULL OR edad_max >= edad_min)
);

-- -----------------------------------------------------------------------------
-- Umbrales configurables: alimentan semáforo de riesgo y alertas
-- -----------------------------------------------------------------------------

CREATE TABLE risk_thresholds (
    id            BIGSERIAL PRIMARY KEY,
    clave         VARCHAR(80) NOT NULL UNIQUE,
    descripcion   VARCHAR(240) NOT NULL,
    valor_numero  NUMERIC(14,2) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by    BIGINT,
    updated_by    BIGINT
);
COMMENT ON TABLE risk_thresholds IS 'Ningún umbral vive en el código: se administra desde la interfaz';

-- -----------------------------------------------------------------------------
-- Periodos: año + mes, con control de cierre
-- -----------------------------------------------------------------------------

CREATE TABLE periods (
    id           BIGSERIAL PRIMARY KEY,
    anio         INTEGER NOT NULL,
    mes          INTEGER NOT NULL,
    cerrado      BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_cierre TIMESTAMPTZ,
    CONSTRAINT uk_period UNIQUE (anio, mes),
    CONSTRAINT ck_period_mes CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT ck_period_anio CHECK (anio BETWEEN 2000 AND 2100)
);
COMMENT ON TABLE periods IS 'El año es atributo del registro; jamás se crea una tabla por año';

-- -----------------------------------------------------------------------------
-- Seguridad: usuarios, roles, permisos y alcance por predio
-- -----------------------------------------------------------------------------

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL UNIQUE,
    descripcion VARCHAR(240)
);

CREATE TABLE permissions (
    id                 BIGSERIAL PRIMARY KEY,
    codigo             VARCHAR(80) NOT NULL UNIQUE,
    descripcion        VARCHAR(240),
    nivel_sensibilidad VARCHAR(20) NOT NULL DEFAULT 'OPERATIVO',
    CONSTRAINT ck_sensibilidad CHECK (nivel_sensibilidad IN ('OPERATIVO', 'AGREGADO'))
);
COMMENT ON COLUMN permissions.nivel_sensibilidad IS 'AGREGADO solo expone totales; OPERATIVO permite ver datos individuales';

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    nombre        VARCHAR(160) NOT NULL,
    role_id       BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    activo        BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by    BIGINT,
    updated_by    BIGINT,
    deleted_at    TIMESTAMPTZ
);

CREATE TABLE user_predio_access (
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    predio_id BIGINT NOT NULL REFERENCES predios(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, predio_id)
);
COMMENT ON TABLE user_predio_access IS 'Acota la consulta de Gerencia de Predio a sus predios autorizados';

-- -----------------------------------------------------------------------------
-- Auditoría: bitácora única de toda la plataforma
-- -----------------------------------------------------------------------------

CREATE TABLE audit_logs (
    id                        BIGSERIAL PRIMARY KEY,
    user_id                   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    accion                    VARCHAR(20) NOT NULL,
    modulo                    VARCHAR(60) NOT NULL,
    entidad                   VARCHAR(60) NOT NULL,
    entidad_id                BIGINT,
    valor_anterior            JSONB,
    valor_nuevo               JSONB,
    contiene_datos_sensibles  BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address                VARCHAR(45),
    fecha_hora                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_audit_accion CHECK (accion IN ('CREATE','UPDATE','DELETE','EXPORT','IMPORT','LOGIN','LOGOUT'))
);

CREATE INDEX idx_audit_entidad ON audit_logs (entidad, entidad_id);
CREATE INDEX idx_audit_usuario ON audit_logs (user_id, fecha_hora DESC);
CREATE INDEX idx_audit_fecha ON audit_logs (fecha_hora DESC);
