# Inventario de Webservices (php-old)

Fuente: `php-old/` — se excluye `gli_dev/` (app CodeIgniter independiente, sin relación con Portal Salud) y `node_modules/`.

**Host único:** `http://10.249.249.3/biows/ords/` (Oracle ORDS)
**Método:** `POST` en todos salvo los marcados como GET
**Headers comunes:**
```
x-api-key: XXXXXX          <-- ofuscada en el código fuente
Content-Type: application/json; charset=utf-8
```
`getDatsNss` añade además `Connection: Keep-Alive` y `Accept-Encoding: gzip,deflate`.

**Constante recurrente:** `id_app = 13` (Portal Salud). El módulo `gestion/` usa `id_app` variable.

---

## 1. Portal Salud — `app/app.php`

### 1.1 `security/login/registro_app` — Login
`app::loginApiRest($userV, $pass)` · usado por `rest/login/checklog.php`
```json
{ "id_usuario": "<user>", "id_password": "<pass>", "id_app": "13" }
```

### 1.2 `security/Catalogo/usuario` — Búsqueda de empleado por NSS
`app::searchUser($nss)`
```json
{
  "Nss": "<nss>",
  "Fecha_consulta": "20/07/26 14:03:11",
  "Usuario_consulta": "<$_SESSION['nombre']>",
  "Aplicacion_id": "Biometrico",
  "Id_aplicacion": "4"
}
```
> Nota: `Fecha_consulta` usa formato `d/m/y H:i:s` (año de 2 dígitos) y timezone UTC.

### 1.3 `security/Catalogo/Candidatos` — Búsqueda de prospecto por CURP
`app::searchProspecto($curp)`
```json
{ "curp": "<curp>" }
```

### 1.4 `security/Servcio/consulta_examen` — Expediente / examen médico del NSS
`app::CheckEmploye($nss,$user)` y `app::getDatsNss($nss)` (misma URL, dos usos)
```json
{ "NNS": "<nss>", "FECHA": "20/07/26 14:03:11", "USUARIO": "<$_SESSION['nombre']>" }
```
Respuesta: `Datos[].Estado` — si `Estado != 0`, `CheckEmploye` dispara un ALTA contra `Servcio/Medico`.

### 1.5 `security/Servcio/Medico` — Alta / cambio del examen médico (el payload más grande)
`app::sendHeredoFamDats($nss, $type, $data)` — dos variantes según `$type`.

**ALTA** (payload mínimo, se dispara automáticamente si el NSS no tiene expediente):
```json
{
  "NNS": "<nss>",
  "FECHA": "20/07/26 14:03:11",
  "TIPO_REGISTROS": "ALTA",
  "TIPO_EMPLEADO": "<typeuser>",
  "USUARIO_CAMBIO": "<$_SESSION['nombre']>",
  "OTRAS": { "OTRAS": "", "OTRAS_OBS": "" }
}
```

**CAMBIO** — objeto con ~40 secciones. Estructura (valores omitidos por brevedad; cada campo `X` suele venir acompañado de `X_OBS`):
```json
{
  "NNS": "...", "FECHA": "...", "TIPO_REGISTROS": "CAMBIO", "USUARIO_CAMBIO": "...",

  "CARDIOPATIA":  { "CARDIOPATIA_ISQUEMICA","HIPERTENSION","HIPOTENSION","INSUFICIENCIA_CARDIACA","INSUFICIENCIA_VASCULAR_PERIFERICA" (+_OBS) },
  "ENDOCRINA":    { "ID","DM","HIPERTIROIDISMO","HIPOTIROIDISMO" (+_OBS) },
  "GENERALES":    { "CANCER","ALERGIAS","MALFORMACIONES" (+_OBS) },
  "MENTALES":     { "NEUROSIS","PSICOSIS","INTENTO_DE_SUICIDIO","ESQUIZOFRENIA" (+_OBS) },
  "NEFROPATIA":   { "IRC","LITASIS" (+_OBS) },
  "NEUMOPATICA":  { "BRONQUITIS","TUBERCULOSIS","ASMA","INSUFICIENCIA_RESPIRATORIA" (+_OBS) },
  "NEUROLOGIA":   { "AVC","EPILEPSIA","PARALISIS_FACIAL","PARKINSON","ALZHEIMER","MIGRANA" (+_OBS, salvo AVC) },
  "OBESIDAD":     { "TRANSTORNO_DE_CRECIMIENTO" (+_OBS) },
  "OTRAS":        { "OTRAS" (+_OBS) },
  "TOXICOLOGIOCAS": { "ALCOHOLISMO","TABAQUISMO","DROGAS" (+_OBS) },

  "SERV_MED_ANT_PATOLOGICOS":     { "HABITACION","NO_DORMITORIOS","NO_HABITANTES","PISO","TECHO","AGUA_POTABLE","LUZ","DRENAJE","CONVIVENCIA_CON_ANIMALES","AIRE_ACONDICIONADO","RELIGION","HIGIENE_PERSONAL","BANO","CAMBIO_DE_ROPA","ASEO_BUCAL","ALIMENTACION","NO_COMIDAS_AL_DIA","BALANCEADA","SUFICIENTE","DEPORTE","FC1","HOBBIES_PASATIEMPOS","FC2" },
  "SERV_MED_INMUNIZACIONES":      { "BLG","SABIN","DPT","HEPATITIS_B","SR","INFLUENZA","TD","view","COVID","FECHA" },
  "SERV_MED_ANT_GIN_OBSTETRICOS": { "MENARCA","RITMO","IVSA","FUM","G","A","P","C","MET_PLANIFICACION","PAPANICOLAOU","INFECC_REP","FUP" },
  "SERV_MED_ANT_PER_PATOLOGICOS": { "ENF_CONGENITAS","GENITAL","ENF_PROP_INF","DERMATOPATIAS","SIST_AUDITIVO","RESPIRATORIO","SIS_OLFATIVO","TRAUMATICOS" },
  "SERV_MED_OFTALMOLOGICO":       { "USO_LENTES","ASTIGMATISMO","PRESBICIA","ESTRABISMO","DALTONISMO","DESPENDIMIENTO_RET","MIOPIA" (+_OBS) },
  "SERV_MED_DIGESTIVO":           { "HERNIAS","CIRROSIS","COLITIS","GASTRITIS","HEMORROIDES","CIRUGIAS" (+_OBS) },
  "SERV_MED_RENAL":               { "INCONTINENSIA","INSUFICIENCIA","LITIASIS" (+_OBS) },
  "SERV_MED_SIST_NERVIOSO":       { "ANEURISMA","ENF_DEG_MUS_NEU","ACC_CER_VASCU","EPILEPSIA","PAR_FACIAL" (+_OBS) },
  "SERV_MED_MUS_ESQUELETICO":     { "ESCOLIOSIS","XIFOSIS","LORDOSISI","LUMBALGIA","BASCULAMIENTO","ALTERACION_DISCAL" (+_OBS) },
  "SERV_MED_CARDIO_NO_PATO":      { "ANEURISMA","HIPERTENSION","HIPOTENSION","INFARTOS ","INSUFIC_CAR" (+_OBS) },
  "SERV_MED_TOXI_NO_PATO":        { "TABAQUISMO","ALCOHOLISMO","DROGAS","ALT_CARGA","PERFORACIONES","TATUAJES","ALERGIAS" (+_OBS) },
  "SERV_MED_ENDOCRI_NO_PATO":     { "DIABETES","HIPERTIROIDISMO","HIPOTIROIDISMO" (+_OBS) },
  "SERV_MED_PADECI_ACTUAL":       { "OBSERVACIONES" },
  "SERV_MED_INT_APARATO_SIST":    { "OBSERVACIONES" },
  "SERV_MED_EXPLORACION_FISICA":  { "PESO","TALLA","IMC","FC","FR","TA","TEMP","ECTOMORFICO","MESOMORFO","ENDOMORFICA","SATISFACCION","COMP_TRABAJO","COMP_ORGANIZACION","TIEMPO","ESPACIO","PERSONA","SERENIDAD","INTERES","ANTAGONICO","ENTUSIASTA","ENOJO","HIPOCRITA","ICTERICA","ANEMICA","TIROIDEA","NOLMAL_FASCIES","PARKINSON","HEMIPLEJICO","ATAXICA","HEMIPARESIA","NORMAL_MARCHA","LENGUAJE","CONVER_FLUIDA","COHERENCIA_PALABLAS" },
  "SERV_MED_CRANEO":              { "OJOS","PUPILAS","CONJUNTIVAS","REFLEJOS","FONDO_OJO" },
  "SERV_MED_AGUDEZA_VISUAL":      { "OD","OIZ","COLORES" },
  "SERV_MED_NARIZ":               { "CAVIDAD_NASAL","MUCOSA","TABIQUE_NASAL","OLFATO" },
  "SERV_MED_COLUMNA_VERTEBRAL":   { "CERVICAL","DORSAL","LUMBAR" },
  "SERV_MED_BOCA":                { "LESIONES","ENCIAS","OROIFARINGE" },
  "SERV_MED_OIDOS":               { "CAE","MEM_TIM","AGUD_AUDIT","OD","OI" },
  "SERV_MED_TORAX":               { "RUIDOS_CARDIACOS","REG_PRECORDIAL","CAMPOS_PULM" },
  "SERV_MED_COLUMNA_VERTEBRAL_AUX": { "DEFORMACIONES","DOLOR","MOVIMIENTOS","MARCHA","LASSEGUE","PUNTA_TALON" },
  "SERV_MED_CUELLO":              { "DEFORMACIONES","TIROIDES","TRAQUEA" (+_OBS; los 3 flags van hardcodeados a 1) },
  "SERV_MED_ABDOMEN":             { "FORMA","VISCEROMEGALIAS","HERNIAS","DOLOR","PERISTALSIS" },
  "SERV_MED_GENITALES":           { "MASCULINO","FEMENINO" },
  "SERV_MED_URINARIO":            { "PUNTOS_URETRALES","FOSAS_RENALES" },
  "SERV_MED_EXTREMIDADES":        { "SUPERIOR","INFERIOR","IVP","EDEMA" },
  "SERV_MED_PIEL":                { "LUNARES","PIGMENTACION","CICATRICES_QUIRUR" },
  "SERV_MED_DIENTES":             { "DIENTE_11".."DIENTE_18", "DIENTE_2","DIENTE_22".."DIENTE_28", "DIENTE_31".."DIENTE_38", "DIENTE_41".."DIENTE_48" },
  "SERV_MED_ESTUDIOS_REALIZADOS": { "RESULTADOS" },
  "SERV_MED_DIAGNOSTICO":         { "OBSERVACIONES" },
  "SERV_MED_PLAN_TERAPIA":        { "OBSERVACIONES" },
  "SERV_MED_RESULTADO_EXAMEN":    { "OBSERVACIONES","APTO","NO_APTO","APTO_CONDICIONADO","APTO_RESTRINGIDO","FIRMA_DIGITAL" },
  "SERV_ANTECEDENTESLAB":         { "edad_inicio_laboral","cantidad_trabajos","pension","trabajos":[{ "nombre","giro","puesto","turno","antiguedad","salida","descripcion","riesgos","epp","observaciones" }] }
}
```

**Trampas a replicar/corregir en la migración**
- `SERV_MED_RESULTADO_EXAMEN`: `APTO`, `NO_APTO`, `APTO_CONDICIONADO`, `APTO_RESTRINGIDO` se envían **siempre vacíos** (`""`) — el dictamen nunca viaja.
- `SERV_ANTECEDENTESLAB` está **hardcodeado con datos de prueba** ("Trabajo 1..4", `edad_inicio_laboral: 20`, `pension: "true"`); no lee el formulario.
- `SERV_MED_CUELLO`: `DEFORMACIONES`/`TIROIDES`/`TRAQUEA` fijos en `1`.
- Typos en llaves que el WS espera literalmente: `"CIRUGIAS _OBS"` y `"INFARTOS "` (con espacio), `LITASIS` (no LITIASIS), `LORDOSISI`, `TOXICOLOGIOCAS`, `MIGRANA`, `BLG` (por BCG), `BANO`. Y la ruta misma: `Servcio` (no `Servicio`).

### 1.6 `security/Servcio/consulta` — Alta de consulta médica
`app::addConsultM($data)`
```json
{
  "FECHA": "20/07/26 14:03:11",
  "NSS": "<idNss>",
  "TIPO_CONSULTA": "...", "AREA_ACCIDENTE": "...", "AREA_ANATOMICA": "...", "CAUSA": "...",
  "PESO": "...", "TALLA": "...", "IMC": "...", "FC": "...", "FR": "...", "TA": "...", "TEMPERATURA": "...",
  "MOTIVO_CONSULTA": "...", "EXPLORACION_FISICA": "...", "DIAGNOSTICO": "...", "TRATAMIENTO": "...",
  "CONSULTA_RELACIONADA": "<idArchivoRel>",
  "ID_ARCHIVOS": "<idArchivoRel>",
  "USUARIO": "747849849",
  "NOMBRE_USUARIO": "<$_SESSION['nombre']>",
  "FIRMA_DIGITAL": "<FIRMA>"
}
```
> `USUARIO` está **hardcodeado** a `"747849849"`. `CONSULTA_RELACIONADA` e `ID_ARCHIVOS` reciben el mismo valor.

### 1.7 `security/Servcio/conuslta_medica_usuario` — Historial de consultas
`app::showConsults($nss)` (nótese el typo `conuslta` en la ruta)
```json
{ "NSS": "<nss>" }
```

### 1.8 `security/Servcio/incapacidades` — Alta de incapacidad
`app::addIncapacidad($data)`
```json
{
  "FECHA": "20/07/26 14:03:11",
  "NSS": "<idNss>",
  "FOLIO_INCAPACIDAD": "...", "RAMO": "...", "TIPO_INCAPACIDAD": "...",
  "FECHA_INICIO": "...", "FECHA_TERMINO": "...", "DIAS_AUTORIZADOS": "...",
  "SALARIO_INTEGRADO": "...", "COSTO": "...", "IMPUTABLE": "...", "ESTADO_DICTAMEN": "...",
  "GOCE_SUEDO": "...", "COMPLEMENTO_SALARIAL": "...",
  "URL_ARCHIVOS": "<idArchivoRel>",
  "USUARIO": "747849849",
  "NOMBRE_USUARIO": "<$_SESSION['nombre']>",
  "FIRMA_DIGITAL": "<FIRMA>",
  "RUBRO": "...", "FECHA_ALTA": "...", "FECHA_PRIMERA_INCAPACIDAD": "...",
  "ST2": "...", "SALARIO_ACUMULADO": "...", "ALTA": "..."
}
```
> `GOCE_SUEDO` (typo por SUELDO) es la llave literal esperada. `USUARIO` también hardcodeado.

### 1.9 `security/Servcio/consulta_incapacidad` — Lista de incapacidades del NSS
`app::showIncapacidades($nss)`
```json
{ "NSS": "<nss>" }
```

### 1.10 `security/Servcio/consulta_incapacidades_fecha` — Incapacidades por rango de fechas
`another::traerDatosGeneralIncap($fecha_inicial, $fecha_final)`
```json
{ "fecha_inicial": "<fecha>", "fecha_final": "<fecha>" }
```

### 1.11 `security/Servcio/indice` — Catálogo ICD (autocompletar diagnóstico)
`app::showICD($data)` · usado por `rest/icd/showIcd.php`
```json
{ "variable": "<texto de búsqueda>" }
```

### 1.12 `security/Servcio/consulta_cuestionario` — Pretest / checklist
`another::showPretest($nss)`
```json
{ "NSS": "<nss>" }
```

---

## 2. Permisos / menús — `rest/validateModules.php`

Encadena dos llamadas: primero resuelve el rol del usuario, luego pide sus menús.

### 2.1 `security/info/consulta_app_rol_usuario` — Rol del usuario
```json
{ "id_usuario": "<$_SESSION['nombre']>", "id_app": "13" }
```
Respuesta: se lee `Resultado.Datos[0].id_rol`.

### 2.2 `security/info/consulta_app_rol_menu` — Menús permitidos para el rol
```json
{ "id_app": 13, "id_rol": "<id_rol del paso anterior>" }
```

---

## 3. Módulo `gestion/` (admin de apps, roles y menús)

Consume un WS CRUD genérico (`crud_alta_app_menu`) parametrizado por `tipo_mov` × `servicio`.

### 3.1 `security/login/registro_app` — Login del admin
`gestion/controller/element/auth.php`
```json
{ "id_usuario": "<user>", "id_password": "<pass>", "id_app": 13 }
```

### 3.2 `security/login/menus` — Apps y menús del usuario
`element/usercard.php` (id_app 13) y `element/module.php` (id_app variable)
```json
{ "id_usuario": "<nss>", "id_password": "123", "id_app": 13 }
```
> `id_password` va **hardcodeado a `"123"`** en ambos: el endpoint se usa como consulta sin validar credencial.

### 3.3 `security/info/consulta_app_rol_usuario` — Roles del usuario en una app
`element/roles.php`
```json
{ "id_usuario": "<nssUserRol>", "id_app": "<idAppUserRol>" }
```

### 3.4 `security/login/registra_rol_usuario` — Asignar rol
`element/saveRol.php`
```json
{
  "id_usuario": "<nssUserRol>",
  "password": "<passInpRols1>",
  "app": "<idAppUserRol>",
  "roles": [ { "rol": "<SelectRol>", "activa": true } ]
}
```

### 3.5 `security/login/login_asigna_modulos` — Asignar módulos
`controller/saveDats/saveConfModules.php`
```json
{ "id_usuario": "<user>", "id_password": "<pass>", "id_app": 13 }
```
> **Bug en el original:** el archivo no hace `POST` (falta `CURLOPT_POST`) y `$user`/`$pass` nunca se definen — el payload se construye con nulls y ni siquiera se envía. Endpoint efectivamente muerto.

### 3.6 `security/info/apps_onest` — Catálogo de aplicaciones **(GET, sin body)**
`appGestion::contarApps()`, `appGestion::buscarIdAp()`, `element/altaMenu.php`
Sin `CURLOPT_POST` ni `POSTFIELDS`. `contarApps()` calcula `max(id_app)+1` en cliente para el siguiente alta.

### 3.7 `security/info/app_menu` — Menús de una app/rol
`appGestion::traerMenus($idApp, $idRol)`
```json
{ "id_app": "<idApp>", "id_rol": "<idRol>" }
```

### 3.8 `security/info/crud_alta_app_menu` — CRUD de apps, roles y menús
Un solo endpoint, 9 operaciones. Payload siempre con la misma forma:
```json
{
  "id_app": "...", "id_menu": "...", "nombre_menu": "...", "id_rol": "...",
  "tipo_mov": "ALTA|CAMBIO|BAJA",
  "servicio": "APP|ROL|MENU"
}
```

| Método (`appGestion`) | `tipo_mov` | `servicio` | Campos significativos |
|---|---|---|---|
| `altaApp`    | ALTA   | APP  | `nombre_menu`=nombre app; `id_menu`=1, `id_rol`=1 fijos |
| `cambioApp`  | CAMBIO | APP  | `nombre_menu`=nombre app; `id_menu`=1, `id_rol`=1 fijos |
| `bajaApp`    | BAJA   | APP  | solo `id_app`; resto relleno (`"1"`) |
| `altaRol`    | ALTA   | ROL  | `id_rol`, `nombre_menu`=nombre rol; `id_menu`=1 fijo |
| `editRol`    | CAMBIO | ROL  | `id_rol`, `nombre_menu`=nombre rol; `id_menu`=1 fijo |
| `bajaRol`    | BAJA   | ROL  | `id_rol`; `nombre_menu`="ADMIN" fijo |
| `altaMenu`   | ALTA   | MENU | `id_menu`, `nombre_menu`, `id_rol` |
| `editMenu`   | CAMBIO | MENU | `id_menu`, `nombre_menu`, `id_rol` |
| `bajaMenu`   | BAJA   | MENU | `id_menu`, `id_rol`; `nombre_menu`="menu" fijo |

---

## Resumen

**18 endpoints distintos** (`app/app.php` 12 · `validateModules.php` 2 · `gestion/` 8, con 2 compartidos con Portal Salud).

| # | Endpoint | Consumidor | Método |
|---|---|---|---|
| 1 | `security/login/registro_app` | portal + gestion | POST |
| 2 | `security/login/menus` | gestion | POST |
| 3 | `security/login/registra_rol_usuario` | gestion | POST |
| 4 | `security/login/login_asigna_modulos` | gestion (roto) | GET de facto |
| 5 | `security/Catalogo/usuario` | portal | POST |
| 6 | `security/Catalogo/Candidatos` | portal | POST |
| 7 | `security/Servcio/Medico` | portal | POST |
| 8 | `security/Servcio/consulta_examen` | portal | POST |
| 9 | `security/Servcio/consulta` | portal | POST |
| 10 | `security/Servcio/conuslta_medica_usuario` | portal | POST |
| 11 | `security/Servcio/incapacidades` | portal | POST |
| 12 | `security/Servcio/consulta_incapacidad` | portal | POST |
| 13 | `security/Servcio/consulta_incapacidades_fecha` | portal | POST |
| 14 | `security/Servcio/consulta_cuestionario` | portal | POST |
| 15 | `security/Servcio/indice` | portal | POST |
| 16 | `security/info/consulta_app_rol_usuario` | portal + gestion | POST |
| 17 | `security/info/consulta_app_rol_menu` | portal | POST |
| 18 | `security/info/apps_onest` | gestion | GET |
| 19 | `security/info/app_menu` | gestion | POST |
| 20 | `security/info/crud_alta_app_menu` | gestion | POST |

**Lo que NO pasa por webservice** (va directo a MySQL en `app/app.php`): tablas `files` (adjuntos por NSS y `type`: `laboratorio`, `examen_medico`, `nota_medica`, `nota_incapacidad`, `JSONCODE`), `tags` (borradores del examen por `nss`+`type`), `antecedentes`, `heredofamiliar`, `neurologia`.

**Archivos duplicados ignorados:** `php-old/app/1`, `php-old/1`, `php-old/12`, `php-old/pdf/1` son copias de respaldo de `app/app.php` y `pdf/pdfGenerator.php` con el mismo contenido.
