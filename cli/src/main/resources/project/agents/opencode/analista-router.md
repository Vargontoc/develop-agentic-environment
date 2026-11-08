---
description: Enrutador del analista técnico. Convierte las tareas aprobadas de una feature en sprints delegando en los analistas especialistas. No escribe código.
mode: primary
model: opencode-go/qwen3.7-plus
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
  - action: edit
    resource: "docs/sprints/*"
    effect: allow
  - action: edit
    resource: "docs/changes/*"
    effect: allow
  - action: subagent
    resource: "analista-*"
    effect: allow
---