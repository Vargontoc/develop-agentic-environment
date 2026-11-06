#!/usr/bin/env bash
# Limita las escrituras de un agente a las rutas permitidas para ese agente.
# Uso (hook PreToolUse de Edit|Write): sdd-specs-only.sh <ruta> [<ruta> ...]
# Raiz del proyecto: carpeta mas cercana, subiendo desde el cwd del hook, que contiene .sdd/project.md.
# Si no se encuentra, CLAUDE_PROJECT_DIR. Varias instancias pueden ejecutarse a la vez:
# no se comparte ningun fichero temporal y el registro se separa por politica.
# Salida: 0 permite la escritura, 2 la bloquea.

POLICY="$*"
RC=2
AGENT=""
FILE=""
CWD=""
ABS=""
ROOT=""

LOGDIR="${TMPDIR:-/tmp}"

log() {
  local tag="$POLICY"
  tag="${tag// /_}"
  tag="${tag//\//_}"
  tag="${tag//\\/_}"
  tag="${tag//./_}"
  printf '[%s] %s agente=%s cwd=%s fichero=%s raiz=%s proyecto=%s permitido=%s\n' \
    "$(date '+%Y-%m-%d %H:%M:%S')" "$1" "$AGENT" "$CWD" "$ABS" "$ROOT" "${CLAUDE_PROJECT_DIR:-}" "$POLICY" \
    >>"$LOGDIR/sdd-hook-$tag.log" 2>/dev/null
  return 0
}

if ! command -v jq >/dev/null 2>&1; then
  echo "Bloqueado: no se encuentra jq en el PATH." >&2
  exit "$RC"
fi

INPUT="$(cat)"
FILE="$(printf '%s' "$INPUT" | jq -r '.tool_input.file_path // empty' 2>/dev/null)"
AGENT="$(printf '%s' "$INPUT" | jq -r '.agent_type // empty' 2>/dev/null)"
CWD="$(printf '%s' "$INPUT" | jq -r '.cwd // empty' 2>/dev/null)"

if [ -z "$FILE" ]; then
  log "DENY sin-ruta"
  echo "Bloqueado: no se pudo determinar la ruta." >&2
  exit "$RC"
fi

# Tabla de agentes con rutas propias. Anade una linea por cada agente nuevo con permisos distintos.
case "$AGENT" in
  docs|docs-readme) POLICY="README.md" ;;
esac

# Raiz del proyecto: sube desde el cwd hasta encontrar .sdd/project.md.
DIR="${CWD:-$PWD}"
[ "$DIR" != "/" ] && DIR="${DIR%/}"
while :; do
  if [ -f "$DIR/.sdd/project.md" ]; then
    ROOT="$DIR"
    break
  fi
  PARENT="$(dirname "$DIR")"
  [ "$PARENT" = "$DIR" ] && break
  DIR="$PARENT"
done
[ -z "$ROOT" ] && ROOT="${CLAUDE_PROJECT_DIR:-}"
[ -z "$ROOT" ] && ROOT="$PWD"
[ "$ROOT" != "/" ] && ROOT="${ROOT%/}"

# Ruta absoluta: las relativas se resuelven contra la raiz.
case "$FILE" in
  /*) ABS="$FILE" ;;
  *)  ABS="$ROOT/$FILE" ;;
esac

# No se admite '..' en ningun tramo de la ruta.
case "$ABS" in
  *..*)
    log "DENY punto-punto"
    echo "Bloqueado: no se permiten '..' en la ruta." >&2
    exit "$RC"
    ;;
esac

# Permitido si la ruta es una de las de la politica o cuelga de ella.
OK=""
for allowed in $POLICY; do
  allowed="${allowed%/}"
  base="$ROOT/$allowed"
  if [ "$ABS" = "$base" ]; then
    OK=1
  else
    case "$ABS" in
      "$base"/*) OK=1 ;;
    esac
  fi
done

if [ -n "$OK" ]; then
  log "ALLOW"
  exit 0
fi

log "DENY"
echo "Bloqueado: este agente solo puede escribir en: $POLICY - raiz del proyecto: $ROOT" >&2
exit "$RC"
