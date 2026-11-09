-- =============================================================================
-- DIAGNOSTICO E-024 (9-nov-2026), v3: "Error al registrar (HTTP 502) ... Problema en el
-- procedimiento PR_SERVICIO_MED_EXAMEN2" al finalizar el EXAMEN INICIAL en PRODUCCION.
--
-- BASE: BIOMETRICO de produccion (base del portal). Lo corre el usuario.
-- SOLO LECTURA: un SELECT y un bloque PL/SQL que solo LEE e imprime con DBMS_OUTPUT.
-- No borra, no actualiza, no inserta.
--
-- Por que: PR_SERVICIO_MED_EXAMEN2 tiene "WHEN OTHERS THEN KRESPUESTA := 1", asi que esconde el
-- error real de Oracle. Muchas de sus variables son VARCHAR2(100): un texto de mas de 100 BYTES
-- (las letras con acento cuentan 2) en cualquier campo lo hace fallar.
--
-- v3: la v2 recorria el JSON con expresiones regulares sobre el CLOB y tardaba demasiado (el
-- usuario la cancelo). Ahora se lee con JSON_OBJECT_T: una pasada por borrador.
-- Las consultas 1 y 4 de la v1 ya se corrieron (9-nov): aqui solo va lo que faltaba.
--
-- Correr con:  @C:\miguel\portalsalud-personal\docs\script-prod\diagnostico-e024-502-finalizar.sql
-- =============================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET LINESIZE 250

PROMPT === Campos de mas de 100 bytes y empleos con N/A o 0, en borradores sin finalizar (ultimos 3 dias) ===
DECLARE
  o      JSON_OBJECT_T;
  claves JSON_KEY_LIST;
  k      VARCHAR2(4000);
  v      VARCHAR2(32767);
  largos NUMBER;
  nas    NUMBER;
BEGIN
  FOR b IN (SELECT ID, NSS, HOJA_ACTUAL, DATOS
              FROM SERV_MED_EXAMEN_BORRADOR
             WHERE ESTATUS = 'BORRADOR'
               AND UPDATED_AT >= SYSTIMESTAMP - INTERVAL '3' DAY
             ORDER BY UPDATED_AT DESC) LOOP
    largos := 0;
    nas := 0;
    DBMS_OUTPUT.PUT_LINE('--- NSS ' || b.NSS || ' (borrador ' || b.ID || ', hoja ' || b.HOJA_ACTUAL || ')');
    BEGIN
      o := JSON_OBJECT_T.PARSE(b.DATOS);
      claves := o.GET_KEYS;
      FOR i IN 1 .. claves.COUNT LOOP
        k := claves(i);
        -- la firma es una imagen (va por CLOB en el procedimiento): no cuenta
        IF LOWER(k) <> 'firma' AND UPPER(k) NOT LIKE '%FIRMA%' THEN
          BEGIN
            v := o.GET_STRING(k);
          EXCEPTION WHEN OTHERS THEN
            v := '(no se pudo leer como texto)';
          END;
          IF LENGTHB(v) > 100 THEN
            largos := largos + 1;
            DBMS_OUTPUT.PUT_LINE('   LARGO ' || LPAD(LENGTHB(v), 5) || ' bytes  ' || k || ' = ' || SUBSTR(v, 1, 80));
          END IF;
          IF k LIKE 'trabajo%' AND UPPER(TRIM(v)) IN ('N/A', 'NA', '0') THEN
            nas := nas + 1;
          END IF;
        END IF;
      END LOOP;
      DBMS_OUTPUT.PUT_LINE('   campos: ' || claves.COUNT || ' | de mas de 100 bytes: ' || largos
                           || ' | empleos con N/A o 0: ' || nas);
    EXCEPTION WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('   no se pudo leer el JSON: ' || SQLERRM);
    END;
  END LOOP;
END;
/
