# develop-agentic-environment
Pequeña aplicación para la instalación de un entorno agéntico de desarrollo (Human-In-The-Loop).

Instala y mantiene los agentes y skills de **Spec-Driven Development** (requisitos, diseño y tareas) en los
proveedores que tengas (Claude, Codex, Gemini, OpenCode), prepara proyectos con la estructura `.sdd` y genera los
agentes de cada módulo y técnico de un proyecto.

## Estado
La CLI (`cli/`) está completa según su especificación (`specs/cli/`): menú interactivo, comandos no interactivos con
salida JSON y contratos documentados. La salida JSON está pensada para que la consuma un visor externo.

El viewer (`viewer/`) está en construcción con JavaFX 21. Incluye la selección de proyecto, el árbol global y de
proyecto, la previsualización y edición de artefactos (agentes, skills) y de los ficheros `.sdd` en solo lectura;
la creación de módulos y técnicos; la creación de agentes y skills desde plantilla; la edición de skills con
múltiples ficheros; la consola PTY integrada con pty4j para ejecutar el CLI interactivo; el watcher de cambios
externos con marcado de nodos obsoletos y recarga con guarda de editores sucios; la propagación de cambios
a grupos de artefactos; y los scripts de empaquetado con jlink/jpackage para generar una app-image que funciona
sin Java del sistema. Consume el CLI mediante el contrato `cli-json` v1.

**Nota:** Corregido bug en la navegación del árbol donde seleccionar un artefacto sin editarlo marcaba incorrectamente el estado como "sin guardar".

## Requisitos
- JDK 21
- Maven 3.9+

## Dependencias externas
La CLI usa `framework-core` y `framework-json` de `vargontoc-framework`. Los artefactos están en
`cli/external-libs/vargontoc-framework/` y hay que instalarlos una vez en el repositorio Maven local (`~/.m2`), desde `cli/`:

```powershell
# POM padre
mvn install:install-file "-Dfile=external-libs/vargontoc-framework/pom.xml" "-DpomFile=external-libs/vargontoc-framework/pom.xml" "-Dpackaging=pom"

# framework-core
mvn install:install-file "-Dfile=external-libs/vargontoc-framework/framework-core/0.1.0-SNAPSHOT/framework-core-0.1.0-SNAPSHOT.jar" "-DpomFile=external-libs/vargontoc-framework/framework-core/0.1.0-SNAPSHOT/framework-core-0.1.0-SNAPSHOT.pom"

# framework-json
mvn install:install-file "-Dfile=external-libs/vargontoc-framework/framework-json/0.1.0-SNAPSHOT/framework-json-0.1.0-SNAPSHOT.jar" "-DpomFile=external-libs/vargontoc-framework/framework-json/0.1.0-SNAPSHOT/framework-json-0.1.0-SNAPSHOT.pom"
```

## Compilar, probar y empaquetar
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

### Empaquetado (app-image)

Para generar una imagen de la aplicación que funcione **sin Java instalado** y **sin conexión a internet**:

#### Requisitos previos

**Windows:**
- JDK 21 o superior (con `jlink` y `jpackage` en el PATH)
- Maven 3.9+
- PowerShell 5.1 o superior

**Linux:**
- JDK 21 o superior (con `jlink` y `jpackage` en el PATH)
- Maven 3.9+
- Bash
- Dependencias de desktop (para el launcher nativo):
  - **Ubuntu/Debian:** `sudo apt-get install -y libxtst-dev libxrender-dev libxxf86vm-dev`
  - **Fedora/RHEL:** `sudo dnf install -y libXtst-devel libXrender-devel libXxf86vm-devel`
  - **Arch:** `sudo pacman -S libxtst libxrender libxxf86vm`

#### Paso 1: Compilar la CLI

Desde la raíz del proyecto:

**Windows:**
```powershell
cd cli
mvn clean package -DskipTests
```

**Linux:**
```bash
cd cli
mvn clean package -DskipTests
```

Esto genera `cli/target/agentic-develop.jar`.

#### Paso 2: Copiar el JAR de la CLI

**Windows:**
```powershell
cd ..
New-Item -ItemType Directory -Force viewer/cli | Out-Null
Copy-Item cli/target/agentic-develop.jar viewer/cli/
```

**Linux:**
```bash
cd ..
mkdir -p viewer/cli
cp cli/target/agentic-develop.jar viewer/cli/
```

#### Paso 3: Empaquetar el Viewer

**Windows:**
```powershell
cd viewer
..\scripts\package-windows.ps1
```

**Linux:**
```bash
cd viewer
../scripts/package-linux.sh
```

**Opciones:**
- `--skip-tests`: Omite las pruebas de Maven (más rápido)
- `--verbose`: Muestra detalles adicionales del proceso
- `--installer` (Windows: `-Installer`): además de la app-image, genera un instalador para distribuir. En Linux un
  `.deb` (requiere `dpkg-dev` y `fakeroot`; se instala con `sudo apt install ./<fichero>.deb`) y en Windows un `.msi`
  (requiere WiX Toolset 3.x en el PATH). Queda en `viewer/target/dist/installer/`.

#### Paso 4: Verificar el empaquetado

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

Esto comprueba:
- Que el runtime Java personalizado está incluido
- Que el JAR del viewer está empaquetado
- Que el JAR de la CLI está en la ubicación correcta
- Que la aplicación puede iniciar sin Java del sistema

#### Paso 5: Ejecutar la aplicación empaquetada

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

La aplicación empaquetada:
- ✅ Funciona sin Java instalado en el sistema
- ✅ Funciona sin conexión a internet
- ✅ Incluye todas las dependencias (JavaFX, Jackson, Pty4J, etc.)
- ✅ Incluye el JAR de la CLI en la ubicación esperada
- ✅ Tamaño total: ~150-200 MB (runtime Java + viewer + CLI)

#### Estructura generada

**Windows:**
```
viewer/target/dist/windows/AgenticViewer/
├── AgenticViewer.exe          # Ejecutable nativo
├── runtime/                   # Runtime Java personalizado
│   ├── bin/
│   │   ├── java.exe
│   │   └── jlink.exe
│   └── lib/
├── app/
│   ├── viewer.jar            # JAR del viewer
│   └── cli/
│       └── agentic-develop.jar  # JAR de la CLI
└── ...
```

**Linux:**
```
viewer/target/dist/linux/AgenticViewer/
├── bin/
│   └── AgenticViewer          # Script launcher nativo
├── runtime/                   # Runtime Java personalizado
│   ├── bin/
│   │   ├── java
│   │   └── jlink
│   └── lib/
├── lib/
│   ├── app/
│   │   ├── viewer.jar        # JAR del viewer
│   │   └── cli/
│   │       └── agentic-develop.jar  # JAR de la CLI
│   └── ...
└── ...
```

#### Resolución de problemas

**Error: "jlink no encontrado"**
- Asegúrate de tener JDK 21+ instalado
- Verifica que `jlink` esté en el PATH: `where jlink` (Windows) o `which jlink` (Linux)

**Error: "jpackage no encontrado"**
- Igual que arriba, `jpackage` viene con JDK 14+

**Error: "CLI jar not found"**
- Ejecuta el Paso 1 y Paso 2 antes de empaquetar
- Verifica que `viewer/cli/agentic-develop.jar` exista

**Error en Linux: "libXtst.so not found"**
- Instala las dependencias de desktop (ver Requisitos previos)

**La aplicación no inicia**
- Ejecuta el script de verificación para diagnosticar el problema
- Revisa los logs en `viewer/target/package/logs/` (si existen)

## Uso
### Menú interactivo
Sin argumentos se abre el menú:

```powershell
java -jar cli/target/agentic-develop.jar
```

1. **Configuración entorno**: instala los agentes y skills globales en los proveedores que elijas.
2. **Iniciar proyecto**: crea la estructura `.sdd` en la ruta indicada.
3. **Crear módulo**: registra un módulo en `.sdd/team.md` y genera los agentes de sus técnicos.
4. **Salir**.

### Comandos
Con argumentos se ejecuta un comando sin menús ni preguntas. La salida estándar es **un único documento JSON** y los
logs van a la salida de error. El código de salida es `0` si todo fue bien, `1` por un error esperado (validación, no
encontrado, conflicto), `2` por un uso incorrecto y `3` por un error interno.

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

Ejemplos:

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

Normas que conviene conocer:
- **Nada se sobrescribe por sorpresa.** Instalar, crear módulos o artefactos y repetir un comando deja intacto lo que ya existe.
  Sobrescribir exige `--overwrite` y deja una copia del contenido anterior.
- **Conflictos.** Si editas un agente por fuera de la aplicación, `save` lo detecta (hash distinto) y no lo pisa sin confirmación.
- **Simulación.** `save --dry-run` y `create-module --dry-run` devuelven lo previsto (diff, conflictos, ficheros) sin escribir.
  `adopt` sin `--confirm` también solo muestra el diff.
- **Propagación.** `save --mode group` actualiza solo los artefactos que ya existen del mismo grupo y ámbito; nunca crea nuevos.
  Los agentes generados antes de que existieran los metadatos cuentan como personalizados y no se tocan.
- **Cabeceras por sistema.** Las cabeceras de los agentes se generan para el sistema operativo en el que se ejecuta el CLI
  (PowerShell en Windows, bash en Linux). Los scripts de los hooks (`hooks/<script>.bat` o `.sh`) los aportas tú en la
  carpeta global del proveedor.

### Viewer (en construcción)
El viewer espera el jar ejecutable en `viewer/cli/agentic-develop.jar`. Si falta, muestra un error de aplicación.
La ventana permite seleccionar un proyecto, validarlo con el CLI e iniciarlo si falta `.sdd`.
El panel lateral muestra el árbol Global y Project, y se refresca al seleccionar o iniciar un proyecto.
Al elegir un agente o una skill se previsualiza (cabecera aparte, cuerpo en texto o renderizado); `.sdd` se muestra en solo lectura.
Los agentes y perfiles se pueden editar: cabecera y cuerpo por separado, con guardado mediante `save` y aviso antes de perder cambios.
El cuerpo compartido de un especialista se puede propagar al grupo, revisando antes los miembros y diffs que devuelve el CLI.
Un artefacto legacy o desvinculado ofrece «Volver al grupo» (adoptar), con diff previo y confirmación.
Desde la raíz, tras construir el CLI:

```powershell
New-Item -ItemType Directory -Force viewer/cli
Copy-Item cli/target/agentic-develop.jar viewer/cli/
```

Después, desde `viewer/`:

```powershell
mvn test        # pruebas de la lógica
mvn javafx:run  # abre la ventana
```

### Dónde guarda cosas
- **Proveedores (global):** `.claude`, `.codex`, `.gemini/config` y `.config/opencode` bajo tu carpeta de usuario.
- **Proveedores (proyecto):** `.claude`, `.agents` (Gemini) y `.opencode` dentro del proyecto. Codex aún no tiene ámbito de proyecto.
- **Estado de la aplicación** (metadatos y copias de seguridad): `%LOCALAPPDATA%\agentic-develop` en Windows y
  `$XDG_STATE_HOME/agentic-develop` (o `~/.local/state/agentic-develop`) en Linux. No se mezcla con los ficheros del proyecto.
- **Menú en Windows:** si ves caracteres extraños (acentos), ejecuta antes `chcp 65001` o usa `java -Dfile.encoding=UTF-8 -jar ...`.

## Documentación
- Especificación de la CLI: `specs/cli/` (requisitos, diseño y tareas).
- Contratos: `docs/contracts/`
  - `cli-json/`: envelope JSON, comandos, opciones y códigos de error y de salida.
  - `artifact-metadata/`: almacén de agrupación, cuerpos compartidos y hashes.
  - `provider-layout/`: rutas, formatos y cabeceras por proveedor y sistema operativo.
  - `sdd-team/`: lectura y escritura de módulos y técnicos en `.sdd/team.md`.
- Rendimiento: `docs/performance/cli-nfr-002.md` (todos los comandos tardan muy por debajo de 1 s con 200 artefactos).

Las pruebas comprueban que estos contratos dicen lo mismo que el código.

## Estructura
- `cli/`: aplicación de línea de comandos (Java 21, Maven).
  - `domain/`, `application/`, `infrastructure/`: reglas, servicios y acceso a disco.
  - `command/`: adaptador no interactivo (JSON). `ui/`: adaptador del menú interactivo.
  - `src/main/resources/`: agentes, skills, cabeceras y plantillas que se instalan.
- `cli/external-libs/`: artefactos de `vargontoc-framework` usados por la CLI.
- `docs/`: contratos e informes.
- `specs/`: especificaciones SDD.
- `viewer/`: aplicación de escritorio JavaFX 21 (en construcción; incluye selección de proyecto, árbol, previsualización, edición, propagación, adopción, creación desde plantilla, edición de skills como directorio, consola PTY, watcher de cambios y empaquetado).
