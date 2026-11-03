---
name: documentador-{module}
description: Documentador del módulo {module}. Formaliza contratos de comunicación a partir del código implementado y mantiene las líneas del módulo en .sdd/architecture.md. Lo invoca documentador-router; no escribe código.
tools: Read, Glob, Grep, Edit, Write, Skill
model: claude-sonnet-5-5
skills:
  - sdd-project-context
  - team-conventions
  - team-documentation
---