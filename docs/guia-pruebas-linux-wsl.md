# Guía: ejecutar las pruebas del CLI en Linux (WSL2)

Objetivo: comprobar en un Linux real lo que en Windows se salta o se simula, sobre todo el permiso de ejecución del
hook `sdd-specs-only.sh` y los enlaces simbólicos.

Tiempo estimado: 20–30 minutos la primera vez.

## Qué se verifica

En Windows la suite termina con **5 tests saltados**. Son los que necesitan un sistema de ficheros POSIX:

| Test | Qué comprueba |
|---|---|
| `PathResolverTest` (2) | Enlaces simbólicos que salen del ámbito |
| `HookInstallerTest.linuxHookIsExecutable` | Hook recién instalado con permiso de ejecución |
| `HookInstallerTest.updatedLinuxHookKeepsItsExecutablePermission` | El permiso se conserva al actualizar |
| `HookInstallerTest.aHooksFolderThatIsASymlinkOutsideTheProviderFolderInstallsNothing` | `hooks/` enlazado hacia fuera no instala nada |

Además `HookParityTest` ejecuta el `.sh` real contra 21 casos.

## Paso 0 — Deja el código disponible para Linux (en Windows)

Al escribir esta guía había cambios **sin confirmar** en Git, entre ellos `HookParityTest.java` y los contratos
`artifact-metadata.md` y `cli-json.md`. Si clonas desde GitHub sin confirmarlos, no llegarán a Linux.

Desde PowerShell, en la raíz del repositorio:

```powershell
git status
git add cli/src/test/java/es/vargontoc/agentic/develop/cli/infrastructure/resources/HookParityTest.java docs/contracts
git commit -m "test(cli): paridad del hook y ajuste de contratos"
git push
```

Revisa `git status` antes: `mejoras-sdd.md` y `specs/viewer/tasks.md` también aparecían modificados y quizá no quieras
incluirlos en ese commit.

Alternativa sin hacer `push`: en el paso 5 clona desde la ruta local (opción B).

## Paso 1 — Abre Ubuntu

Tienes WSL2 con la distribución `Ubuntu`. Desde PowerShell:

```powershell
wsl -d Ubuntu
```

## Paso 2 — Comprueba dónde estás

```bash
lsb_release -d
uname -sr
pwd
```

- `pwd` debe empezar por `/home/<tu usuario>`. Si empieza por `/mnt/c` o `/mnt/d`, haz `cd ~`.
- **No ejecutes los tests desde `/mnt/...`**: ese montaje no se comporta como Linux en permisos ni en enlaces
  simbólicos y podrías falsear el resultado.

## Paso 3 — Instala las herramientas básicas

```bash
sudo apt update
sudo apt install -y git jq zip unzip curl
jq --version
```

`jq` es imprescindible: lo usa el hook `.sh`.

## Paso 4 — Instala Java 21 y Maven 3.9

El README exige **JDK 21 y Maven 3.9+**. El Maven de `apt` suele ser más antiguo, así que se instalan con SDKMAN:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk list java            # busca la última 21.x de Temurin (identificador acabado en -tem)
sdk install java <identificador-21-tem>
sdk install maven 3.9.9

java -version
mvn -v
```

`java -version` debe decir 21 y `mvn -v` debe decir 3.9.x. Si prefieres no usar SDKMAN, `sudo apt install -y
openjdk-21-jdk` sirve para Java, pero comprueba la versión de Maven.

## Paso 5 — Obtén el código dentro de Linux

**Opción A (recomendada, requiere el `push` del paso 0):**

```bash
mkdir -p ~/src && cd ~/src
git clone https://github.com/Vargontoc/develop-agentic-environment.git
cd develop-agentic-environment
```

**Opción B (sin `push`, desde tu copia de Windows):**

```bash
mkdir -p ~/src && cd ~/src
git config --global --add safe.directory '*'
git clone /mnt/d/Workspace/develop-agentic-environment
cd develop-agentic-environment
```

La opción B solo trae lo **confirmado**. Los ficheros sin confirmar no se copian.

Comprueba que están los ficheros del hook y los fines de línea correctos:

```bash
ls cli/src/main/resources/global/hooks/
file cli/src/main/resources/global/hooks/*
```

Resultado esperado: el `.bat` con `CRLF line terminators` y el `.sh` sin esa mención. Eso confirma que el
`.gitattributes` funciona. Si el `.sh` sale con CRLF, **para aquí**: el hook fallaría con `bad interpreter`.

## Paso 6 — Configura el acceso al framework en el Maven de Linux

La CLI descarga `framework-core` y `framework-json` (`0.1.0`) de GitHub Packages. El `~/.m2` de Linux es distinto
del de Windows, así que crea ahí `~/.m2/settings.xml` con tus credenciales, como indica la guía de desarrollo
(sección «Dependencias externas»). Comprueba que resuelve, desde `cli/`:

```bash
cd cli
mvn dependency:resolve
```

Debe terminar con `BUILD SUCCESS`.

## Paso 7 — Ejecuta la suite

Sin `-o` la primera vez: Maven tiene que descargar las dependencias.

```bash
mvn clean test
```

### Resultado esperado

- `BUILD SUCCESS` y `Failures: 0, Errors: 0`.
- Los 5 tests de la tabla del principio **se ejecutan y pasan**.
- Quedan saltados unos 3, y deberían ser estos, que solo tienen sentido en otro sistema:
  - `HookInstallerTest.linuxOnANonPosixFileSystemInstallsAndWarnsThatItCouldNotBeMadeExecutable` (solo aplica sin POSIX).
  - `HookParityTest.theBatVariantGivesTheExpectedResultForEveryCase` (el `.bat` solo corre en Windows).
  - `HookParityTest.bothVariantsAgreeOnEveryCase` (la paridad necesita las dos variantes ejecutables).
- `HookParityTest.theShVariantGivesTheExpectedResultForEveryCase` **no** debe saltarse. Si lo hace, falta `jq` o
  `bash`.

Para ver solo el resumen de saltados y fallos:

```bash
mvn test 2>&1 | grep -E "Tests run:|FAIL|ERROR\]|BUILD"
```

Si algo falla, guarda la salida completa y compártela:

```bash
mvn test 2>&1 | tee ~/salida-tests.txt
```

## Paso 8 — Prueba manual del hook con el CLI real (opcional)

Comprueba el permiso de ejecución con una instalación real, sin tocar tu `~/.claude`: se usa un `HOME` temporal.

```bash
mvn clean package -DskipTests
export HOME_PRUEBA=$(mktemp -d)

HOME=$HOME_PRUEBA java -jar target/agentic-develop.jar configure-provider --provider claude
ls -l $HOME_PRUEBA/.claude/hooks/
```

Esperado: `-rwx------ ... sdd-specs-only.sh` y ningún `.bat`. Si el nombre del comando cambió, consulta
`docs/contracts/cli-json/cli-json.md`.

Repetir la instalación no debe cambiar nada ni avisar:

```bash
HOME=$HOME_PRUEBA java -jar target/agentic-develop.jar configure-provider --provider claude
```

Modifica el hook y repite: debe avisar de que está modificado y **no** sobrescribirlo.

```bash
echo "# cambio mio" >> $HOME_PRUEBA/.claude/hooks/sdd-specs-only.sh
HOME=$HOME_PRUEBA java -jar target/agentic-develop.jar configure-provider --provider claude
tail -n 1 $HOME_PRUEBA/.claude/hooks/sdd-specs-only.sh
```

En la salida JSON, `warnings` debe contener un texto con `modificado por el usuario`, y la última línea del fichero
sigue siendo `# cambio mio`.

Prueba directa del hook (debe imprimir `0` y luego `2`):

```bash
mkdir -p $HOME_PRUEBA/proyecto/.sdd $HOME_PRUEBA/proyecto/specs
echo "x" > $HOME_PRUEBA/proyecto/.sdd/project.md
cd $HOME_PRUEBA/proyecto

echo '{"tool_input":{"file_path":"specs/a.md"},"cwd":"'$PWD'"}' | $HOME_PRUEBA/.claude/hooks/sdd-specs-only.sh specs; echo "salida: $?"
echo '{"tool_input":{"file_path":"README.md"},"cwd":"'$PWD'"}'  | $HOME_PRUEBA/.claude/hooks/sdd-specs-only.sh specs; echo "salida: $?"
```

## Paso 9 — Limpieza

```bash
rm -rf "$HOME_PRUEBA"
```

El repositorio clonado en `~/src` puedes dejarlo para repetir las pruebas con `git pull`.

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| `bad interpreter: /bin/bash^M` al ejecutar el `.sh` | El `.sh` tiene CRLF. Revisa el paso 5 (`.gitattributes`). |
| `Could not resolve dependencies ... framework-core` | Falta el paso 6, o se hizo con otro usuario/`HOME`. |
| `release version 21 not supported` | `java -version` no es 21. Revisa el paso 4. |
| Saltan los tests de enlaces simbólicos | Estás dentro de `/mnt/...`. Mueve el repositorio a `~/src`. |
| `HookParityTest` `.sh` saltado | Falta `jq` (`sudo apt install jq`). |
| `fatal: detected dubious ownership` | Falta el `safe.directory` de la opción B del paso 5. |

## Más adelante: multiplataforma

Para no depender de ejecutar esto a mano, lo natural es una **matriz de CI** (por ejemplo GitHub Actions con
`ubuntu-latest` y `windows-latest`) que ejecute `mvn test` en ambos sistemas en cada commit. El repositorio está en
GitHub, así que es directo. WSL queda como entorno de depuración rápida cuando algo falle en Linux.
