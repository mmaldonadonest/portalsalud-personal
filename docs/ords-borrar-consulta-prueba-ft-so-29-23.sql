-- =============================================================================
-- BASE: ORDS (esquema de los WS Servcio/*), tabla TBL_SERV_CONSULTA_MEDICA
-- Borra la consulta de prueba capturada el 20-oct-2026 para revisar los impresos FT-SO-29
-- (pase de medicamento) y FT-SO-23 (pruebas especiales). Solo la del NSS 30048315698 con
-- esta relacion y motivo de prueba.
-- Sus datos de formatos van aparte: portal-borrar-pruebas-ft-so-29-23-31.sql (base del PORTAL).
-- Lo corre el usuario. Revisar que el SELECT regrese 1 fila antes del DELETE.
-- =============================================================================

-- 1) Debe salir exactamente 1 fila: motivo "PRUEBA DEL PORTAL - NO VALIDA (FT-SO-29 y FT-SO-23 ..."
SELECT FECHA, NSS, TIPO_CONSULTA, MOTIVO_CONSULTA, DIAGNOSTICO, CONSULTA_RELACIONADA
  FROM TBL_SERV_CONSULTA_MEDICA
 WHERE NSS = '30048315698'
   AND CONSULTA_RELACIONADA = '0c0256bc93f348028323c5ac656c2171'
   AND MOTIVO_CONSULTA LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 2) Borrado (mismas condiciones: si algo no coincide, no borra nada)
DELETE FROM TBL_SERV_CONSULTA_MEDICA
 WHERE NSS = '30048315698'
   AND CONSULTA_RELACIONADA = '0c0256bc93f348028323c5ac656c2171'
   AND MOTIVO_CONSULTA LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 3) Si dijo "1 fila eliminada":
COMMIT;
