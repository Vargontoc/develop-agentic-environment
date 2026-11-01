---
name: sdd-tasklist
description: Guía para redactar specs/<feature>/tasks.md con tareas pequeñas, verificables y trazables a REQ-xxx y DEC-xxx, con pruebas automáticas y manuales. Úsala solo en la fase de tareas de SDD.
---

# Fase de tareas (SDD)

Produces `specs/<feature>/tasks.md`. Divide el diseño en tareas pequeñas que un desarrollador pueda implementar y verificar una a una. Describe QUÉ entrega cada tarea y cómo se comprueba, nunca el código: el analista técnico del proyecto propondrá la implementación.

## Antes de escribir

1. Comprueba `.sdd/project.md` con la skill `sdd-project-context`. Si falla, sigue sus instrucciones y termina.
2. Lee `specs/<feature>/requirements.md` y `specs/<feature>/design.md`. Ambos deben existir con `Estado: aprobado`. Si no, no escribas nada y responde `PREGUNTAS` indicando el bloqueo.
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
- **Depende de:** la tarea que implementa esa interfaz, porque el contrato debe reflejar lo implementado, no solo lo diseñado.
- **Cubre:** los `REQ-xxx` y `DEC-xxx` que dependan de esa interfaz.
- **Hecho cuando:** el contrato existe en `docs/contracts/<protocolo>/` y describe la interfaz tal como quedó implementada.
- **Pruebas automáticas:** caso feliz: el contrato es válido según las reglas de su formato, si el formato lo permite; caso de error: el contrato recoge las respuestas de error que exigen los requisitos.
- **Prueba manual:** repasar que cada interfaz de la fila aparece en el contrato y coincide con lo implementado.
- Si una fila no indica protocolo, no puedes ubicar el contrato: avísalo en `Avisos:`.

## Pruebas dentro de cada tarea

- **Automáticas:** el camino feliz y al menos un caso de error o de límite. Sácalos de los criterios de aceptación, de los requisitos "Si <condición no deseada>" y de los umbrales de los NFR. Si el requisito no tiene caso de error, propón uno, márcalo como `(propuesto)` y avísalo en `Avisos:`: indica que al requisito le falta ese caso.
- **Manual:** pasos concretos y numerados que una persona pueda ejecutar (qué hacer y qué debe verse). Solo se admite `no aplica: <motivo>` para tareas sin comportamiento observable.
- Describe qué se prueba y qué resultado se espera, no el código de la prueba.

## Si ya existe tasks.md

- Conserva los IDs. Los nuevos continúan la numeración y un ID retirado no se reutiliza.
- Conserva el estado de las casillas: una tarea `[x]` no se modifica ni se borra. Si un cambio en los requisitos o el diseño la afecta, crea una tarea nueva y avísalo en `Avisos:`.
- Conserva las rutas que el desarrollador haya completado (las que ya no dicen `por definir`).
- Nunca marques una casilla: solo la marca el desarrollador al implementar la tarea.

## Formato del fichero

La tercera línea es siempre `Estado: borrador`. Cada vez que crees o modifiques el fichero, déjala en `borrador`: solo el router la pasa a `aprobado`, y solo tras la aprobación explícita del usuario. La casilla va en el encabezado de cada tarea, con ese formato exacto: `### [ ] TASK-001 <título>`.

```markdown
# Tareas: <feature>

Estado: borrador

## Resumen

<1 a 2 frases: cómo se ha dividido el trabajo.>

## Tareas

### [ ] TASK-001 <título corto>

- **Cubre:** REQ-001, DEC-001
- **Componente:** <id del mapa> | <ruta>
- **Depende de:** -
- **Alcance:** <una frase: qué entrega la tarea>
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 y <resultado observable concreto de esta tarea>.
- **Pruebas automáticas:**
  - Caso feliz: <qué se prueba y resultado esperado>
  - Caso de error o límite: <qué se prueba y resultado esperado>
- **Prueba manual:**
  1. <paso y resultado esperado>

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-001 |
```

Si una sección no tiene contenido, déjala con "Ninguno.".

## Cómo respondes al router

La primera línea es `PREGUNTAS`, `LISTO` o `SIN CAMBIOS`.

- `PREGUNTAS`: lista numerada de dudas, cada una con su propuesta por defecto. No has escrito nada.
- `LISTO`: ruta del fichero, resumen de 1 a 3 frases, número de tareas y una línea `Avisos:` si procede (casos de error propuestos, componentes sin entrada en el mapa, sin mapa, tareas ya hechas afectadas por cambios).
- `SIN CAMBIOS`: una frase explicando por qué.