# Contrato `provider-layout`: rutas, formatos y cabeceras

Dónde vive cada proveedor, qué formato tienen sus agentes y cómo varía la cabecera según el sistema operativo.
Fuentes únicas de las rutas: `PathResolver` (carpetas globales) y `ProviderLayout` (dónde cuelga cada tipo de
artefacto).

## Rutas globales

Raíz de usuario: `%USERPROFILE%` en Windows y `$HOME` en Linux. Sin ella la aplicación no puede funcionar
(`internal.env`).

| Proveedor (`id`) | Windows | Linux |
|---|---|---|
| Claude (`claude`) | `%USERPROFILE%\.claude` | `$HOME/.claude` |
| Codex (`codex`) | `%USERPROFILE%\.codex` | `$HOME/.codex` |
| Gemini (`gemini`) | `%USERPROFILE%\.gemini\config` | `$HOME/.gemini/config` |
| OpenCode (`opencode`) | `%USERPROFILE%\.config\opencode` | `$XDG_CONFIG_HOME/opencode` (si es absoluta) o `$HOME/.config/opencode` |

La carpeta global es la que el CLI comprueba para saber si el proveedor está instalado; en Claude es además donde se
instala el hook. Los artefactos no cuelgan siempre de ella (ver «Ubicación de los artefactos»).

Las variables `XDG_*` solo se admiten si son rutas absolutas; si no, se ignoran. Toda ruta que se escribe se comprueba
dentro de su ámbito tras resolver `..` y enlaces simbólicos (`validation.path.out_of_scope`). Linux distingue
mayúsculas y minúsculas.

## Ubicación de los artefactos

Cada artefacto tiene una **ruta lógica**, igual en todos los proveedores (`agents/<nombre>.<md|toml>`,
`skills/<nombre>`, `profiles/<nombre>.toml`), y una **ubicación física** que depende del proveedor y del ámbito. La
ruta lógica es la que usan las salidas del CLI (`created`, `skipped`); la ubicación es relativa a la **base** del
proveedor: su carpeta global (Claude, OpenCode) o el home del usuario / la raíz del proyecto (Codex, Gemini).

Raíz de proyecto = la carpeta del proyecto. `~` = raíz de usuario.

| Proveedor | Skill | Agente | Perfil |
|---|---|---|---|
| Claude | global `~/.claude/skills/<s>/`; proyecto `.claude/skills/<s>/` | `~/.claude/agents/<a>.md`; `.claude/agents/<a>.md` | — |
| OpenCode | `~/.config/opencode/skills/<s>/`; `.opencode/skills/<s>/` | `~/.config/opencode/agents/<a>.md`; `.opencode/agents/<a>.md` | — |
| Codex | `~/.agents/skills/<s>/`; `.agents/skills/<s>/`, con `agents/openai.yaml` | `~/.codex/agents/<a>.toml`; `.codex/agents/<a>.toml` | `~/.codex/profiles/<p>.toml`; `.codex/profiles/<p>.toml` |
| Gemini | `~/.gemini/skills/<s>/`; `.agents/skills/<s>/` | `~/.gemini/config/agents/<a>/agent.md`; `.agents/agents/<a>/agent.md` | — |

Carpetas de proyecto de cada proveedor: Claude `.claude`; OpenCode `.opencode`; Codex `.agents` y `.codex`; Gemini
`.agents`. En todos los proveedores hay ámbito de proyecto. Además de ellas, el proyecto lleva `.sdd/` (ver `sdd-team`).

Particularidades:

- **Codex y Gemini comparten `.agents/skills/` en el proyecto.** Las skills son las mismas en formato y contenido;
  Codex añade `agents/openai.yaml` a cada una. Si la skill ya la dejó el otro proveedor, no se reescribe: solo se añade
  ese fichero, y si la skill estaba registrada y sin cambios, su registro se actualiza para que el fichero no cuente
  como una modificación. Una única ruta tiene un único registro.
- **Router de Codex en proyecto:** es una skill (`.agents/skills/<técnico>-router/SKILL.md`, con su
  `agents/openai.yaml`), no un agente. Los especialistas de Codex son `.codex/agents/<técnico>_<módulo>.toml`.
- **Perfiles de Codex en proyecto:** uno por agente y módulo, `.codex/profiles/<técnico>-<módulo>.toml`
  (`default_permissions` y la tabla `permissions."<técnico>-<módulo>"`). No hay perfil del router. Crear un módulo crea
  un fichero; no se editan ficheros ya existentes. Hoy los perfiles son ficheros independientes: el CLI no los fusiona
  en `config.toml` ni los referencia.
- **Gemini:** cada agente es una carpeta con `agent.md` (`.agents/agents/<técnico>-<módulo>/agent.md`).

## Artefactos instalados

Rutas lógicas, relativas a la base del proveedor y ámbito.

| Tipo | Global | Proyecto |
|---|---|---|
| Agente | `agents/<nombre>.<md\|toml>` | `agents/<técnico>-router.md` (uno por técnico y proveedor), `agents/<técnico>-<módulo>.md` (Codex: `agents/<técnico>_<módulo>.toml`) |
| Skill | `skills/<nombre>/` (directorio con `SKILL.md` y apoyo) | `skills/<nombre>/` |
| Perfil | `profiles/sdd.config.toml` (solo Codex) | `profiles/<técnico>-<módulo>.toml` (solo Codex) |
| Hook | `hooks/sdd-specs-only.bat` (Windows) o `hooks/sdd-specs-only.sh` (Linux), en la carpeta global de Claude | — (solo ámbito global) |

Los nombres de agente de Codex usan `_` (`sdd_agent.toml`, `analista_core.toml`); sus grupos usan `-`.
Lo que no figura en el catálogo (agentes y skills del usuario) no es un artefacto de la aplicación y nunca se toca.
El hook no está en el catálogo, pero la aplicación lo instala y lo registra (ver «Hook `sdd-specs-only`»).
El árbol (`tree`) lo muestra como un nodo `hook` bajo el nodo global de Claude; nunca en el proyecto ni en los demás proveedores.

## Hook `sdd-specs-only`

`configure-provider` (y la instalación global del menú) instala el hook que restringe las escrituras de los agentes SDD,
solo en la carpeta global de Claude y después de sus agentes y skills.

- **Ruta:** `hooks/sdd-specs-only.bat` en Windows y `hooks/sdd-specs-only.sh` en Linux, dentro de la carpeta global de
  Claude (ver «Rutas globales»). Solo se instala la variante del sistema del equipo, nunca las dos. La ruta se
  valida dentro del ámbito global, también ante una carpeta `hooks/` que sea un enlace hacia fuera: si sale, no se
  instala nada y se avisa.
- **Contenido:** el recurso incorporado en la aplicación, tal cual. El hook no lleva marcadores ni se compone.
- **Linux:** se escribe con permiso de ejecución solo para el usuario (`rwx------`). Si el sistema de ficheros no
  permite fijarlo, el hook queda instalado y se avisa.
- **Solo ámbito global.** Crear un módulo, un técnico o instalar en un proyecto no instala ni modifica ningún hook;
  los permisos de proyecto se configuran a mano. Nunca se crea `hooks/` en la carpeta de proyecto del proveedor.
- **Solo ese fichero.** El resto de `hooks/` es del usuario y la aplicación no lo toca.
- **Actualización por hash** (ver `artifact-metadata`, «Hook `sdd-specs-only`»):

| Situación | Resultado |
|---|---|
| El hook no existe | Se instala y se registra. En `created`. |
| Existe, con registro y sin cambios del usuario, y la versión incorporada es distinta | Se sustituye y se actualiza el registro. En `created`, sin aviso. |
| Existe, con registro y sin cambios del usuario, e igual a la incorporada | No se reescribe. En `skipped`, sin aviso. |
| Existe y su hash difiere del registrado | No se sobrescribe ni se cambia el registro. En `skipped` y un aviso: modificado por el usuario, no se ha actualizado. |
| Existe sin registro (también almacén ausente o ilegible) | No se sobrescribe ni se crea registro. En `skipped` y un aviso: no se puede comprobar si fue modificado. Se repite en cada ejecución. |

Los avisos no son errores: no cambian el código de salida ni detienen la instalación del resto de artefactos.
Las rutas de `created`, `skipped` y `warnings` son relativas a la carpeta de Claude (`hooks/sdd-specs-only.sh`).

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
- El cuerpo y la cabecera de los agentes de proyecto admiten además los placeholders `{module}`, `{slug-provider}`,
  `{slug-path-agents}` y `{slug-path-specialist:<rol>}`, que se resuelven al generar cada artefacto.
  `{slug-provider}` es la carpeta (o carpetas) del proveedor en el proyecto; `{slug-path-agents}`, la de sus agentes;
  `{slug-path-specialist:<rol>}`, la ruta del especialista de ese rol con `<modulo>` sin resolver
  (`.claude/agents/analista-<modulo>.md`, `.codex/agents/analista_<modulo>.toml`,
  `.agents/agents/analista-<modulo>/agent.md`).
- El comando de los marcadores `{hook:}` apunta a `hooks/<script>.bat` o `.sh` en la carpeta global del proveedor.
  La aplicación instala `sdd-specs-only` (ver «Hook `sdd-specs-only`»); los demás scripts de `hooks/` son del usuario.

## Casos de error

| Caso | Resultado |
|---|---|
| Proveedor desconocido | `validation.provider.unknown` con el valor escrito. |
| Ámbito global sin raíz de usuario | `validation.artifact.scope_unsupported` (en `create-artifact`). |
| Carpeta de proyecto de un proveedor fuera del proyecto (enlace hacia fuera) | Aviso y omisión (en `create-module`/`add-technician`). |
| Falta la cabecera de un proveedor para un agente | El agente no existe en su catálogo; en proyecto se avisa «no existe el recurso …» y no se genera. |
| Marcador de cabecera vacío | `internal.resource.header_marker`. |
| Hook modificado por el usuario | Se omite con un aviso; no es un error. |
| Hook existente sin registro | Se omite con un aviso, en cada ejecución; no es un error. |
| Ruta del hook fuera del ámbito global (por ejemplo, `hooks/` es un enlace hacia fuera) | No se instala; se omite con un aviso que recoge el motivo. No es un error. |
| Ruta fuera del ámbito | `validation.path.out_of_scope`. |
| Sin raíz de usuario | `internal.env`. |
