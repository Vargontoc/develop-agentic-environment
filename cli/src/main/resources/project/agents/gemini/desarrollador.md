---
name: desarrollador-{module}
description: Desarrollador del módulo {module}. Implementa las tareas de un sprint en el código del módulo, ejecuta sus pruebas y las marca como hechas en tasks.md. Lo invoca desarrollador-router; no hace operaciones de git.
mainAgent: false
subagent: true
model: pro
commandExecutionPolicy: auto
tools:
  - view_file
  - list_dir
  - grep_search
  - find_by_name
  - write_to_file
  - replace_file_content
  - multi_replace_file_content
  - run_command
---