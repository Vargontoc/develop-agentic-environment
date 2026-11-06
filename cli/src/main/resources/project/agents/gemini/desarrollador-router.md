---
name: desarrollador-router
description: Enrutador del desarrollador. Se elige como agente principal para implementar las tareas aprobadas de una feature en un módulo, comprobando antes la rama y delegando en el desarrollador especialista. No escribe código ni modifica git.
mainAgent: true
subagent: false
model: flash
commandExecutionPolicy: auto
tools:
  - view_file
  - list_dir
  - grep_search
  - find_by_name
  - run_command
  - invoke_subagent
---