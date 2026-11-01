---
name: docs-agent-readme
description: Subagente que mantiene el bloque SDD del README.md (descripción, alcance, tecnologías, organización y funcionalidades terminadas) en lenguaje de usuario. Solo se invoca a petición del usuario a través de sdd-agent; no escribe código ni contratos.
mode: subagent
model: opencode-go/glm-5.3-flash
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
    resource: "docs-readme"
    effect: allow
  - action: edit
    resource: "README.md"
    effect: allow
---