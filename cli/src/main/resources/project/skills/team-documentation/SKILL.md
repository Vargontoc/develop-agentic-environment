---
name: team-documentation
description: Guía para formalizar contratos de comunicación en docs/contracts/<protocolo>/<modulo>/, mantener las líneas de un módulo en .sdd/architecture.md y actualizar el README. Úsala solo en el documentador especialista.
---

# Documentación técnica (contratos, mapa y README)

Describes lo que el producto hace realmente y distingues siempre entre lo implementado y lo solo diseñado: un contrato dice si describe un diseño pendiente o una interfaz contrastada con el código, y el mapa refleja lo que existe. No escribes código.

## Antes de escribir

1. Aplica la comprobación previa de `team-conventions`.
2. Lee `specs/<feature>/tasks.md` (`Estado: aprobado`), `design.md` (secciones "Interfaces entre componentes", "Contratos afectados" y "Cambios sugeridos al mapa"), los `REQ-xxx` que tus tareas cubren, `.sdd/architecture.md` si existe y tu línea de `.sdd/team.md`. El modo README no necesita feature ni `tasks.md` (ver abajo).
3. Del código lee solo tu carpeta de trabajo y solo lo necesario para describir la interfaz: controladores, manejadores, tipos de mensaje, DTOs. Aplica "Búsqueda de ficheros" de `sdd-project-context`.
4. Lee lo que ya exista en tu carpeta de salida y, si el protocolo ya tiene contratos en el proyecto, su formato.
5. Dudas bloqueantes (por ejemplo, hay que decidir entre dos interpretaciones): no escribas y responde `PREGUNTAS` (máximo 5, cada una con propuesta).
6. Si el título de la tarea empieza por el Prefijo de un tipo de `.sdd/task-types.md` (léelo con Read y su ruta exacta), sigue la skill indicada en su campo Skill en lugar de esta guía.
7. Si el código contradice un requisito aprobado o una decisión arquitectónica, no corrijas en silencio el documento para adaptarlo al código: responde `BLOQUEADO` (`INCOMPATIBLE_CON_DISEÑO`) si es una inconsistencia objetiva, o `PREGUNTAS` si hay que tomar una decisión.

## Modo contratos

Lee `contract-strategy` en `## Opciones` de `.sdd/team.md` (`implementation-first` por defecto; un valor desconocido se avisa y se usa el defecto).

| Estrategia | Cuándo redactas | Estado del contrato |
|---|---|---|
| `implementation-first` | Después de que la tarea de la que depende esté `### [x]` | `contrastado con el código` |
| `contract-first` | A partir del Design aprobado, antes de que exista la implementación; el desarrollador implementa contra él | `diseño pendiente de implementación` hasta que lo contrastes |

Comprueba la coherencia con las dependencias de `tasks.md`: con `implementation-first` la tarea `Contrato:` depende de la implementación; con `contract-first` es la implementación la que depende del contrato. Si las dependencias dicen lo contrario a la opción, responde `BLOQUEADO` (`ESTRATEGIA_CONTRADICTORIA`) sin escribir.

Por cada tarea `Contrato:` que el router te asigne:

1. Comprueba sus dependencias reales (`Depende de`), no un orden fijo por tipo de tarea. Con `implementation-first`, si la tarea de la que depende no está `### [x]`, no escribas nada para ella: responde `PARCIAL` con ese motivo.
2. Carpeta de salida: `docs/contracts/<protocolo>/<modulo>/` (créala escribiendo el fichero directamente con Write).
3. Formato por defecto:

| Protocolo | Formato | Fichero |
|---|---|---|
| `openapi` | OpenAPI 3.x en YAML | `openapi.yaml` |
| `websocket` | Markdown: canales, mensajes (dirección, payload, ejemplo) y errores | `websocket.md` |
| otro | Markdown: participantes, mensajes (dirección, payload, ejemplo) y errores | `<protocolo>.md` |

   Si el proyecto ya tiene contratos de ese protocolo en otro formato, sigue ese.
4. Describe solo lo que piden los requisitos y el diseño y, si hay código, lo que existe. Incluye los errores que exigen los requisitos y los que el código devuelve de verdad. Ejemplos mínimos. No inventes nada. Añade al principio un comentario con dos líneas: `Origen: specs/<feature> (REQ-xxx)` y `Estado del contrato: diseño pendiente de implementación | contrastado con el código`.
5. Nunca marques un contrato como `contrastado con el código` sin evidencia de implementación: léela en el código de tu módulo. Si ya estaba como diseño pendiente y ahora existe la implementación, compáralos, corrige las diferencias que sean del contrato (no del diseño aprobado) y cambia el estado. Si el código se aparta del diseño aprobado, aplica el punto 7 de "Antes de escribir".
6. Si el contrato ya existe, actualízalo conservando lo que sigue siendo válido. No borres operaciones: si el código ya no las tiene, indícalo en `Avisos:`.
7. Cuando el fichero esté escrito, en `specs/<feature>/tasks.md` cambia `### [ ] TASK-xxx` por `### [x] TASK-xxx` con Edit, y nada más. Con `contract-first`, una tarea queda `[x]` al quedar redactado el contrato, aunque su estado siga siendo `diseño pendiente de implementación`; vuelves a contrastarlo cuando el router te lo pida tras la implementación. Antes de escribir, vuelve a leer `tasks.md`.
8. Si existe `.sdd/architecture.md`, añade o actualiza la línea del contrato: `<protocolo> | <ruta del contrato> | <origen> -> <destino>`, con el `id` del mapa de tu módulo como origen y el del componente destino según el diseño (`?` si no consta). Solo si el componente existe (ver "Modo mapa").

## Modo mapa

El mapa se actualiza de forma progresiva: no hace falta que todas las tareas de la feature estén terminadas. Un componente se incorpora cuando su existencia y su responsabilidad están verificadas, aunque queden otras tareas pendientes.

1. Lee "Cambios sugeridos al mapa" de `design.md` y aplica solo las líneas que te corresponden: las de ruta bajo tu carpeta de trabajo o bajo `docs/contracts/*/<modulo>/`. Prefijos: `+` añadir, `~` modificar, `-` quitar. Formato de línea: el de `sdd-design`.
2. Comprueba con Read que cada componente que añades existe de verdad (ruta real, y que hace lo que dice el diseño). Un componente solo planificado no se incorpora: indícalo en `Avisos:`.
3. Para un componente que era `por definir`, usa la ruta real que el desarrollador dejó en `tasks.md`.
4. Actualiza únicamente las entradas de tu módulo. No toques las de otros módulos (si hace falta, indícalo en `Avisos:` para delegarlo a su responsable) ni la línea `Actualizado`.
5. Comunica en `Avisos:` las discrepancias entre lo construido y lo que sugería el diseño. Si contradicen un requisito aprobado o una decisión arquitectónica, aplica el punto 7 de "Antes de escribir".
6. Si `.sdd/architecture.md` no existe, no lo crees: avísalo en `Avisos:`.
7. Relee el mapa justo antes de escribir; si cambió desde que lo leíste, responde `BLOQUEADO` (`CAMBIO_CONCURRENTE`).

## Modo README

Solo si eres el módulo `readme-owner` de `.sdd/team.md` y el router te lo pide. No necesita feature activa ni `tasks.md` aprobado: es mantenimiento general del proyecto. Sigue la skill `docs-readme`.

- Además de tu módulo, puedes leer la documentación general (`docs/`, `.sdd/`, contratos, guía) y las evidencias técnicas necesarias para redactar el README.
- Tu permiso de escritura adicional es exclusivamente `README.md` (o los recursos documentales que el encargo autorice de forma expresa), dentro de lo que permite `docs-readme`.
- Si no eres el responsable, responde `BLOQUEADO` (`RUTA_FUERA_DE_ALCANCE`).

## Cómo respondes

La primera línea es `PREGUNTAS`, `LISTO`, `PARCIAL`, `SIN CAMBIOS` o `BLOQUEADO` (ver `team-conventions`). No respondas `LISTO` sin haber comprobado las condiciones de finalización de tus tareas.

- `LISTO`: ficheros escritos o modificados, tareas marcadas y una línea `Avisos:` si procede (operaciones que el código ya no tiene, rutas que no existen, componentes solo planificados, discrepancias con el diseño, sin mapa). Indica el estado de cada contrato (`diseño pendiente de implementación` o `contrastado con el código`).
- `PARCIAL`: tareas hechas, pendientes y el motivo.
- `SIN CAMBIOS`: una frase explicando por qué.
