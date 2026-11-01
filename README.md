# develop-agentic-environment
Pequeña aplicación para la instalación de un entorno agentico de desarrollo (Human-In-The-Loop)

## Estado
En desarrollo. La CLI (`cli/`) comprueba el entorno y detecta qué proveedores
(Claude, OpenCode, Codex, Gemini) están disponibles. El menú interactivo en
terminal está en construcción.

## Requisitos
- JDK 21
- Maven 3.9+

## Dependencias externas
La CLI usa `framework-core` de `vargontoc-framework`. Los artefactos necesarios
están en `cli/external-libs/vargontoc-framework/` y hay que instalarlos una vez
en el repositorio Maven local (`~/.m2`), desde `cli/`:

```powershell
# POM padre
mvn install:install-file "-Dfile=external-libs/vargontoc-framework/pom.xml" "-DpomFile=external-libs/vargontoc-framework/pom.xml" "-Dpackaging=pom"

# framework-core
mvn install:install-file "-Dfile=external-libs/vargontoc-framework/framework-core/0.1.0-SNAPSHOT/framework-core-0.1.0-SNAPSHOT.jar" "-DpomFile=external-libs/vargontoc-framework/framework-core/0.1.0-SNAPSHOT/framework-core-0.1.0-SNAPSHOT.pom"
```

## Compilar
Desde `cli/`:

```powershell
mvn clean compile
```

## Estructura
- `cli/`: aplicación de línea de comandos (Java 21, Maven).
- `cli/external-libs/`: artefactos de `vargontoc-framework` usados por la CLI.
