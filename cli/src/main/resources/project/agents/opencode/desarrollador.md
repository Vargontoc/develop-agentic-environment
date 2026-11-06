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
  # Rutas de código del módulo: las mismas que en .sdd/team.md. Añade las que falten (recursos, pom.xml del módulo).
---