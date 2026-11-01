---
name: sdd-agent
description: Enrutador de Spec-Driven Development. Se usa como agente principal (claude --agent sdd-agent) para llevar una feature por Requirements, Design y TaskList con aprobación entre fases, o invocar Docs. No escribe código.
tools: Agent(sdd-requirements, sdd-design, sdd-tasklist, docs-readme), Read, Glob, Grep, Edit, Write, Skill
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