---
name: team-development
description: Guía para implementar las tareas de un sprint en el código de un módulo, con sus pruebas unitarias y mvn, y marcarlas como hechas en tasks.md. Úsala solo en el desarrollador especialista.
---

# Desarrollo de tareas

Implementas las tareas que el router te asigna en el código de tu módulo, con sus pruebas, y las marcas como hechas.

## Antes de escribir

1. Aplica la comprobación previa de `team-conventions`.
2. Lee `specs/<feature>/tasks.md` (`Estado: aprobado`) y, de cada tarea asignada, su ficha. Lee `design.md` y los `REQ-xxx` que cubren de `requirements.md`.
3. Con `sprints: si` (por defecto), lee el sprint que el router te indica: debe estar `Estado: aprobado`. Sigue su enfoque, ficheros y orden. Con `sprints: no`, decide tú el orden según `Depende de`.
4. Lee `.sdd/architecture.md` si el router te da su ruta, y tu línea de `.sdd/team.md`.
5. Del código lee solo las rutas de tu módulo y solo lo necesario.
6. Si una tarea depende de otra que sigue `### [ ]` (de cualquier módulo), no la empieces: responde `PARCIAL` con ese motivo.
7. Si el sprint o la tarea es contradictoria o insuficiente, no toques código y responde `PREGUNTAS` (máximo 5, cada una con propuesta).
8. Si el título de la tarea empieza por el `Prefijo` de un tipo de `.sdd/task-types.md` (léelo con Read y su ruta exacta), sigue la skill indicada en su campo `Skill` en lugar de esta guía.

## Cómo implementas

Para cada tarea, en orden:

1. Implementa los cambios dentro de las rutas de tu módulo. No toques nada fuera de ellas, salvo `tasks.md` como se indica abajo.
2. Escribe las pruebas automáticas de la tarea que sean unitarias (sin base de datos real, red ni otros módulos): el caso feliz y el de error o límite. Las de integración y las manuales las hace el usuario: no las escribas. Si la tarea no pide pruebas automáticas, no las inventes.
3. Ejecuta **solo las pruebas que escribiste o modificaste en esta tarea**, nunca la suite completa: `mvn test -Dtest=<ClaseTest>[,<OtraClaseTest>]` (o `<ClaseTest>#<método>`), desde la carpeta de trabajo de tu módulo. Si la tarea no tiene pruebas, ejecuta `mvn compile`. La suite completa (`mvn test` sin `-Dtest`, `mvn verify`) la ejecuta el usuario. Si el sprint pide ejecutar la suite completa o `mvn verify`, no lo hagas y anótalo en `Avisos:`. Dónde ejecutarlo: en OpenCode con el parámetro `workdir` de la herramienta shell, sin `cd` en el comando; en Claude Code, empezando el comando por un `cd` a esa carpeta, porque el directorio no se conserva entre comandos de un subagente. La primera ejecución puede tardar (descarga dependencias): pasa un timeout de hasta 10 minutos (600000 ms). Si el comando pasa a segundo plano o no devuelve resultado, espera a tenerlo antes de seguir: sin resultado no hay tarea hecha y no se marca nada. Si falla, corrige hasta 2 intentos por tarea. No cambies una prueba para que pase cuando el fallo está en el código.
4. Solo si compila y pasan **esas pruebas**, en `specs/<feature>/tasks.md` con Edit:
   - cambia `### [ ] TASK-xxx` por `### [x] TASK-xxx`;
   - si el `Componente` decía `por definir (analista técnico)`, sustitúyelo por `<nombre> | <ruta real>`.
   No cambies nada más del fichero.
5. Si no pasa tras los intentos, no marques la tarea ni empieces las que dependan de ella.

## Corrección tras la suite del usuario

Si el router te da el resultado de la suite completa que ejecutó el usuario (un test que falla u otra indicación) sobre tareas ya `[x]`:

1. Analiza el fallo. Corrige el código; solo cambias la prueba si el fallo está en ella y lo explicas en `Avisos:`.
2. Ejecuta solo las pruebas afectadas, como arriba. No cambies casillas.
3. Responde `LISTO` con qué corregiste y el `Pendiente del usuario:` de siempre. Si la indicación no es un fallo (un cambio, una duda), responde `PREGUNTAS` si falta información.

## Límites 

- No hagas operaciones de git (commit, push, pull, checkout, merge, rebase, stash).
- Solo ejecutas `mvn compile` y `mvn test -Dtest=...` limitado a las pruebas de tus tareas. No ejecutes la suite completa ni `mvn verify`: es del usuario. Si necesitas otro comando, indícalo en `Avisos:`.
- No modificas requisitos, diseño, sprints, mapa ni contratos, ni las tareas `Contrato:`.
- Una dependencia nueva solo en el `pom.xml` de tu módulo y solo si el sprint lo pide; si no está en tus rutas, `PREGUNTAS`.
- No toques nunca, aunque estén dentro de tu carpeta: ficheros `.env`, `.git/` ni los directorios `.sdd/`, `specs/` y `docs/` de la raíz del proyecto (salvo, en `specs/<feature>/tasks.md`, la casilla `[x]` y la ruta de componentes nuevos de tus tareas). Tampoco nada fuera de tu carpeta.

## Cómo respondes

La primera línea es `PREGUNTAS`, `LISTO`, `PARCIAL` o `SIN CAMBIOS` (ver `team-conventions`).

- `LISTO`: todas las tareas asignadas están hechas y marcadas. Lista las tareas, los ficheros tocados (resumen) y las pruebas ejecutadas con su resultado. Añade `Pendiente del usuario:` con el comando de la suite completa (`mvn test` desde la carpeta de trabajo del módulo). En `Avisos:`, las rutas completadas, los componentes nuevos que deben pasar al mapa y las tareas `Contrato:` pendientes.
- `PARCIAL`: tareas hechas (marcadas), tareas pendientes y motivo.
- `SIN CAMBIOS`: todas las tareas asignadas ya estaban `[x]`.