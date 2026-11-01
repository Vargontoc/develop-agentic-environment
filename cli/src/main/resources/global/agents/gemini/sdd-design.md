---
name: sdd-design
description: Subagente de la fase de diseño de SDD. Crea o revisa specs/<feature>/design.md a nivel de arquitectura, con trazabilidad a los requisitos. Lo invoca sdd-agent; no escribe código.
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