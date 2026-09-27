# Incidente 27-sep-2026 — Cuenta BIOMETRICO bloqueada en producción

**Severidad:** alta — se bloqueó la cuenta dueña del esquema del servicio médico en producción.
**Duración:** desde las 14:03 hasta el desbloqueo manual.
**Causa raíz:** dos configuraciones actuando juntas, ninguna de ellas del código de negocio.

---

## Qué pasó

Se intentó arrancar el WAR del Portal Salud **desde el equipo local, apuntando a
`BIOMETRICO@PDBPRD` (producción)**, para comprobar que el esquema recién creado validaba
contra las entidades JPA.

El arranque falló con credencial inválida y, en el segundo intento, la cuenta ya estaba
bloqueada.

```
14:03:39  ORA-01017: invalid username/password; logon denied
14:04:36  ORA-28000: The account is locked.
```

---

## Causa raíz

### 1. La contraseña nunca se llenó

El archivo `setenv.bat` del Tomcat local se preparó con un texto de relleno:

```bat
set "SPRING_DATASOURCE_PASSWORD=<PONER LA CONTRASENA>"
```

Ese texto se quedó tal cual, y Tomcat lo mandó a Oracle **como si fuera la contraseña** de
`BIOMETRICO`. De ahí el `ORA-01017`.

**Falla de diseño:** un marcador de posición que el sistema puede usar como valor válido. El
arranque debió detenerse antes de intentar conectarse, no intentarlo con un placeholder.

### 2. El pool de conexiones no estaba configurado — esto es lo que causó el bloqueo

Un error de contraseña, por sí solo, no bloquea nada. Lo que lo convirtió en bloqueo fueron
los valores por defecto de HikariCP, que el proyecto nunca había configurado:

| Parámetro | Valor por defecto | Efecto |
|---|---:|---|
| `maximum-pool-size` | 10 | Tamaño máximo del pool |
| `minimum-idle` | **igual al máximo → 10** | **Conexiones que abre al arrancar** |

Hikari **precarga el pool al desplegar**. Con la credencial mala, esas diez aperturas son
**diez intentos de login fallidos**. Y el perfil `DEFAULT` de Oracle bloquea la cuenta
exactamente a los **diez intentos**.

```
1 arranque  ×  10 conexiones de precarga  =  10 logins fallidos  =  cuenta bloqueada
```

**No hicieron falta muchos reintentos: bastó un solo arranque.** El redespliegue automático
de Tomcat sólo lo remató.

---

## Impacto

`BIOMETRICO` es la cuenta **dueña de las 84 tablas** del servicio médico: las 53 del
expediente clínico del PHP legacy, las 11 que este proyecto creó por ORDS y las 20 del portal.

El alcance real dependió de con qué usuario se conecten ORDS y el PHP:

- Si se conectan **como `BIOMETRICO`** → ambos quedaron fuera de servicio mientras duró el
  bloqueo.
- Si se conectan con **otro usuario** → el impacto se limitó a nuestra prueba.

> Pendiente de confirmar y dejar documentado.

---

## Corrección aplicada

### En la aplicación — `application.properties`

```properties
spring.datasource.hikari.minimum-idle=0                 # el pool arranca VACÍO
spring.datasource.hikari.initialization-fail-timeout=1  # prueba UNA vez y aborta
spring.datasource.hikari.connection-timeout=5000        # 5 s en vez de 30
```

Un arranque fallido pasa de **hasta diez intentos de login a uno**. Va en el properties base
para que aplique a local, test y producción por igual.

### En el arranque — `setenv.bat`

Se agregó un freno que corta con `exit /b 1` si la contraseña sigue con el texto de relleno:

```
[setenv] ALTO: SPRING_DATASOURCE_PASSWORD sigue con el texto de relleno.
```

---

## Qué falta

**1. Desbloquear la cuenta** — requiere privilegio de DBA:

```sql
ALTER USER BIOMETRICO ACCOUNT UNLOCK;

SELECT username, account_status, lock_date FROM dba_users WHERE username = 'BIOMETRICO';

-- A los cuántos intentos bloquea, y si se libera sola:
SELECT p.resource_name, p.limit
  FROM dba_users u JOIN dba_profiles p ON p.profile = u.profile
 WHERE u.username = 'BIOMETRICO'
   AND p.resource_name IN ('FAILED_LOGIN_ATTEMPTS', 'PASSWORD_LOCK_TIME');
```

**2. Recompilar el WAR.** El artefacto que existe hoy **no lleva la corrección de Hikari**:
se compiló antes. Si se vuelve a probar con ese WAR, un error de contraseña vuelve a gastar
diez intentos.

**3. `autoDeploy="false"` en el Tomcat de producción** (`conf/server.xml`), para que una
aplicación que falló no se reintente sola.

---

## La lección de fondo

El incidente vuelve a apuntar a una decisión que ya estaba sobre la mesa desde el 23-sep y
que sigue abierta:

> **El WAR no debería conectarse como `BIOMETRICO`**, que es el dueño del esquema y además
> tiene `DROP ANY VIEW`, `CREATE ROLE` y `EXP/IMP_FULL_DATABASE`.

Con un usuario propio de bajo privilegio —`CREATE SESSION` más DML sobre las 20 tablas del
portal— este mismo error de dedo habría bloqueado **una cuenta de aplicación**: molesto y
reversible en un minuto. En lugar de eso bloqueó la cuenta de la que depende todo el servicio
médico.

Hoy `SPRING_DATASOURCE_USERNAME` sigue sin definirse en producción, así que **todavía se está
a tiempo** de pedir ese usuario al DBA. Conviene aprovechar la misma conversación del
desbloqueo.

---

## Procedimiento correcto para probar credenciales

El error de método fue probar una credencial **arrancando la aplicación**. Una aplicación
reintenta, precarga pools y redespliega: es la peor herramienta posible para verificar un
usuario y contraseña.

1. Conectar **una sola vez** en SQL Developer con la credencial.
2. Si entra, copiarla a la configuración.
3. Recién entonces arrancar el Tomcat.

Nunca al revés.
