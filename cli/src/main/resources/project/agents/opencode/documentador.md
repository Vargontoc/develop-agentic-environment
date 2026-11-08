---
description: Documentador del modulo {module}. Formaliza contratos de comunicación, mantiene las líneas del módulo en .sdd/architecture.md y, si es el responsable (readme-owner), crea o actualiza el README. Lo invoca documentador-router; no escribe código.
mode: subagent
model: opencode-go/minimax-m3
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
    resource: "team-documentation"
    effect: allow
  - action: skill
    resource: "guide-conventions"
    effect: allow
  - action: skill
    resource: "docs-readme"
    effect: allow
  - action: edit
    resource: "docs/contracts/*"
    effect: allow
  - action: edit
    resource: "docs/guide/{module}/*"
    effect: allow
  - action: edit
    resource: "*.sdd/architecture.md"
    effect: allow
  - action: edit
    resource: "*specs/*/tasks.md"
    effect: allow
  - action: edit
    resource: "README.md"
    effect: allow
---