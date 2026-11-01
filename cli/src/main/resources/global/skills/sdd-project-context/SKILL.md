---
name: sdd-project-context
description: Comprueba que el proyecto tiene .sdd/project.md (existe, no está vacío y tiene las secciones obligatorias) y guía su creación. Úsala antes de cualquier fase SDD.
---

# Contexto del proyecto SDD

Los agentes SDD necesitan el fichero `.sdd/project.md` en la raíz del proyecto (el directorio de trabajo actual). Esta skill define cómo comprobarlo y qué hacer si falla.

## Comprobación

Lee `.sdd/project.md` con Read y su ruta exacta (nunca con Glob ni Grep, que pueden omitir los directorios que empiezan por punto) y verifica, en este orden:

1. El fichero existe.
2. No está vacío. Se ignoran espacios, saltos de línea y comentarios HTML (`<!-- ... -->`).
3. Contiene estos cuatro encabezados de nivel 2, con el mismo texto, tildes incluidas:
   - `## Nombre del proyecto`
   - `## Descripción`
   - `## Alcance`
   - `## Stack tecnológico`
4. Cada uno de los cuatro tiene al menos una línea de contenido propio antes del siguiente encabezado. Una línea vacía o un comentario HTML no cuenta como contenido.

Se permiten secciones adicionales y no se validan. No juzgues la calidad del contenido: comprueba solo lo anterior.

## Si la comprobación falla

No hagas nada más. No deleges en otros agentes, no crees ni modifiques ficheros y no intentes rellenar el documento.

Responde solo con estos cuatro puntos, de forma breve:

- Qué falla: falta el fichero, está vacío, o qué secciones faltan o están vacías.
- Dónde debe estar: `.sdd/project.md`, en la raíz del proyecto.
- Dónde está la guía y la plantilla: `~/.claude/skills/sdd-project-context/template.md`.
- Una sugerencia mínima: copiar la plantilla a `.sdd/project.md` y rellenar las cuatro secciones.

## Si la comprobación pasa

Continúa con la tarea que te hayan encargado. Usa el contenido del fichero como contexto del proyecto, sin repetirlo ni resumirlo al usuario.

## Búsqueda de ficheros

Glob y Grep sirven para localizar candidatos, nunca para concluir que algo no existe: según la herramienta, pueden omitir los directorios ocultos (los que empiezan por punto) y los ignorados por `.gitignore`.

- Para saber si un fichero concreto existe, usa Read con su ruta exacta. Si Read dice que no existe, entonces no existe.
- Para listar una carpeta (por ejemplo `specs/` o `docs/sprints/<layer>/<modulo>/`): si tu herramienta Read admite carpetas (OpenCode), úsala; si no (Claude Code), usa Glob con la ruta de esa carpeta.
- Si un listado sale vacío o falta algo que debería estar, no concluyas que no existe: confírmalo con Read sobre una ruta exacta conocida o pregunta al usuario.