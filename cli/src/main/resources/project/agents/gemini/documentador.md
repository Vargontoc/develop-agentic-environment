---
name: documentador-{module}
description: Documentador del modulo {module}. Formaliza contratos de comunicación a partir del código implementado mantiene las líneas del módulo en .sdd/architecture.md y, si es el responsable (readme-owner), crea o actualiza el README. Lo invoca documentador-router; no escribe código.
mainAgent: false
subagent: true
model: flash
commandExecutionPolicy: off
tools:
  - view_file
  - list_dir
  - grep_search
  - find_by_name
  - write_to_file
  - replace_file_content
  - multi_replace_file_content
---