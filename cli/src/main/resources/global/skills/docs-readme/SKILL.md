---
name: docs-readme
description: Guía para mantener el bloque SDD del README.md (qué es, alcance, tecnologías, organización y funcionalidades terminadas) en lenguaje de usuario, sin tocar lo que el usuario escribe fuera del bloque. Úsala solo cuando se pida actualizar el README.
---

# README del proyecto (SDD)

Mantienes un bloque delimitado dentro de `README.md` que explica el proyecto a una persona no técnica: qué es, qué hace y con qué está hecho. Todo lo demás del README (cómo compilar, ejecutar, desplegar y probar) es del proyecto y del usuario: no lo escribes ni lo tocas.

## Antes de escribir

1. Comprueba `.sdd/project.md` con la skill `sdd-project-context`. Si falla, sigue sus instrucciones y termina.
2. Lee con Read y la ruta exacta `.sdd/project.md` y `.sdd/architecture.md` (si Read dice que no existe, no hay mapa; no lo busques con Glob ni Grep) y, de cada carpeta `specs/<feature>/` (lista specs/ según "Búsqueda de ficheros"), `requirements.md` y `tasks.md`. No explores el resto del repositorio ni leas código.
3. Lee el `README.md` actual si existe.

## Qué funcionalidades cuentan como terminadas

Una feature se incluye solo si su `tasks.md` existe, tiene al menos una tarea y TODAS las casillas de sus encabezados (`### [x]`) están marcadas. Si tiene alguna `### [ ]`, o no tiene `tasks.md`, no se incluye. Nunca marques ni desmarques casillas. Lista en `Avisos:` las features excluidas y cuántas tareas les faltan.

## Cómo redactar

- Para una persona sin conocimientos técnicos: frases cortas y llanas, sin siglas ni jerga. Nada de IDs (`REQ-`, `TASK-`, `DEC-`), rutas de ficheros, versiones ni configuración. Nombra las tecnologías por su nombre, sin más.
- Escribe solo lo que consta en las fuentes. No inventes capacidades ni anticipes lo que aún no está hecho.
- Describe cada funcionalidad por lo que permite hacer a quien usa el proyecto, no por cómo funciona por dentro. Sale de la sección `Contexto` y de los requisitos de su `requirements.md`.
- Escribe en español.

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

- Si no hay mapa, omite la sección "Cómo está organizado" y avísalo.
- Si no hay ninguna funcionalidad terminada, la sección "Qué puede hacer hoy" dice "Todavía no hay funcionalidades terminadas.".
- Si `project.md` no menciona qué no incluye, omite esa parte.
- El nombre legible de una funcionalidad sale del título de su `requirements.md`, sin el prefijo "Requisitos:".

## Dónde escribir

- **No existe `README.md`:** créalo con `# <Nombre del proyecto>`, una línea en blanco y el bloque.
- **Existe con las dos marcas, una vez cada una y en orden:** sustituye solo el texto entre `<!-- sdd:start -->` y `<!-- sdd:end -->` (marcas incluidas) usando Edit. Nunca uses Write sobre un README existente: lo que el usuario escribió fuera del bloque debe quedar intacto, carácter por carácter.
- **Existe sin ninguna marca:** añade al final el bloque, tras una línea en blanco, usando Edit y sin tocar nada más. Avísalo en `Avisos:` por si el usuario prefiere moverlo.
- **Marcas incompletas, repetidas o en orden inverso:** no escribas nada y responde `PREGUNTAS` explicando el problema y proponiendo cómo corregirlo.
- Si el bloque resultante es idéntico al que ya hay, no escribas y responde `SIN CAMBIOS`.

## Cómo respondes al router

La primera línea es `PREGUNTAS`, `LISTO` o `SIN CAMBIOS`.

- `PREGUNTAS`: lista numerada de problemas, cada uno con su propuesta. No has escrito nada.
- `LISTO`: dónde escribiste (README creado, bloque actualizado o bloque añadido al final), qué funcionalidades incluye y una línea `Avisos:` si procede (features excluidas con tareas pendientes, sin mapa, bloque añadido por no haber marcas).
- `SIN CAMBIOS`: una frase explicando por qué.

El README no tiene fase de aprobación: el usuario revisa el cambio directamente en el fichero.