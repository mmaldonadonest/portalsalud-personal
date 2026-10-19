-- ===========================================================================
--  ALINEAR LOS NOMBRES DE PREDIO DEL HISTORICO CON EL CATALOGO DEL PORTAL
--  Base del PORTAL (JDBC), tablas dentro de BIOMETRICO.
--  Correr en QA y en PRODUCCION. Es idempotente: la segunda vez cambia 0 filas.
--
--  EL PROBLEMA
--    Las pantallas de Analisis filtran con UPPER(PREDIO) = UPPER(?), donde el
--    valor sale del select de predios, y ese select sale del catalogo
--    SERV_MED_PREDIO (docs/ords-predio-cuenta.sql, 17 sitios).
--    El cargador escribio cuatro de esos nombres con otra grafia:
--
--        catalogo (lo que pide la pantalla)   historico (lo que se guardo)
--        ----------------------------------   ----------------------------
--        MACRO I                              MACRO 1
--        MACRO II                             MACRO 2
--        MIKELS                               MKLS
--        U TEPALCAPA                          UT
--
--    Igualdad exacta: no empareja y la ficha sale en ceros con los datos
--    cargados y a la vista. Sin error, sin aviso. Los otros once nombres
--    (AIFA, ATIZAPAN, CHARCON, FLORA, MERCURIO, SIGLO XXI, SMO, TOLUCA,
--    TULTIPARK, WORLD PARK, Z VALLEJO, FORANEO) ya coincidian.
--
--  SE CAMBIA EL HISTORICO, NO EL CATALOGO
--    El catalogo lo comparte el mapeo cuenta->predio de /admin/predios, que ya
--    tiene asignaciones hechas. Renombrarlo romperia esas. El historico lo lee
--    solo el modulo de Analisis y se acaba de cargar.
--
--  EL CARGADOR YA QUEDO CORREGIDO (Normalizador.PREDIOS_CANONICOS), asi que una
--  carga futura escribe directo la forma del catalogo y esto no se repite.
--
--  HACE COMMIT AL FINAL. La reversa es correrlo al reves, pero no hace falta:
--  ninguno de los cuatro nombres viejos es un predio real del catalogo.
-- ===========================================================================

SET LINESIZE 200
SET PAGESIZE 100
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK OFF

PROMPT
PROMPT ==== ANTES: como estan escritos hoy ======================================

COLUMN predio FORMAT A22
COLUMN nota   FORMAT A34

SELECT NVL(PREDIO, '(nulo)') AS predio,
       COUNT(*)              AS eventos,
       CASE PREDIO
         WHEN 'MACRO 1' THEN '--> se renombra a MACRO I'
         WHEN 'MACRO 2' THEN '--> se renombra a MACRO II'
         WHEN 'MKLS'    THEN '--> se renombra a MIKELS'
         WHEN 'UT'      THEN '--> se renombra a U TEPALCAPA'
         ELSE ' '
       END                   AS nota
  FROM SERV_MED_BITACORA_EVENTO
 GROUP BY PREDIO
 ORDER BY eventos DESC;


PROMPT
PROMPT ==== CAMBIO ==============================================================

DECLARE
  -- Pares viejo -> nuevo. Para agregar otro, agregarlo aqui y ya.
  TYPE t_txt IS TABLE OF VARCHAR2(120);
  viejos t_txt := t_txt('MACRO 1', 'MACRO 2', 'MKLS',   'UT');
  nuevos t_txt := t_txt('MACRO I', 'MACRO II','MIKELS', 'U TEPALCAPA');
  n_eve  NUMBER;
  n_met  NUMBER;
  n_lot  NUMBER;
BEGIN
  FOR i IN 1 .. viejos.COUNT LOOP
    -- Comparacion laxa (UPPER + TRIM) por si quedo algun espacio de mas; la
    -- escritura es siempre la forma exacta del catalogo.
    UPDATE SERV_MED_BITACORA_EVENTO
       SET PREDIO = nuevos(i)
     WHERE UPPER(TRIM(PREDIO)) = UPPER(viejos(i));
    n_eve := SQL%ROWCOUNT;

    -- METRICA tiene UNIQUE (LOTE_ID, ORIGEN, PREDIO, ANIO, MES, CONCEPTO,
    -- SUBCONCEPTO). No puede chocar: dentro de un lote el predio es uno solo,
    -- asi que el renombre no junta dos filas distintas bajo la misma llave.
    UPDATE SERV_MED_BITACORA_METRICA
       SET PREDIO = nuevos(i)
     WHERE UPPER(TRIM(PREDIO)) = UPPER(viejos(i));
    n_met := SQL%ROWCOUNT;

    -- LOTE.PREDIO no lo lee ninguna pantalla, pero si se deja sin tocar el
    -- reporte de la bitacora contradice a los datos.
    UPDATE SERV_MED_BITACORA_LOTE
       SET PREDIO = nuevos(i)
     WHERE UPPER(TRIM(PREDIO)) = UPPER(viejos(i));
    n_lot := SQL%ROWCOUNT;

    DBMS_OUTPUT.PUT_LINE(RPAD(viejos(i), 10) || ' -> ' || RPAD(nuevos(i), 13)
      || ' eventos: ' || LPAD(n_eve, 6)
      || ' · metricas: ' || LPAD(n_met, 5)
      || ' · lotes: ' || LPAD(n_lot, 4));
  END LOOP;
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('');
  DBMS_OUTPUT.PUT_LINE('COMMIT hecho.');
END;
/


PROMPT
PROMPT ==== DESPUES: ningun predio del historico debe quedar fuera del catalogo ==
PROMPT    Esta consulta tiene que salir VACIA. Lo que aparezca aqui es un predio
PROMPT    que el select de la pantalla no ofrece, o sea datos invisibles.
PROMPT
PROMPT    Los 17 nombres van escritos a mano y NO con un join a SERV_MED_PREDIO:
PROMPT    ese catalogo lo lee el portal por ORDS y puede no estar en esta misma
PROMPT    base. Si lo esta, el join es mejor; la lista sale de la siembra de
PROMPT    docs/ords-predio-cuenta.sql.

WITH catalogo AS (
  SELECT 'AIFA' nombre FROM DUAL UNION ALL SELECT 'ATIZAPAN'    FROM DUAL UNION ALL
  SELECT 'CHARCON'     FROM DUAL UNION ALL SELECT 'FLORA'       FROM DUAL UNION ALL
  SELECT 'MACRO I'     FROM DUAL UNION ALL SELECT 'MACRO II'    FROM DUAL UNION ALL
  SELECT 'MERCURIO'    FROM DUAL UNION ALL SELECT 'MIKELS'      FROM DUAL UNION ALL
  SELECT 'SIGLO XXI'   FROM DUAL UNION ALL SELECT 'SMO'         FROM DUAL UNION ALL
  SELECT 'TOLUCA'      FROM DUAL UNION ALL SELECT 'TULTIPARK'   FROM DUAL UNION ALL
  SELECT 'U TEPALCAPA' FROM DUAL UNION ALL SELECT 'WORLD PARK'  FROM DUAL UNION ALL
  SELECT 'Z VALLEJO'   FROM DUAL UNION ALL SELECT 'FORANEO'     FROM DUAL UNION ALL
  SELECT 'SIN DATO'    FROM DUAL
)
SELECT e.PREDIO AS predio, COUNT(*) AS eventos
  FROM SERV_MED_BITACORA_EVENTO e
 WHERE e.PREDIO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM catalogo c
                    WHERE UPPER(TRIM(c.nombre)) = UPPER(TRIM(e.PREDIO)))
 GROUP BY e.PREDIO
 ORDER BY 2 DESC;

PROMPT
PROMPT ==== Lo mismo para las metricas (consumo de medicamentos) =================
PROMPT    Aqui SI se espera un renglon: MIKELS-UT-FLORA. Esa carpeta trae un solo
PROMPT    archivo de consumo para los tres predios y el Excel no los separa, asi
PROMPT    que no se puede repartir sin inventar. Es pregunta para servicio medico.

WITH catalogo AS (
  SELECT 'AIFA' nombre FROM DUAL UNION ALL SELECT 'ATIZAPAN'    FROM DUAL UNION ALL
  SELECT 'CHARCON'     FROM DUAL UNION ALL SELECT 'FLORA'       FROM DUAL UNION ALL
  SELECT 'MACRO I'     FROM DUAL UNION ALL SELECT 'MACRO II'    FROM DUAL UNION ALL
  SELECT 'MERCURIO'    FROM DUAL UNION ALL SELECT 'MIKELS'      FROM DUAL UNION ALL
  SELECT 'SIGLO XXI'   FROM DUAL UNION ALL SELECT 'SMO'         FROM DUAL UNION ALL
  SELECT 'TOLUCA'      FROM DUAL UNION ALL SELECT 'TULTIPARK'   FROM DUAL UNION ALL
  SELECT 'U TEPALCAPA' FROM DUAL UNION ALL SELECT 'WORLD PARK'  FROM DUAL UNION ALL
  SELECT 'Z VALLEJO'   FROM DUAL UNION ALL SELECT 'FORANEO'     FROM DUAL UNION ALL
  SELECT 'SIN DATO'    FROM DUAL
)
SELECT m.PREDIO AS predio, COUNT(*) AS metricas
  FROM SERV_MED_BITACORA_METRICA m
 WHERE m.PREDIO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM catalogo c
                    WHERE UPPER(TRIM(c.nombre)) = UPPER(TRIM(m.PREDIO)))
 GROUP BY m.PREDIO
 ORDER BY 2 DESC;

PROMPT
PROMPT ===========================================================================
PROMPT  Listo. Recargar /analisis/predio y elegir MACRO I o MACRO II.
PROMPT ===========================================================================
PROMPT

SET FEEDBACK ON
