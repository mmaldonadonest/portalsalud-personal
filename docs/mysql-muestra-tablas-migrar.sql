-- =====================================================================================
-- mysql-muestra-tablas-migrar.sql  —  ORIGEN MariaDB `servicioMedico` (legacy PHP), SOLO LECTURA.
-- Saca la muestra real de las 2 tablas que se migran a Oracle (ver
-- docs/etl-migracion-historica-especificacion.md y docs/muestra-tablas-mysql-migracion.html).
--
-- Conexión (spec §1):  host 10.249.249.4  puerto 3306  base servicioMedico  usuario root
-- IMPORTANTE: abrir la sesión en utf8mb4, si no los acentos salen mal:
--    mysql --default-character-set=utf8mb4 -h 10.249.249.4 -u root -p servicioMedico < este.sql > muestra.txt
--
-- NUNCA hacer SELECT * FROM files sin recortar `url`: son ~18 GB de base64.
-- =====================================================================================

-- ---------- 0. Contexto ----------
SELECT VERSION() version, DATABASE() base, NOW() momento;
SHOW TABLES;
SHOW VARIABLES LIKE 'character_set_%';

-- ---------- 1. Conteos reales al momento de la extracción ----------
-- (los números de la spec son un snapshot de 2026-08-13; la base sigue creciendo)
SELECT 'files' tabla, COUNT(*) filas FROM files
UNION ALL SELECT 'tags', COUNT(*) FROM tags;

-- ---------- 2. Estructura ----------
SHOW CREATE TABLE files;
SHOW CREATE TABLE tags;

-- ---------- 3. Muestra de `files` (sin el base64 completo) ----------
SELECT id, nss, name, date_upload, type,
       LENGTH(url)                 AS largo_base64,
       ROUND(LENGTH(url)*3/4/1024) AS kb_aprox_decodificado,
       LEFT(url, 24)               AS url_inicio   -- JVBERi0 = %PDF, iVBORw0 = PNG, /9j/ = JPG
  FROM files
 ORDER BY id
 LIMIT 20;

-- Muestra dirigida: 5 de cada tipo funcional + 5 con type = hash MD5 (32 chars)
(SELECT 'examen_medico' grupo, id, nss, name, date_upload, type, LENGTH(url) largo FROM files WHERE type='examen_medico' ORDER BY id LIMIT 5)
UNION ALL (SELECT 'laboratorio', id, nss, name, date_upload, type, LENGTH(url) FROM files WHERE type='laboratorio' ORDER BY id LIMIT 5)
UNION ALL (SELECT 'nota_medica', id, nss, name, date_upload, type, LENGTH(url) FROM files WHERE type='nota_medica' ORDER BY id LIMIT 5)
UNION ALL (SELECT 'nota_incapacidad', id, nss, name, date_upload, type, LENGTH(url) FROM files WHERE type='nota_incapacidad' ORDER BY id LIMIT 5)
UNION ALL (SELECT 'hash_md5(32)', id, nss, name, date_upload, type, LENGTH(url) FROM files WHERE LENGTH(type)=32 ORDER BY id LIMIT 5);

-- Distribución por tipo (polimórfico: categoría funcional o hash MD5 de consulta relacionada)
SELECT CASE WHEN LENGTH(type)=32 THEN '<hash md5 de consulta>' ELSE type END tipo,
       COUNT(*) filas, MIN(date_upload) desde, MAX(date_upload) hasta
  FROM files GROUP BY 1 ORDER BY filas DESC;

-- Volumen por año (para dimensionar la carga y el filesystem destino)
SELECT YEAR(date_upload) anio, COUNT(*) archivos,
       ROUND(SUM(LENGTH(url))*3/4/1024/1024/1024, 2) gb_aprox
  FROM files GROUP BY anio ORDER BY anio;

-- Extensiones presentes en `name`
SELECT LOWER(SUBSTRING_INDEX(name, '.', -1)) ext, COUNT(*) filas
  FROM files WHERE name LIKE '%.%' GROUP BY ext ORDER BY filas DESC;

-- ---------- 4. Calidad conocida en `files` (spec §5) ----------
SELECT 'url vacía (huérfanas, se excluyen)' hallazgo, COUNT(*) filas FROM files WHERE url IS NULL OR url = ''
UNION ALL SELECT 'name sin extensión (magic-bytes)', COUNT(*) FROM files WHERE name NOT LIKE '%.%'
UNION ALL SELECT 'duplicados de contenido (MD5 url)', COUNT(*) FROM (SELECT MD5(url) h FROM files WHERE url<>'' GROUP BY h HAVING COUNT(*)>1) d
UNION ALL SELECT 'name con posible encoding roto', COUNT(*) FROM files WHERE name REGEXP 'Ã|Â|�';
SELECT id, nss, name, type FROM files WHERE name NOT LIKE '%.%' ORDER BY id;                       -- los 3 sin extensión
SELECT id, nss, name FROM files WHERE name REGEXP 'Ã|Â|�' ORDER BY id LIMIT 20;                    -- encoding roto (cosmético)

-- ---------- 5. Muestra de `tags` (EAV: type = campo, content = valor) ----------
SELECT id, nss, type, LEFT(content, 60) content_inicio, LENGTH(content) largo
  FROM tags ORDER BY id LIMIT 30;

-- Un NSS completo: cómo se ve un Pre-Test entero en EAV (cambiar el NSS por uno real de la muestra)
SELECT id, type, LEFT(content, 60) content_inicio
  FROM tags WHERE nss = (SELECT nss FROM tags WHERE type='nomPRETEST' AND content<>'' ORDER BY id DESC LIMIT 1)
 ORDER BY type;

-- Los 137 tipos distintos y su frecuencia (verificación del ETL: ninguno debe quedar sin clasificar)
SELECT type, COUNT(*) filas,
       SUM(CASE WHEN content IS NULL OR content='' THEN 1 ELSE 0 END) vacios
  FROM tags GROUP BY type ORDER BY filas DESC;
SELECT COUNT(DISTINCT type) tipos_distintos FROM tags;   -- esperado: 137

-- La firma digital del Pre-Test es base64 de un canvas (dataURL): se revisa aparte, no se imprime
SELECT id, nss, LENGTH(content) largo, LEFT(content, 30) inicio
  FROM tags WHERE type='drawdataUrlPRETEST' AND content<>'' ORDER BY id DESC LIMIT 5;

-- ---------- 6. Calidad conocida en `tags` (spec §5) ----------
SELECT 'content vacío (27% es normal, EAV disperso)' hallazgo, COUNT(*) filas FROM tags WHERE content IS NULL OR content=''
UNION ALL SELECT 'nss NULL (legítimas, no excluir)', COUNT(*) FROM tags WHERE nss IS NULL
UNION ALL SELECT 'content con encoding roto', COUNT(*) FROM tags WHERE content REGEXP 'Ã|Â|�';

-- ---------- 7. Rango de ids (NO es denso: el PHP hace DELETE+INSERT, no UPDATE) ----------
SELECT 'files' tabla, MIN(id) id_min, MAX(id) id_max, COUNT(*) filas, MAX(id)-MIN(id)+1-COUNT(*) huecos FROM files
UNION ALL SELECT 'tags', MIN(id), MAX(id), COUNT(*), MAX(id)-MIN(id)+1-COUNT(*) FROM tags;

-- ---------- 8. Exportar la muestra a CSV (opcional, para compartir) ----------
-- Requiere permisos de FILE en el servidor MariaDB; si no, usar el cliente:
--   mysql ... -e "SELECT ..." --batch > muestra_files.tsv
-- SELECT id, nss, name, date_upload, type, LENGTH(url)
--   INTO OUTFILE '/tmp/muestra_files.csv' FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n'
--   FROM files ORDER BY id LIMIT 200;
