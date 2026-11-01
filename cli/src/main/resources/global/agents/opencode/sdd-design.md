---
description: Subagente de la fase de diseño de SDD. Crea o revisa specs/<feature>/design.md a nivel de arquitectura, con trazabilidad a los requisitos. Lo invoca sdd-agent; no escribe código.
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
    resource: "sdd-design"
    effect: allow
  - action: edit
    resource: "specs/*/design.md"
    effect: allow
---