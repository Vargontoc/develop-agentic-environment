---
name: sdd-requirements
description: Subagente de la fase de requisitos de SDD. Crea o revisa specs/<feature>/requirements.md en formato EARS. Lo invoca sdd-agent; no escribe código.
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
