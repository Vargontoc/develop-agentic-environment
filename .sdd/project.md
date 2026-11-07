## Nombre del proyecto

develop-agentic-environment (repositorio `develop-agentic-environment`).

## Descripción

Aplicación para instalar y mantener un entorno agéntico de desarrollo con humano en el bucle (Human-In-The-Loop). Instala y mantiene los agentes y skills de Spec-Driven Development (requisitos, diseño y tareas) en los proveedores del usuario (Claude, Codex, Gemini y OpenCode), prepara proyectos con la estructura `.sdd` y genera los agentes de cada módulo y técnico. Está pensada para desarrolladores que usan agentes de IA con especificaciones aprobadas por una persona entre fases.

## Alcance

Incluye:
- `cli/`: CLI con menú interactivo y comandos no interactivos con salida JSON (contrato `cli-json` v1). Instala agentes, skills y perfiles globales, y crea proyectos, módulos y técnicos.
- `viewer/`: visor de escritorio que consume la CLI mediante `cli-json`. Árbol de artefactos, edición, consola integrada, propagación de cambios entre grupos de artefactos y empaquetado como app-image.
- Contratos documentados en `docs/contracts/` y especificaciones en `specs/`.
- Recursos de los agentes, skills, perfiles y hooks que la CLI instala (`cli/src/main/resources`).

Fuera de alcance:
- Escribir el código de los proyectos del usuario: los agentes SDD solo producen especificaciones.
- Un servicio remoto o multiusuario: todo se ejecuta en el equipo del usuario.
- Gestionar los permisos de proyecto de cada herramienta: se configuran manualmente.

## Stack tecnológico

- Java 21 y Maven 3.9+.
- CLI: jar ejecutable (`cli/target/agentic-develop.jar`) con todas sus dependencias; usa `framework-core` y `framework-json` de `vargontoc-framework` (0.1.0-SNAPSHOT, en `cli/external-libs/`) y SLF4J.
- Viewer: JavaFX 21, consola PTY con pty4j, empaquetado con jlink y jpackage (app-image sin Java del sistema).
- Sistemas soportados: Windows (PowerShell, scripts `.bat`) y Linux (bash, scripts `.sh`).
- Formatos: Markdown con cabecera YAML para Claude, Gemini y OpenCode; TOML para Codex.
