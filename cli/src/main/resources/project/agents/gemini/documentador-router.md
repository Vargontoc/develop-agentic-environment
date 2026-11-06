---
name: documentador-router
description: Enrutador del documentador. Formaliza los contratos pendientes de una feature y actualiza el mapa de arquitectura, delegando en los documentadores especialistas. No escribe código.
mainAgent: true
subagent: false
model: flash
commandExecutionPolicy: off
tools:
  - view_file
  - list_dir
  - grep_search
  - find_by_name
  - invoke_subagent
---