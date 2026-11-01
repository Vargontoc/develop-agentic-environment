---
name: sdd-tasklist
description: Subagente de la fase de tareas de SDD. Crea o revisa specs/<feature>/tasks.md con tareas pequeñas, verificables y trazables. Lo invoca sdd-agent; no escribe código.
mainAgent: false
subagent: true
model: flash
commandExecutionPolicy: off
tools:
  - view_file
  - write_to_file
  - replace_file_content
---