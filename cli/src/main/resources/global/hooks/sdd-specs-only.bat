@echo off
rem Limita las escrituras de un agente a las rutas permitidas para ese agente.
rem Uso (hook PreToolUse de Edit|Write): sdd-specs-only.bat <ruta> [<ruta> ...]
rem Raiz del proyecto: carpeta mas cercana, subiendo desde el cwd del hook, que contiene .sdd\project.md.
rem Si no se encuentra, CLAUDE_PROJECT_DIR. Varias instancias pueden ejecutarse a la vez:
rem cada una usa su propia carpeta temporal y su registro se separa por politica.
setlocal EnableExtensions EnableDelayedExpansion
set "RC=2"
set "WORK="
set "AGENT="
set "FILE="
set "CWD="
set "ABS="
set "ROOT="
set "POLICY=%*"

where jq >nul 2>&1
if errorlevel 1 (
  echo Bloqueado: no se encuentra jq en el PATH. 1>&2
  goto fin
)

for /l %%N in (1,1,50) do if not defined WORK call :mkwork
if not defined WORK (
  echo Bloqueado: no se pudo crear una carpeta temporal. 1>&2
  goto fin
)

jq -c "." > "%WORK%\in.json" 2>nul
jq -r --arg d - "(.tool_input.file_path // $d), (.agent_type // $d), (.cwd // $d)" "%WORK%\in.json" > "%WORK%\out.txt" 2>nul

set "I=0"
for /f "usebackq delims=" %%L in ("%WORK%\out.txt") do (
  set /a I+=1
  if !I!==1 set "FILE=%%L"
  if !I!==2 set "AGENT=%%L"
  if !I!==3 set "CWD=%%L"
)
if "!FILE!"=="-" set "FILE="
if "!AGENT!"=="-" set "AGENT="
if "!CWD!"=="-" set "CWD="

if not defined FILE (
  call :log "DENY sin-ruta"
  echo Bloqueado: no se pudo determinar la ruta. 1>&2
  goto fin
)

rem Tabla de agentes con rutas propias. Anade una linea por cada agente nuevo con permisos distintos.
if /i "!AGENT!"=="docs" set "POLICY=README.md"
if /i "!AGENT!"=="docs-readme" set "POLICY=README.md"
set "DIR=!CWD!"
if not defined DIR set "DIR=%CD%"
set "DIR=!DIR:/=\!"
if "!DIR:~-1!"=="\" if not "!DIR:~-2!"==":\" set "DIR=!DIR:~0,-1!"
:up
if exist "!DIR!\.sdd\project.md" (
  set "ROOT=!DIR!"
  goto rootdone
)
for %%I in ("!DIR!\..") do set "PARENT=%%~fI"
if /i "!PARENT!"=="!DIR!" goto rootdone
set "DIR=!PARENT!"
goto up
:rootdone
if not defined ROOT set "ROOT=%CLAUDE_PROJECT_DIR%"
if not defined ROOT set "ROOT=%CD%"
set "ROOT=!ROOT:/=\!"
if "!ROOT:~-1!"=="\" set "ROOT=!ROOT:~0,-1!"

set "FILE=!FILE:/=\!"
set "ABS=!FILE!"
if not "!FILE:~1,1!"==":" if not "!FILE:~0,2!"=="\\" set "ABS=!ROOT!\!FILE!"

set "CHK=!ABS:..=!"
if not "!CHK!"=="!ABS!" (
  call :log "DENY punto-punto"
  echo Bloqueado: no se permiten '..' en la ruta. 1>&2
  goto fin
)

set "OK="
for %%A in (!POLICY!) do call :check "%%~A"
if defined OK (
  call :log "ALLOW"
  set "RC=0"
  goto fin
)

call :log "DENY"
echo Bloqueado: este agente solo puede escribir en: !POLICY! - raiz del proyecto: !ROOT! 1>&2
goto fin

:fin
if defined WORK rmdir /s /q "!WORK!" 2>nul
exit /b !RC!

:mkwork
set "T=%TEMP%\sdd-hook-%RANDOM%-%RANDOM%"
md "%T%" 2>nul && set "WORK=%T%"
exit /b 0

:log
set "TAG=!POLICY!"
set "TAG=!TAG: =_!"
set "TAG=!TAG:/=_!"
set "TAG=!TAG:\=_!"
set "TAG=!TAG:.=_!"
2>nul >>"%TEMP%\sdd-hook-!TAG!.log" echo [%DATE% %TIME%] %~1 agente=!AGENT! cwd=!CWD! fichero=!ABS! raiz=!ROOT! proyecto=%CLAUDE_PROJECT_DIR% permitido=!POLICY!
exit /b 0

:check
set "ALLOWED=%~1"
set "ALLOWED=!ALLOWED:/=\!"
if "!ALLOWED:~-1!"=="\" set "ALLOWED=!ALLOWED:~0,-1!"
set "BASE=!ROOT!\!ALLOWED!"
call :strlen BASE LEN
if /i "!ABS!"=="!BASE!" set "OK=1"
if /i "!ABS:~0,%LEN%!"=="!BASE!" if "!ABS:~%LEN%,1!"=="\" set "OK=1"
exit /b 0

:strlen
set "STR=!%~1!"
set "N=0"
:strlen_loop
if defined STR (
  set "STR=!STR:~1!"
  set /a N+=1
  goto strlen_loop
)
set "%~2=!N!"
exit /b 0