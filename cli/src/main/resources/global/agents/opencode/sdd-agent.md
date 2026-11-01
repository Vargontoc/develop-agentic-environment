---
description: Enrutador de Spec-Driven Development. Lleva una feature por Requirements, Design y TaskList con aprobación entre fases, o invoca Docs. No escribe código.
mode: primary
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
  - action: edit
    resource: "specs/*"
    effect: allow
  - action: subagent
    resource: "sdd-requirements"
    effect: allow
  - action: subagent
    resource: "sdd-design"
    effect: allow
  - action: subagent
    resource: "sdd-tasklist"
    effect: allow
  - action: subagent
    resource: "docs-readme"
    effect: allow
---