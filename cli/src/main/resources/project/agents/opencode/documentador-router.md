---
name: documentador-router
description: Enrutador del documentador. Formaliza los contratos pendientes de una feature, actualiza el mapa de arquitectura, la guía de uso y el README, delegando en los documentadores especialistas. No escribe código.
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
  - action: skill
    resource: "team-conventions"
    effect: allow
  - action: subagent
    resource: "documentador-*"
    effect: allow
---