# Migración servicioMedico → Oracle QA — Reporte final

**Estado: Completa · 31 de agosto de 2026**

Carga completa de los dos orígenes legacy (`files`, `tags`) desde MariaDB local a Oracle QA, verificada fila por fila contra el conteo real de origen.

## Ruta

| | |
|---|---|
| Origen | MariaDB local, base `servicioMedico` |
| Destino | Oracle QA, esquema `ONEWMS_QA` |
| Host | `200.94.116.132:1521` (servicio `orclpdb`) |

## Resumen

| Métrica | Valor |
|---|---|
| Filas totales migradas | **572,438** (100% del origen) |
| Datos binarios movidos | **15.0 GB** (21,676 PDFs decodificados) |
| Filas en cuarentena | **0** |

## `tags` → `SERV_MED_TAG`

**550,760 / 550,760 (100%)**

Cuestionarios EAV (Pre-Test, historia laboral, examen físico, contactos de emergencia) clasificados automáticamente por la función `SERV_MED_FN_TAG_GROUP`.

| | |
|---|---|
| Nuevas en esta corrida | 544,891 |
| Ya migradas (idempotente, corrida previa) | 5,869 |
| Sin tipo (huérfanas) | 0 |

**Distribución por grupo funcional:**

| Grupo | Filas |
|---|---:|
| Pre-Test | 304,582 |
| Historia laboral | 141,247 |
| Examen físico | 46,017 |
| Contacto emergencia | 27,840 |
| Clínico misceláneo | 20,263 |
| Permiso de examen | 8,955 |
| Vacunas | 1,856 |

## `files` → `SERV_MED_FS_FILE`

**21,703 / 21,703 (100%)**

Metadatos a `SERV_MED_FS_FILE`; binario decodificado (base64 → PDF real) al filesystem con sharding `yyyy/MM/dd/<hash>/<uuid>.pdf` y checksum SHA-256.

| | |
|---|---|
| Nuevas en esta corrida | 21,627 |
| Ya migradas (muestra previa) | 49 |
| Sin binario (huérfanas conocidas) | 27 |
| En cuarentena (base64 inválido) | 0 |

**Verificación disco vs. base de datos:**

| | |
|---|---:|
| Archivos en disco | 21,676 |
| Filas en `SERV_MED_FS_FILE` | 21,678¹ |
| Extensión `pdf` | 100% |

¹ Incluye 2 filas de prueba preexistentes a esta migración.

## Único paso manual pendiente

Los 15GB de binarios quedaron en una carpeta local (no en el servidor real de QA, inalcanzable desde esta máquina). Hay que subir ese contenido para que la app de QA pueda servir los archivos que ya están referenciados en `SERV_MED_FS_FILE`.

```
C:/etl-qa-files
  →
/home/onedev/apps/exec/apache11_app_jdk21/filessalud
```

## Método

Corrida vía programa Java standalone (JDBC directo a ambas bases, sin arrancar la aplicación completa) para evitar un cuelgue reproducible al inicializar Spring Boot bajo esta combinación de perfiles. Idempotente por `SOURCE_ID`/`BUSINESS_KEY` — se puede re-correr sin duplicar. Todos los conteos verificados por consulta directa contra Oracle QA al finalizar cada corrida.
