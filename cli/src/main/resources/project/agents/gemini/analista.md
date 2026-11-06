---
name: analista-{module}
description: Analista técnico del modulo {module}. Crea o revisa sprints en docs/sprints/{module} a partir de las tareas de una feature. Lo invoca analista-router; no escribe código.
mainAgent: false
subagent: true
model: pro
commandExecutionPolicy: off
tools:
  - view_file
  - list_dir
  - grep_search
  - find_by_name
  - write_to_file
  - replace_file_content
---