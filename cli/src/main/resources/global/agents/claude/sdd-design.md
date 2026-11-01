---
name: sdd-design
description: Subagente de la fase de diseño de SDD. Crea o revisa specs/<feature>/design.md a nivel de arquitectura, con trazabilidad a los requisitos. Lo invoca sdd-agent; no escribe código.
tools: Read, Glob, Edit, Write, Skill
skills:
  - sdd-project-context
  - sdd-design
hooks:
  PreToolUse:
    - matcher: "Edit|Write"
      hooks:
        - type: command
          shell: powershell
          command: '& "$env:USERPROFILE\.claude\hooks\sdd-specs-only.bat" specs; exit $LASTEXITCODE'
model: claude-sonnet-5-5
omitClaudeMd: true
---