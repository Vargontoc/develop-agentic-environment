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
| Hook | `hooks/sdd-specs-only.bat` (Windows) o `hooks/sdd-specs-only.sh` (Linux) | — (solo ámbito global) |

Los nombres de agente de Codex usan `_` (`sdd_agent.toml`); sus grupos usan `-`.
Lo que no figura en el catálogo (agentes y skills del usuario) no es un artefacto de la aplicación y nunca se toca.
El hook no está en el catálogo, pero la aplicación lo instala y lo registra (ver «Hook `sdd-specs-only`»).

## Hook `sdd-specs-only`

`configure-provider` (y la instalación global del menú) instala el hook que restringe las escrituras de los agentes SDD,
una vez por proveedor y después de sus agentes y skills.

- **Ruta:** `hooks/sdd-specs-only.bat` en Windows y `hooks/sdd-specs-only.sh` en Linux, dentro de la carpeta global del
  proveedor (ver «Rutas globales»). Solo se instala la variante del sistema del equipo, nunca las dos. La ruta se
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

Los avisos no son errores: no cambian el código de salida ni detienen la instalación del resto de artefactos ni de los
demás proveedores. Las rutas de `created`, `skipped` y `warnings` son relativas al proveedor
(`hooks/sdd-specs-only.sh`).

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
- El comando de los marcadores `{hook:}` apunta a `hooks/<script>.bat` o `.sh` en la carpeta global del proveedor.
  La aplicación instala `sdd-specs-only` (ver «Hook `sdd-specs-only`»); los demás scripts de `hooks/` son del usuario.

## Casos de error

| Caso | Resultado |
|---|---|
| Proveedor desconocido | `validation.provider.unknown` con el valor escrito. |
| Proveedor sin ámbito de proyecto | `validation.artifact.scope_unsupported` (en `create-artifact`) o aviso y omisión (en `create-module`/`add-technician`). |
| Falta la cabecera de un proveedor para un agente | El agente no existe en su catálogo; en proyecto se avisa «no existe el recurso …» y no se genera. |
| Marcador de cabecera vacío | `internal.resource.header_marker`. |
| Hook modificado por el usuario | Se omite con un aviso; no es un error. |
| Hook existente sin registro | Se omite con un aviso, en cada ejecución; no es un error. |
| Ruta del hook fuera del ámbito global (por ejemplo, `hooks/` es un enlace hacia fuera) | No se instala; se omite con un aviso que recoge el motivo. No es un error. |
| Ruta fuera del ámbito | `validation.path.out_of_scope`. |
| Sin raíz de usuario | `internal.env`. |
