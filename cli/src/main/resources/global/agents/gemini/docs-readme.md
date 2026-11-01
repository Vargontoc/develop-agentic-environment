---
name: docs-readme-agent
description: Subagente que mantiene el bloque SDD del README.md (descripción, alcance, tecnologías, organización y funcionalidades terminadas) en lenguaje de usuario. Solo a petición del usuario mediante sdd-agent; no escribe código ni contratos.
mainAgent: false
subagent: true
model: flash
commandExecutionPolicy: off
tools:
  - view_file
  - list_dir
  - find_by_name
  - write_to_file
  - replace_file_content
---