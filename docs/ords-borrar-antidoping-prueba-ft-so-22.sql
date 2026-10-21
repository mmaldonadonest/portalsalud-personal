-- =============================================================================
-- BASE: ORDS (esquema de los WS Servcio/*), tabla SERV_MED_ANTIDOPING_RESULTADO
-- Borra el antidoping POSITIVO de prueba capturado el 20-oct-2026 para revisar el
-- impreso FT-SO-22 (Aceptacion a CECOSAMA). Solo el registro 61 del NSS 30048315698.
-- Lo corre el usuario. Revisar que el SELECT regrese 1 fila antes del DELETE.
-- =============================================================================

-- 1) Debe salir exactamente 1 fila: THC, POSITIVO, "PRUEBA DEL PORTAL - NO VALIDA ..."
SELECT REG_ID, NSS, FECHA_REGISTRO, TIPO_PRUEBA, SUSTANCIA, RESULTADO, OBSERVACIONES
  FROM SERV_MED_ANTIDOPING_RESULTADO
 WHERE REG_ID = 61
   AND NSS = '30048315698'
   AND RESULTADO = 'POSITIVO'
   AND OBSERVACIONES LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 2) Borrado (mismas condiciones: si algo no coincide, no borra nada)
DELETE FROM SERV_MED_ANTIDOPING_RESULTADO
 WHERE REG_ID = 61
   AND NSS = '30048315698'
   AND RESULTADO = 'POSITIVO'
   AND OBSERVACIONES LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 3) Si dijo "1 fila eliminada":
COMMIT;
