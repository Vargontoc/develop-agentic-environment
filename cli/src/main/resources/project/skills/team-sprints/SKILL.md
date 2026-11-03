---
name: team-sprints
description: Guía para redactar sprints técnicos en docs/sprints/<modulo>/ a partir de las tareas de una feature. Úsala solo en el analista especialista.
---

# Sprints técnicos

Un sprint es el plan técnico de las tareas de UNA feature para UN módulo: enfoque técnico y cambios que debe realizar el desarrollador. No es una ventana de tiempo.

## Antes de escribir

1. Aplica la comprobación previa de `team-conventions`.
2. Lee `specs/<feature>/tasks.md` (debe tener `Estado: aprobado`; si no, responde `PREGUNTAS` indicando el bloqueo), `design.md`, los `REQ-xxx` de `requirements.md` que tus tareas cubren, `.sdd/architecture.md` si el router te da su ruta y tu línea en `.sdd/team.md`.
3. Lee el código solo en las rutas de tu módulo y solo lo necesario para especificar los cambios. Del resto de módulos fíate de `design.md`, el mapa y `docs/contracts/`.
4. Lee los sprints existentes en tu carpeta para numerar y mantener coherencia.
5. Dudas bloqueantes (respuestas distintas darían planes distintos): no escribas y responde `PREGUNTAS`. Las no bloqueantes van como supuestos.

## Fichero y numeración

`docs/sprints/<modulo>/sprint-NN-<feature>.md`, con NN de dos cifras: el siguiente al mayor existente en tu carpeta (01 si no hay). Si ya existe el sprint de esa feature con `Estado: aprobado` y el router no pide rehacerlo, no lo toques y responde `SIN CAMBIOS`. Si lo pide, modifícalo en el mismo fichero y déjalo en `borrador`.

## Reglas

- Una sección por cada tarea que el router te asigne, y solo esas.
- Puedes nombrar ficheros, clases y métodos, pero no escribas la implementación.
- Componente nuevo (`por definir (analista técnico)` en la tarea): propón la ubicación siguiendo las convenciones del módulo y márcala `(propuesta)`. El desarrollador completará la ruta en `tasks.md` al implementar.
- Pruebas: concreta las de `tasks.md` (qué clase de prueba, qué casos, resultado esperado, comando del proyecto, por ejemplo `mvn test`). Mantén el caso de error y la prueba manual.
- Tareas que dependan de otro módulo: anótalas en "Dependencias externas" con su `TASK-xxx`, su módulo y lo que debe existir antes.

## Formato

La tercera línea es siempre `Estado: borrador`.

```markdown
# Sprint NN: <feature> - <modulo>

Estado: borrador

## Alcance

- Tareas: TASK-001, TASK-003 (de `specs/<feature>/tasks.md`)
- Dependencias externas: <TASK-xxx (módulo): qué debe existir antes, o Ninguna.>

## Enfoque técnico

<3 a 8 líneas: cómo se resuelve, patrones y decisiones técnicas relevantes.>

## Cambios por tarea

### TASK-001 <título>

- **Ficheros:** `<ruta>` (nuevo | modificar): <qué cambia>
- **Enfoque:** <cómo, sin código>
- **Pruebas automáticas:** <clase y casos: feliz y de error o límite; comando>
- **Prueba manual:** <pasos y resultado esperado>

## Orden de implementación

1. TASK-001
2. TASK-003

## Riesgos y dudas técnicas

- <riesgo y mitigación, o Ninguno.>
```

## Cómo respondes

La primera línea es `PREGUNTAS`, `LISTO` o `SIN CAMBIOS` (ver `team-conventions`). En `LISTO`, indica la ruta del sprint, el resumen y en `Avisos:` las rutas propuestas, las dependencias externas y lo que no pudiste cubrir.