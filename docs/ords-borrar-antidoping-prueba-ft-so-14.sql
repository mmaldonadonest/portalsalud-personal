-- =============================================================================
-- BASE: ORDS (esquema de los WS Servcio/*), tabla SERV_MED_ANTIDOPING_RESULTADO
-- Borra el antidoping NEGATIVO de prueba capturado el 20-oct-2026 para revisar el
-- impreso FT-SO-14 (Cadena de custodia). Solo el registro 62 del NSS 30048315698.
-- Sus datos de custodia van aparte: portal-borrar-custodia-prueba-ft-so-14.sql (base del PORTAL).
-- Lo corre el usuario. Revisar que el SELECT regrese 1 fila antes del DELETE.
-- =============================================================================

-- 1) Debe salir exactamente 1 fila: folio PRUEBA-FTSO14, NEGATIVO, "PRUEBA DEL PORTAL - NO VALIDA ..."
SELECT REG_ID, NSS, FECHA_REGISTRO, FOLIO, TIPO_PRUEBA, SUSTANCIA, RESULTADO, OBSERVACIONES
  FROM SERV_MED_ANTIDOPING_RESULTADO
 WHERE REG_ID = 62
   AND NSS = '30048315698'
   AND FOLIO = 'PRUEBA-FTSO14'
   AND OBSERVACIONES LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 2) Borrado (mismas condiciones: si algo no coincide, no borra nada)
DELETE FROM SERV_MED_ANTIDOPING_RESULTADO
 WHERE REG_ID = 62
   AND NSS = '30048315698'
   AND FOLIO = 'PRUEBA-FTSO14'
   AND OBSERVACIONES LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 3) Si dijo "1 fila eliminada":
COMMIT;
