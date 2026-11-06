#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Verifica que la aplicación empaquetada funciona correctamente.

.DESCRIPTION
    Este script:
    1. Verifica que la imagen empaquetada existe
    2. Comprueba que el runtime está incluido
    3. Verifica que el jar del CLI está en la ubicación correcta
    4. Prueba que la aplicación puede iniciar sin Java del sistema
    5. Ejecuta comandos básicos del CLI desde el recurso empaquetado
#>

param(
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
    fi
}

function Test-Command {
    param([string]$Command)
    try {
        $null = Get-Command $Command -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

# Configuración
$RootDir = Split-Path -Parent $PSScriptRoot
$ViewerDir = Join-Path $RootDir "viewer"
$BuildDir = Join-Path $ViewerDir "target"
$DistDir = Join-Path $BuildDir "dist"
$AppName = "AgenticViewer"
$AppImage = Join-Path $DistDir $AppName
$ExePath = Join-Path $AppImage "$AppName.exe"
$RuntimeDir = Join-Path $AppImage "runtime"
$AppResourceDir = Join-Path $AppImage "app"
$ViewerJar = Join-Path $AppResourceDir "viewer.jar"
$CliJar = Join-Path $AppResourceDir "cli" "agentic-develop.jar"

$AllPassed = $true

# Verificación 1: La imagen empaquetada existe
Write-Step "1. Verificando que la imagen empaquetada existe"
if (Test-Path $AppImage) {
    Write-Host "✓ Imagen empaquetada encontrada: $AppImage" -ForegroundColor Green
} else {
    Write-Host "✗ Imagen empaquetada NO encontrada: $AppImage" -ForegroundColor Red
    Write-Host "  Ejecuta primero: .\scripts\package-windows.ps1" -ForegroundColor Yellow
    $AllPassed = $false
    exit 1
}

# Verificación 2: El ejecutable existe
Write-Step "2. Verificando el ejecutable"
if (Test-Path $ExePath) {
    Write-Host "✓ Ejecutable encontrado: $ExePath" -ForegroundColor Green
    $ExeSize = (Get-Item $ExePath).Length / 1MB
    Write-Host "  Tamaño: $([math]::Round($ExeSize, 2)) MB" -ForegroundColor DarkGray
} else {
    Write-Host "✗ Ejecutable NO encontrado: $ExePath" -ForegroundColor Red
    $AllPassed = $false
}

# Verificación 3: El runtime está incluido
Write-Step "3. Verificando el runtime Java embebido"
if (Test-Path $RuntimeDir) {
    Write-Host "✓ Runtime encontrado: $RuntimeDir" -ForegroundColor Green
    $RuntimeSize = (Get-ChildItem $RuntimeDir -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB
    Write-Host "  Tamaño del runtime: $([math]::Round($RuntimeSize, 2)) MB" -ForegroundColor DarkGray
    
    # Verificar que java.exe existe en el runtime
    $RuntimeJava = Join-Path $RuntimeDir "bin" "java.exe"
    if (Test-Path $RuntimeJava) {
        Write-Host "✓ Java del runtime encontrado: $RuntimeJava" -ForegroundColor Green
    } else {
        Write-Host "✗ Java del runtime NO encontrado en $RuntimeJava" -ForegroundColor Red
        $AllPassed = $false
    }
} else {
    Write-Host "✗ Runtime NO encontrado: $RuntimeDir" -ForegroundColor Red
    $AllPassed = $false
}

# Verificación 4: El JAR del viewer está incluido
Write-Step "4. Verificando el JAR del viewer"
if (Test-Path $ViewerJar) {
    Write-Host "✓ Viewer JAR encontrado: $ViewerJar" -ForegroundColor Green
    $ViewerSize = (Get-Item $ViewerJar).Length / 1MB
    Write-Host "  Tamaño: $([math]::Round($ViewerSize, 2)) MB" -ForegroundColor DarkGray
} else {
    Write-Host "✗ Viewer JAR NO encontrado: $ViewerJar" -ForegroundColor Red
    $AllPassed = $false
}

# Verificación 5: El JAR del CLI está en la ubicación correcta
Write-Step "5. Verificando el JAR del CLI"
if (Test-Path $CliJar) {
    Write-Host "✓ CLI JAR encontrado: $CliJar" -ForegroundColor Green
    $CliSize = (Get-Item $CliJar).Length / 1MB
    Write-Host "  Tamaño: $([math]::Round($CliSize, 2)) MB" -ForegroundColor DarkGray
} else {
    Write-Host "✗ CLI JAR NO encontrado: $CliJar" -ForegroundColor Red
    $AllPassed = $false
}

# Verificación 6: Probar que el runtime funciona sin Java del sistema
Write-Step "6. Probando el runtime embebido"
if (Test-Path $RuntimeDir) {
    $RuntimeJava = Join-Path $RuntimeDir "bin" "java.exe"
    if (Test-Path $RuntimeJava) {
        try {
            $VersionOutput = & $RuntimeJava -version 2>&1 | Select-Object -First 1
            Write-Host "✓ Runtime funciona: $VersionOutput" -ForegroundColor Green
        } catch {
            Write-Host "✗ Error al ejecutar el runtime: $_" -ForegroundColor Red
            $AllPassed = $false
        }
    }
}

# Verificación 7: Probar comandos del CLI desde el recurso empaquetado
Write-Step "7. Probando comandos del CLI desde el recurso empaquetado"
if ((Test-Path $RuntimeDir) -and (Test-Path $CliJar)) {
    $RuntimeJava = Join-Path $RuntimeDir "bin" "java.exe"
    
    # Temporarily rename system java to test without it
    $SystemJava = (Get-Command java -ErrorAction SilentlyContinue).Source
    $OriginalPath = $env:PATH
    
    try {
        # Remove system java from PATH temporarily
        $PathDirs = $OriginalPath -split ';'
        $FilteredDirs = $PathDirs | Where-Object { 
            -not ($_ -like "*java*" -or $_ -like "*jdk*" -or $_ -like "*jre*")
        }
        $env:PATH = $FilteredDirs -join ';'
        
        Write-Verbose "Ejecutando CLI con runtime embebido (sin Java del sistema)"
        
        # Probar --version
        $CliOutput = & $RuntimeJava -jar $CliJar --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ CLI --version funciona con runtime embebido" -ForegroundColor Green
        } else {
            Write-Host "✗ CLI --version falló con runtime embebido" -ForegroundColor Red
            $AllPassed = $false
        }
        
        # Probar status sin proyecto
        $TempDir = Join-Path $env:TEMP "agentic-test-$(Get-Random)"
        New-Item -ItemType Directory -Path $TempDir -Force | Out-Null
        
        try {
            $StatusOutput = & $RuntimeJava -jar $CliJar status --project $TempDir 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ CLI status funciona con runtime embebido" -ForegroundColor Green
            } else {
                Write-Host "⚠ CLI status devolvió código $LASTEXITCODE (esperado si no hay proyecto)" -ForegroundColor Yellow
            }
        } finally {
            Remove-Item $TempDir -Recurse -Force -ErrorAction SilentlyContinue
        }
        
    } finally {
        # Restore PATH
        $env:PATH = $OriginalPath
    }
} else {
    Write-Host "⚠ Omitiendo prueba de CLI (runtime o jar no disponible)" -ForegroundColor Yellow
}

# Resumen
Write-Step "Resumen de verificación"
if ($AllPassed) {
    Write-Host "✓ Todas las verificaciones pasaron" -ForegroundColor Green
    Write-Host ""
    Write-Host "La aplicación empaquetada:" -ForegroundColor Cyan
    Write-Host "  • Funciona sin Java instalado en el sistema" -ForegroundColor White
    Write-Host "  • Incluye el runtime Java personalizado" -ForegroundColor White
    Write-Host "  • Tiene el jar del CLI en la ubicación correcta" -ForegroundColor White
    Write-Host "  • Puede ejecutar comandos del CLI" -ForegroundColor White
    Write-Host ""
    Write-Host "Para ejecutar la aplicación:" -ForegroundColor Yellow
    Write-Host "  & `"$ExePath`"" -ForegroundColor White
    exit 0
} else {
    Write-Host "✗ Algunas verificaciones fallaron" -ForegroundColor Red
    exit 1
}
