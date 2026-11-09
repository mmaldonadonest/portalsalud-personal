-- =============================================================================
-- E-024 (9-nov-2026): en que esquema vive el modulo ORDS "Servcio" (handler Medico)?
-- BASE: BIOMETRICO de produccion, CONECTADO COMO BIOMETRICO. Lo corre el usuario.
-- SOLO LECTURA: unicamente SELECT.
--
-- Por que: e024-bitacora-examen-medico-v2.sql encontro PR_SERVICIO_MED_EXAMEN1/2 en BIOMETRICO pero
-- NO el handler Servcio/Medico (ORA-20002). El handler debe estar en otro esquema (la URL es
-- /biows/ords/security) que llama a los procedimientos de BIOMETRICO por permiso o sinonimo.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\e024-donde-vive-ords.sql
-- =============================================================================
SET LINESIZE 200
COLUMN GRANTEE FORMAT A25
COLUMN OWNER FORMAT A25
COLUMN SYNONYM_NAME FORMAT A35
COLUMN TABLE_NAME FORMAT A35
COLUMN PATTERN FORMAT A30

PROMPT === 1) A quien le dio BIOMETRICO permiso de ejecutar los procedimientos del examen ===
SELECT GRANTEE, TABLE_NAME, PRIVILEGE
  FROM USER_TAB_PRIVS_MADE
 WHERE TABLE_NAME IN ('PR_SERVICIO_MED_EXAMEN1', 'PR_SERVICIO_MED_EXAMEN2')
 ORDER BY GRANTEE, TABLE_NAME;

PROMPT === 2) Sinonimos que apuntan a esos procedimientos (desde cualquier esquema visible) ===
SELECT OWNER, SYNONYM_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_SYNONYMS
 WHERE TABLE_NAME IN ('PR_SERVICIO_MED_EXAMEN1', 'PR_SERVICIO_MED_EXAMEN2')
 ORDER BY OWNER;

PROMPT === 3) Esta BIOMETRICO habilitado en ORDS? (si sale vacio, no publica servicios) ===
SELECT PARSING_SCHEMA, PATTERN, STATUS FROM USER_ORDS_SCHEMAS;

PROMPT === 4) Modulos ORDS de BIOMETRICO, por si el nombre o la ruta se escriben distinto ===
SELECT m.NAME MODULO, m.URI_PREFIX, t.URI_TEMPLATE
  FROM USER_ORDS_MODULES m
  LEFT JOIN USER_ORDS_TEMPLATES t ON t.MODULE_ID = m.ID
 WHERE UPPER(m.NAME) LIKE '%SERV%' OR UPPER(t.URI_TEMPLATE) LIKE '%MEDICO%'
 ORDER BY m.NAME, t.URI_TEMPLATE;
