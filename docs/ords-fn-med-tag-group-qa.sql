-- =============================================================================
-- SERV_MED_FN_TAG_GROUP faltante en QA (ONEWMS_QA) - encontrado 31-ago-2026 al correr
-- el ETL standalone de tags: ORA-00904 "SERV_MED_FN_TAG_GROUP": invalid identifier.
-- SERV_MED_TAG (la tabla) SI existe en QA; solo falta esta funcion.
-- Copia exacta de src/main/resources/db/sql/app_domain/tags-salud.sql (bloque 3),
-- ya aplicada y validada contra Oracle LOCAL de desarrollo.
-- =============================================================================
CREATE OR REPLACE FUNCTION SERV_MED_FN_TAG_GROUP(p_type IN VARCHAR2)
RETURN VARCHAR2
DETERMINISTIC
IS
BEGIN
  -- Pre-Test de ingreso (sufijo *PRETEST en todos sus campos)
  IF p_type LIKE '%PRETEST%' THEN
    RETURN 'PRETEST';
  END IF;

  -- Firma digital (base64 de canvas HTML - candidato a SERV_MED_FS_FILE en fase futura)
  IF p_type = 'drawdataUrlPRETEST' THEN
    RETURN 'FIRMA_DIGITAL';
  END IF;

  -- Examen fisico: audiometria OD/OI, agudeza visual, SpO2, Romberg, voz, traquea
  IF p_type IN (
    'ODAGAUDIINPEXP', 'ODCAEINPEXP', 'ODEXCAEEXFISINP',
    'ODEXFISINPCERC', 'ODEXFISINPCERCIZQ', 'ODMEMTIMINPEXP',
    'OIAGAUDIINPEXP', 'OICAEINPEXP',  'OIMEMTIMINPEXP',
    'LENTEOJODERECHOEXP', 'LENTEOJOIZQUIERDOEXP',
    'ROMBERGEXFIS', 'STPO2', 'VOZCLARAYFUERTE',
    'TRAQUEA_OBSINP', 'OBSGENERALINPEX'
  ) THEN
    RETURN 'EXAMEN_FISICO';
  END IF;

  -- Contactos de emergencia (prefijo contactoEmer*)
  IF p_type LIKE 'contactoEmer%' THEN
    RETURN 'CONTACTO_EMERGENCIA';
  END IF;

  -- Historia laboral: hasta 4 empleos anteriores (sufijo 1..4)
  IF p_type IN (
    'nombre1',       'nombre2',       'nombre3',       'nombre4',
    'puesto1',       'puesto2',       'puesto3',       'puesto4',
    'giro1',         'giro2',         'giro3',         'giro4',
    'antiguedad1',   'antiguedad2',   'antiguedad3',   'antiguedad4',
    'salida1',       'salida2',       'salida3',       'salida4',
    'turno1',        'turno2',        'turno3',        'turno4',
    'riesgos1',      'riesgos2',      'riesgos3',      'riesgos4',
    'epp1',          'epp2',          'epp3',          'epp4',
    'observaciones1','observaciones2','observaciones3','observaciones4',
    'descripcion1',  'descripcion2',  'descripcion3',  'descripcion4',
    'cantidad_trabajos', 'edad_inicio_laborar'
  ) THEN
    RETURN 'HISTORIA_LABORAL';
  END IF;

  -- Vacunas / inmunizaciones
  IF p_type = 'fechaINFLUEN' THEN
    RETURN 'VACUNAS';
  END IF;

  -- Permisos de examen medico
  IF p_type IN ('checkExamPermiso', 'tipoExamenInputO') THEN
    RETURN 'PERMISO_EXAMEN';
  END IF;

  -- Incapacidades
  IF p_type LIKE '%INCAP%' OR p_type LIKE '%incap%' THEN
    RETURN 'INCAPACIDAD';
  END IF;

  -- Campos clinicos misceláneos
  IF p_type IN (
    'CCA', 'DIENTE_2', 'Drenaje', 'ciruOBS', 'pension',
    'cadaCuandoDep', 'cadaCuandoPasa'
  ) THEN
    RETURN 'CLINICO_MISC';
  END IF;

  -- Cualquier tipo no clasificado queda en OTRO para revision posterior
  RETURN 'OTRO';

END SERV_MED_FN_TAG_GROUP;
/

-- Verificacion
SELECT SERV_MED_FN_TAG_GROUP('drawdataUrlPRETEST') AS debe_dar_firma_digital FROM dual;
