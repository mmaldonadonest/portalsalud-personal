-- =============================================================================
-- V2 — Datos de catálogos
-- Valores extraídos del archivo 10 REPORTE MORBILIDAD 2026 GERENCIA.xlsm
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Predios (17) y sus alias en la hoja ABAST ANTDP
-- -----------------------------------------------------------------------------

INSERT INTO predios (nombre, orden) VALUES
    ('AIFA', 1), ('ATIZAPAN', 2), ('CHARCON', 3), ('FLORA', 4),
    ('MACRO I', 5), ('MACRO II', 6), ('MERCURIO', 7), ('MIKELS', 8),
    ('SIGLO XXI', 9), ('SMO', 10), ('TOLUCA', 11), ('TULTIPARK', 12),
    ('U TEPALCAPA', 13), ('WORLD PARK', 14), ('Z VALLEJO', 15),
    ('FORANEO', 16), ('SIN DATO', 17);

INSERT INTO predio_aliases (predio_id, alias)
SELECT p.id, a.alias
FROM (VALUES
    ('MACRO I', 'M1'), ('MACRO II', 'M2'), ('SIGLO XXI', 'SIGLO'),
    ('TULTIPARK', 'TULTI'), ('U TEPALCAPA', 'UT'), ('WORLD PARK', 'WP'),
    ('Z VALLEJO', 'Z VALL'), ('FORANEO', 'FOR'), ('SIN DATO', 'SD 1')
) AS a(predio, alias)
JOIN predios p ON p.nombre = a.predio;

-- -----------------------------------------------------------------------------
-- Cuentas / clientes (56)
-- -----------------------------------------------------------------------------

INSERT INTO cuentas (nombre, orden) VALUES
    ('COMEDOR',1),('MANTENIMIENTO',2),('PREVENCION',3),('SERV GRAL',4),('STAFF',5),
    ('MIKELS',6),('SIGLO XXI',7),('SMO',8),('UNILEVER TEPALCAPA',9),('FLORA',10),
    ('CLARINS',11),('FISCAL',12),('MILWAUKEE',13),('PLANETA',14),('PUIG',15),
    ('WOLTER KLAUWER',16),('BSD',17),('47 BRAND',18),('5.11 TACTICAL',19),('ADIDDAS',20),
    ('ADOLFO DGUEZ',21),('COMBIBLOCK',22),('CORTE FIEL',23),('CYTIE',24),('ENVIO PACK',25),
    ('FANDELI',26),('JBL',27),('MADDEN',28),('MARTI',29),('PIAGUI',30),
    ('RHEEM',31),('ROYAL CANIN',32),('SUBURBIA',33),('UNILEVER CIVAC',34),('UNILEVER POP',35),
    ('SEARS',36),('CARTERS',37),('HISENSE',38),('P&G MERCURIO',39),('AVANTE',40),
    ('HABERS',41),('U LERMA',42),('ALKA',43),('RICHS',44),('TARKETT',45),
    ('DIAGEO',46),('CANCUN',47),('CBI',48),('IRAPUATO',49),('JIUTEPEC',50),
    ('MARTI GDL',51),('TEMPE',52),('ECOMMERCE',53),('CROSS DOCK',54),('ZARA CADENAS',55),
    ('ZARA ALMACEN',56),('WILSON',57),('S/D',58);

-- -----------------------------------------------------------------------------
-- Agencias (5)
-- -----------------------------------------------------------------------------

INSERT INTO agencias (nombre, orden) VALUES
    ('GLI',1),('ORLAMEX',2),('VALLE LERMA',3),('ONEST',4),('OTRO',5);

-- -----------------------------------------------------------------------------
-- Áreas (20)
-- -----------------------------------------------------------------------------

INSERT INTO areas (nombre, orden) VALUES
    ('STAFF',1),('CALIDAD',2),('CAPACITACION',3),('COMEDOR',4),('DESPACHO',5),
    ('EMBARQUES',6),('INVENTARIOS',7),('LOCKERS',8),('MTTO',9),('OPERACION',10),
    ('PATIO',11),('PREVENCION / SEG TRANSPORTE',12),('REC HUMANOS / RECLUTAMIENTO',13),
    ('RECUPERADO',14),('SALUD OCUP',15),('SEG E HIG',16),('SERV GRAL / LIMPIEZA',17),
    ('SISTEMAS',18),('TRAFICO',19),('CLIENTE / EXTERNO',20);

-- -----------------------------------------------------------------------------
-- Puestos (37)
-- -----------------------------------------------------------------------------

INSERT INTO puestos (nombre, orden) VALUES
    ('INCLUSION',1),('CLIENTE / EXTERNO',2),('ADMON',3),('ANALISTA',4),('ATN CLIENTES',5),
    ('AUDITOR',6),('AUTOMATISTA',7),('AUX ALMACEN / AYUDANTE GRAL',8),('BECARIA',9),
    ('CALIDAD',10),('CAPTURISTA',11),('CHOFER',12),('COCINA',13),('COORDINADOR',14),
    ('COSTURA',15),('DIRECCION',16),('DOCUMENTADOR',17),('EMBARQUES',18),('FACTURISTA',19),
    ('GERENTE',20),('INVENTARIOS',21),('JEFATURA',22),('LAVADOR TARIMA',23),
    ('LIMPIEZA / INTENDENCIA',24),('MANIOBRISTA',25),('MANTENIMIENTO',26),('MAQUILA',27),
    ('MEDICO / ENFERMERA',28),('MESA CONTROL',29),('MONITORISTA / PREVENCION / VIGILANCIA',30),
    ('MONTACARGISTA',31),('PATINERO',32),('PLANEADOR TR2 / TRACKER',33),('SEG E HIG',34),
    ('SUPERVISOR',35),('SURTIDOR',36),('TASKER',37);

-- -----------------------------------------------------------------------------
-- Causas de atención (26) con su agrupación para el dashboard
-- -----------------------------------------------------------------------------

INSERT INTO attention_causes (nombre, categoria, orden) VALUES
    ('ACIDO PEPTICA','DIGESTIVO',1),
    ('ALERGIA / INTOXICACION','OTROS',2),
    ('BUCODENTAL','OTROS',3),
    ('CIRCULATORIO','CARDIOVASCULAR',4),
    ('CURACION','OTROS',5),
    ('DERMATOLOGICO','OTROS',6),
    ('DIGESTIVO','DIGESTIVO',7),
    ('EMBARAZO (DETECCION/CONTROL)','PREVENTIVO',8),
    ('ENDOCRINO','OTROS',9),
    ('GENITOURINARIO','OTROS',10),
    ('GINECOLOGICO','OTROS',11),
    ('INYECCION','OTROS',12),
    ('NEUROLOGICO','NEUROLOGICO',13),
    ('OFTALMICO','OTROS',14),
    ('OTICO','OTROS',15),
    ('PSICOSOMATICO','NEUROLOGICO',16),
    ('RESPIRATORIO','RESPIRATORIO',17),
    ('TENSION ARTERIAL (DETEC/CONTROL)','CARDIOVASCULAR',18),
    ('TOMA DE GLUCOSA','PREVENTIVO',19),
    ('VACUNA / METODO PF / SEG SALUD','PREVENTIVO',20),
    ('FISIOTERAPIA','MUSCULOESQUELETICO',21),
    ('NOM 035','PREVENTIVO',22),
    ('MT - ALGIA / MIALGIA / CONTUSION','MUSCULOESQUELETICO',23),
    ('MP - ALGIA / MIALGIA / CONTUSION','MUSCULOESQUELETICO',24),
    ('GRAL - ALGIA / MIALGIA / CONTUSION','MUSCULOESQUELETICO',25),
    ('CERVICAL / DORSAL / LUMBAR','MUSCULOESQUELETICO',26);

-- -----------------------------------------------------------------------------
-- Tipos de lesión musculoesquelética (15)
-- MT = miembro torácico · MP = miembro pélvico
-- -----------------------------------------------------------------------------

INSERT INTO injury_types (nombre, region_corporal, orden) VALUES
    ('AMPUTACION MT','MT',1),
    ('AMPUTACION MP','MP',2),
    ('ALGIA/CONTUSION MT','MT',3),
    ('ALGIA/CONTUSION MP','MP',4),
    ('CERVICALGIA/DORSALGIA/LUMBALGIA','COLUMNA',5),
    ('CONTUSION/TRAUMA CABEZA','CABEZA',6),
    ('CONTUSION/TRAUMA TRONCO','TRONCO',7),
    ('ESGUINCE MT','MT',8),
    ('ESGUINCE MP','MP',9),
    ('LUXACION MT','MT',10),
    ('LUXACION MP','MP',11),
    ('FX MT','MT',12),
    ('FX MP','MP',13),
    ('FX CABEZA/TRONCO','CABEZA',14),
    ('POLICONTUNDIDO','MULTIPLE',15);

-- -----------------------------------------------------------------------------
-- Catálogos clínicos de apoyo
-- -----------------------------------------------------------------------------

INSERT INTO medical_exam_types (codigo, nombre, orden) VALUES
    ('INGRESO','NUEVO INGRESO',1), ('PERIODICO','PERIODICO',2),
    ('POS_INCAPACIDAD','POS INCAPACIDAD',3), ('PRETEST','PRETEST',4);

INSERT INTO medical_exam_results (codigo, nombre, orden) VALUES
    ('APTO','APTO',1), ('NO_APTO','NO APTO',2),
    ('CONDICIONADO','CONDICIONADO',3), ('INCLUSION','INCLUSION',4);

INSERT INTO disability_types (codigo, nombre, orden) VALUES
    ('ENF_GENERAL','ENFERMEDAD GENERAL',1), ('MATERNIDAD','MATERNIDAD',2),
    ('ACC_LABORAL','ACCIDENTE LABORAL',3), ('ACC_TRAYECTO','ACCIDENTE TRAYECTO',4),
    ('INTERNA','INTERNA',5);

INSERT INTO accident_types (codigo, nombre, orden) VALUES
    ('LABORAL','LABORAL',1), ('TRAYECTO','TRAYECTO',2);

INSERT INTO accident_causes (nombre, orden) VALUES
    ('ACCID VIAL',1),('AGRESION',2),('CAIDA',3),('GOLPE',4),
    ('HERIDA',5),('ERGONOMICA',6),('IMPROCEDENTE',7);

INSERT INTO accident_statuses (codigo, nombre, orden) VALUES
    ('PENDIENTE','PENDIENTE',1), ('CALIFICADO','CALIFICADO',2),
    ('IMPROCEDENTE','IMPROCEDENTE',3), ('BAJA_INCONCLUSO','BAJA INCONCLUSO',4);

INSERT INTO drug_test_types (codigo, nombre, orden) VALUES
    ('ANTIDOPING','ANTIDOPING',1), ('ALCOHOLEMIA','ALCOHOLEMIA',2);

INSERT INTO drug_test_results (codigo, nombre, orden) VALUES
    ('NEGATIVO','NEGATIVO',1), ('POSITIVO','POSITIVO',2);

INSERT INTO drug_test_statuses (codigo, nombre, orden) VALUES
    ('NA','N/A',1),('CAPA','CAPA',2),('NO_INTERESADO','NO INTERESADO/BAJA',3),
    ('NO_CONTRATADO','NO CONTRATADO',4),('RECAIDA','RECAIDA',5);

INSERT INTO age_ranges (nombre, edad_min, edad_max, orden) VALUES
    ('18-25',18,25,1),('26-35',26,35,2),('36-45',36,45,3),
    ('46-55',46,55,4),('55+',56,NULL,5);

-- -----------------------------------------------------------------------------
-- Umbrales de riesgo y alerta
-- -----------------------------------------------------------------------------

INSERT INTO risk_thresholds (clave, descripcion, valor_numero) VALUES
    ('morbilidad_critico','Días de incapacidad acumulados para clasificar un predio en riesgo crítico',280),
    ('morbilidad_alto','Días de incapacidad acumulados para riesgo alto',180),
    ('morbilidad_medio','Días de incapacidad acumulados para riesgo medio',60),
    ('antidoping_cad_critico','Días restantes de caducidad que disparan alerta crítica',30),
    ('antidoping_cad_alerta','Días restantes de caducidad que disparan advertencia',60),
    ('antidoping_cad_vigilar','Días restantes de caducidad que ponen el lote en vigilancia',90),
    ('antidoping_stock_min','Porcentaje mínimo de existencia antes de alertar',20),
    ('variacion_relevante','Variación porcentual mensual que genera un insight automático',15);

-- -----------------------------------------------------------------------------
-- Periodos 2025-2027
-- -----------------------------------------------------------------------------

INSERT INTO periods (anio, mes)
SELECT anio, mes
FROM generate_series(2025, 2027) AS anio,
     generate_series(1, 12) AS mes;

-- -----------------------------------------------------------------------------
-- Roles y permisos
-- -----------------------------------------------------------------------------

INSERT INTO roles (nombre, descripcion) VALUES
    ('SUPER_ADMIN','Control total de la plataforma'),
    ('SALUD_OCUPACIONAL','Captura y administración de información médica'),
    ('GERENTE_SALUD','Consulta completa y análisis'),
    ('GERENTE_PREDIO','Consulta acotada a sus predios autorizados'),
    ('DIRECCION','Dashboard ejecutivo consolidado, solo agregados'),
    ('CONSULTA','Solo lectura agregada');

INSERT INTO permissions (codigo, descripcion, nivel_sensibilidad) VALUES
    ('attention.read','Consultar atenciones médicas','OPERATIVO'),
    ('attention.create','Registrar atenciones médicas','OPERATIVO'),
    ('attention.update','Modificar atenciones médicas','OPERATIVO'),
    ('disability.read','Consultar incapacidades','OPERATIVO'),
    ('disability.create','Registrar incapacidades','OPERATIVO'),
    ('disability.update','Modificar incapacidades','OPERATIVO'),
    ('accident.read','Consultar accidentes','OPERATIVO'),
    ('accident.create','Registrar accidentes','OPERATIVO'),
    ('exam.read','Consultar exámenes médicos','OPERATIVO'),
    ('exam.create','Registrar exámenes médicos','OPERATIVO'),
    ('drugtest.read','Consultar pruebas de antidoping','OPERATIVO'),
    ('drugtest.create','Registrar pruebas de antidoping','OPERATIVO'),
    ('inventory.manage','Administrar inventario de pruebas','OPERATIVO'),
    ('maternity.read','Consultar casos de maternidad','OPERATIVO'),
    ('maternity.create','Registrar casos de maternidad','OPERATIVO'),
    ('employee.read','Consultar empleados','OPERATIVO'),
    ('employee.write','Administrar empleados','OPERATIVO'),
    ('employee.medical.read','Ver información médica individual identificable','OPERATIVO'),
    ('dashboard.executive','Ver el dashboard ejecutivo','AGREGADO'),
    ('reports.export','Exportar reportes','AGREGADO'),
    ('import.execute','Ejecutar importaciones de Excel','OPERATIVO'),
    ('admin.catalogs','Administrar catálogos','OPERATIVO'),
    ('admin.users','Administrar usuarios','OPERATIVO'),
    ('admin.permissions','Administrar roles y permisos','OPERATIVO'),
    ('audit.read','Consultar la bitácora de auditoría','OPERATIVO');

-- SUPER_ADMIN recibe todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.nombre = 'SUPER_ADMIN';

-- SALUD_OCUPACIONAL: captura y consulta operativa
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.nombre = 'SALUD_OCUPACIONAL'
  AND p.codigo IN ('attention.read','attention.create','attention.update',
                   'disability.read','disability.create','disability.update',
                   'accident.read','accident.create','exam.read','exam.create',
                   'drugtest.read','drugtest.create','inventory.manage',
                   'maternity.read','maternity.create','employee.read','employee.write',
                   'employee.medical.read','dashboard.executive','reports.export','import.execute');

-- GERENTE_SALUD: consulta completa sin captura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.nombre = 'GERENTE_SALUD'
  AND p.codigo IN ('attention.read','disability.read','accident.read','exam.read',
                   'drugtest.read','maternity.read','employee.read','employee.medical.read',
                   'dashboard.executive','reports.export','audit.read');

-- GERENTE_PREDIO: igual que gerencia de salud, acotado por user_predio_access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.nombre = 'GERENTE_PREDIO'
  AND p.codigo IN ('attention.read','disability.read','accident.read','exam.read',
                   'drugtest.read','maternity.read','employee.read',
                   'dashboard.executive','reports.export');

-- DIRECCION: solo información agregada, sin datos médicos individuales
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.nombre = 'DIRECCION' AND p.codigo IN ('dashboard.executive','reports.export');

-- CONSULTA: solo lectura agregada
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.nombre = 'CONSULTA' AND p.codigo = 'dashboard.executive';

-- -----------------------------------------------------------------------------
-- Usuario administrador inicial
-- Contraseña: CambiarEnProduccion123!  (BCrypt, coste 10)
-- Debe cambiarse en el primer inicio de sesión.
-- -----------------------------------------------------------------------------

INSERT INTO users (email, password_hash, nombre, role_id)
SELECT 'admin@saludocupacional.mx',
       '$2a$10$IFfAdDYNwCVTeZt9Ptp7d.LTiPxEi1hhFNB/qyWadC8eNkJeWYrtO',
       'Administrador del Sistema',
       r.id
FROM roles r WHERE r.nombre = 'SUPER_ADMIN';
