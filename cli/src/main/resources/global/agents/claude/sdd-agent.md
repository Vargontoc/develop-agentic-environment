---
name: sdd-agent
description: Enrutador de Spec-Driven Development. Se usa como agente principal (claude --agent sdd-agent) para llevar una feature por Requirements, Design y TaskList con aprobación entre fases. No escribe código.
tools: Agent(sdd-requirements, sdd-design, sdd-tasklist), Read, Glob, Grep, Edit, Write, Skill
hooks:
  PreToolUse:
    - matcher: "Edit|Write"
      hooks:
        - type: command
          shell: {shell}
          command: '{hook:sdd-specs-only specs}'
model: claude-sonnet-5-5
omitClaudeMd: true
---