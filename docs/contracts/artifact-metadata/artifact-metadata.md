# Contrato `artifact-metadata`: agrupación, cuerpo compartido y hashes

Almacén JSON que recuerda lo que la aplicación generó: a qué grupo lógico pertenece cada artefacto, el cuerpo
compartido de cada grupo y el hash de la última composición conocida. **No se edita a mano.** Versión de esquema: **1**.

## Ubicación

- Fichero `metadata.json` en el directorio de estado de la aplicación, nunca dentro de un proyecto ni de un proveedor.
  - Windows: `%LOCALAPPDATA%\agentic-develop` (si falta: `%USERPROFILE%\AppData\Local\agentic-develop`).
  - Linux: `$XDG_STATE_HOME/agentic-develop` (si falta o no es absoluta: `$HOME/.local/state/agentic-develop`).
- Las copias de seguridad de este fichero y de cualquier texto sustituido están en `backups/` dentro del mismo
  directorio, con el nombre `<aaaaMMdd-HHmmssSSS>-<resumen de la ruta>-<fichero>.bak` (UTC).
- Cada escritura es atómica y, si sustituye un `metadata.json` existente, deja su copia.

## Estructura

```json
{
  "schemaVersion": 1,
  "groups": {
    "<groupId>": { "id": "<groupId>", "scope": "global|project", "sharedBody": "<texto>" }
  },
  "artifacts": {
    "<ruta absoluta>": { "path": "<ruta absoluta>", "groupId": "<groupId>", "hash": "<sha-256 hex>", "state": "LINKED|DETACHED" }
  }
}
```

Los mapas van ordenados por clave para que el fichero sea determinista. No se admiten propiedades desconocidas.

### Grupos (`groups`)

| Campo | Descripción |
|---|---|
| `id` | Igual que la clave. |
| `scope` | `global` o `project`: un grupo nunca mezcla ámbitos. |
| `sharedBody` | Cuerpo compartido por todas las copias del grupo. En especialistas de proyecto conserva los placeholders `{module}`, `{slug-provider}` y `{slug-path-agents}` sin resolver; cada artefacto los resuelve con su módulo y proveedor. Vacío en skills y perfiles. |

Identificadores de grupo:

| Artefacto | `groupId` |
|---|---|
| Agente global | `global/agent/<nombre con _ → ->` (`sdd_agent` → `global/agent/sdd-agent`) |
| Skill | `<ámbito>/skill/<nombre>` |
| Perfil | `<ámbito>/profile/<nombre>` |
| Router de proyecto | `project/router/<técnico>` |
| Especialista de proyecto | `project/specialist/<técnico>` |
| Hook `sdd-specs-only` | `global/hook/sdd-specs-only` (fijo: el hook no es una entrada del catálogo) |

Un grupo abarca todos los proveedores que tengan ese artefacto y, para especialistas, todos los módulos. El grupo del
hook es siempre `global`, no tiene cuerpo compartido (`sharedBody` vacío) y abarca el hook de todos los proveedores.

### Artefactos (`artifacts`)

| Campo | Descripción |
|---|---|
| `path` | Ruta absoluta normalizada con `/`, igual que la clave. Es un fichero (agente, perfil, hook) o un directorio (skill). |
| `groupId` | Grupo al que pertenece; debe ser un identificador conocido. |
| `hash` | Hash de la última composición conocida (ver «Hashes»). |
| `state` | Estado de vinculación. |

## Estados de vinculación

| Estado | Significado | Efecto |
|---|---|---|
| `LINKED` | Vinculado al grupo. | La propagación lo actualiza. |
| `DETACHED` | Desvinculado por el usuario. | La propagación lo omite siempre. Puede volver al grupo con `adopt --confirm`, que sustituye su cuerpo por el compartido (con copia del anterior) y lo deja `LINKED`. |
| *(sin registro)* | **Legacy**: generado antes de existir los metadatos o con el almacén perdido. | Se describe como válido y **personalizado** (`legacy` en `describe`/`tree`); la propagación no lo sobrescribe y lo informa; solo se adopta con `adopt --confirm` (sin `--confirm` solo se muestra el diff). Al adoptar queda `LINKED` con el hash de lo escrito. |

Un artefacto con registro cuyo hash en disco difiere del registrado está **modificado externamente**: guardar o
propagar devuelve conflicto y no sobrescribe sin confirmación (se conserva copia).

## Hook `sdd-specs-only`

El hook que la aplicación instala en `hooks/` de la carpeta global de cada proveedor (ver `provider-layout`) es un
artefacto de **tipo fichero** con registro propio. No forma parte del catálogo de agentes, skills y perfiles.

- Cada hook registrado tiene `groupId` `global/hook/sdd-specs-only` y estado `LINKED`. El hook nunca se desvincula
  (`DETACHED`) ni participa en la propagación, en `adopt`, en `describe` ni en `tree`.
- El hash es el del fichero completo (ver «Hashes»). Se registra con lo que quedó en disco tras escribirlo, no con el
  contenido que se quiso escribir.
- Con registro, la aplicación compara el hash en disco con el registrado:
  - **Igual** (sin cambios del usuario): lo sustituye por la versión incorporada si difiere y actualiza el registro; si
    es igual no lo reescribe. Sin aviso.
  - **Distinto** (modificado por el usuario): no lo sobrescribe, no cambia el registro y avisa.
- **Sin registro** (sin entrada, almacén ausente, ilegible o de otra versión, o ruta registrada con otro grupo): no se
  adopta. No se sobrescribe, no se crea registro y se avisa en cada ejecución, aunque el contenido coincida con el
  incorporado. No existe `adopt` para el hook.
- Si el fichero no existe se instala y se registra, también cuando quedaba un registro antiguo de una instalación anterior.
- Los avisos viajan en `warnings` de `configure-provider` (ver `cli-json`); no cambian el código de salida.

## Hashes

- SHA-256 en hexadecimal minúscula del contenido lógico en UTF-8, con los fines de línea normalizados a `\n`: CRLF y LF
  dan el mismo hash.
- **Agentes y perfiles:** hash del fichero completo (cabecera y cuerpo).
- **Hook:** hash del fichero completo, con la misma regla (SHA-256 sobre UTF-8 con fines de línea normalizados).
- **Skills:** hash del directorio completo. Para cada fichero, su hash (texto UTF-8 normalizado, o SHA-256 de los bytes
  si no es texto UTF-8); luego el SHA-256 de las líneas `<ruta relativa con />\0<hash>\n` ordenadas por ruta.
  Cualquier fichero añadido, borrado o modificado dentro de la skill cambia el hash.
- Tras cada escritura de la aplicación el registro se actualiza con el hash de lo que quedó en disco.

## Versión de esquema y casos de error

La lectura nunca falla: ante cualquier problema el almacén se trata como **vacío** (todo artefacto pasa a legacy y no
se borra contenido) y se escribe un aviso en el log.

| Caso | Comportamiento |
|---|---|
| Almacén ausente | Vacío; se crea al primer registro. |
| Ilegible o JSON corrupto | Vacío (aviso en el log). La siguiente escritura lo sustituye dejando copia del anterior. |
| `schemaVersion` distinta de 1 o ausente | Vacío (aviso `unsupported schema version`). |
| Propiedad desconocida | Vacío. |
| Inconsistente: clave distinta de `id`/`path`, `scope`, `groupId`, `hash` o `state` ausentes | Vacío. |

Un cambio incompatible de esta estructura sube `schemaVersion`; no hay migración automática en v1.
