---
name: docs-readme
description: Guía para crear y mantener el bloque SDD del README.md (qué es, alcance, tecnologías, organización y funcionalidades disponibles) en lenguaje de usuario, a partir de SDD, la documentación y las evidencias del repositorio, sin tocar lo que el usuario escribe fuera del bloque. La invoca el documentador responsable del README (o el subagente docs-readme) cuando se pida crear o actualizar el README.
---

# README del proyecto (SDD)

Creas y mantienes un bloque delimitado dentro de `README.md` que explica el proyecto a una persona no técnica: qué es, qué hace y con qué está hecho. Todo lo que el usuario escribe fuera del bloque (cómo compilar, ejecutar, desplegar y probar, entre otros) es suyo: no lo escribes ni lo tocas. No requiere una feature activa ni un `tasks.md` aprobado: es mantenimiento general del proyecto.

## Antes de escribir

1. Comprueba `.sdd/project.md` con la skill `sdd-project-context`. Si falla, sigue sus instrucciones y termina.
2. Lee con Read y la ruta exacta `.sdd/project.md` y `.sdd/architecture.md` (si Read dice que no existe, no hay mapa; no lo busques con Glob ni Grep) y, de cada carpeta `specs/<feature>/` (lista specs/ según "Búsqueda de ficheros"), `requirements.md` y `tasks.md`.
3. Fuentes de evidencia, según quién te invoque: el `documentador` puede leer además la documentación (`docs/contracts/`, `docs/guide/`, sprints con su `## Validación`, `docs/validation/`) y los ficheros de construcción para las tecnologías (`pom.xml`, `package.json` y similares). No explores el código fuente más allá de lo necesario para confirmar una afirmación. El subagente `docs-readme` del flujo SDD se limita a `.sdd/` y `specs/`.
4. Lee el `README.md` actual si existe.
5. Idioma: el configurado por el usuario en el encargo; si no, el del README existente; si no, el de `.sdd/project.md`; por defecto, español.

## Qué funcionalidades se presentan como disponibles

Un README público no presenta como operativo lo que solo está especificado o no ha superado la validación necesaria. Clasifica cada feature por evidencia, no por estar aprobada en SDD:

| Estado | Evidencia | Dónde aparece |
|---|---|---|
| Implementada y validada | `tasks.md` con tareas y todas `### [x]`, y validación `correcta` (suite del módulo en `## Validación` del sprint o en `docs/validation/`) | "Qué puede hacer hoy" |
| Implementada, pendiente de validación | Todas las tareas `[x]` pero validación `pendiente` o `fallida`, o sin registro | "En validación", solo si el usuario lo acepta; por defecto, solo en `Avisos:` |
| Planificada | Con alguna `### [ ]`, o sin `tasks.md` | No aparece; va en `Avisos:` con las tareas que faltan |

Si el proyecto no usa sprints ni registro de validación (no hay evidencia posible), trata como validada la feature con todas las tareas `[x]` y dilo en `Avisos:`. Nunca marques ni desmarques casillas.

## Cómo redactar

- Para una persona sin conocimientos técnicos: frases cortas y llanas, sin siglas ni jerga. Nada de IDs (`REQ-`, `TASK-`, `DEC-`), rutas de ficheros, versiones ni configuración. Nombra las tecnologías por su nombre, sin más.
- Escribe solo lo que consta en las fuentes. No inventes capacidades ni anticipes lo que aún no está hecho.
- Describe cada funcionalidad por lo que permite hacer a quien usa el proyecto, no por cómo funciona por dentro. Sale de la sección `Contexto` y de los requisitos de su `requirements.md`.
- Escribe en el idioma indicado en "Antes de escribir".
- Adapta las secciones al tipo de proyecto (librería, aplicación, servicio, herramienta de línea de comandos): omite las que no apliquen y no inventes datos de instalación o uso que no consten en las fuentes.

## Formato del bloque

El bloque va entre estas dos marcas, exactas, cada una en su propia línea:

```markdown
<!-- sdd:start -->
<!-- Bloque generado por el agente docs. Se reescribe entero: escribe fuera de las marcas. -->

## Qué es

<Nombre del proyecto y qué es, en 2 a 4 frases, a partir de las secciones Nombre del proyecto y Descripción de project.md.>

## Qué incluye y qué no

**Incluye:**
- <de la sección Alcance de project.md>

**No incluye:**
- <de la sección Alcance, si lo menciona>

## Con qué está hecho

- <tecnología>: <para qué se usa, en pocas palabras, a partir de la sección Stack tecnológico de project.md>

## Cómo está organizado

<Una frase o una lista corta: las partes del proyecto y qué hace cada una, en lenguaje llano, a partir de architecture.md.>

## Qué puede hacer hoy

### <Nombre legible de la funcionalidad>

<1 a 3 frases: qué permite hacer.>

<!-- sdd:end -->
```

- Estos encabezados son la estructura por defecto; puedes renombrarlos al idioma elegido o añadir una sección propia del tipo de proyecto (por ejemplo "Cómo usarla", solo con datos de la guía de uso).
- Si no hay mapa, omite la sección "Cómo está organizado" y avísalo.
- Si no hay ninguna funcionalidad terminada, la sección "Qué puede hacer hoy" dice "Todavía no hay funcionalidades terminadas.".
- Si `project.md` no menciona qué no incluye, omite esa parte.
- El nombre legible de una funcionalidad sale del título de su `requirements.md`, sin el prefijo "Requisitos:".

## Dónde escribir

Dos modos. **Creación** (no existe README): puedes proponer una estructura completa. **Mantenimiento** (existe): solo modificas el bloque entre las marcas; nunca reescribes libremente el README.

- **No existe `README.md`:** créalo con `# <Nombre del proyecto>`, una línea en blanco y el bloque.
- **Existe con las dos marcas, una vez cada una y en orden:** sustituye solo el texto entre `<!-- sdd:start -->` y `<!-- sdd:end -->` (marcas incluidas) usando Edit. Nunca uses Write sobre un README existente: lo que el usuario escribió fuera del bloque debe quedar intacto, carácter por carácter.
- **Existe sin ninguna marca:** añade al final el bloque, tras una línea en blanco, usando Edit y sin tocar nada más. Avísalo en `Avisos:` por si el usuario prefiere moverlo.
- **Marcas incompletas, repetidas o en orden inverso:** no escribas nada y responde `PREGUNTAS` explicando el problema y proponiendo cómo corregirlo.
- **El contenido manual y el generado se solapan** (el usuario escribió dentro del bloque, o duplica lo de fuera): no decidas; responde `PREGUNTAS` con las dos versiones y tu propuesta.
- Antes de escribir, vuelve a leer el README: si cambió desde que lo leíste, no escribas y responde `BLOQUEADO` (`CAMBIO_CONCURRENTE`).
- Si el bloque resultante es idéntico al que ya hay, no escribas y responde `SIN CAMBIOS`.

## Cómo respondes al router

La primera línea es `PREGUNTAS`, `LISTO`, `SIN CAMBIOS` o `BLOQUEADO` (formato de `team-conventions` si lo usa el documentador).

- `PREGUNTAS`: lista numerada de problemas, cada uno con su propuesta. No has escrito nada.
- `LISTO`: dónde escribiste (README creado, bloque actualizado o bloque añadido al final), qué funcionalidades incluye y una línea `Avisos:` si procede (features excluidas con tareas pendientes o sin validar, sin mapa, bloque añadido por no haber marcas, idioma elegido).
- `SIN CAMBIOS`: una frase explicando por qué.

El README no tiene fase de aprobación: recomienda al usuario revisar el cambio directamente en el fichero antes de hacer el commit.