# Plan: activar las firmas digitalizadas en producción

Pendiente para el 22-oct-2026. El WAR con el módulo ya está en `main` (commit `5269e35`).
Sin estos pasos el portal funciona normal: los formatos salen con la línea para firmar a mano.

## Antes de empezar

1. Desplegar en producción el WAR nuevo (`target/portal-salud.war` del commit `5269e35`).
2. En la base del portal deben existir las tablas del módulo (ya se corrieron el 20-oct):
   `docs/portal-aviso-privacidad-firma.sql` y `docs/portal-firmas-digitalizadas.sql`.

## Llave de cifrado (Tomcat como servicio, systemd)

Todo en el servidor de producción, con un usuario con `sudo`.

1. Ver el nombre del servicio (abajo se usa `tomcat`; cambiarlo si es otro):

   ```
   systemctl list-units --type=service | grep -i tomcat
   ```

2. Generar la llave en un archivo que solo lea root:

   ```
   sudo mkdir -p /etc/portal-salud
   echo "PORTAL_FIRMAS_LLAVE=$(openssl rand -base64 32)" | sudo tee /etc/portal-salud/firmas.env > /dev/null
   sudo chmod 600 /etc/portal-salud/firmas.env
   sudo chown root:root /etc/portal-salud/firmas.env
   ```

3. Respaldar la llave en el gestor de contraseñas de la organización (si se pierde, cada quien
   vuelve a registrar su firma). Nunca por correo, chat, repo, WAR ni `.properties`:

   ```
   sudo cat /etc/portal-salud/firmas.env
   ```

4. Decirle al servicio que lea el archivo (crea un `override.conf`, no toca el servicio original):

   ```
   sudo systemctl edit tomcat
   ```

   Escribir entre los comentarios, guardar y cerrar:

   ```
   [Service]
   EnvironmentFile=/etc/portal-salud/firmas.env
   ```

   El archivo del paso 2 debe existir antes; si falta, systemd no arranca Tomcat.
   Con `EnvironmentFile=-/etc/portal-salud/firmas.env` (con guion) arranca aunque falte, con las firmas apagadas.

5. Reiniciar y comprobar en el log del portal:

   ```
   sudo systemctl daemon-reload
   sudo systemctl restart tomcat
   ```

   Debe aparecer: `[firmas] llave de cifrado cargada desde PORTAL_FIRMAS_LLAVE: firmas digitalizadas disponibles`.
   Si aparece `sin llave valida ... quedan APAGADAS`, la variable no llegó o está mal copiada.

**La llave no se cambia después**: las firmas guardadas dejarían de leerse.

## Cargar la firma del Dr. Cerón

1. Un administrador entra a **Administración › Firmas digitalizadas** y carga
   `C:\miguel\firma-dr\firma-dr-ceron-original.png` para el usuario `68958027838`. Queda **pendiente**.
2. El Dr. entra al portal, ve el botón **«Activa tu firma»**, lee el aviso y lo acepta.
   Recomendado: que registre una firma nueva (la anterior sigue en el historial de GitHub).
3. Probar: imprimir un formato y verificar su folio en **Verificar folio**.

## Pruebas que faltan (antes de producción)

Lo probado el 20 y 21-oct (Playwright en el Tomcat local, con datos reales y limpieza por `.sql`): los 7 formatos B,
las respuestas de SM, y del módulo de firmas: registrar con imagen, cifrado en disco, folio y verificación, revocar,
carga por administrador con aceptación, sin llave y sin tablas.

Falta probar:

1. **En QA, detrás de nginx**: las pantallas nuevas redirigen (`/perfil/firma`, `/admin/firmas`, FT-SO-31) y en septiembre
   hubo errores de ruta solo en QA. Es lo más importante.
2. Registrar la firma **dibujándola** (solo se probó subiendo imagen).
3. **Dos personas distintas**: un documento elaborado por una enfermera con su firma y la del Dr. (hasta hoy el Dr. fue ambas).
4. **Firmante dado de baja** (EMP_STATUS distinto de 0) y la revisión diaria que apaga su firma.
5. **Aviso nuevo**: publicar la versión 2 y ver que la firma deja de salir hasta volver a aceptar.
6. Desactivar una firma desde Administración.
7. Firmas en nota de consulta, incapacidad, pretest, FT-SO-22, FT-SO-25, FT-SO-02 y FT-SO-23 (se probaron examen, FT-SO-14 y FT-SO-29).
8. Roles: poner `portal.firmas.roles` con un rol y ver que los demás imprimen sin firma.
9. ~~Paquete para QA~~ **HECHO 21-oct**: `C:\miguel\entrega-qa-formatos-b-21oct.zip` (prueba automática, reporte,
   formatos 2026 y evidencia; firmas difuminadas a propósito). Decisión del usuario 21-oct: los puntos 2 y 4 se
   validan con el uso; 3, 5 y 6 los prueba él en producción.

## Desarrollo que falta

1. ~~«Elaboró» del examen médico y del pretest~~ **HECHO 21-oct (sin commit)**: se fija al guardar el examen médico, al
   finalizar el examen inicial y al guardar el pretest. Los documentos anteriores siguen fijándose en su primera impresión.
2. Marca tenue sobre la firma (opcional; se bajó de prioridad).
3. Artículos del centro de ayuda para «Mi firma digitalizada», «Verificar folio» y los formatos nuevos (opcional).

## Otros pendientes

- Correr la limpieza de las pruebas del 20 y 21-oct: `docs/ords-borrar-consulta-prueba-21oct.sql` (ORDS) y
  `docs/portal-borrar-prueba-firmas.sql` (portal). Luego borrar los `.enc` de prueba del disco local.
- Regresar a Apto el examen del NSS 30048315698.
- Texto final del aviso de privacidad (legal o RH): se publica como versión nueva con un INSERT.
- Firma vieja del Dr. en el historial de GitHub: quitarla reescribe la historia del repo (solo con permiso).
