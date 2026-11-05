# Contrato `sdd-team`: módulos y técnicos en `.sdd/team.md`

Cómo el CLI lee y escribe los módulos de un proyecto. El formato de `team.md` es el actual y no cambia: lo edita el
usuario y lo amplía la creación de módulos.

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

Los avisos salen en `status` (`project.warnings`) y en `tree` (`warnings`).

## Escritura (`create-module`)

- Si el módulo ya está registrado (también por una línea mal formada con ese nombre) no se duplica: se informa como omitido.
- La línea nueva se añade tras la última línea de módulo de la sección; si no hay ninguna, tras la línea en blanco que
  sigue al encabezado, dejando otra en blanco antes de la siguiente sección.
- Se conservan los fines de línea del fichero (CRLF o LF) y se deja una copia de seguridad del contenido anterior.
- Sin `team.md` o sin la sección `## Módulos` no se crea nada de `team.md`: se avisa y se siguen generando los artefactos.
- Con `--dry-run` no se escribe.

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
