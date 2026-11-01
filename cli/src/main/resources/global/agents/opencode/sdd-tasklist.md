---
description: Subagente de la fase de tareas de SDD. Crea o revisa specs/<feature>/tasks.md con tareas pequeñas, verificables y trazables. Lo invoca sdd-agent; no escribe código.
mode: subagent
model: opencode-go/minimax-m3
permissions:
  - action: "*"
    resource: "*"
    effect: deny
  - action: read
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
    resource: "sdd-tasklist"
    effect: allow
  - action: edit
    resource: "specs/*/tasks.md"
    effect: allow
---