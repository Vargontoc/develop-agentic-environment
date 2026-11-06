#!/usr/bin/env pwsh
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
    [switch]$Verbose
)

$ErrorActionPreference = "Stop"
$script:Verbose = $Verbose

function Write-Step {
    param([string]$Message)
    Write-Host "`n===> $Message" -ForegroundColor Cyan
}

function Write-Verbose {
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
$CliJar = Join-Path $ViewerDir "cli" "agentic-develop.jar"
$ViewerJar = Join-Path $BuildDir "viewer-0.1.0-SNAPSHOT.jar"
$FatJar = Join-Path $BuildDir "viewer-fat.jar"
$AppName = "AgenticViewer"
$AppVersion = "0.1.0"

# Verificar herramientas
Write-Step "Verificando herramientas"
$javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
Write-Verbose "Java: $javaVersion"

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
    Write-Error "El jar del CLI no existe en $CliJar. Compílalo primero con: cd cli && mvn package"
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

Write-Verbose "Extrayendo $ViewerJar"
Set-Location $TempExtract
jar xf $ViewerJar

# Extraer dependencias desde el classpath de Maven
Write-Verbose "Obteniendo classpath de dependencias"
$Dependencies = mvn dependency:build-classpath -DincludeScope=runtime -q -Dmdep.outputFile=CON 2>&1 | 
    Where-Object { $_ -notmatch "^\[" -and $_ -match "\.jar" }

if ($Dependencies) {
    $JarFiles = $Dependencies.Split(';') | Where-Object { $_ -match "\.jar$" }
    foreach ($DepJar in $JarFiles) {
        if (Test-Path $DepJar) {
            Write-Verbose "Extrayendo $DepJar"
            jar xf $DepJar
        }
    }
}

# Crear MANIFEST.MF
$ManifestDir = Join-Path $TempExtract "META-INF"
if (-not (Test-Path $ManifestDir)) {
    New-Item -ItemType Directory -Path $ManifestDir | Out-Null
}

$ManifestContent = @"
Manifest-Version: 1.0
Main-Class: es.vargontoc.agentic.viewer.App
"@
Set-Content -Path (Join-Path $ManifestDir "MANIFEST.MF") -Value $ManifestContent -Encoding UTF8

# Empaquetar fat JAR
Write-Verbose "Creando $FatJar"
Set-Location $BuildDir
jar cfm $FatJar (Join-Path $TempExtract "META-INF" "MANIFEST.MF") -C $TempExtract .

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
Write-Verbose "Módulos: $AllModules"

# Paso 4: Crear runtime personalizado con jlink
Write-Step "4. Creando runtime personalizado con jlink"

if (Test-Path $RuntimeDir) {
    Remove-Item $RuntimeDir -Recurse -Force
}

$jlinkCmd = "jlink --module-path `"$((Get-Command java).Source | Split-Path | Split-Path)\jmods`" --add-modules $AllModules --output `"$RuntimeDir`" --no-header-files --no-man-pages --strip-debug --compress=2"

Write-Verbose "Ejecutando: $jlinkCmd"
Invoke-Expression $jlinkCmd

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

$jpackageCmd = @"
jpackage 
  --type app-image 
  --name $AppName 
  --input "$AppDir" 
  --main-jar viewer.jar 
  --main-class es.vargontoc.agentic.viewer.App 
  --runtime "$RuntimeDir" 
  --dest "$DistDir" 
  --app-version $AppVersion 
  --vendor "Agentic Development" 
  --description "Agentic Development Environment Viewer" 
  --java-options "-Dfile.encoding=UTF-8"
  --java-options "-Dstdout.encoding=UTF-8"
  --java-options "-Dstderr.encoding=UTF-8"
"@

Write-Verbose "Ejecutando jpackage"
Invoke-Expression $jpackageCmd

$AppImage = Join-Path $DistDir $AppName
if (-not (Test-Path $AppImage)) {
    Write-Error "No se pudo crear la imagen de la aplicación"
    exit 1
}

Write-Host "✓ Aplicación empaquetada: $AppImage" -ForegroundColor Green

# Paso 7: Verificar que el CLI jar está en el directorio correcto
Write-Step "7. Verificando estructura final"

$FinalCliJar = Join-Path $AppImage "app" "cli" "agentic-develop.jar"
if (-not (Test-Path $FinalCliJar)) {
    Write-Warning "El jar del CLI no está en $FinalCliJar"
    Write-Host "Creando directorio cli y copiando jar..." -ForegroundColor Yellow
    
    $FinalCliDir = Join-Path $AppImage "app" "cli"
    New-Item -ItemType Directory -Path $FinalCliDir -Force | Out-Null
    Copy-Item $CliJar $FinalCliJar
    
    Write-Host "✓ CLI jar copiado a $FinalCliJar" -ForegroundColor Green
}

# Resumen
Write-Step "Resumen"
Write-Host "✓ Aplicación empaquetada exitosamente" -ForegroundColor Green
Write-Host ""
Write-Host "Ubicación: $AppImage" -ForegroundColor Cyan
Write-Host "Ejecutable: $(Join-Path $AppImage "$AppName.exe")" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ejecutar:" -ForegroundColor Yellow
Write-Host "  & `"$AppImage\$AppName.exe`"" -ForegroundColor White
Write-Host ""
Write-Host "La aplicación incluye:" -ForegroundColor Yellow
Write-Host "  - Runtime Java personalizado ($(Get-ChildItem $RuntimeDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB) MB)"
Write-Host "  - Viewer JAR con todas las dependencias"
Write-Host "  - CLI JAR en cli/agentic-develop.jar"
Write-Host ""
Write-Host "Funciona sin Java instalado en el sistema y sin conexión a internet." -ForegroundColor Green
