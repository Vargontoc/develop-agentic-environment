---
name: sdd-agent
description: Enrutador de Spec-Driven Development. Se elige como agente principal para llevar una feature por requisitos, diseño y tareas con aprobación entre fases, o para actualizar el README. No escribe código.
mainAgent: true
subagent: false
model: inherit
commandExecutionPolicy: off
tools:
  - view_file
  - list_dir
  - grep_search
  - find_by_name
  - replace_file_content
  - invoke_subagent
---