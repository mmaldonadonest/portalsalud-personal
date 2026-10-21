-- =============================================================================
-- BASE: ORDS (esquema de los WS Servcio/*), tabla TBL_SERV_CONSULTA_MEDICA
-- Borra la consulta de PASE DE SALIDA de prueba capturada el 20-oct-2026 para revisar el
-- impreso FT-SO-02. Solo la del NSS 30048315698 con esta relacion y motivo de prueba.
-- Sus datos de pase van aparte: portal-borrar-pase-prueba-ft-so-02.sql (base del PORTAL).
-- Lo corre el usuario. Revisar que el SELECT regrese 1 fila antes del DELETE.
-- =============================================================================

-- 1) Debe salir exactamente 1 fila: tipo pase_salida, motivo "PRUEBA DEL PORTAL - NO VALIDA (FT-SO-02 ..."
SELECT FECHA, NSS, TIPO_CONSULTA, MOTIVO_CONSULTA, DIAGNOSTICO, CONSULTA_RELACIONADA
  FROM TBL_SERV_CONSULTA_MEDICA
 WHERE NSS = '30048315698'
   AND CONSULTA_RELACIONADA = '67e5ba9c3eff4a3aac78ed0184cb808a'
   AND MOTIVO_CONSULTA LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 2) Borrado (mismas condiciones: si algo no coincide, no borra nada)
DELETE FROM TBL_SERV_CONSULTA_MEDICA
 WHERE NSS = '30048315698'
   AND CONSULTA_RELACIONADA = '67e5ba9c3eff4a3aac78ed0184cb808a'
   AND MOTIVO_CONSULTA LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 3) Si dijo "1 fila eliminada":
COMMIT;
