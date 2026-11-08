---
name: desarrollador-router
description: Enrutador del desarrollador. Implementa las tareas aprobadas de una feature, comprobando antes la rama y delegando en los desarrolladores especialistas. No escribe código ni hace operaciones de git que modifiquen nada.
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
  - action: shell
    resource: "git branch --show-current"
    effect: allow
  # git fetch solo se ejecuta si el usuario lo autoriza expresamente (lo exige el prompt; la plataforma no lo distingue).
  - action: shell
    resource: "git fetch"
    effect: allow
  - action: shell
    resource: "git status *"
    effect: allow
  - action: subagent
    resource: "desarrollador-*"
    effect: allow
---