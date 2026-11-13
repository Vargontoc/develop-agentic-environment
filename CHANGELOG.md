# Changelog

Todos los cambios relevantes de este proyecto se documentan aquí.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[versionado semántico](docs/versionado.md). La CLI y el visor se publican siempre juntos y con el mismo número.

## [Sin publicar]

## [1.0.0] - pendiente de fecha

Primera versión estable.

### Añadido

#### CLI

- Menú interactivo y comandos no interactivos que devuelven **un único documento JSON** (contrato `cli-json`,
  `schemaVersion` 1). Códigos de salida: `0` correcto, `1` error esperado, `2` uso incorrecto, `3` error interno.
- Instalación de agentes, skills, perfiles y hooks globales en **Claude, Codex, Gemini y OpenCode**
  (`configure-provider`). No pisa lo que ya existe sin permiso y guarda copia del contenido sobrescrito.
- Preparación de proyectos con la estructura `.sdd` (`init`) y lectura del estado (`status`, `tree`, `describe`).
- Gestión del equipo: módulos y técnicos (`create-module`, `add-technician`) y lectura y escritura de `.sdd/team.md`
  (`team`, `team-update`), con simulación previa (`--dry-run`) y detección de cambios externos.
- Catálogo y creación de agentes y skills desde plantillas (`catalog`, `create-artifact`).
- Guardado seguro de artefactos con control de conflictos, diff y propagación del cuerpo compartido a su grupo
  (`save`), y vuelta al grupo de artefactos personalizados o desvinculados (`adopt`).
- Hook `sdd-specs-only` para Linux (`.sh`) y Windows (`.bat`). Un hook modificado por el usuario no se sobrescribe.
- Jar ejecutable único (`agentic-develop.jar`).

#### Visor de escritorio

- Árbol de navegación de artefactos globales, de proyecto y de módulo, con búsqueda y agrupación por tipo.
- Vista y edición de agentes, skills y ficheros `.sdd`, con aviso de cambios sin guardar y recuperación de borradores.
- Creación guiada de módulos, técnicos y artefactos, con revisión previa de lo que se va a escribir.
- Revisión de diferencias antes de propagar cambios a un grupo y vigilancia de cambios externos de ficheros.
- Consola integrada con el menú interactivo de la CLI.
- Comprobación de compatibilidad entre el visor y la CLI al arrancar (versión y `schemaVersion`).

#### Distribución

- Aplicación empaquetada con su propio runtime de Java: no requiere Java instalado ni conexión a internet.
- Instaladores: `.msi` para Windows y `.deb` para Linux (x64), además del jar de la CLI.
- Cada Release incluye las sumas SHA-256 de todos los ficheros.

### Notas

- El instalador de Windows **no está firmado**: Windows SmartScreen mostrará un aviso al ejecutarlo.
- No hay instalador para arquitecturas ARM.

[Sin publicar]: https://github.com/Vargontoc/develop-agentic-environment/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Vargontoc/develop-agentic-environment/releases/tag/v1.0.0
