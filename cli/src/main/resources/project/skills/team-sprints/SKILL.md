---
name: team-sprints
description: Guía para redactar sprints técnicos en docs/sprints/<modulo>/ a partir de las tareas de una feature. Úsala solo en el analista especialista.
---

# Sprints técnicos

Un sprint es el plan técnico de las tareas de UNA feature para UN módulo: enfoque técnico y cambios que debe realizar el desarrollador. No es una ventana de tiempo ni una unidad que deba ejecutarse entera de una vez: puede ejecutarse en varios lotes, y el módulo puede visitarse varias veces si sus tareas se intercalan con las de otros módulos (Core → Persistence → Core). No crees un sprint distinto por cada visita.

## Antes de escribir

1. Aplica la comprobación previa de `team-conventions`.
2. Lee `specs/<feature>/tasks.md` (debe tener `Estado: aprobado`; si no, responde `PREGUNTAS` indicando el bloqueo), `design.md`, los `REQ-xxx` de `requirements.md` que tus tareas cubren, `.sdd/architecture.md` si el router te da su ruta y tu línea en `.sdd/team.md`.
3. Lee el código solo en las rutas de tu módulo y solo lo necesario para especificar los cambios. Del resto de módulos fíate de `design.md`, el mapa y `docs/contracts/`. Fundamenta el plan en el código existente: nombra clases y ficheros reales, no supuestos; si algo no existe, márcalo `(nuevo)`.
4. Lee los sprints existentes en tu carpeta para numerar y mantener coherencia.
5. Dudas bloqueantes (respuestas distintas darían planes distintos): no escribas y responde `PREGUNTAS`. Las no bloqueantes van como supuestos.
6. Anota la `Revisión` actual de `requirements.md`, `design.md` y `tasks.md` (1 si no la tienen): irán en `## Origen SDD`.

## Fichero y numeración

`docs/sprints/<modulo>/sprint-NN-<feature>.md`, con NN de dos cifras: el siguiente al mayor existente en tu carpeta (01 si no hay). Si ya existe el sprint de esa feature con `Estado: aprobado` y el router no pide rehacerlo, no lo toques y responde `SIN CAMBIOS`. Si lo pide, modifícalo en el mismo fichero y déjalo en `borrador`.

## Sprint desactualizado

Si el router te pide revisar el impacto de un sprint cuyas revisiones de `## Origen SDD` no coinciden con las actuales (o que no tiene `## Origen SDD`):

1. Compara qué cambió en `requirements.md`, `design.md` y `tasks.md` y qué tareas o decisiones del sprint afecta.
2. Revisa solo la parte afectada; conserva las tareas y referencias que siguen siendo válidas y no elimines trabajo ya terminado (`[x]`). No cambies `## Validación`.
3. Si solo hay que confirmar compatibilidad, actualiza las revisiones de `## Origen SDD` sin rediseñar el plan y dilo en `Avisos:`. Si hay cambios sustantivos, modifica lo afectado y déjalo en `borrador` para nueva aprobación.
4. Si el cambio es tan grande que no puedes decidirlo, responde `PREGUNTAS`.

## Reglas

- Una sección por cada tarea que el router te asigne, y solo esas.
- Puedes nombrar ficheros, clases y métodos, pero no escribas la implementación.
- Componente nuevo (`por definir (analista técnico)` en la tarea): propón la ubicación siguiendo las convenciones del módulo y márcala `(propuesta)`. El desarrollador completará la ruta en `tasks.md` al implementar.
- Pruebas: concreta las de `tasks.md` (qué clase de prueba, qué casos, resultado esperado, comando del proyecto, por ejemplo `mvn test`). Mantén el caso de error y la prueba manual.
- Dependencias: construye a partir de `Depende de` de `tasks.md`. Las de tareas del mismo módulo van en "Dependencias internas"; las de otros módulos, en "Dependencias externas" con su `TASK-xxx`, su módulo y la **condición de entrega** que debe cumplirse (por ejemplo, "interfaz implementada" o "contrato aprobado"). Una tarea no queda habilitada solo porque la anterior figure `[x]`: debe cumplirse esa condición.
- Orden: propón un orden válido dentro del módulo y, junto a cada tarea, la condición que la habilita. No asumas que todas las tareas del módulo se ejecutan seguidas. Si detectas un ciclo o una referencia inexistente, responde `BLOQUEADO` (`DEPENDENCIA_NO_CUMPLIDA`) con las referencias concretas.
- Pruebas proporcionadas: las específicas de cada tarea. No pidas la suite completa en el sprint; la ejecuta el usuario y su resultado se registra en `## Validación`.

## Formato

La tercera línea es siempre `Estado: borrador`. `## Origen SDD` y `## Validación` son obligatorias; `## Validación` se crea con todos los campos en `pendiente`.

```markdown
# Sprint NN: <feature> - <modulo>

Estado: borrador

## Alcance

- Tareas: TASK-001, TASK-003 (de `specs/<feature>/tasks.md`)
- Dependencias internas: <TASK-xxx depende de TASK-yyy, o Ninguna.>
- Dependencias externas: <TASK-xxx (módulo): condición de entrega que debe cumplirse antes, o Ninguna.>

## Origen SDD

- Requirements: <revisión>
- Design: <revisión>
- Tasks: <revisión>

## Enfoque técnico

<3 a 8 líneas: cómo se resuelve, patrones y decisiones técnicas relevantes.>

## Cambios por tarea

### TASK-001 <título>

- **Ficheros:** `<ruta>` (nuevo | modificar): <qué cambia>
- **Enfoque:** <cómo, sin código>
- **Pruebas automáticas:** <clase y casos: feliz y de error o límite; comando>
- **Prueba manual:** <pasos y resultado esperado>

## Orden de implementación

1. TASK-001 (habilitada por: <condición o "sin dependencias">)
2. TASK-003 (habilitada por: <condición, p. ej. TASK-002 de persistence entregada>)

## Riesgos y dudas técnicas

- <riesgo y mitigación, o Ninguno.>

## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
```

## Cómo respondes

La primera línea es `PREGUNTAS`, `LISTO`, `SIN CAMBIOS` o `BLOQUEADO` (ver `team-conventions`). En `LISTO`, indica la ruta del sprint, el resumen y en `Avisos:` las rutas propuestas, las dependencias externas y lo que no pudiste cubrir.