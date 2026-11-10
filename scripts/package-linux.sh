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
INSTALLER=false

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
        --installer)
            INSTALLER=true
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
CLI_SOURCE="$ROOT_DIR/cli/target/agentic-develop.jar"
if [ -f "$CLI_SOURCE" ]; then
    if [ ! -f "$CLI_JAR" ] || [ "$CLI_SOURCE" -nt "$CLI_JAR" ]; then
        mkdir -p "$(dirname "$CLI_JAR")"
        cp "$CLI_SOURCE" "$CLI_JAR"
        log_success "CLI sincronizado desde cli/target/agentic-develop.jar"
    fi
fi
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
# Se usa -f porque aquí el directorio actual es el de extracción, que no tiene pom.xml.
if ! DEPS_CP=$(mvn -f "$VIEWER_DIR/pom.xml" dependency:build-classpath -DincludeScope=runtime -q -Dmdep.outputFile=/dev/stdout); then
    echo "Error: no se pudo obtener el classpath de dependencias con Maven."
    echo "$DEPS_CP"
    exit 1
fi

if [ -n "$DEPS_CP" ]; then
    IFS=':' read -ra JAR_FILES <<< "$DEPS_CP"
    for DEP_JAR in "${JAR_FILES[@]}"; do
        if [ -f "$DEP_JAR" ]; then
            log_verbose "Extrayendo $DEP_JAR"
            jar xf "$DEP_JAR"
        fi
    done
fi

# Eliminar las clases de JavaFX del fat JAR (estarán en el runtime vía jlink)
log_verbose "Eliminando clases de JavaFX del fat JAR (estarán en el runtime)"
if [ -d "javafx" ]; then
    rm -rf javafx
fi

# También eliminar archivos de módulos de JavaFX
find . -name "module-info.class" -delete 2>/dev/null || true

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

# Descargar JavaFX jmods si no existe
JAVAFX_JMODS_DIR="$PACKAGE_DIR/javafx-jmods"
JAVAFX_VERSION="21.0.2"

if [ ! -d "$JAVAFX_JMODS_DIR" ]; then
    log_verbose "Descargando JavaFX jmods $JAVAFX_VERSION"
    
    mkdir -p "$PACKAGE_DIR"
    
    # Detectar plataforma
    ARCH=$(uname -m)
    case $ARCH in
        x86_64)
            JAVAFX_ARCH="x64"
            ;;
        aarch64)
            JAVAFX_ARCH="aarch64"
            ;;
        *)
            echo "Error: Arquitectura no soportada: $ARCH"
            exit 1
            ;;
    esac
    
    # Descargar los jmods (no el SDK completo)
    # Gluon publica los jmods como .zip (no existe .tar.gz)
    JAVAFX_URL="https://download2.gluonhq.com/openjfx/${JAVAFX_VERSION}/openjfx-${JAVAFX_VERSION}_linux-${JAVAFX_ARCH}_bin-jmods.zip"
    JAVAFX_ZIP="$PACKAGE_DIR/javafx-jmods.zip"

    if ! curl -fL -o "$JAVAFX_ZIP" "$JAVAFX_URL"; then
        echo "Error: No se pudo descargar JavaFX jmods desde $JAVAFX_URL"
        exit 1
    fi

    log_verbose "Extrayendo JavaFX jmods"
    if command -v unzip &> /dev/null; then
        unzip -q -o "$JAVAFX_ZIP" -d "$PACKAGE_DIR"
    else
        (cd "$PACKAGE_DIR" && jar xf "$JAVAFX_ZIP")
    fi
    rm -f "$JAVAFX_ZIP"
    
    # Renombrar el directorio extraído
    EXTRACTED_DIR=$(find "$PACKAGE_DIR" -maxdepth 1 -type d -name "javafx-jmods-*" | head -1)
    if [ -n "$EXTRACTED_DIR" ]; then
        mv "$EXTRACTED_DIR" "$JAVAFX_JMODS_DIR"
    fi
fi

# El zip de Gluon contiene los .jmod directamente en la carpeta extraída
JAVAFX_JMODS="$JAVAFX_JMODS_DIR"
if [ -d "$JAVAFX_JMODS_DIR/jmods" ]; then
    JAVAFX_JMODS="$JAVAFX_JMODS_DIR/jmods"
fi

if ! ls "$JAVAFX_JMODS"/*.jmod &> /dev/null; then
    echo "Error: Los jmods de JavaFX no se encontraron en $JAVAFX_JMODS"
    exit 1
fi

log_verbose "JavaFX jmods encontrados en: $JAVAFX_JMODS"

# Listar los jmods disponibles
log_verbose "Jmods de JavaFX disponibles: $(ls "$JAVAFX_JMODS"/*.jmod | xargs -n1 basename | sed 's/.jmod$//' | tr '\n' ', ')"

# Combinar module-paths
MODULE_PATHS="$JMODS_PATH:$JAVAFX_JMODS"

log_verbose "Ejecutando jlink con módulos: $ALL_MODULES"
jlink --module-path "$MODULE_PATHS" --add-modules "$ALL_MODULES" --output "$RUNTIME_DIR" \
  --no-header-files --no-man-pages --strip-debug --compress=2

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

log_verbose "Ejecutando jpackage"
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --input "$APP_DIR" \
  --main-jar viewer.jar \
  --main-class es.vargontoc.agentic.viewer.App \
  --runtime-image "$RUNTIME_DIR" \
  --dest "$DIST_DIR" \
  --app-version "$APP_VERSION" \
  --vendor "Agentic Development" \
  --description "Agentic Development Environment Viewer" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "-Dstdout.encoding=UTF-8" \
  --java-options "-Dstderr.encoding=UTF-8"

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

# Paso 8 (opcional): instalador .deb para distribución
if [ "$INSTALLER" = true ]; then
    log_step "8. Generando instalador .deb"

    if ! command -v dpkg-deb &> /dev/null || ! command -v fakeroot &> /dev/null; then
        echo "Error: para generar el .deb hacen falta dpkg-deb y fakeroot."
        echo "Instálalos con: sudo apt install -y dpkg-dev fakeroot"
        exit 1
    fi

    INSTALLER_DIR="$DIST_DIR/installer"
    rm -rf "$INSTALLER_DIR"

    # Parte de la app-image ya construida: el instalador la copia a /opt/agentic-viewer
    jpackage \
      --type deb \
      --app-image "$APP_IMAGE" \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "Agentic Development" \
      --description "Agentic Development Environment Viewer" \
      --linux-package-name "agentic-viewer" \
      --linux-menu-group "Development" \
      --linux-shortcut \
      --dest "$INSTALLER_DIR"

    DEB_FILE=$(find "$INSTALLER_DIR" -maxdepth 1 -name '*.deb' | head -1)
    if [ -z "$DEB_FILE" ]; then
        echo "Error: no se generó el paquete .deb en $INSTALLER_DIR"
        exit 1
    fi

    log_success "Instalador creado: $DEB_FILE"
fi

# Resumen
log_step "Resumen"
log_success "Aplicación empaquetada exitosamente"
echo ""
if [ "$INSTALLER" = true ]; then
    echo "Instalador: $DEB_FILE"
    echo "Instalar con: sudo apt install ./$(basename "$DEB_FILE")"
    echo ""
fi
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
