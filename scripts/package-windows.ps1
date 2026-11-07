<#
.SYNOPSIS
    Genera la imagen empaquetada del Agentic Viewer para Windows.

.DESCRIPTION
    Este script:
    1. Compila el viewer con Maven
    2. Crea un fat JAR con todas las dependencias
    3. Genera un runtime Java personalizado con jlink
    4. Empaqueta la aplicación con jpackage
    5. Incluye el jar del CLI en el directorio de distribución

.NOTES
    Requiere:
    - JDK 21 o superior con jlink y jpackage
    - Maven
    - El jar del CLI en viewer/cli/agentic-develop.jar
#>

param(
    [switch]$SkipTests,
    [switch]$Verbose,
    [switch]$Installer
)

$ErrorActionPreference = "Stop"
$script:Verbose = $Verbose

function Write-Step {
    param([string]$Message)
    Write-Host "`n===> $Message" -ForegroundColor Cyan
}

function Write-VerboseMsg {
    param([string]$Message)
    if ($script:Verbose) {
        Write-Host "  $Message" -ForegroundColor DarkGray
    }
}

# Configuración
$RootDir = Split-Path -Parent $PSScriptRoot
$ViewerDir = Join-Path $RootDir "viewer"
$BuildDir = Join-Path $ViewerDir "target"
$PackageDir = Join-Path $BuildDir "package"
$RuntimeDir = Join-Path $PackageDir "runtime"
$AppDir = Join-Path $PackageDir "app"
$DistDir = Join-Path $BuildDir "dist"
$CliJar = Join-Path (Join-Path $ViewerDir "cli") "agentic-develop.jar"
$ViewerJar = Join-Path $BuildDir "viewer-0.1.0-SNAPSHOT.jar"
$FatJar = Join-Path $BuildDir "viewer-fat.jar"
$AppName = "AgenticViewer"
$AppVersion = "0.1.0"

# Verificar herramientas
Write-Step "Verificando herramientas"
try {
    $oldErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    $ErrorActionPreference = $oldErrorAction
} catch {
    $javaVersion = "Java no detectado"
}
Write-VerboseMsg "Java: $javaVersion"

$jlinkPath = Get-Command jlink -ErrorAction SilentlyContinue
if (-not $jlinkPath) {
    Write-Error "jlink no encontrado. Asegúrate de tener JDK 21+ en el PATH."
    exit 1
}

$jpackagePath = Get-Command jpackage -ErrorAction SilentlyContinue
if (-not $jpackagePath) {
    Write-Error "jpackage no encontrado. Asegúrate de tener JDK 21+ en el PATH."
    exit 1
}

$mvnPath = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnPath) {
    Write-Error "Maven no encontrado. Asegúrate de tener mvn en el PATH."
    exit 1
}

# Verificar CLI jar
if (-not (Test-Path $CliJar)) {
    Write-Error "El jar del CLI no existe en $CliJar. Compílalo primero con: cd cli; mvn package"
    exit 1
}

Write-Host "✓ Todas las herramientas disponibles" -ForegroundColor Green

# Paso 1: Compilar y empaquetar viewer
Write-Step "1. Compilando viewer con Maven"
Set-Location $ViewerDir
if ($SkipTests) {
    mvn clean package -DskipTests -q
} else {
    mvn clean package -q
}

if (-not (Test-Path $ViewerJar)) {
    Write-Error "No se generó el JAR del viewer en $ViewerJar"
    exit 1
}

Write-Host "✓ Viewer compilado: $ViewerJar" -ForegroundColor Green

# Paso 2: Crear fat JAR con todas las dependencias
Write-Step "2. Creando fat JAR"

# Extraer el JAR principal
$TempExtract = Join-Path $BuildDir "temp-extract"
if (Test-Path $TempExtract) {
    Remove-Item $TempExtract -Recurse -Force
}
New-Item -ItemType Directory -Path $TempExtract | Out-Null

Write-VerboseMsg "Extrayendo $ViewerJar"
Set-Location $TempExtract
jar xf $ViewerJar

# Extraer dependencias desde el classpath de Maven
Write-VerboseMsg "Obteniendo classpath de dependencias"
$Dependencies = mvn dependency:build-classpath -DincludeScope=runtime -q -Dmdep.outputFile=CON 2>&1 | 
    Where-Object { $_ -notmatch "^\[" -and $_ -match "\.jar" }

if ($Dependencies) {
    $JarFiles = $Dependencies.Split(';') | Where-Object { $_ -match "\.jar$" }
    foreach ($DepJar in $JarFiles) {
        if (Test-Path $DepJar) {
            Write-VerboseMsg "Extrayendo $DepJar"
            jar xf $DepJar
        }
    }
}

# Eliminar las clases de JavaFX del fat JAR (estarán en el runtime vía jlink)
Write-VerboseMsg "Eliminando clases de JavaFX del fat JAR (estarán en el runtime)"
$JavafxDirs = Get-ChildItem -Path $TempExtract -Directory -Filter "javafx" -ErrorAction SilentlyContinue
if ($JavafxDirs) {
    foreach ($Dir in $JavafxDirs) {
        Write-VerboseMsg "Eliminando $($Dir.FullName)"
        Remove-Item $Dir.FullName -Recurse -Force
    }
}

# También eliminar archivos de módulos de JavaFX
$ModuleInfoFiles = Get-ChildItem -Path $TempExtract -Recurse -Filter "module-info.class" -ErrorAction SilentlyContinue
if ($ModuleInfoFiles) {
    foreach ($File in $ModuleInfoFiles) {
        Remove-Item $File.FullName -Force -ErrorAction SilentlyContinue
    }
}

# Crear MANIFEST.MF
$ManifestDir = Join-Path $TempExtract "META-INF"
if (-not (Test-Path $ManifestDir)) {
    New-Item -ItemType Directory -Path $ManifestDir | Out-Null
}

$ManifestPath = Join-Path $ManifestDir "MANIFEST.MF"
$manifestLines = "Manifest-Version: 1.0`r`nMain-Class: es.vargontoc.agentic.viewer.App`r`n"
[System.IO.File]::WriteAllText($ManifestPath, $manifestLines, (New-Object System.Text.UTF8Encoding $false))

# Empaquetar fat JAR
Write-VerboseMsg "Creando $FatJar"
Set-Location $BuildDir
jar cfm $FatJar $ManifestPath -C $TempExtract .

# Limpiar temp
Remove-Item $TempExtract -Recurse -Force

if (-not (Test-Path $FatJar)) {
    Write-Error "No se pudo crear el fat JAR"
    exit 1
}

Write-Host "✓ Fat JAR creado: $FatJar" -ForegroundColor Green

# Paso 3: Determinar módulos necesarios
Write-Step "3. Determinando módulos Java necesarios"

# Módulos base requeridos
$Modules = @(
    "java.base",
    "java.desktop",
    "java.logging",
    "java.xml",
    "java.sql",
    "java.management",
    "java.naming",
    "java.scripting",
    "jdk.unsupported"
)

# Añadir módulos de JavaFX
$JavafxModules = @(
    "javafx.base",
    "javafx.controls",
    "javafx.fxml",
    "javafx.graphics",
    "javafx.media",
    "javafx.swing",
    "javafx.web"
)

$AllModules = ($Modules + $JavafxModules) -join ","
Write-VerboseMsg "Módulos: $AllModules"

# Paso 4: Crear runtime personalizado con jlink
Write-Step "4. Creando runtime personalizado con jlink"

if (Test-Path $RuntimeDir) {
    Remove-Item $RuntimeDir -Recurse -Force
}

$JavaHome = (Get-Command java).Source | Split-Path | Split-Path
$jmodsPath = Join-Path $JavaHome "jmods"

# Descargar JavaFX jmods si no existe
$JavafxJmodsDir = Join-Path $PackageDir "javafx-jmods"
$JavafxVersion = "21.0.2"
$JavafxZip = Join-Path $PackageDir "javafx-jmods.zip"

if (-not (Test-Path $JavafxJmodsDir)) {
    Write-VerboseMsg "Descargando JavaFX jmods $JavafxVersion"
    
    if (-not (Test-Path $PackageDir)) {
        New-Item -ItemType Directory -Path $PackageDir | Out-Null
    }
    
    # Descargar los jmods (no el SDK completo)
    $JavafxUrl = "https://download2.gluonhq.com/openjfx/$JavafxVersion/openjfx-${JavafxVersion}_windows-x64_bin-jmods.zip"
    
    try {
        $oldProgress = $ProgressPreference
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $JavafxUrl -OutFile $JavafxZip
        $ProgressPreference = $oldProgress
        
        Write-VerboseMsg "Extrayendo JavaFX jmods"
        Expand-Archive -Path $JavafxZip -DestinationPath $PackageDir -Force
        Remove-Item $JavafxZip -Force
        
        # Renombrar el directorio extraído
        $ExtractedDir = Get-ChildItem -Path $PackageDir -Directory -Filter "javafx-jmods-*" | Select-Object -First 1
        if ($ExtractedDir) {
            Rename-Item $ExtractedDir.FullName $JavafxJmodsDir
        }
    } catch {
        Write-Error "No se pudo descargar JavaFX jmods: $_"
        exit 1
    }
}

# El zip de Gluon contiene los .jmod directamente en la carpeta extraída
$JavafxJmods = $JavafxJmodsDir
if (Test-Path (Join-Path $JavafxJmodsDir "jmods")) {
    $JavafxJmods = Join-Path $JavafxJmodsDir "jmods"
}

if (-not (Test-Path $JavafxJmods)) {
    Write-Error "Los jmods de JavaFX no se encontraron en $JavafxJmods"
    exit 1
}

Write-VerboseMsg "JavaFX jmods encontrados en: $JavafxJmods"

# Listar los jmods disponibles
$AvailableJmods = Get-ChildItem -Path $JavafxJmods -Filter "*.jmod" | ForEach-Object { $_.BaseName }
Write-VerboseMsg "Jmods de JavaFX disponibles: $($AvailableJmods -join ', ')"

# Combinar module-paths
$ModulePaths = "$jmodsPath;$JavafxJmods"

$jlinkArgs = @(
    "--module-path", $ModulePaths,
    "--add-modules", $AllModules,
    "--output", $RuntimeDir,
    "--no-header-files",
    "--no-man-pages",
    "--strip-debug",
    "--compress=2"
)

Write-VerboseMsg "Ejecutando jlink con módulos: $AllModules"
& jlink @jlinkArgs

if (-not (Test-Path $RuntimeDir)) {
    Write-Error "No se pudo crear el runtime personalizado"
    exit 1
}

Write-Host "✓ Runtime creado: $RuntimeDir" -ForegroundColor Green

# Paso 5: Preparar directorio de la aplicación
Write-Step "5. Preparando directorio de la aplicación"

if (Test-Path $AppDir) {
    Remove-Item $AppDir -Recurse -Force
}
New-Item -ItemType Directory -Path $AppDir | Out-Null

# Copiar fat JAR
Copy-Item $FatJar (Join-Path $AppDir "viewer.jar")

# Copiar CLI jar
$CliDir = Join-Path $AppDir "cli"
New-Item -ItemType Directory -Path $CliDir | Out-Null
Copy-Item $CliJar (Join-Path $CliDir "agentic-develop.jar")

Write-Host "✓ Aplicación preparada en: $AppDir" -ForegroundColor Green

# Paso 6: Empaquetar con jpackage
Write-Step "6. Empaquetando con jpackage"

if (Test-Path $DistDir) {
    Remove-Item $DistDir -Recurse -Force
}

$jpackageArgs = @(
    "--type", "app-image",
    "--name", $AppName,
    "--input", $AppDir,
    "--main-jar", "viewer.jar",
    "--main-class", "es.vargontoc.agentic.viewer.App",
    "--runtime-image", $RuntimeDir,
    "--dest", $DistDir,
    "--app-version", $AppVersion,
    "--vendor", "Agentic Development",
    "--description", "Agentic Development Environment Viewer",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Dstdout.encoding=UTF-8",
    "--java-options", "-Dstderr.encoding=UTF-8"
)

Write-VerboseMsg "Ejecutando jpackage"
& jpackage @jpackageArgs

$AppImage = Join-Path $DistDir $AppName
if (-not (Test-Path $AppImage)) {
    Write-Error "No se pudo crear la imagen de la aplicación"
    exit 1
}

Write-Host "✓ Aplicación empaquetada: $AppImage" -ForegroundColor Green

# Paso 7: Verificar que el CLI jar está en el directorio correcto
Write-Step "7. Verificando estructura final"

$FinalCliDir = Join-Path (Join-Path $AppImage "app") "cli"
$FinalCliJar = Join-Path $FinalCliDir "agentic-develop.jar"
if (-not (Test-Path $FinalCliJar)) {
    Write-Warning "El jar del CLI no está en $FinalCliJar"
    Write-Host "Creando directorio cli y copiando jar..." -ForegroundColor Yellow
    
    New-Item -ItemType Directory -Path $FinalCliDir -Force | Out-Null
    Copy-Item $CliJar $FinalCliJar
    
    Write-Host "✓ CLI jar copiado a $FinalCliJar" -ForegroundColor Green
}

# Paso 8 (opcional): instalador .msi para distribución
$MsiFile = $null
if ($Installer) {
    Write-Step "8. Generando instalador .msi"

    # jpackage necesita WiX Toolset 3.x (candle.exe y light.exe) para generar .msi
    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -or -not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
        Write-Error "Para generar el .msi hace falta WiX Toolset 3.x (candle.exe y light.exe) en el PATH."
        exit 1
    }

    $InstallerDir = Join-Path $DistDir "installer"
    if (Test-Path $InstallerDir) {
        Remove-Item $InstallerDir -Recurse -Force
    }

    # El UUID de actualización debe ser fijo para que una versión nueva sustituya a la anterior
    $msiArgs = @(
        "--type", "msi",
        "--app-image", $AppImage,
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--vendor", "Agentic Development",
        "--description", "Agentic Development Environment Viewer",
        "--win-upgrade-uuid", "6b1f6a52-3c1e-4c0a-9f3e-8d2b7a5c41e9",
        "--win-menu",
        "--win-shortcut",
        "--win-dir-chooser",
        "--dest", $InstallerDir
    )

    Write-VerboseMsg "Ejecutando jpackage (msi)"
    & jpackage @msiArgs

    $MsiFile = Get-ChildItem -Path $InstallerDir -Filter "*.msi" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $MsiFile) {
        Write-Error "No se generó el instalador .msi en $InstallerDir"
        exit 1
    }

    Write-Host "✓ Instalador creado: $($MsiFile.FullName)" -ForegroundColor Green
}

# Resumen
Write-Step "Resumen"
Write-Host "✓ Aplicación empaquetada exitosamente" -ForegroundColor Green
Write-Host ""
if ($MsiFile) {
    Write-Host "Instalador: $($MsiFile.FullName)" -ForegroundColor Cyan
    Write-Host ""
}
Write-Host "Ubicación: $AppImage" -ForegroundColor Cyan
Write-Host "Ejecutable: $(Join-Path $AppImage "$AppName.exe")" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ejecutar:" -ForegroundColor Yellow
Write-Host "  & `"$AppImage\$AppName.exe`"" -ForegroundColor White
Write-Host ""
Write-Host "La aplicación incluye:" -ForegroundColor Yellow
Write-Host "  - Runtime Java empaquetado (no requiere Java del sistema)" -ForegroundColor White
Write-Host "  - Viewer JAR con todas las dependencias" -ForegroundColor White
Write-Host "  - CLI JAR en cli/agentic-develop.jar" -ForegroundColor White
Write-Host ""
Write-Host "Funciona sin conexión a internet." -ForegroundColor Green
