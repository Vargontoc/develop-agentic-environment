# Contrato `sdd-team`: módulos y técnicos en `.sdd/team.md`

Cómo el CLI lee y escribe los módulos de un proyecto. El formato de `team.md` es el actual y no cambia: lo edita el
usuario y lo amplía la creación de módulos.

## Quién lee y quién escribe

- **Escribe** solo el CLI: `create-module` (añade un módulo), `team-update` (reescribe las secciones de módulos y
  opciones) y `init` (crea la plantilla inicial). Siempre con copia de seguridad, conservando el fin de línea y sin tocar
  el resto del fichero. El usuario también puede editarlo a mano.
- **Lee** el CLI (módulos y técnicos en `status`, `tree`, `create-module`, `add-technician`; módulos y opciones en `team`)
  y, en solo lectura, un cliente como el visor, que puede abrir `.sdd/team.md`, `.sdd/project.md` y
  `.sdd/architecture.md` directamente del disco (ver la excepción en el contrato `cli-json`). Un cliente nunca debe
  escribir en `.sdd`; para editar el equipo usa `team-update`.

## Sección de módulos

El CLI solo interpreta la sección que empieza en la línea exacta `## Módulos` y termina en la siguiente línea que
empiece por `## ` (o al final del fichero). Lo demás del fichero no se lee ni se modifica.

Cada módulo es una línea de lista con tres campos separados por `|`:

```
- <nombre> | <identificador o -> | <rutas separadas por espacios o ->
```

| Campo | Regla |
|---|---|
| `nombre` | Obligatorio, sin espacios. Es el nombre del módulo. |
| `identificador` | Obligatorio; `-` significa «sin identificador». |
| `rutas` | Obligatorio; `-` significa «ninguna». Si no, rutas separadas por espacios. |

La línea que escribe `create-module` es `- <módulo> | - | -`. Ejemplo:

```
## Módulos

- facturacion | FAC | src/facturacion docs/facturacion
- alfa | - | -
```

Se ignoran las líneas vacías, los comentarios HTML (`<!-- ... -->`, también en varias líneas) y todo lo que no sea una
línea de módulo, que se anota como aviso.

## Sección de opciones

La sección que empieza en la línea exacta `## Opciones` contiene una lista de pares `clave: valor`. El CLI interpreta
estas claves; las demás se ignoran con un aviso. Valores por defecto entre paréntesis:

| Clave | Valores | Por defecto |
|---|---|---|
| `sprints` | `si`, `no` | `si` |
| `git-policy` | `balanced`, `strict` | `balanced` |
| `contract-strategy` | `implementation-first`, `contract-first` | `implementation-first` |
| `readme-owner` | nombre de un módulo de la sección de módulos | sin valor |

```md
## Opciones

- sprints: si
- git-policy: balanced
- contract-strategy: implementation-first
- readme-owner: facturacion
```

Las claves y los valores son sensibles a mayúsculas. Una clave o un valor no admitido, o un `readme-owner` que no sea un
módulo registrado, se anota como `unknown_option` y se usa el valor por defecto; el fichero no se modifica al leerlo.

## Lectura tolerante

Un fallo nunca impide devolver el resto de módulos. Cada problema se devuelve como aviso con su línea (desde 1; `0` si no
aplica):

| Aviso (`type`) | Cuándo | Efecto |
|---|---|---|
| `team_missing` | No existe `.sdd/team.md` o el proyecto no es un directorio. | Sin módulos. |
| `team_unreadable` | El fichero existe pero no se puede leer. | Sin módulos. |
| `section_missing` | No hay línea `## Módulos`. | Sin módulos. |
| `malformed_line` | Línea de la sección que no empieza por `-`, no tiene tres campos, o tiene campos vacíos o nombre con espacios. | La línea se ignora. |
| `duplicate_module` | Nombre ya registrado antes. | Se conserva el primero. |
| `unknown_option` | Clave o valor de `## Opciones` no admitido, o `readme-owner` que no es un módulo registrado. | Se usa el valor por defecto. |

Los avisos salen en `status` (`project.warnings`) y en `tree` (`warnings`).

## Escritura (`create-module`)

- Si el módulo ya está registrado (también por una línea mal formada con ese nombre) no se duplica: se informa como omitido.
- La línea nueva se añade tras la última línea de módulo de la sección; si no hay ninguna, tras la línea en blanco que
  sigue al encabezado, dejando otra en blanco antes de la siguiente sección.
- Se conservan los fines de línea del fichero (CRLF o LF) y se deja una copia de seguridad del contenido anterior.
- Sin `team.md` o sin la sección `## Módulos` no se crea nada de `team.md`: se avisa y se siguen generando los artefactos.
- Con `--dry-run` no se escribe.

## Escritura completa (`team-update`)

`team-update` recibe por `--stdin` el estado completo de módulos y opciones y reescribe solo las líneas de lista de
`## Módulos` y `## Opciones`:

- Conserva comentarios, líneas no-lista, el resto de secciones y el fin de línea (CRLF o LF).
- Si la sección no existe, la añade al final.
- Los módulos se normalizan y validan con las mismas reglas que `create-module`; no se permiten duplicados.
- `readme-owner` debe ser `null` o un módulo de la lista.
- `--expect-hash` compara con el `hash` que devolvió `team`; si no coincide, devuelve `conflict.team.modified_externally`
  y no escribe.
- Escribe con copia de seguridad. Con `--dry-run` devuelve `diff` y `hash` sin tocar el fichero.
- Quitar un módulo de la lista **no borra** sus artefactos: dejan de ser especialistas de la aplicación y el árbol los
  ignora; el usuario decide si limpia los ficheros.

## Técnicos y artefactos por módulo

Los técnicos de un módulo no están en `team.md`: se derivan de los artefactos instalados.

| Técnico | Router (único por proveedor) | Especialista (uno por módulo) |
|---|---|---|
| `analista` | `agents/analista-router.md` | `agents/analista-<módulo>.md` |
| `desarrollador` | `agents/desarrollador-router.md` | `agents/desarrollador-<módulo>.md` |
| `documentador` | `agents/documentador-router.md` | `agents/documentador-<módulo>.md` |

Reglas del árbol (`tree`):
- Un técnico aparece **existente** bajo un módulo cuando está instalado su especialista en algún proveedor, y
  **pendiente** (`pending: true`) cuando aún no se ha generado.
- Cada router aparece una sola vez bajo su proveedor, nunca repetido por módulo.
- Solo los módulos registrados en `team.md` tienen especialistas en el catálogo: un especialista de un módulo que no
  consta en `team.md` no es un artefacto de la aplicación.

## Nombre de módulo

Solo letras ASCII y guiones simples (nunca al inicio, al final ni seguidos), mínimo 3 caracteres, distinto de
`router` (reservado), normalizado a minúsculas. Errores: `validation.module.name_invalid_chars`,
`validation.module.name_invalid_hyphens`, `validation.module.name_too_short`, `validation.module.name_reserved`.

## `add-technician`

Exige que el módulo conste en `team.md` (`not_found.module` si no) y no modifica el fichero. Genera solo los agentes de
los técnicos pedidos; lo que ya existe se omite sin sobrescribir.
