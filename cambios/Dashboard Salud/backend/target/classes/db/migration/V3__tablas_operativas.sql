-- =============================================================================
-- V3 — Tablas operativas
-- Un hecho real existe una sola vez: los accidentes y casos de maternidad que
-- generan incapacidad la referencian, nunca la duplican.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Empleados: entidad central a la que se vincula todo evento
-- -----------------------------------------------------------------------------

CREATE TABLE employees (
    id                BIGSERIAL PRIMARY KEY,
    numero_empleado   VARCHAR(40) UNIQUE,
    nombre            VARCHAR(200) NOT NULL,
    genero            VARCHAR(20),
    fecha_nacimiento  DATE,
    predio_id         BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id         BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    area_id           BIGINT REFERENCES areas(id) ON DELETE SET NULL,
    puesto_id         BIGINT REFERENCES puestos(id) ON DELETE SET NULL,
    agencia_id        BIGINT REFERENCES agencias(id) ON DELETE SET NULL,
    fecha_ingreso     DATE,
    activo            BOOLEAN NOT NULL DEFAULT TRUE,
    codigo_externo_rh VARCHAR(60),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT ck_employee_genero CHECK (genero IS NULL OR genero IN ('FEMENINO','MASCULINO'))
);

CREATE INDEX idx_employee_predio ON employees (predio_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_employee_numero ON employees (numero_empleado) WHERE deleted_at IS NULL;

COMMENT ON COLUMN employees.fecha_nacimiento IS 'La edad se calcula al consultar; nunca se almacena un rango fijo';
COMMENT ON COLUMN employees.codigo_externo_rh IS 'Identificador en Oracle/ERP para la integración futura con Recursos Humanos';

-- -----------------------------------------------------------------------------
-- Incapacidades: fuente única de días perdidos y costo
-- Se crea antes que accidentes y maternidad porque ambos la referencian.
-- -----------------------------------------------------------------------------

CREATE TABLE disabilities (
    id                 BIGSERIAL PRIMARY KEY,
    employee_id        BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    predio_id          BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id          BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    area_id            BIGINT REFERENCES areas(id) ON DELETE SET NULL,
    period_id          BIGINT NOT NULL REFERENCES periods(id) ON DELETE RESTRICT,
    disability_type_id BIGINT NOT NULL REFERENCES disability_types(id) ON DELETE RESTRICT,
    fecha_inicio       DATE NOT NULL,
    fecha_fin          DATE,
    dias_incapacidad   INTEGER NOT NULL,
    folio_imss         VARCHAR(60),
    es_interna         BOOLEAN NOT NULL DEFAULT FALSE,
    origen_tipo        VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    origen_id          BIGINT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by         BIGINT,
    updated_by         BIGINT,
    deleted_at         TIMESTAMPTZ,
    CONSTRAINT ck_disability_dias CHECK (dias_incapacidad >= 0),
    CONSTRAINT ck_disability_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT ck_disability_origen CHECK (origen_tipo IN ('MANUAL','ACCIDENTE','MATERNIDAD'))
);

CREATE INDEX idx_disability_predio_periodo ON disabilities (predio_id, period_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_disability_empleado ON disabilities (employee_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_disability_tipo ON disabilities (disability_type_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_disability_origen ON disabilities (origen_tipo, origen_id);

COMMENT ON COLUMN disabilities.origen_tipo IS 'MANUAL: capturada directamente. ACCIDENTE o MATERNIDAD: generada por su módulo dueño, sin recaptura';
COMMENT ON TABLE disabilities IS 'Toda métrica de días perdidos y costo se calcula desde aquí; ningún módulo suma por su cuenta';

-- Costo aislado del caso clínico para poder darle permisos distintos
CREATE TABLE disability_costs (
    id             BIGSERIAL PRIMARY KEY,
    disability_id  BIGINT NOT NULL UNIQUE REFERENCES disabilities(id) ON DELETE CASCADE,
    monto          NUMERIC(14,2) NOT NULL,
    moneda         VARCHAR(3) NOT NULL DEFAULT 'MXN',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_costo_monto CHECK (monto >= 0)
);

-- -----------------------------------------------------------------------------
-- Atenciones médicas: fusiona las hojas ATEN, CAUSAS y MUSC-ESQU
-- -----------------------------------------------------------------------------

CREATE TABLE medical_attentions (
    id                    BIGSERIAL PRIMARY KEY,
    employee_id           BIGINT REFERENCES employees(id) ON DELETE SET NULL,
    predio_id             BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id             BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    period_id             BIGINT NOT NULL REFERENCES periods(id) ON DELETE RESTRICT,
    fecha_atencion        DATE NOT NULL,
    attention_cause_id    BIGINT REFERENCES attention_causes(id) ON DELETE SET NULL,
    injury_type_id        BIGINT REFERENCES injury_types(id) ON DELETE SET NULL,
    es_personal_inclusion BOOLEAN NOT NULL DEFAULT FALSE,
    observaciones         TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by            BIGINT,
    updated_by            BIGINT,
    deleted_at            TIMESTAMPTZ,
    -- Toda atención se clasifica por causa médica o por tipo de lesión
    CONSTRAINT ck_attention_clasificacion CHECK (
        attention_cause_id IS NOT NULL OR injury_type_id IS NOT NULL)
);

CREATE INDEX idx_attention_predio_periodo ON medical_attentions (predio_id, period_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_attention_causa ON medical_attentions (attention_cause_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_attention_lesion ON medical_attentions (injury_type_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_attention_empleado ON medical_attentions (employee_id) WHERE deleted_at IS NULL;

COMMENT ON COLUMN medical_attentions.employee_id IS 'Admite nulo solo para datos históricos migrados sin identificación de la persona';

-- -----------------------------------------------------------------------------
-- Exámenes médicos
-- -----------------------------------------------------------------------------

CREATE TABLE medical_exams (
    id                      BIGSERIAL PRIMARY KEY,
    employee_id             BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    predio_id               BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id               BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    period_id               BIGINT NOT NULL REFERENCES periods(id) ON DELETE RESTRICT,
    fecha_examen            DATE NOT NULL,
    medical_exam_type_id    BIGINT NOT NULL REFERENCES medical_exam_types(id) ON DELETE RESTRICT,
    medical_exam_result_id  BIGINT NOT NULL REFERENCES medical_exam_results(id) ON DELETE RESTRICT,
    observaciones           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              BIGINT,
    updated_by              BIGINT,
    deleted_at              TIMESTAMPTZ
);

CREATE INDEX idx_exam_predio_periodo ON medical_exams (predio_id, period_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_exam_empleado ON medical_exams (employee_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Accidentabilidad: puede generar una incapacidad vinculada
-- -----------------------------------------------------------------------------

CREATE TABLE accidents (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    predio_id           BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id           BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    area_id             BIGINT REFERENCES areas(id) ON DELETE SET NULL,
    puesto_id           BIGINT REFERENCES puestos(id) ON DELETE SET NULL,
    period_id           BIGINT NOT NULL REFERENCES periods(id) ON DELETE RESTRICT,
    fecha_accidente     DATE NOT NULL,
    accident_type_id    BIGINT NOT NULL REFERENCES accident_types(id) ON DELETE RESTRICT,
    accident_cause_id   BIGINT NOT NULL REFERENCES accident_causes(id) ON DELETE RESTRICT,
    accident_status_id  BIGINT NOT NULL REFERENCES accident_statuses(id) ON DELETE RESTRICT,
    genero              VARCHAR(20),
    genera_incapacidad  BOOLEAN NOT NULL DEFAULT FALSE,
    disability_id       BIGINT UNIQUE REFERENCES disabilities(id) ON DELETE SET NULL,
    costo_calificado    NUMERIC(14,2),
    costo_improcedente  NUMERIC(14,2),
    descripcion         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          BIGINT,
    updated_by          BIGINT,
    deleted_at          TIMESTAMPTZ,
    CONSTRAINT ck_accident_genero CHECK (genero IS NULL OR genero IN ('FEMENINO','MASCULINO')),
    -- Si declara que genera incapacidad, debe existir el vínculo
    CONSTRAINT ck_accident_incapacidad CHECK (
        genera_incapacidad = FALSE OR disability_id IS NOT NULL)
);

CREATE INDEX idx_accident_predio_periodo ON accidents (predio_id, period_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_accident_tipo ON accidents (accident_type_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_accident_causa ON accidents (accident_cause_id) WHERE deleted_at IS NULL;

COMMENT ON COLUMN accidents.disability_id IS 'Los días perdidos se leen de esta incapacidad; no se duplican aquí';

-- -----------------------------------------------------------------------------
-- Maternidad: también genera su incapacidad vinculada
-- -----------------------------------------------------------------------------

CREATE TABLE maternity_cases (
    id                       BIGSERIAL PRIMARY KEY,
    employee_id              BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    predio_id                BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id                BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    period_id                BIGINT NOT NULL REFERENCES periods(id) ON DELETE RESTRICT,
    fecha_inicio_incapacidad DATE NOT NULL,
    fecha_probable_parto     DATE,
    dias_incapacidad         INTEGER NOT NULL,
    estatus                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    disability_id            BIGINT UNIQUE REFERENCES disabilities(id) ON DELETE SET NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by               BIGINT,
    updated_by               BIGINT,
    deleted_at               TIMESTAMPTZ,
    CONSTRAINT ck_maternity_dias CHECK (dias_incapacidad >= 0),
    CONSTRAINT ck_maternity_estatus CHECK (estatus IN ('ACTIVO','CONCLUIDO'))
);

CREATE INDEX idx_maternity_predio_periodo ON maternity_cases (predio_id, period_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Antidoping: lotes, movimientos y pruebas
-- El stock nunca se edita: se calcula desde los movimientos.
-- -----------------------------------------------------------------------------

CREATE TABLE drug_test_batches (
    id               BIGSERIAL PRIMARY KEY,
    lote             VARCHAR(60) NOT NULL UNIQUE,
    predio_id        BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    fecha_caducidad  DATE NOT NULL,
    cantidad_inicial INTEGER NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by       BIGINT,
    updated_by       BIGINT,
    CONSTRAINT ck_batch_cantidad CHECK (cantidad_inicial > 0)
);

CREATE INDEX idx_batch_caducidad ON drug_test_batches (fecha_caducidad) WHERE activo = TRUE;

CREATE TABLE drug_test_inventory_movements (
    id         BIGSERIAL PRIMARY KEY,
    batch_id   BIGINT NOT NULL REFERENCES drug_test_batches(id) ON DELETE RESTRICT,
    predio_id  BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    tipo       VARCHAR(20) NOT NULL,
    cantidad   INTEGER NOT NULL,
    fecha      DATE NOT NULL,
    referencia VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('ENTRADA','CONSUMO','AJUSTE')),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_movimiento_lote ON drug_test_inventory_movements (batch_id, fecha);

COMMENT ON TABLE drug_test_inventory_movements IS 'Existencia = cantidad_inicial + entradas - consumos ± ajustes';

CREATE TABLE drug_tests (
    id                   BIGSERIAL PRIMARY KEY,
    employee_id          BIGINT NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    predio_id            BIGINT NOT NULL REFERENCES predios(id) ON DELETE RESTRICT,
    cuenta_id            BIGINT REFERENCES cuentas(id) ON DELETE SET NULL,
    area_id              BIGINT REFERENCES areas(id) ON DELETE SET NULL,
    puesto_id            BIGINT REFERENCES puestos(id) ON DELETE SET NULL,
    agencia_id           BIGINT REFERENCES agencias(id) ON DELETE SET NULL,
    period_id            BIGINT NOT NULL REFERENCES periods(id) ON DELETE RESTRICT,
    fecha_prueba         DATE NOT NULL,
    drug_test_type_id    BIGINT NOT NULL REFERENCES drug_test_types(id) ON DELETE RESTRICT,
    drug_test_result_id  BIGINT REFERENCES drug_test_results(id) ON DELETE SET NULL,
    drug_test_status_id  BIGINT REFERENCES drug_test_statuses(id) ON DELETE SET NULL,
    batch_id             BIGINT REFERENCES drug_test_batches(id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           BIGINT,
    updated_by           BIGINT,
    deleted_at           TIMESTAMPTZ
);

CREATE INDEX idx_drugtest_predio_periodo ON drug_tests (predio_id, period_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_drugtest_tipo ON drug_tests (drug_test_type_id) WHERE deleted_at IS NULL;

-- -----------------------------------------------------------------------------
-- Importación de Excel
-- -----------------------------------------------------------------------------

CREATE TABLE import_batches (
    id                    BIGSERIAL PRIMARY KEY,
    archivo_nombre        VARCHAR(260) NOT NULL,
    usuario_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    fecha_importacion     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado                VARCHAR(20) NOT NULL DEFAULT 'PREVIEW',
    registros_totales     INTEGER NOT NULL DEFAULT 0,
    registros_correctos   INTEGER NOT NULL DEFAULT 0,
    registros_advertencia INTEGER NOT NULL DEFAULT 0,
    registros_rechazados  INTEGER NOT NULL DEFAULT 0,
    detalle_errores       JSONB,
    CONSTRAINT ck_import_estado CHECK (estado IN ('PREVIEW','CONFIRMADO','RECHAZADO'))
);

CREATE TABLE import_batch_rows (
    id               BIGSERIAL PRIMARY KEY,
    import_batch_id  BIGINT NOT NULL REFERENCES import_batches(id) ON DELETE CASCADE,
    fila_origen      INTEGER NOT NULL,
    hoja_origen      VARCHAR(60),
    entidad_destino  VARCHAR(60) NOT NULL,
    payload          JSONB NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo   VARCHAR(400),
    CONSTRAINT ck_import_row_estado CHECK (estado IN ('PENDIENTE','IMPORTADO','RECHAZADO','DUPLICADO'))
);

CREATE INDEX idx_import_row_lote ON import_batch_rows (import_batch_id, estado);

-- -----------------------------------------------------------------------------
-- Adjuntos
-- -----------------------------------------------------------------------------

CREATE TABLE attachments (
    id             BIGSERIAL PRIMARY KEY,
    entidad        VARCHAR(60) NOT NULL,
    entidad_id     BIGINT NOT NULL,
    nombre_archivo VARCHAR(260) NOT NULL,
    url_storage    VARCHAR(600) NOT NULL,
    tipo_mime      VARCHAR(120) NOT NULL,
    tamano_bytes   BIGINT NOT NULL,
    subido_por     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attachment_entidad ON attachments (entidad, entidad_id);
