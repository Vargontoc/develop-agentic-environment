---
name: team-conventions
description: Convenciones de la capa técnica del proyecto (routers y especialistas de analista, desarrollador y documentador): registro .sdd/team.md, opciones, nombres, estados, permisos, validación y protocolo de respuesta. Úsala antes de actuar en cualquier agente técnico.
---

# Convenciones de la capa técnica

La capa SDD (specs) define QUÉ y CÓMO a alto nivel. La capa técnica lo ejecuta por módulo con especialistas propios del proyecto, coordinados por tres routers: `analista-router`, `desarrollador-router` y `documentador-router`.

Esta skill es la única fuente de estas reglas. Los routers y especialistas las consumen; no las reproducen ni las matizan.

## Comprobación previa

1. `.sdd/project.md`: aplica la comprobación de la skill `sdd-project-context`.
2. `.sdd/team.md`: existe, no está vacío y tiene al menos una línea de módulo bajo `## Módulos`.
3. Se cumplen las validaciones del registro (ver abajo).

Si algo falla, no hagas nada más: no delegues, no crees ni modifiques ficheros. Responde con qué falla y una sugerencia mínima: crear o corregir `.sdd/team.md` desde la plantilla `team.md`.

Para comprobar un fichero de un directorio oculto (`.sdd/...`), usa Read con la ruta exacta: si Read dice que no existe, no existe. No uses Glob ni Grep para ello.

`.sdd/architecture.md` es opcional: un proyecto puede empezar sin él. Solo se exige cuando la tarea lo necesita.

## Registro `.sdd/team.md`

Una línea por módulo bajo `## Módulos`, siempre con este patrón:

`- <modulo> | <id del mapa o -> | <rutas>`

| Campo | Regla |
|---|---|
| `modulo` | Identificador del módulo, único en el registro. Sin espacios. |
| `id del mapa` | `id` del módulo en `.sdd/architecture.md`, o `-`. Relaciona el `Componente` de una tarea con su módulo. |
| `rutas` | Rutas de código autorizadas del módulo (main, test, y ficheros como `pom.xml` si necesita dependencias), relativas al repositorio y separadas por espacios, o `-`. |

### Validaciones obligatorias

- No puede haber dos módulos con el mismo `modulo`.
- Un `id del mapa` distinto de `-` debe ser único y, si existe `.sdd/architecture.md`, identificable en él.
- Las rutas pertenecen al repositorio. Los prefijos de ruta se comparan por segmentos de directorio, no por texto (`core` no contiene a `core-api`).
- Una ruta compartida por dos módulos debe declararse expresamente. Si no, se bloquea la escritura en ella hasta que el usuario decida quién es responsable (ver Permisos).
- Una opción desconocida se notifica al usuario; no se interpreta en silencio.

### Opciones

Al final del fichero, bajo `## Opciones`:

| Opción | Valores | Por defecto | Efecto |
|---|---|---|---|
| `sprints` | `si` / `no` | `si` | Con `no`, no interviene el analista y el desarrollador trabaja desde `tasks.md` y `design.md`. |
| `git-policy` | `balanced` / `strict` | `balanced` | Cómo comprueba el estado de Git el `desarrollador-router`. |
| `contract-strategy` | `implementation-first` / `contract-first` | `implementation-first` | Si el contrato se redacta antes o después de implementar. |
| `readme-owner` | `<modulo>` | sin valor | Módulo responsable del README. Debe existir en el registro. |

Si una opción falta, vale el valor por defecto; si `readme-owner` falta cuando se pide tocar el README, el router pide al usuario que elija: no elige el primer módulo.

## Especialistas

- Cada módulo tiene un especialista por rol, con nombre `<rol>-<modulo>` (roles: `analista`, `desarrollador`, `documentador`). El nombre lógico es único e independiente del formato de cada plataforma; si una herramienta necesita otro formato físico, lo resuelve el generador, nunca los routers. Viven en el directorio de agentes de cada proveedor del proyecto.
- Un especialista lee solo las rutas de su módulo en `.sdd/team.md` y lo que su encargo indique, y escribe solo donde su rol lo permite (tabla de Permisos). Si algo te impide hacerlo o dudas de si puedes, no lo eludas: informa.
- El router comprueba que el especialista existe antes de delegar. Si falta, lo comunica y no lo sustituye por otro.

## Estados y vigencia

Los ficheros que requieren aprobación (los sprints y los documentos de cambio) llevan en la tercera línea `Estado: borrador` o `Estado: aprobado`. Solo el router correspondiente pasa a `aprobado`, y solo tras la aprobación explícita del usuario. Quien crea o modifica el fichero lo deja en `borrador`.

La aprobación no implica vigencia permanente. Un sprint declara en `## Origen SDD` las revisiones en las que se basa (`Requirements: N`, `Design: N`, `Tasks: N`; las mismas `Revisión` de los ficheros de `specs/<feature>/`). Un sprint `aprobado` cuyas revisiones de origen no coinciden con las actuales está **desactualizado**: no se inicia trabajo pendiente desde él hasta una revisión de impacto del analista. Sin `## Origen SDD`, su vigencia es **desconocida**, no inválida: se revisa en su siguiente uso. Lo ya terminado no se invalida ni cambia de estado.

## Permisos

El router coordina; el especialista ejecuta. Los routers no toman decisiones técnicas.

| Rol | Escribe | No hace |
|---|---|---|
| analista | `docs/sprints/<modulo>/` y documentos de cambio de su módulo | código |
| desarrollador | código y pruebas unitarias de su módulo; en `specs/<feature>/tasks.md` solo la casilla `[x]` y la ruta de los componentes nuevos de sus tareas; ejemplos de uso en `specs/<feature>/<modulo>/example.md`; solo cuando el router se lo encarga, la sección `## Validación` del sprint (o `docs/validation/<feature>/<modulo>.md` con `sprints: no`) y las casillas de sus tareas en documentos de cambio | cambiar requisitos, diseño ni sprints |
| documentador | `docs/contracts/<protocolo>/`, `docs/guide/<modulo>/manual-uso.md`, las líneas de su módulo en `.sdd/architecture.md`, la casilla `[x]` de las tareas `Contrato:` y `Guia:`, y el README si es el responsable | código |
| analista-router | solo la aprobación de planes (`Estado: aprobado`) | cualquier otra escritura |
| desarrollador-router | nada; delega el registro de validación en el especialista | cualquier escritura general |
| documentador-router | nada | cualquier escritura |

Ficheros compartidos (`tasks.md`, `.sdd/architecture.md`, `README.md`, sprints, documentos de cambio): se modifica únicamente lo que el rol permite, relee el contenido vigente justo antes de escribir y no toca nada más. Si lo que iba a cambiar ya no coincide con lo leído, devuelve `BLOQUEADO` y conserva el contenido. Si dos módulos comparten carpeta, no se asume que sea toda de uno: se resuelve con rutas más específicas o se bloquea.

Estas reglas están escritas en los prompts; no equivalen a un aislamiento técnico garantizado por la plataforma. No afirmes que existe hasta comprobarlo.

## Protocolo de los especialistas

Los especialistas no pueden hablar con el usuario. Su respuesta empieza con una de estas líneas:

- `PREGUNTAS`: falta una decisión del usuario. Máximo 5, cada una con propuesta por defecto. No has escrito nada.
- `LISTO`: encargo completado y verificado dentro de tu alcance. Ruta del fichero, resumen de 1 a 3 frases y una línea `Avisos:` si procede.
- `PARCIAL` (solo desarrollador y documentador): has hecho una parte. Indica las tareas terminadas, las pendientes, el motivo (un test que falla, una duda, una dependencia sin cumplir) y el último error relevante, resumido.
- `SIN CAMBIOS`: no hay modificaciones necesarias; una frase explicando por qué.
- `BLOQUEADO`: una condición objetiva impide continuar. No has escrito nada. Usa este formato:

```
BLOQUEADO
Código: <CODIGO>
Motivo: <qué ocurre, con los valores concretos>
Acción requerida: <quién debe hacer qué>
Archivos afectados: <rutas>
```

  Códigos habituales: `SPRINT_DESACTUALIZADO`, `DEPENDENCIA_NO_CUMPLIDA`, `RUTA_FUERA_DE_ALCANCE`, `RUTA_COMPARTIDA`, `CAMBIO_CONCURRENTE`, `ESPECIALISTA_INEXISTENTE`, `CONFIGURACION_INVALIDA`, `INCOMPATIBLE_CON_DISEÑO`.
- `DERIVAR` (solo el analista en modo cambio): el cambio pertenece a SDD.

Ningún especialista responde `LISTO` sin haber comprobado las condiciones de finalización que le corresponden.

El router transmite las preguntas y los bloqueos al usuario y vuelve a delegar con sus respuestas.

## Qué significa "hecho"

`### [x] TASK-xxx` significa: la tarea está implementada, compila, sus pruebas específicas pasan y se cumplen sus condiciones de finalización dentro del alcance del especialista. No significa que el módulo ni la feature hayan superado la suite completa o la integración.

| Nivel | Responsable | Evidencia |
|---|---|---|
| Implementación de la tarea | desarrollador | código y pruebas específicas correctas |
| Validación del módulo | usuario | suite completa del módulo satisfactoria |
| Validación de integración y manual | usuario | resultado de las comprobaciones acordadas |

Una prueba fallida necesaria para completar la tarea impide marcarla. Las tareas cuyo título empieza por `Contrato:` o `Guia:` son del documentador.

### Registro de validación

El resultado de los niveles que corresponden al usuario se guarda en una sección del sprint, sin añadir estados a las tareas:

```
## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
```

Estados admitidos: `pendiente | correcta | fallida | no aplica`. Una validación `correcta` conserva al menos el origen de la confirmación y el alcance comprobado. Si el código afectado cambia después, esa validación no se reutiliza como evidencia del nuevo estado.

Con `sprints: no` no hay sprint: el registro es `docs/validation/<feature>/<modulo>.md`, creado solo cuando haya una validación, sin ser un plan ni requerir aprobación.

El `desarrollador-router` no escribe: delega el registro en el desarrollador especialista, que solo toca esa sección.

## Git

Ningún agente hace commit, push, pull, checkout, merge, rebase ni stash: los hace el usuario. Solo `desarrollador-router` consulta el estado de la rama, y solo en lectura, según `git-policy`.

## Particularidades de este proyecto

- Seguir el patron SOLID lo mas acotado posible pero sobre todo patron (KISS, Keep It Simple, Stupid)  y YAGNI (You Aren´t Gonna Need It). Buenas practicas pero que el codigo sea sencillo para los ejemplos que va leer el documentador. 
