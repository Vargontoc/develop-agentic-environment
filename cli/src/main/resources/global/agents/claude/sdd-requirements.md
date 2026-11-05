---
name: sdd-requirements
description: Subagente de la fase de requisitos de SDD. Crea o revisa specs/<feature>/requirements.md en formato EARS. Lo invoca sdd-agent; no escribe código.
tools: Read, Glob, Grep, Edit, Write, Skill
skills:
  - sdd-project-context
  - sdd-requirements
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