---
description: Subagente de la fase de requisitos de SDD. Crea o revisa specs/<feature>/requirements.md en formato EARS. Lo invoca sdd-agent; no escribe código.
mode: subagent
model: opencode-go/kimi-k2.7-code
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
    resource: "sdd-requirements"
    effect: allow
  - action: edit
    resource: "specs/*/requirements.md"
    effect: allow
---