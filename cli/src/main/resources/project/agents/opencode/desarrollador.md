---
name: desarrollador-{module}
description: Desarrollador del framework del módulo {module}. Implementa las tareas de un sprint en el código del módulo, ejecuta sus pruebas y las marca como hechas en tasks.md. Lo invoca desarrollador-router; no hace operaciones de git.
mode: subagent
model: openai/gpt-5.6-terra
permissions:
  - action: "*"
    resource: "*"
    effect: deny
  - action: read
    resource: "*"
    effect: allow
  - action: glob
    resource: "*"
    effect: allow
  - action: grep
    resource: "*"
    effect: allow
  - action: read
    resource: "*.env"
    effect: deny
  - action: read
    resource: "*.env.*"
    effect: deny
  - action: skill
    resource: "sdd-project-context"
    effect: allow
  - action: skill
    resource: "team-conventions"
    effect: allow
  - action: skill
    resource: "team-development"
    effect: allow
  - action: skill
    resource: "team-changes"
    effect: allow
  - action: skill
    resource: "example-conventions"
    effect: allow
  - action: edit
    resource: "*specs/*/tasks.md"
    effect: allow
  - action: edit
    resource: "*specs/*/{module}/example.md"
    effect: allow
  - action: edit
    resource: "*docs/sprints/*"
    effect: allow
  - action: edit
    resource: "*docs/changes/*"
    effect: allow
  - action: edit
    resource: "*docs/validation/*"
    effect: allow
  # Los permisos de docs/ y specs/ son por fichero, no por sección: que solo se toque la sección permitida (casilla [x], ## Validación) lo garantiza el prompt, no la plataforma.
  # Rutas de código del módulo: las mismas que en .sdd/team.md. Añade las que falten (recursos, pom.xml del módulo).
---