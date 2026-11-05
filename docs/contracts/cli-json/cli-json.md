# Contrato `cli-json`: envelope, comandos y errores

Versión del esquema: **1** (`schemaVersion`). Un cambio incompatible de cualquier estructura de este documento sube
ese número. Añadir un campo nuevo a un `data` o un comando nuevo no es incompatible.

El CLI sin argumentos abre el menú interactivo. Con argumentos es no interactivo: no muestra menús, no pide nada por
consola y solo lee la entrada estándar si se le pasa `--stdin`.

## Salida y logs

- La **salida estándar** recibe exactamente **un documento JSON en una línea** (el envelope), UTF-8.
- La **salida de error** recibe los logs y las trazas. Nunca van en el envelope.
- El proceso termina con el código de salida de la tabla siguiente.

## Envelope

```json
{"schemaVersion":1,"command":"init","ok":true,"data":{...},"errors":[]}
```

| Campo | Tipo | Descripción |
|---|---|---|
| `schemaVersion` | entero | Versión del contrato. |
| `command` | texto | Comando ejecutado; el texto escrito por el usuario si no existe; `help` y `version` para las ayudas. |
| `ok` | booleano | `true` si el comando terminó bien. |
| `data` | objeto \| `null` | Resultado del comando; `null` si hubo error. |
| `errors` | lista | Vacía si `ok`; con **un** elemento si no. |

Cada elemento de `errors`:

| Campo | Tipo | Descripción |
|---|---|---|
| `code` | texto | Código estable (ver «Errores»). |
| `path` | texto \| `null` | Ruta o entrada afectada, tal como la escribió el usuario; `null` si no aplica. |
| `message` | texto | Mensaje comprensible, sin trazas. |

Convenciones de `data`: las rutas devueltas por la aplicación van como texto con `/`, también en Windows; las
enumeraciones van en minúsculas; los campos sin valor aparecen como `null`.

## Códigos de salida

| Código | Significado | Cuándo |
|---|---|---|
| 0 | Correcto | `ok = true`. |
| 1 | Error de dominio | Validación, no encontrado o conflicto (códigos `validation.*`, `not_found.*`, `conflict.*`). |
| 2 | Uso incorrecto | Códigos `usage.*`. No se modifica nada. |
| 3 | Error interno | Códigos `internal.*`. |

## Sintaxis

```
<comando> [--opción valor | --opción=valor | --indicador]
```

- `--help` o `-h` (solo, o junto a un comando) devuelve la ayuda en `data.commands`.
- `--version` devuelve `{"version": "..."}`.
- Una opción desconocida, repetida, sin valor, un argumento sobrante o una opción obligatoria ausente es un error de uso.
- `--project <ruta>` admite ruta relativa; el CLI la resuelve contra el directorio actual.

## Comandos

### `help` (`--help`, `-h`)

`data`: `{"commands":[{"name","description","options":[{"name","flag","required","description"}]}],"globalOptions":[{"name","description"}]}`.
Con un comando, `commands` solo contiene ese comando.

### `version` (`--version`)

`data`: `{"version": texto}`.

### `init`

Crea la estructura `.sdd` del proyecto. Idempotente: si ya existe no cambia nada.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--project` | sí | Ruta del proyecto. |

`data`: `{"path", "created", "alreadyConfigured"}`. Errores: `not_found.project.path`, `validation.project.not_directory`,
`validation.project.path_invalid`.

### `status`

Solo lee. Opción: `--project` (no obligatoria).

`data`: `{"providers":[{"id","name","state","missing":[ruta relativa]}],"project":{"path","initialized","modules":[texto],"warnings":[{"type","line"}]}|null}`.
`state`: `not_installed`, `unconfigured`, `partial`, `configured`. `warnings[].type` (en minúsculas): `team_missing`,
`team_unreadable`, `section_missing`, `malformed_line`, `duplicate_module`.

### `tree`

Solo lee. Opción: `--project` (no obligatoria).

`data`: `{"global":[Nodo],"project":[Nodo],"warnings":[{"type","line"}],"problems":[{"path","code"}]}`.
`Nodo`: `{"type","name","path"|null,"provider"|null,"status"|null,"pending","children":[Nodo]}`.
`type`: `provider`, `agent`, `skill`, `profile`, `sdd`, `module`, `technician`. `status`: `linked`, `detached`, `legacy`.
Cada router aparece una sola vez bajo su proveedor. El nodo `sdd` lleva `pending: true` si el proyecto aún no tiene `.sdd`; un técnico
lleva `pending: true` si ningún proveedor tiene su especialista, y sus `children` son los especialistas instalados.

### `describe`

Solo lee.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--path` | sí | Ruta del agente o de la skill (o de un fichero de la skill). |
| `--project` | no | Proyecto, necesario para artefactos de proyecto. |

`data`: `{"provider","scope","kind","name","path","format"|null,"module"|null,"header","body","sharedBody"|null,"hash","status","modifiedExternally","files":[ruta relativa]}`.
`scope`: `global`, `project`. `kind`: `agent`, `skill`, `profile`. `format`: `markdown`, `codex_toml` o `null` (skills y
perfiles). En una skill, `files` lista `SKILL.md` y sus ficheros de apoyo. `sharedBody` es `null` si el artefacto no tiene
registro. En un especialista de proyecto, `sharedBody` conserva los placeholders sin resolver.

Nota para consumidores: `body` y `sharedBody` pueden superar 1.000.000 de caracteres (ficheros de 1 MB). El lector JSON
debe permitir cadenas de ese tamaño (en Jackson, `StreamReadConstraints.maxStringLength`).

### `configure-provider`

Instala los agentes, skills y perfiles globales del proveedor. No sobrescribe. Idempotente.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--provider` | sí | `claude`, `codex`, `gemini` u `opencode`. |

`data`: `{"provider","path","created":[ruta relativa al proveedor],"skipped":[...],"warnings":[texto]}`.
Errores: `validation.provider.unknown`, `internal.env`.

### `create-module`

Registra el módulo en `.sdd/team.md` y genera sus agentes y skills. No sobrescribe. Idempotente.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--project` | sí | Ruta del proyecto. |
| `--module` | sí | Nombre: solo letras y guiones simples, mínimo 3, distinto de `router`. Se normaliza a minúsculas. |
| `--technicians` | sí | Lista separada por comas: `analista`, `desarrollador`, `documentador`. |
| `--providers` | sí | Lista separada por comas de proveedores. Los que no admiten proyecto dan un aviso. |
| `--dry-run` | no | Devuelve lo previsto sin escribir nada. |

`data`: `{"module","dryRun","created":[texto],"skipped":[texto],"warnings":[texto]}`. Con `--dry-run`, las skills se
listan una a una (`Claude: skills/<nombre>`).

### `add-technician`

Genera solo los agentes de los técnicos indicados en un módulo que ya consta en `.sdd/team.md`; no toca los demás
ni `team.md`. Lo que ya existe se omite. Idempotente.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--project` | sí | Ruta del proyecto. |
| `--module` | sí | Módulo existente en `.sdd/team.md`. |
| `--technicians` | sí | Lista separada por comas: `analista`, `desarrollador`, `documentador`. |
| `--providers` | sí | Lista separada por comas de proveedores. |
| `--dry-run` | no | Devuelve lo previsto sin escribir nada. |

`data`: la misma forma que `create-module`. Error propio: `not_found.module`.

### `create-artifact`

Crea un agente o una skill del catálogo a partir de su plantilla.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--provider` | sí | Proveedor. |
| `--scope` | sí | `global` o `project`. |
| `--kind` | sí | `agent` o `skill`. |
| `--name` | sí | Nombre del artefacto en el catálogo. |
| `--project` | no | Necesaria con `--scope project`. |
| `--content-file` / `--stdin` | no | Contenido; excluyentes. Sin ninguno, la plantilla tal cual. En un agente es el cuerpo; en una skill, su `SKILL.md`. |
| `--overwrite` | no | Sobrescribe un artefacto existente (se conserva copia). |

`data`: `{"provider","scope","kind","name","path","replaced","custom","backup"|null}`. Errores: `conflict.artifact.already_exists`,
`validation.artifact.not_catalogued`, `validation.artifact.scope_unsupported`, `validation.module.no_project`.

### `save`

Guarda el contenido de un artefacto o lo propaga a su grupo.

| Opción | Obligatoria | Descripción |
|---|---|---|
| `--path` | sí | Ruta del agente o de la skill. |
| `--content-file` / `--stdin` | sí (una) | Contenido nuevo; excluyentes. |
| `--mode` | no | `instance` (por defecto) o `group`. |
| `--file` | no | Solo skills: fichero de la skill que se guarda (por defecto `SKILL.md`). |
| `--project` | no | Proyecto, para artefactos de proyecto. |
| `--overwrite` | no | Sobrescribe aunque haya cambios externos (se conserva copia). |

`data`: `{"mode","path","groupId"|null,"written","tracked","groupUpdated","diff","backups":[ruta],"members":[Miembro]}`.
`Miembro`: `{"path","provider"|null,"module"|null,"outcome","diff","backups":[ruta]}`.
`outcome`: `updated`, `unchanged`, `conflict`, `detached`, `custom`, `missing`, `out_of_scope`, `failed`.
En `instance`, `members` está vacío y `diff` lleva el cambio (`-`/`+` por línea; en skills, `@@ <fichero>` antes de cada
fichero). En `group`, `written` indica si algún miembro cambió y cada miembro lleva su `diff`. La propagación nunca
crea artefactos ausentes ni cruza de ámbito; los miembros en conflicto se omiten sin error global. Un guardado sin
cambios no escribe nada.
Errores: `conflict.artifact.modified_externally` (solo en `instance`), `not_found.group` (`group` sobre un artefacto sin
registro), `validation.artifact.not_catalogued`, `validation.content.unreadable`.

## Errores

### De uso (código de salida 2)

| Código | Cuándo |
|---|---|
| `usage.unknown_command` | Comando inexistente. |
| `usage.unknown_option` | Opción no declarada por el comando. |
| `usage.missing_option` | Falta una opción obligatoria (incluye «contenido» en `save`). |
| `usage.missing_value` | Opción sin valor. |
| `usage.duplicate_option` | Opción repetida. |
| `usage.unexpected_argument` | Argumento sobrante o combinación no permitida (`--stdin` con `--content-file`). |
| `usage.invalid_value` | Valor fuera de los admitidos (`--mode`, `--scope`, `--kind`, técnicos). |

### De dominio e internos

| Código | Categoría | Salida |
|---|---|---|
| `validation.project.path_invalid` | validación | 1 |
| `not_found.project.path` | no encontrado | 1 |
| `validation.project.not_directory` | validación | 1 |
| `validation.module.name_invalid_chars` | validación | 1 |
| `validation.module.name_invalid_hyphens` | validación | 1 |
| `validation.module.name_too_short` | validación | 1 |
| `validation.module.name_reserved` | validación | 1 |
| `not_found.module` | no encontrado | 1 |
| `validation.module.no_project` | validación | 1 |
| `not_found.artifact.path` | no encontrado | 1 |
| `validation.artifact.not_editable` | validación | 1 |
| `validation.artifact.not_catalogued` | validación | 1 |
| `conflict.artifact.already_exists` | conflicto | 1 |
| `validation.artifact.scope_unsupported` | validación | 1 |
| `validation.artifact.kind_mismatch` | validación | 1 |
| `validation.provider.unknown` | validación | 1 |
| `validation.path.out_of_scope` | validación | 1 |
| `validation.artifact.format_invalid` | validación | 1 |
| `conflict.artifact.modified_externally` | conflicto | 1 |
| `validation.content.unreadable` | validación | 1 |
| `not_found.group` | no encontrado | 1 |
| `conflict.artifact.already_tracked` | conflicto | 1 |
| `internal.env` | interno | 3 |
| `internal.project.sdd_resource` | interno | 3 |
| `internal.project.copy` | interno | 3 |
| `internal.module.skills_resource` | interno | 3 |
| `internal.module.write` | interno | 3 |
| `internal.resource.read` | interno | 3 |
| `internal.resource.header_marker` | interno | 3 |
| `internal.text.read` | interno | 3 |
| `internal.text.write` | interno | 3 |
| `internal.unexpected` | interno | 3 |

`internal.unexpected` es cualquier fallo no previsto: el envelope lleva solo el mensaje genérico
«Error interno inesperado» y la traza va al log (salida de error).

## Propiedades

- **Idempotencia:** `init`, `configure-provider`, `create-module`, `add-technician` y `create-artifact` (sin
  `--overwrite`) no sobrescriben nada al repetirse; lo existente se informa como omitido o como conflicto.
- **Atomicidad y copias:** toda escritura de texto es atómica y, si sustituye contenido, deja una copia en el
  directorio de copias de la aplicación.
- **Entrada:** solo `save --stdin` y `create-artifact --stdin` leen la entrada estándar, y solo cuando se piden.
