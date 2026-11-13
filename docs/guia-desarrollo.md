# Guía de desarrollo

Esta guía reúne lo necesario para construir, probar y empaquetar el proyecto. Para saber qué es y cómo empezar a usarlo, consulta el [README](../README.md).

## Requisitos

- JDK 21.
- Maven 3.9 o superior.
- Windows o Linux.
- En Linux, dependencias de escritorio para el lanzador nativo:
  - **Ubuntu/Debian:** `sudo apt-get install -y libxtst-dev libxrender-dev libxxf86vm-dev`
  - **Fedora/RHEL:** `sudo dnf install -y libXtst-devel libXrender-devel libXxf86vm-devel`
  - **Arch:** `sudo pacman -S libxtst libxrender libxxf86vm`

## Dependencias externas

La CLI usa `framework-core` y `framework-json` (versión `0.1.0`) de `vargontoc-framework`, publicados en
GitHub Packages (`https://maven.pkg.github.com/Vargontoc/vargontoc-framework`). Maven los descarga solo, pero
GitHub Packages exige credenciales incluso para leer. Añade a `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>TU_USUARIO_GITHUB</username>
      <password>TOKEN_CON_read:packages</password>
    </server>
  </servers>
</settings>
```

El token es un *personal access token (classic)* con el permiso `read:packages`. En CI se usa `GITHUB_TOKEN`.

## Compilar y probar

### CLI

Desde `cli/`:

```powershell
mvn clean test       # compila y ejecuta las pruebas
mvn clean package    # además genera el jar ejecutable: cli/target/agentic-develop.jar
```

El jar incluye todas las dependencias: no hace falta nada más que un JDK 21 para ejecutarlo.

### Viewer

Desde `viewer/`:

```powershell
mvn test             # pruebas de la lógica
mvn javafx:run       # abre la ventana
mvn clean package    # genera el JAR del viewer
```

El viewer espera el jar del CLI en `viewer/cli/agentic-develop.jar`. Para copiarlo desde la raíz:

**Windows:**

```powershell
cd cli
mvn clean package -DskipTests
cd ..
New-Item -ItemType Directory -Force viewer/cli | Out-Null
Copy-Item cli/target/agentic-develop.jar viewer/cli/
```

**Linux:**

```bash
cd cli
mvn clean package -DskipTests
cd ..
mkdir -p viewer/cli
cp cli/target/agentic-develop.jar viewer/cli/
```

## Empaquetar una aplicación de escritorio

El empaquetado genera una app-image que funciona **sin Java instalado** y **sin conexión a internet**.

### Requisitos previos

- JDK 21 o superior con `jlink` y `jpackage` en el `PATH`.
- Maven 3.9 o superior.
- El jar del CLI copiado en `viewer/cli/agentic-develop.jar`.

### Windows

Desde la raíz:

```powershell
cd viewer
..\scripts\package-windows.ps1
```

### Linux

Desde la raíz:

```bash
cd viewer
../scripts/package-linux.sh
```

### Opciones

- `--skip-tests`: omite las pruebas de Maven.
- `--verbose`: muestra detalles adicionales del proceso.
- `--installer` (Windows: `-Installer`): genera además un instalador. En Linux es un `.deb`
  (requiere `dpkg-dev` y `fakeroot`) y en Windows un `.msi` (requiere WiX Toolset 3.x).
  Queda en `viewer/target/dist/installer/`.

### Verificar el paquete

**Windows:**

```powershell
cd viewer
..\scripts\verify-package.ps1
```

**Linux:**

```bash
cd viewer
../scripts/verify-package-linux.sh
```

La verificación comprueba que el runtime Java está incluido, que el JAR del viewer y el de la CLI
están en su sitio y que la aplicación puede iniciar sin Java del sistema.

### Ejecutar la aplicación empaquetada

**Windows:**

```powershell
cd viewer
.\target\dist\windows\AgenticViewer\AgenticViewer.exe
```

**Linux:**

```bash
cd viewer
./target/dist/linux/AgenticViewer/bin/AgenticViewer
```

### Estructura generada

**Windows:**

```
viewer/target/dist/windows/AgenticViewer/
├── AgenticViewer.exe
├── runtime/
├── app/
│   ├── viewer.jar
│   └── cli/
│       └── agentic-develop.jar
└── ...
```

**Linux:**

```
viewer/target/dist/linux/AgenticViewer/
├── bin/
│   └── AgenticViewer
├── runtime/
├── lib/
│   └── app/
│       ├── viewer.jar
│       └── cli/
│           └── agentic-develop.jar
└── ...
```

### Resolución de problemas

- **`jlink` o `jpackage` no encontrados:** instala un JDK 21+ y comprueba que están en el `PATH`.
- **CLI jar not found:** ejecuta antes la compilación del CLI y copia el jar a `viewer/cli/`.
- **`libXtst.so not found` en Linux:** instala las dependencias de escritorio indicadas arriba.
- **La aplicación no inicia:** usa el script de verificación y revisa los logs en
  `viewer/target/package/logs/` si existen.

## Uso de la CLI

Sin argumentos se abre el menú interactivo:

```powershell
java -jar cli/target/agentic-develop.jar
```

1. **Configuración entorno:** instala los agentes y skills globales en los proveedores que elijas.
2. **Iniciar proyecto:** crea la estructura `.sdd` en la ruta indicada.
3. **Crear módulo:** registra un módulo en `.sdd/team.md` y genera los agentes de sus técnicos.
4. **Salir.**

Con argumentos se ejecuta un comando sin menús ni preguntas. La salida estándar es **un único
documento JSON** y los logs van a la salida de error. El código de salida es `0` si todo fue bien,
`1` por un error esperado (validación, no encontrado, conflicto), `2` por un uso incorrecto y `3`
por un error interno.

### CLI en la aplicación empaquetada

La versión empaquetada también incluye el CLI y su propio Java. Para abrir el menú interactivo sin
instalar nada:

**Windows:**

```powershell
cd viewer\target\dist\windows\AgenticViewer
.\runtime\bin\java.exe -jar app\cli\agentic-develop.jar
```

**Linux:**

```bash
cd viewer/target/dist/linux/AgenticViewer
./runtime/bin/java -jar lib/app/cli/agentic-develop.jar
```

```powershell
java -jar cli/target/agentic-develop.jar <comando> [opciones]
java -jar cli/target/agentic-develop.jar --help
```

| Comando | Para qué sirve | Opciones principales |
|---|---|---|
| `status` | Estado de los proveedores y del proyecto. Solo lee. | `--project` |
| `tree` | Árbol de artefactos globales y de proyecto. Solo lee. | `--project` |
| `describe` | Cabecera, cuerpo, hash y estado de un agente o skill. Solo lee. | `--path`, `--project` |
| `init` | Crea `.sdd` en un proyecto. | `--project` |
| `configure-provider` | Instala agentes y skills globales en un proveedor. | `--provider` |
| `create-module` | Registra un módulo y genera sus agentes y skills. | `--project`, `--module`, `--technicians`, `--providers`, `--dry-run` |
| `add-technician` | Añade técnicos a un módulo existente. | `--project`, `--module`, `--technicians`, `--providers`, `--dry-run` |
| `catalog` | Lista lo que se puede crear y devuelve su plantilla compuesta. Solo lee. | `--provider`, `--scope`, `--kind`, `--name` |
| `create-artifact` | Crea un agente o skill desde su plantilla. | `--provider`, `--scope`, `--kind`, `--name`, `--content-file`/`--stdin`, `--overwrite` |
| `save` | Guarda el contenido (o la cabecera) de un artefacto o lo propaga a su grupo. | `--path`, `--content-file`/`--stdin`, `--header-file`/`--header-stdin`, `--mode`, `--overwrite`, `--dry-run` |
| `adopt` | Vuelve a vincular a su grupo un artefacto legacy o desvinculado. | `--path`, `--group`, `--confirm`, `--dry-run` |

### Ejemplos

```powershell
# Instalar los agentes y skills de Claude y revisar el estado
java -jar cli/target/agentic-develop.jar configure-provider --provider claude
java -jar cli/target/agentic-develop.jar status

# Preparar un proyecto y crear un módulo (primero en simulación, sin escribir nada)
java -jar cli/target/agentic-develop.jar init --project C:\proyectos\mi-app
java -jar cli/target/agentic-develop.jar create-module --project C:\proyectos\mi-app --module facturacion --technicians analista,desarrollador --providers claude --dry-run
java -jar cli/target/agentic-develop.jar create-module --project C:\proyectos\mi-app --module facturacion --technicians analista,desarrollador --providers claude

# Cambiar el cuerpo compartido de un especialista y propagarlo a todos los módulos (primero viendo el diff)
java -jar cli/target/agentic-develop.jar save --path C:\proyectos\mi-app\.claude\agents\analista-facturacion.md --project C:\proyectos\mi-app --mode group --content-file cuerpo.md --dry-run
java -jar cli/target/agentic-develop.jar save --path C:\proyectos\mi-app\.claude\agents\analista-facturacion.md --project C:\proyectos\mi-app --mode group --content-file cuerpo.md

# Ver la plantilla de un agente antes de crearlo, y devolver un agente personalizado a su grupo
java -jar cli/target/agentic-develop.jar catalog --provider claude --scope global --kind agent --name sdd-design
java -jar cli/target/agentic-develop.jar adopt --path C:\proyectos\mi-app\.claude\agents\analista-otro.md --project C:\proyectos\mi-app --confirm
```

### Reglas que conviene conocer

- **Nada se sobrescribe por sorpresa.** Instalar, crear módulos o artefactos y repetir un comando
  deja intacto lo que ya existe. Sobrescribir exige `--overwrite` y deja una copia del contenido
  anterior.
- **Conflictos.** Si editas un agente por fuera de la aplicación, `save` lo detecta (hash distinto)
  y no lo pisa sin confirmación.
- **Simulación.** `save --dry-run` y `create-module --dry-run` devuelven lo previsto (diff,
  conflictos, ficheros) sin escribir. `adopt` sin `--confirm` también solo muestra el diff.
- **Propagación.** `save --mode group` actualiza solo los artefactos que ya existen del mismo grupo
  y ámbito; nunca crea nuevos. Los agentes generados antes de que existieran los metadatos cuentan
  como personalizados y no se tocan.
- **Cabeceras por sistema.** Las cabeceras se generan para el sistema operativo en el que se ejecuta
  el CLI (PowerShell en Windows, bash en Linux). `configure-provider` instala además el hook
  `sdd-specs-only` en la carpeta global del proveedor y no pisa uno que hayas modificado.
- **Todos los proveedores admiten proyecto.** `create-module --providers claude,codex,gemini,opencode`
  genera cada uno en su estructura.

## Estructura del repositorio

- `cli/`: aplicación de línea de comandos.
  - `domain/`, `application/`, `infrastructure/`: reglas, servicios y acceso a disco.
  - `command/`: adaptador no interactivo (JSON).
  - `ui/`: adaptador del menú interactivo.
  - `src/main/resources/`: agentes, skills, cabeceras y plantillas que se instalan.
- `viewer/`: aplicación de escritorio JavaFX.
- `docs/contracts/`: contratos de datos y de organización.
- `specs/`: especificaciones SDD (requisitos, diseño y tareas).
- `scripts/`: empaquetado y verificación.

## Capa técnica por proveedor

Los routers (`analista-router`, `desarrollador-router`, `documentador-router`) y los especialistas
por módulo (`<rol>-<modulo>`) se instalan en el proyecto. Cada proveedor usa su propia estructura:

| Proveedor | Skills | Agentes | Perfiles |
|---|---|---|---|
| Claude | `.claude/skills/<skill>/SKILL.md` | `.claude/agents/<rol>-<modulo>.md` | — |
| OpenCode | `.opencode/skills/<skill>/SKILL.md` | `.opencode/agents/<rol>-<modulo>.md` | — |
| Codex | `.agents/skills/<skill>/SKILL.md` (cada una con `agents/openai.yaml`) | `.codex/agents/<rol>_<modulo>.toml` | `.codex/profiles/<rol>-<modulo>.toml` |
| Gemini / Antigravity | `.agents/skills/<skill>/SKILL.md` | `.agents/agents/<rol>-<modulo>/agent.md` | — |

En el ámbito global es lo mismo con `~` (Codex: `~/.agents/skills` y `~/.codex/agents|profiles`;
Gemini: `~/.gemini/skills` y `~/.gemini/config/agents/<agente>/agent.md`). El contrato completo está
en [docs/contracts/provider-layout](../docs/contracts/provider-layout/provider-layout.md).

## Flujo de trabajo

1. **Especificación (SDD).** `sdd-agent` lleva una feature por requisitos, diseño y tareas
   (`specs/<feature>/`), con aprobación entre fases. No escribe código ni README.
2. **Capa técnica.** Con los módulos registrados en `.sdd/team.md`
   (`<modulo> | <id del mapa> | <rutas>`), tres routers delegan en un especialista por módulo
   (`<rol>-<modulo>`):
   - `analista-router`: convierte las tareas aprobadas en sprints por módulo
     (`docs/sprints/<modulo>/`).
   - `desarrollador-router`: implementa las tareas del sprint, comprueba antes la rama según
     `git-policy` y no hace operaciones de git que modifiquen nada.
   - `documentador-router`: redacta los contratos (`docs/contracts/`), mantiene el mapa
     `.sdd/architecture.md` y el `README.md` (el módulo indicado en `readme-owner`).
3. **Opciones** en la sección `## Opciones` de `.sdd/team.md`: `sprints` (`si`/`no`), `git-policy`
   (`balanced`/`strict`), `contract-strategy` (`implementation-first`/`contract-first`) y
   `readme-owner`.

Los permisos por rol los exige el texto de los prompts y, donde el proveedor lo permite, su
configuración (OpenCode, perfiles de Codex, hook global); no hay un aislamiento garantizado por la
plataforma.

## Dónde guarda cosas

- **Proveedores (global):** `.claude`, `.codex`, `.agents` (skills de Codex), `.gemini` y
  `.config/opencode` bajo tu carpeta de usuario.
- **Proveedores (proyecto):** `.claude`, `.opencode`, `.agents` (Codex y Gemini) y `.codex`
  (Codex) dentro del proyecto.
- **Estado de la aplicación** (metadatos y copias de seguridad): `%LOCALAPPDATA%\agentic-develop`
  en Windows y `$XDG_STATE_HOME/agentic-develop` (o `~/.local/state/agentic-develop`) en Linux.
  No se mezcla con los ficheros del proyecto.
- **Estado del visor:** dentro de la carpeta anterior, en `viewer/`:
  - `viewer.log`: diagnóstico y errores. Se abre desde **Ayuda → Abrir carpeta de logs**.
  - `drafts.json`: borradores de cambios sin guardar. Se aplican al volver a abrir el artefacto
    y se pueden descartar desde **Archivo → Descartar borradores**.
- **Menú en Windows:** si ves caracteres extraños (acentos), ejecuta antes `chcp 65001` o usa
  `java -Dfile.encoding=UTF-8 -jar ...`.

## Documentación

- Especificación de la CLI: `specs/cli/`.
- Especificación del visor: `specs/viewer/`.
- Contratos: `docs/contracts/`.
- Rendimiento: `docs/performance/cli-nfr-002.md`.
- Pruebas en Linux/WSL: `docs/guia-pruebas-linux-wsl.md`.
- Plan de release: `docs/plan-release-1.0.md`.
