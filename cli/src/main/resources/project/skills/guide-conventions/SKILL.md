---
name: guide-conventions
description: Convenciones de la guía de uso del módulo (docs/guide/<modulo>/), redactada por el documentador a partir de los ejemplos del desarrollador. Úsala solo en el documentador especialista, en modo guía.
---

# Guía de uso del módulo

La guía explica cómo usar los componentes del módulo a quien los consume. Se redacta **solo** a partir de `specs/<feature>/<modulo>/example.md` (ver `example-conventions`): en modo guía no lees código. Si falta información, no la inventes: responde `PREGUNTAS` o dilo en `Avisos:`.

## Estructura

```
docs/guide/<modulo>/
├── manual-uso.md            índice de áreas del módulo
└── <area>/
    ├── indice.md            qué hay en el área y enlaces a cada componente
    └── <componente>.md      un fichero por componente
```

- Un área agrupa componentes que se usan juntos (por responsabilidad, no por paquete). Nómbrala en minúsculas, con guiones.
- `manual-uso.md` solo enlaza las áreas con una frase cada una.

## Fichero de componente

```markdown
# <NombreDelComponente>

<Para qué sirve, 1 a 3 frases.>

## Uso

<ejemplo de código copiado de example.md, sin cambios de significado>

## Notas

<límites, errores y requisitos previos, si constan>
```

## Reglas

- Describe lo que hace hoy el componente según sus ejemplos y su tarea `[x]`. No describas lo solo planificado.
- Si ya existe la guía, actualiza solo lo afectado y conserva el resto. No borres componentes: si ya no constan en los ejemplos, indícalo en `Avisos:`.
- Cuando el fichero esté escrito, marca `### [x]` en `specs/<feature>/tasks.md` solo en tus tareas `Guía:` (relee el fichero antes de escribir).
- La tarea `Guía:` depende de la del ejemplo (`Depende de`): si esa sigue `[ ]`, responde `PARCIAL`.
