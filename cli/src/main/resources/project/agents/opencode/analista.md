---
name: analista-{module}
description: Analista técnico de framework modulo {module}. Crea o revisa sprints en docs/sprints/{module} a partir de las tareas de una feature. Lo invoca analista-router; no escribe código.
mode: subagent
model: opencode-go/deepseek-v4-pro
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
    resource: "team-sprints"
    effect: allow
  - action: edit
    resource: "*docs/sprints/*"
    effect: allow
---