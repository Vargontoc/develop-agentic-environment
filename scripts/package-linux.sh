#!/bin/bash
#
# Genera la imagen empaquetada del Agentic Viewer para Linux.
#
# Este script:
# 1. Compila el viewer con Maven
# 2. Crea un fat JAR con todas las dependencias
# 3. Genera un runtime Java personalizado con jlink
# 4. Empaqueta la aplicación con jpackage
# 5. Incluye el jar del CLI en el directorio de distribución
#
# Requisitos:
# - JDK 21 o superior con jlink y jpackage
# - Maven
# - El jar del CLI en viewer/cli/agentic-develop.jar
#

set -e

SKIP_TESTS=false
VERBOSE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

log_step() {
    echo ""
    echo "===> $1"
}

log_verbose() {
    if [ "$VERBOSE" = true ]; then
        echo "  $1"
    fi
}

log_success() {
    echo "✓ $1"
}

# Configuración
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
VIEWER_DIR="$ROOT_DIR/viewer"
BUILD_DIR="$VIEWER_DIR/target"
PACKAGE_DIR="$BUILD_DIR/package"
RUNTIME_DIR="$PACKAGE_DIR/runtime"
APP_DIR="$PACKAGE_DIR/app"
DIST_DIR="$BUILD_DIR/dist"
CLI_JAR="$VIEWER_DIR/cli/agentic-develop.jar"
VIEWER_JAR="$BUILD_DIR/viewer-0.1.0-SNAPSHOT.jar"
FAT_JAR="$BUILD_DIR/viewer-fat.jar"
APP_NAME="AgenticViewer"
APP_VERSION="0.1.0"

# Verificar herramientas
log_step "Verificando herramientas"

if ! command -v java &> /dev/null; then
    echo "Error: java no encontrado. Asegúrate de tener JDK 21+ en el PATH."
    exit 1
fi

if ! command -v jlink &> /dev/null; then
    echo "Error: jlink no encontrado. Asegúrate de tener JDK 21+ en el PATH."
    exit 1
fi

if ! command -v jpackage &> /dev/null; then
    echo "Error: jpackage no encontrado. Asegúrate de tener JDK 21+ en el PATH."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven no encontrado. Asegúrate de tener mvn en el PATH."
    exit 1
fi

# Verificar CLI jar
if [ ! -f "$CLI_JAR" ]; then
    echo "Error: El jar del CLI no existe en $CLI_JAR. Compílalo primero con: cd cli && mvn package"
    exit 1
fi

log_success "Todas las herramientas disponibles"

# Paso 1: Compilar y empaquetar viewer
log_step "1. Compilando viewer con Maven"
cd "$VIEWER_DIR"

if [ "$SKIP_TESTS" = true ]; then
    mvn clean package -DskipTests -q
else
    mvn clean package -q
fi

if [ ! -f "$VIEWER_JAR" ]; then
    echo "Error: No se generó el JAR del viewer en $VIEWER_JAR"
    exit 1
fi

log_success "Viewer compilado: $VIEWER_JAR"

# Paso 2: Crear fat JAR con todas las dependencias
log_step "2. Creando fat JAR"

# Extraer el JAR principal
TEMP_EXTRACT="$BUILD_DIR/temp-extract"
rm -rf "$TEMP_EXTRACT"
mkdir -p "$TEMP_EXTRACT"

log_verbose "Extrayendo $VIEWER_JAR"
cd "$TEMP_EXTRACT"
jar xf "$VIEWER_JAR"

# Extraer dependencias desde el classpath de Maven
log_verbose "Obteniendo classpath de dependencias"
DEPS_CP=$(mvn dependency:build-classpath -DincludeScope=runtime -q -Dmdep.outputFile=/dev/stdout 2>/dev/null)

if [ -n "$DEPS_CP" ]; then
    IFS=':' read -ra JAR_FILES <<< "$DEPS_CP"
    for DEP_JAR in "${JAR_FILES[@]}"; do
        if [ -f "$DEP_JAR" ]; then
            log_verbose "Extrayendo $DEP_JAR"
            jar xf "$DEP_JAR"
        fi
    done
fi

# Crear MANIFEST.MF
mkdir -p META-INF
cat > META-INF/MANIFEST.MF <<EOF
Manifest-Version: 1.0
Main-Class: es.vargontoc.agentic.viewer.App
EOF

# Empaquetar fat JAR
log_verbose "Creando $FAT_JAR"
cd "$BUILD_DIR"
jar cfm "$FAT_JAR" "$TEMP_EXTRACT/META-INF/MANIFEST.MF" -C "$TEMP_EXTRACT" .

# Limpiar temp
rm -rf "$TEMP_EXTRACT"

if [ ! -f "$FAT_JAR" ]; then
    echo "Error: No se pudo crear el fat JAR"
    exit 1
fi

log_success "Fat JAR creado: $FAT_JAR"

# Paso 3: Determinar módulos necesarios
log_step "3. Determinando módulos Java necesarios"

# Módulos base requeridos
MODULES="java.base,java.desktop,java.logging,java.xml,java.sql,java.management,java.naming,java.scripting,jdk.unsupported"

# Añadir módulos de JavaFX
JAVAFX_MODULES="javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web"

ALL_MODULES="$MODULES,$JAVAFX_MODULES"
log_verbose "Módulos: $ALL_MODULES"

# Paso 4: Crear runtime personalizado con jlink
log_step "4. Creando runtime personalizado con jlink"

rm -rf "$RUNTIME_DIR"

# Obtener la ruta de los jmods
JAVA_HOME_DIR=$(dirname $(dirname $(readlink -f $(which java))))
JMODS_PATH="$JAVA_HOME_DIR/jmods"

JLINK_CMD="jlink --module-path \"$JMODS_PATH\" --add-modules $ALL_MODULES --output \"$RUNTIME_DIR\" --no-header-files --no-man-pages --strip-debug --compress=2"

log_verbose "Ejecutando: $JLINK_CMD"
eval $JLINK_CMD

if [ ! -d "$RUNTIME_DIR" ]; then
    echo "Error: No se pudo crear el runtime personalizado"
    exit 1
fi

log_success "Runtime creado: $RUNTIME_DIR"

# Paso 5: Preparar directorio de la aplicación
log_step "5. Preparando directorio de la aplicación"

rm -rf "$APP_DIR"
mkdir -p "$APP_DIR"

# Copiar fat JAR
cp "$FAT_JAR" "$APP_DIR/viewer.jar"

# Copiar CLI jar
mkdir -p "$APP_DIR/cli"
cp "$CLI_JAR" "$APP_DIR/cli/agentic-develop.jar"

log_success "Aplicación preparada en: $APP_DIR"

# Paso 6: Empaquetar con jpackage
log_step "6. Empaquetando con jpackage"

rm -rf "$DIST_DIR"

JPACKAGE_CMD="jpackage \
  --type app-image \
  --name $APP_NAME \
  --input \"$APP_DIR\" \
  --main-jar viewer.jar \
  --main-class es.vargontoc.agentic.viewer.App \
  --runtime \"$RUNTIME_DIR\" \
  --dest \"$DIST_DIR\" \
  --app-version $APP_VERSION \
  --vendor \"Agentic Development\" \
  --description \"Agentic Development Environment Viewer\" \
  --java-options \"-Dfile.encoding=UTF-8\" \
  --java-options \"-Dstdout.encoding=UTF-8\" \
  --java-options \"-Dstderr.encoding=UTF-8\""

log_verbose "Ejecutando jpackage"
eval $JPACKAGE_CMD

APP_IMAGE="$DIST_DIR/$APP_NAME"
if [ ! -d "$APP_IMAGE" ]; then
    echo "Error: No se pudo crear la imagen de la aplicación"
    exit 1
fi

log_success "Aplicación empaquetada: $APP_IMAGE"

# Paso 7: Verificar que el CLI jar está en el directorio correcto
log_step "7. Verificando estructura final"

FINAL_CLI_JAR="$APP_IMAGE/lib/app/cli/agentic-develop.jar"
if [ ! -f "$FINAL_CLI_JAR" ]; then
    echo "Warning: El jar del CLI no está en $FINAL_CLI_JAR"
    echo "Creando directorio cli y copiando jar..."
    
    mkdir -p "$(dirname "$FINAL_CLI_JAR")"
    cp "$CLI_JAR" "$FINAL_CLI_JAR"
    
    log_success "CLI jar copiado a $FINAL_CLI_JAR"
fi

# Resumen
log_step "Resumen"
log_success "Aplicación empaquetada exitosamente"
echo ""
echo "Ubicación: $APP_IMAGE"
echo "Ejecutable: $APP_IMAGE/bin/$APP_NAME"
echo ""
echo "Para ejecutar:"
echo "  $APP_IMAGE/bin/$APP_NAME"
echo ""
echo "La aplicación incluye:"
echo "  - Runtime Java personalizado"
echo "  - Viewer JAR con todas las dependencias"
echo "  - CLI JAR en cli/agentic-develop.jar"
echo ""
echo "Funciona sin Java instalado en el sistema y sin conexión a internet."
