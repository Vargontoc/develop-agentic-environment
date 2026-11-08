---
name: sdd-tasklist
description: Guía para redactar specs/<feature>/tasks.md con tareas pequeñas, verificables y trazables a REQ-xxx y DEC-xxx, con pruebas automáticas y manuales. Úsala solo en la fase de tareas de SDD.
---

# Fase de tareas (SDD)

Produces `specs/<feature>/tasks.md`. Divide el diseño en tareas pequeñas que un desarrollador pueda implementar y verificar una a una. Describe QUÉ entrega cada tarea y cómo se comprueba, nunca el código: el analista técnico del proyecto propondrá la implementación.

## Antes de escribir

1. Comprueba `.sdd/project.md` con la skill `sdd-project-context`. Si falla, sigue sus instrucciones y termina.
2. Lee `specs/<feature>/requirements.md` y `specs/<feature>/design.md`. Ambos deben existir con `Estado: aprobado`. Si no, no escribas nada y responde `PREGUNTAS` indicando el bloqueo. Anota la `Revisión` de cada uno (1 si no la tienen): las usarás en `Basado en:`. Si `design.md` tiene un `Basado en: requirements@N` distinto de la revisión actual de `requirements.md`, el diseño no está vigente: no escribas nada y responde `BLOQUEADO` con código `DEPENDENCIA_DESACTUALIZADA`.
3. Si el router te da la ruta de `.sdd/architecture.md`, léelo entero. No explores el repositorio: tus fuentes son esos tres ficheros.
4. Si `specs/<feature>/tasks.md` ya existe, léelo y sigue "Si ya existe tasks.md". Si tiene `Estado: aprobado` y el router no pide rehacerlo de forma explícita, no lo toques y responde `SIN CAMBIOS`.
5. Si hay dudas bloqueantes (el diseño no permite cubrir un requisito, o respuestas distintas darían planes sustancialmente distintos), no escribas el fichero y responde `PREGUNTAS` (máximo 5, cada una con propuesta por defecto). Las dudas no bloqueantes van como supuestos en `Avisos:`.

## Cómo dividir

- **Pequeñas y verificables.** Cada tarea toca un componente del diseño y tiene un resultado observable. Si toca varios, divídela. Excepción: una tarea de integración, cuyo resultado verificable es la comunicación entre dos componentes de la tabla "Interfaces entre componentes"; cita ambos.
- **Orden por dependencias.** Una tarea solo puede depender de tareas anteriores. Sin ciclos.
- **Cobertura.** Todo `REQ-xxx` y `NFR-xxx` debe estar cubierto por al menos una tarea, y la tabla final lo demuestra. Si no puedes cubrir alguno con el diseño, es una duda bloqueante.
- **Una tarea aparte para un NFR transversal** (por ejemplo, rendimiento medido de extremo a extremo) solo cuando no pertenezca a un componente concreto. Las pruebas normales van dentro de cada tarea, nunca como tareas aparte.
- No inventes trabajo que ni los requisitos ni el diseño pidan.

## Componente y ubicación

Para ahorrar búsquedas al desarrollador, cada tarea cita el componente con su `id` y su `ruta` según `.sdd/architecture.md`.

- Componente existente con entrada en el mapa: `<id> | <ruta>`.
- Componente existente sin entrada en el mapa: `<nombre del diseño> | no consta en el mapa`, y avísalo en `Avisos:`.
- Componente nuevo: `<nombre del diseño> | por definir (analista técnico)`. No inventes ni sugieras rutas: se completará al implementarlo.

## Tipos de tarea del proyecto

Un proyecto puede definir tipos de tarea propios en `.sdd/task-types.md`. Léelo con Read y su ruta exacta: si Read dice que no existe, no hay tipos propios y no generes ninguna tarea que no salga de los requisitos y el diseño.

Si existe, genera las tareas adicionales que describe cada tipo, solo cuando se cumpla su condición `Cuándo`, y ninguna otra. Cada una sigue la ficha normal, con estos matices:

- El título empieza por el `Prefijo` del tipo, seguido de dos puntos.
- `Depende de`, `Hecho cuando` y las pruebas se toman del tipo.
- Si con lo que dicen los requisitos y el diseño no puedes decidir si se cumple la condición, no la generes y avísalo en `Avisos:`.

## Tareas de contratos

Por cada fila de la tabla "Contratos afectados" de `design.md` crea una tarea para formalizar ese contrato. Si la tabla dice "Ninguno.", no crees ninguna. Esta tarea la hace la capa técnica del proyecto, no un agente SDD.

- **Título:** empieza siempre por `Contrato: <protocolo> - <qué cambia>`, por ejemplo `### [ ] TASK-007 Contrato: openapi - alta de usuario`. Así los routers técnicos las distinguen sin interpretar.
- **Alcance:** crear o actualizar el contrato del protocolo indicado en la fila, con el cambio que describe.
- **Componente:** el que expone la interfaz según "Interfaces entre componentes" del diseño (si no está claro, el origen), con su `id` y `ruta` del mapa. Como ubicación del contrato, `docs/contracts/<protocolo>/`, con el nombre del fichero `por definir (analista técnico)` si el contrato es nuevo.
- **Depende de:** según la estrategia de contratos (ver "Estrategia de contratos"): con `implementation-first`, la tarea que implementa esa interfaz; con `contract-first`, ninguna tarea de esa interfaz, y es la tarea de implementación la que depende de la del contrato.
- **Cubre:** los `REQ-xxx` y `DEC-xxx` que dependan de esa interfaz.
- **Hecho cuando:** el contrato existe en `docs/contracts/<protocolo>/` y describe la interfaz: tal como quedó implementada (`implementation-first`) o tal como se ha diseñado (`contract-first`).
- **Pruebas:** aplica "Pruebas dentro de cada tarea". Normalmente: el contrato es válido según las reglas de su formato, si el formato lo permite, y recoge las respuestas de error que exigen los requisitos.
- **Prueba manual:** repasar que cada interfaz de la fila aparece en el contrato y coincide con la implementación (o con el diseño, en `contract-first`).
- Si una fila no indica protocolo, no puedes ubicar el contrato: avísalo en `Avisos:`.

### Estrategia de contratos

- `implementation-first` (por defecto, si nada dice lo contrario): primero se implementa la interfaz y después se formaliza el contrato.
- `contract-first`: primero se define el contrato y después se implementa.

La fuente única es la opción `contract-strategy` de `## Opciones` en `.sdd/team.md` (léela con Read y su ruta exacta). Si `team.md` no existe, no define la opción o su valor es desconocido, usa `implementation-first` y avísalo en `Avisos:` si el valor era desconocido. No leas la estrategia de ningún otro fichero. Respeta además las dependencias que añada `.sdd/task-types.md`, siempre que no generen ciclos.

## Pruebas dentro de cada tarea

Cada tarea define las verificaciones necesarias para demostrar su "Hecho cuando". Incluye escenarios positivos, negativos y de límite solo cuando correspondan al comportamiento especificado. No inventes errores ni requisitos nuevos para completar una estructura de pruebas.

- **Automáticas:** sácalas de los criterios de aceptación, de los requisitos "Si <condición no deseada>", de los umbrales de los NFR y de los `ALT-xxx` del diseño que afecten a la tarea. Un escenario por línea, con su resultado esperado.
- **Manual:** pasos concretos y numerados que una persona pueda ejecutar (qué hacer y qué debe verse).
- Cuando no procedan pruebas automáticas o manuales, escribe `No aplica: <justificación>`. Todo comportamiento observable debe tener alguna verificación apropiada: `No aplica` no es una forma de omitirla.
- Describe qué se prueba y qué resultado se espera, no el código de la prueba.

## Escenarios alternativos del diseño

Los `ALT-xxx` de `design.md` que afecten a una tarea deben quedar reflejados en su alcance o en sus pruebas, y en `Cubre`. No crees una tarea independiente por escenario.

## Si ya existe tasks.md

- Conserva los IDs. Los nuevos continúan la numeración y un ID retirado no se reutiliza.
- Conserva el estado de las casillas: una tarea `[x]` no se modifica ni se borra. Si un cambio en los requisitos o el diseño la afecta, crea una tarea nueva y avísalo en `Avisos:`.
- Conserva las rutas que el desarrollador haya completado (las que ya no dicen `por definir`).
- Nunca marques una casilla: solo la marca el desarrollador al implementar la tarea.

## Formato del fichero

La tercera línea es siempre `Estado: borrador`. Cada vez que crees o modifiques el fichero, déjala en `borrador`: solo el router la pasa a `aprobado`, y solo tras la aprobación explícita del usuario. La casilla va en el encabezado de cada tarea, con ese formato exacto: `### [ ] TASK-001 <título>`.

Las líneas cuarta y quinta son `Revisión: N` y `Basado en: requirements@N, design@M` (las revisiones con las que has trabajado). `Revisión` empieza en 1 y la incrementas solo cuando cambia el contenido sustantivo, incluido un cambio solo de `Basado en:`. Si el fichero existente no las tiene, `Revisión` se interpreta como 1 y las añades al modificarlo.

```markdown
# Tareas: <feature>

Estado: borrador
Revisión: 1
Basado en: requirements@1, design@1

## Resumen

<1 a 2 frases: cómo se ha dividido el trabajo.>

## Tareas

### [ ] TASK-001 <título corto>

- **Cubre:** REQ-001, DEC-001, ALT-001
- **Componente:** <id del mapa> | <ruta>
- **Depende de:** -
- **Alcance:** <una frase: qué entrega la tarea>
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 y <resultado observable concreto de esta tarea>.
- **Pruebas automáticas:**
  - <escenario y resultado esperado>
  - <escenario y resultado esperado>
- **Prueba manual:**
  1. <acción y resultado esperado>

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-001 |
```

Si una sección no tiene contenido, déjala con "Ninguno.".

## Cómo respondes al router

## Verificación previa a LISTO

Antes de escribir y de responder `LISTO`, comprueba:

- Todos los `REQ-xxx` y `NFR-xxx` están cubiertos por alguna tarea y la tabla de cobertura lo refleja.
- Las referencias `REQ`, `NFR` y `DEC` de cada tarea existen en los ficheros aprobados.
- Las dependencias entre tareas no forman ciclos y ninguna tarea depende de sí misma.
- Los IDs de las tareas ya existentes se conservan, y las `[x]` no cambian.
- Las tareas nuevas tienen un resultado verificable ("Hecho cuando") y pruebas proporcionales o un `No aplica` justificado.
- Cada `ALT-xxx` relevante de `design.md` aparece en el alcance, las pruebas o el `Cubre` de alguna tarea.
- Las dependencias de las tareas de contrato respetan la estrategia de `contract-strategy` de `.sdd/team.md`.
- Cada fila de "Contratos afectados" tiene su tarea de contrato.
- No hay tareas sin respaldo en los requisitos, el diseño o las reglas de `.sdd/task-types.md`.

Si una comprobación falla y puedes corregirla, corrígela. Si no puedes, no devuelvas `LISTO`: responde `PREGUNTAS` si falta una decisión, o `BLOQUEADO` si no (por ejemplo, un ciclo que viene de las dependencias de un tipo de `.sdd/task-types.md`).

## Cómo respondes al router

La primera línea es `PREGUNTAS`, `LISTO`, `SIN CAMBIOS` o `BLOQUEADO`.

- `PREGUNTAS`: lista numerada de dudas, cada una con su propuesta por defecto. No has escrito nada.
- `LISTO`: debajo, estas líneas:
  - `Fichero:` ruta del fichero.
  - `Resumen:` 1 a 3 frases y número de tareas.
  - `Verificación:` `correcta`.
  - `Avisos:` si procede (componentes sin entrada en el mapa, sin mapa, tareas ya hechas afectadas por cambios, compatibilidad confirmada).
- `SIN CAMBIOS`: una frase explicando por qué.
- `BLOQUEADO`: hay una condición que impide completar el trabajo. No has escrito nada (haz las comprobaciones antes de escribir). Debajo: `Código:` (por ejemplo `DEPENDENCIA_DESACTUALIZADA`, `CICLO_DEPENDENCIAS`, `COBERTURA_INCOMPLETA`), `Motivo:`, `Acción requerida:` y `Fichero:`. Si falla una escritura, dilo expresamente y no afirmes que el fichero se ha conservado sin haberlo comprobado.