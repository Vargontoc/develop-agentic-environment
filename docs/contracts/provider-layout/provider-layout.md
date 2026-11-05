# Contrato `provider-layout`: rutas, formatos y cabeceras

Dónde vive cada proveedor, qué formato tienen sus agentes y cómo varía la cabecera según el sistema operativo.
Fuente única de las rutas: `PathResolver`.

## Rutas globales

Raíz de usuario: `%USERPROFILE%` en Windows y `$HOME` en Linux. Sin ella la aplicación no puede funcionar
(`internal.env`).

| Proveedor (`id`) | Windows | Linux |
|---|---|---|
| Claude (`claude`) | `%USERPROFILE%\.claude` | `$HOME/.claude` |
| Codex (`codex`) | `%USERPROFILE%\.codex` | `$HOME/.codex` |
| Gemini (`gemini`) | `%USERPROFILE%\.gemini\config` | `$HOME/.gemini/config` |
| OpenCode (`opencode`) | `%USERPROFILE%\.config\opencode` | `$XDG_CONFIG_HOME/opencode` (si es absoluta) o `$HOME/.config/opencode` |

Las variables `XDG_*` solo se admiten si son rutas absolutas; si no, se ignoran. Toda ruta que se escribe se comprueba
dentro de su ámbito tras resolver `..` y enlaces simbólicos (`validation.path.out_of_scope`). Linux distingue
mayúsculas y minúsculas.

## Rutas de proyecto

Solo algunos proveedores admiten ámbito de proyecto. La carpeta es relativa a la raíz del proyecto.

| Proveedor | Carpeta de proyecto |
|---|---|
| Claude | `.claude` |
| Gemini | `.agents` |
| OpenCode | `.opencode` |
| Codex | *(sin soporte; se omite con un aviso)* |

Además de la carpeta del proveedor, el proyecto lleva `.sdd/` (ver `sdd-team`).

## Artefactos instalados

Rutas relativas a la carpeta del proveedor.

| Tipo | Global | Proyecto |
|---|---|---|
| Agente | `agents/<nombre>.<md\|toml>` | `agents/<técnico>-router.md` (uno por técnico y proveedor), `agents/<técnico>-<módulo>.md` |
| Skill | `skills/<nombre>/` (directorio con `SKILL.md` y apoyo) | `skills/<nombre>/` |
| Perfil | `sdd.config.toml` (solo Codex) | — |

Los nombres de agente de Codex usan `_` (`sdd_agent.toml`); sus grupos usan `-`.
Lo que no figura en el catálogo (agentes y skills del usuario) no es un artefacto de la aplicación y nunca se toca.

## Formatos

| Formato | Proveedores | Estructura |
|---|---|---|
| Markdown | Claude, Gemini, OpenCode | Cabecera YAML entre `---` y, tras una línea en blanco, el cuerpo. |
| TOML de Codex | Codex | Cabecera con `developer_instructions = ` y el cuerpo como cadena multilínea entre `"""`; el cuerpo se escapa para no romper el TOML y se recompone sin pérdida. |

La cabecera es propia de cada proveedor y de cada agente; el **cuerpo** es compartido. Guardar o propagar solo cambia el
cuerpo (la cabecera se conserva tal cual). Skills y perfiles no tienen formato de cabecera/cuerpo.

## Cabeceras por sistema operativo

Cada agente tiene **un solo recurso** de cabecera por proveedor, con marcadores que se sustituyen al instalar según
el sistema de destino (el del equipo en el que se ejecuta el CLI).

| Marcador | Windows | Linux |
|---|---|---|
| `{shell}` | `powershell` | `bash` |
| `{home}` | `$env:USERPROFILE` | `$HOME` |
| `{os}` | `windows` | `linux` |
| `{hook:<script> <args>}` | `& "$env:USERPROFILE\<dir>\hooks\<script>.bat" <args>; exit $LASTEXITCODE` | `"$HOME/<dir>/hooks/<script>.sh" <args>` |

`<dir>` es la carpeta global del proveedor relativa a la raíz de usuario, con `/`: `.claude`, `.codex`,
`.gemini/config` y `.config/opencode` (en Windows se escribe con `\`).

Reglas:
- En Linux la cabecera generada no contiene `powershell`, `USERPROFILE` ni barras invertidas.
- Cualquier otra llave de la cabecera se deja intacta. Un `{hook:}` sin script es un error del recurso
  (`internal.resource.header_marker`, con el marcador como ruta) y el artefacto afectado se omite con un aviso.
- El cuerpo y la cabecera de los agentes de proyecto admiten además los placeholders `{module}`, `{slug-provider}` y
  `{slug-path-agents}`, que se resuelven al generar cada artefacto.
- El script del hook (`hooks/<script>.bat` o `.sh`) lo aporta el usuario en la carpeta global del proveedor; la
  aplicación no lo instala.

## Casos de error

| Caso | Resultado |
|---|---|
| Proveedor desconocido | `validation.provider.unknown` con el valor escrito. |
| Proveedor sin ámbito de proyecto | `validation.artifact.scope_unsupported` (en `create-artifact`) o aviso y omisión (en `create-module`/`add-technician`). |
| Falta la cabecera de un proveedor para un agente | El agente no existe en su catálogo; en proyecto se avisa «no existe el recurso …» y no se genera. |
| Marcador de cabecera vacío | `internal.resource.header_marker`. |
| Ruta fuera del ámbito | `validation.path.out_of_scope`. |
| Sin raíz de usuario | `internal.env`. |
