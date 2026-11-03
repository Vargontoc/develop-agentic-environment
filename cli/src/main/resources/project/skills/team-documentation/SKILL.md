---
name: team-documentation
description: Guía para formalizar contratos de comunicación en docs/contracts/<protocolo>/<layer>/<modulo>/ y mantener las líneas de un módulo en .sdd/architecture.md. Úsala solo en el documentador especialista.
---

# Documentación técnica (contratos y mapa)

Describes lo que el módulo hace realmente, no lo que se diseñó: un contrato refleja el código implementado y el mapa refleja lo que existe. No escribes código.

## Antes de escribir

1. Aplica la comprobación previa de `team-conventions`.
2. Lee `specs/<feature>/tasks.md` (`Estado: aprobado`), `design.md` (secciones "Interfaces entre componentes", "Contratos afectados" y "Cambios sugeridos al mapa"), los `REQ-xxx` que tus tareas cubren, `.sdd/architecture.md` si existe y tu línea de `.sdd/team.md`.
3. Del código lee solo tu carpeta de trabajo y solo lo necesario para describir la interfaz: controladores, manejadores, tipos de mensaje, DTOs. Aplica "Búsqueda de ficheros" de `sdd-project-context`.
4. Lee lo que ya exista en tu carpeta de salida y, si el protocolo ya tiene contratos en el proyecto, su formato.
5. Dudas bloqueantes (por ejemplo, el código y el diseño se contradicen): no escribas y responde `PREGUNTAS` (máximo 5, cada una con propuesta).
6. Si el título de la tarea empieza por el Prefijo de un tipo de `.sdd/task-types.md` (léelo con Read y su ruta exacta), sigue la skill indicada en su campo Skill en lugar de esta guía.

## Modo contratos

Por cada tarea `Contrato:` que el router te asigne:

1. La tarea de la que depende (`Depende de`) debe estar `### [x]`. Si no, no escribas nada para ella: responde `PARCIAL` con ese motivo.
2. Carpeta de salida: `docs/contracts/<protocolo>/<layer>/<modulo>/` (créala escribiendo el fichero directamente con Write).
3. Formato por defecto:

| Protocolo | Formato | Fichero |
|---|---|---|
| `openapi` | OpenAPI 3.x en YAML | `openapi.yaml` |
| `websocket` | Markdown: canales, mensajes (dirección, payload, ejemplo) y errores | `websocket.md` |
| otro | Markdown: participantes, mensajes (dirección, payload, ejemplo) y errores | `<protocolo>.md` |

   Si el proyecto ya tiene contratos de ese protocolo en otro formato, sigue ese.
4. Describe solo lo que existe en el código y lo que piden los requisitos. Incluye los errores que exigen los requisitos y los que el código devuelve de verdad. Ejemplos mínimos. No inventes nada. Añade al principio un comentario: `Origen: specs/<feature> (REQ-xxx)`.
5. Si el contrato ya existe, actualízalo conservando lo que sigue siendo válido. No borres operaciones: si el código ya no las tiene, indícalo en `Avisos:`.
6. Cuando el fichero esté escrito, en `specs/<feature>/tasks.md` cambia `### [ ] TASK-xxx` por `### [x] TASK-xxx` con Edit, y nada más.
7. Si existe `.sdd/architecture.md`, añade o actualiza la línea del contrato: `<protocolo> | <ruta del contrato> | <origen> -> <destino>`, con el `id` del mapa de tu módulo como origen y el del componente destino según el diseño (`?` si no consta).

## Modo mapa

Cuando el router te lo pida para una feature (con todas las tareas `[x]`):

1. Aplica de "Cambios sugeridos al mapa" de `design.md` solo las líneas que te corresponden: las de ruta bajo tu carpeta de trabajo o bajo `docs/contracts/*/<layer>/<modulo>/`. Prefijos: `+` añadir, `~` modificar, `-` quitar. Formato de línea: el de `sdd-design`.
2. Para un componente que era `por definir`, usa la ruta real que el desarrollador dejó en `tasks.md`.
3. Comprueba con Read que cada ruta que añades existe; si no, no la añadas y avísalo.
4. No toques las líneas de otros módulos ni la línea `Actualizado`.
5. Si `.sdd/architecture.md` no existe, no lo crees: avísalo en `Avisos:`.

## Cómo respondes

La primera línea es `PREGUNTAS`, `LISTO`, `PARCIAL` o `SIN CAMBIOS` (ver `team-conventions`).

- `LISTO`: ficheros escritos o modificados, tareas marcadas y una línea `Avisos:` si procede (operaciones que el código ya no tiene, rutas que no existen, sin mapa).
- `PARCIAL`: tareas hechas, pendientes y el motivo.
- `SIN CAMBIOS`: una frase explicando por qué.