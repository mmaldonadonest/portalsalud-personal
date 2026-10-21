-- =============================================================================
-- BASE: ORDS (esquema de los WS Servcio/*), tabla TBL_SERV_CONSULTA_MEDICA
-- Borra la consulta de prueba del 21-oct-2026 (tipo Pruebas Especiales con pase de medicamento)
-- capturada para revisar los cambios por las respuestas de Servicio Medico. Solo la del NSS
-- 30048315698 con esta relacion y motivo de prueba.
-- Sus datos de formatos van aparte: portal-borrar-prueba-firmas.sql (base del PORTAL).
-- Lo corre el usuario. Revisar que el SELECT regrese 1 fila antes del DELETE.
-- =============================================================================

-- 1) Debe salir exactamente 1 fila: tipo pruebas_especiales, motivo "PRUEBA DEL PORTAL - NO VALIDA (respuestas SM..."
SELECT FECHA, NSS, TIPO_CONSULTA, MOTIVO_CONSULTA, CONSULTA_RELACIONADA
  FROM TBL_SERV_CONSULTA_MEDICA
 WHERE NSS = '30048315698'
   AND CONSULTA_RELACIONADA = '543a5826778f46bb8aebec3e9f378738'
   AND MOTIVO_CONSULTA LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 2) Borrado (mismas condiciones)
DELETE FROM TBL_SERV_CONSULTA_MEDICA
 WHERE NSS = '30048315698'
   AND CONSULTA_RELACIONADA = '543a5826778f46bb8aebec3e9f378738'
   AND MOTIVO_CONSULTA LIKE 'PRUEBA DEL PORTAL - NO VALIDA%';

-- 3) Si dijo "1 fila eliminada":
COMMIT;
