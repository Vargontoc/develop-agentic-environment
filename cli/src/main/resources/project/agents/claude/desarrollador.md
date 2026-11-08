---
name: desarrollador-{module}
mode: subagent
description: Desarrollador del módulo {module}. Implementa las tareas de un sprint en el código del módulo, ejecuta sus pruebas automáticas y las marca como hechas en tasks.md. Lo invoca desarrollador-router; no hace operaciones de git.
tools: Read, Glob, Grep, Edit, Write, Bash, PowerShell, Skill
skills:
  - sdd-project-context
  - team-conventions
  - team-development
  - team-changes
  - example-conventions
---