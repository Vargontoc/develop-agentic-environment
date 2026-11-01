---
name: sdd-tasklist
description: Subagente de la fase de tareas de SDD. Crea o revisa specs/<feature>/tasks.md con tareas pequeñas, verificables y trazables. Lo invoca sdd-agent; no escribe código.
tools: Read, Edit, Write, Skill
skills:
  - sdd-project-context
  - sdd-tasklist
hooks:
  PreToolUse:
    - matcher: "Edit|Write"
      hooks:
        - type: command
          shell: powershell
          command: '& "$env:USERPROFILE\.claude\hooks\sdd-specs-only.bat" specs; exit $LASTEXITCODE'
omitClaudeMd: true
model: claude-haiku-5-5
---