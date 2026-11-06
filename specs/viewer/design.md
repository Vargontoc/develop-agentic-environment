# Diseño: viewer

Estado: borrador

## Resumen

El viewer será una aplicación JavaFX 21 dentro del mismo ecosistema Java/Maven del CLI. Toda la lógica de dominio se delega en el CLI mediante `cli-json`; la aplicación solo selecciona proyectos, pide estado y árbol, describe artefactos, edita cabecera y cuerpo, previsualiza cambios con `--dry-run`, confirma escrituras y ejecuta el menú interactivo en una consola PTY con pty4j. No hay Rust, Node ni una segunda capa de servicio.

## Principio de simplicidad

Un único módulo Maven, una sola aplicación JavaFX y estado con propiedades y listas observables de JavaFX. Sin FXML, sin inyección de dependencias, sin librerías de estado, sin buses de eventos y sin capas intermedias. Cada pantalla llama directamente al cliente del CLI que necesita.

## Supuestos sobre el sistema actual

- La feature `cli` está implementada y el contrato `cli-json` v1 está en `docs/contracts/`.
- El CLI devuelve un único documento JSON por línea en stdout, logs en stderr y códigos 0/1/2/3.
- Existen `catalog`, `save --dry-run`, `save --header-file`/`--header-stdin` y `adopt`.
- `describe` devuelve `header`, `body`, `sharedBody`, `hash`, `status` y `modifiedExternally`.
- El CLI no lee `.sdd`; el contrato permite leerlo directamente en solo lectura.
- No existe `.sdd/architecture.md` ni `.sdd/project.md` en la raíz.
- `viewer/` contiene el esqueleto, la sesión de proyecto, la navegación, la previsualización, la edición, la propagación, la adopción y la creación de módulos y técnicos (hasta TASK-010); el resto está por construir.
- El viewer espera el jar ejecutable en `cli/agentic-develop.jar` dentro de su propio directorio. Si falta, muestra un error de aplicación y no ejecuta comandos.
- Se dispone de JDK 21 y Maven; JavaFX y pty4j se resuelven como dependencias Maven.
- La latencia por comando incluye el arranque de la JVM (≈0,3-0,5 s); se evita el sondeo continuo.
- Se abre un proyecto a la vez.

## Componentes

Las entradas son responsabilidades lógicas; varias pueden vivir en el mismo paquete o fichero.

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| `javafx-shell` | Ventana, ciclo de vida, diálogos nativos y tareas en segundo plano. | nuevo (TASK-001) |
| `viewer-ui` | Layout, panel lateral, pestañas, estados de vista y mensajes. | nuevo |
| `project-session` | Selección, validación y sesión del proyecto activo; refresco inicial. | nuevo |
| `cli-process` | Resolver Java y jar, ejecutar el CLI con `ProcessBuilder`, enviar stdin y capturar stdout/stderr/código. | nuevo |
| `cli-client` | Parsear el envelope, validar `schemaVersion`, exponer comandos tipados y cachear lecturas. | nuevo |
| `artifact-editor` | Editar cabecera, cuerpo y ficheros de skill; buffers sucios y modos instancia/grupo. | nuevo |
| `markdown-preview` | Renderizar Markdown con commonmark-java y mostrarlo en WebView, con frontmatter separado. | nuevo |
| `diff-review` | Mostrar diff, miembros afectados y conflictos antes de confirmar. | nuevo |
| `creation-wizard` | Formularios de módulo, técnico y artefacto con catálogo y simulación. | nuevo |
| `terminal-console` | Consola de texto y entrada para el CLI interactivo. | nuevo |
| `pty-host` | Crear y gestionar el pseudo-terminal con pty4j, resize y cierre. | nuevo |
| `change-watcher` | Detectar cambios externos con `WatchService` y marcar nodos obsoletos. | nuevo |
| `error-presenter` | Traducir códigos del CLI a mensaje, ruta y acción sugerida. | nuevo |
| `packaging` | Empaquetar con jpackage y runtime Java; resolver el jar del CLI. | nuevo |

## Flujo

1. Arranque: `javafx-shell` abre la ventana; `project-session` muestra el selector de carpeta.
2. Selección: `project-session` valida con `status --project`; si falta `.sdd`, ofrece `init`; si es válido, pide `status` y `tree` al `cli-client` en segundo plano.
3. Árbol: `viewer-ui` pinta Global y Project con proveedores, agentes, skills, `.sdd`, módulos, técnicos pendientes y avisos.
4. Previsualización: al seleccionar un artefacto, `cli-client` ejecuta `describe`; `artifact-editor` muestra cabecera, cuerpo compartido, cuerpo resuelto, formato y estado. Los ficheros de `.sdd` se leen directamente en solo lectura.
5. Edición: `artifact-editor` mantiene buffers de cabecera y cuerpo, marca cambios y ofrece texto o vista renderizada.
6. Guardado: `diff-review` ejecuta `save --dry-run`; muestra diff, miembros y conflictos; al confirmar, `cli-client` ejecuta el guardado real y se refresca el árbol.
7. Creación de módulo o técnico: `creation-wizard` pide datos, ejecuta `create-module` o `add-technician` con `--dry-run`, muestra el resultado y confirma.
8. Creación de artefacto: `creation-wizard` pide `catalog --name`, muestra la plantilla, permite editarla y ejecuta `create-artifact`.
9. Adopción: `diff-review` ejecuta `adopt` sin confirmar; tras aceptar, repite con `--confirm`.
10. Terminal: `pty-host` lanza el CLI interactivo con el proyecto como directorio de trabajo y `terminal-console` muestra la sesión.
11. Cambio externo: `change-watcher` detecta la modificación, marca el nodo obsoleto y el usuario decide recargar.
12. Error: `error-presenter` muestra código, ruta y acción sugerida sin cerrar la aplicación.

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| `viewer-ui` | `cli-client` | Estado, árbol, descripción, guardado y creación | llamada Java |
| `cli-client` | `cli-process` | Comando, opciones, contenido y timeout | llamada Java |
| `cli-process` | CLI | Argumentos, stdin, stdout, stderr y código de salida | proceso, `cli-json` v1 |
| `viewer-ui` | `artifact-editor` | Artefacto seleccionado y modo de edición | JavaFX |
| `artifact-editor` | `markdown-preview` | Texto Markdown y frontmatter | llamada Java |
| `diff-review` | `cli-client` | Simulación y confirmación de guardado o adopción | llamada Java |
| `creation-wizard` | `cli-client` | Catálogo, simulación y creación | llamada Java |
| `terminal-console` | `pty-host` | Entrada, salida, resize y cierre | llamada Java |
| `pty-host` | CLI interactivo | Sesión de pseudo-terminal | PTY |
| `change-watcher` | `viewer-ui` | Rutas modificadas y marca de obsolescencia | evento Java |
| `error-presenter` | `viewer-ui` | Mensaje, código y acción sugerida | llamada Java |
| `packaging` | `cli-process` | Ruta del jar y del runtime Java | recursos empaquetados |

## Modelo de datos lógico

- **Sesión de proyecto:** proyecto activo, estado, árbol, avisos y selección.
- **Nodo de árbol:** proveedor, agente, skill, perfil, `.sdd`, módulo o técnico; hijos, estado, pendiente y obsolescencia.
- **Vista de artefacto:** resultado de `describe`; proveedor, ámbito, tipo, formato, módulo, cabecera, cuerpo, cuerpo compartido, hash y estado.
- **Buffer de edición:** contenido de cabecera, cuerpo o fichero de skill; original, cambios y modo.
- **Previsualización de cambio:** resultado de `save --dry-run` o `adopt`; miembros, diffs, conflictos y copias previstas.
- **Borrador de creación:** proveedor, ámbito, tipo, nombre, plantilla y contenido.
- **Sesión de terminal:** proceso, directorio de trabajo, tamaño y estado.
- **Preferencia local:** último proyecto, tamaño de ventana y paneles; nunca metadatos de dominio.
- **Aviso de error:** código, ruta, mensaje y acción sugerida.
- **Evento de cambio:** ruta modificada y momento.
- **Entrada de caché:** comando, parámetros, resultado y momento.

## Decisiones

### DEC-001 JavaFX 21 y pty4j, un solo stack Java

- **Decisión:** Aplicación JavaFX 21 dentro de un módulo Maven, con pty4j para la consola y jpackage para distribuir.
- **Alternativas descartadas:** Tauri (exige Rust y MSVC), Electron (segundo runtime y peso), aplicación web local (no es escritorio real).
- **Cubre:** NFR-001.

### DEC-002 El CLI es la única fuente de verdad del dominio

- **Decisión:** El viewer lee y modifica el dominio exclusivamente mediante `cli-json`; no compone agentes ni resuelve propagación.
- **Alternativas descartadas:** biblioteca Java compartida (acopla el ciclo de vida) o reimplementar reglas (divergencia).
- **Cubre:** REQ-001 a REQ-005, REQ-007, REQ-009, REQ-015, NFR-003, NFR-006.

### DEC-003 Sin Rust, Node ni servicios intermedios

- **Decisión:** Todo el viewer es Java; no hay host nativo adicional ni servidor local.
- **Alternativas descartadas:** Tauri o Electron (toolchain y runtime extra) o un servidor HTTP local (abre puertos y añade seguridad).
- **Cubre:** NFR-006.

### DEC-004 Proceso por comando con caché, sin daemon

- **Decisión:** Cada comando lanza el CLI con `ProcessBuilder`; las lecturas se cachean y se invalidan tras escrituras o cambios externos.
- **Alternativas descartadas:** daemon persistente (complejidad) o refresco periódico (repite la JVM y compite con el editor).
- **Cubre:** NFR-002.

### DEC-005 Editor con RichTextFX

- **Decisión:** Usar RichTextFX para el editor de texto con resaltado básico de Markdown, TOML y YAML.
- **Alternativas descartadas:** `TextArea` (sin resaltado), WebView con CodeMirror (vuelve a introducir web) o Monaco (pesado).
- **Cubre:** REQ-006, REQ-015.

### DEC-006 Preview Markdown con commonmark-java y WebView

- **Decisión:** Convertir Markdown a HTML con commonmark-java y mostrarlo en un WebView de JavaFX, con el frontmatter en un panel aparte y sin cargar recursos remotos.
- **Alternativas descartadas:** renderizar todo el fichero o usar un navegador externo.
- **Cubre:** REQ-006.

### DEC-007 Consola PTY con pty4j, sin emulador completo

- **Decisión:** Ejecutar el CLI interactivo en un PTY de pty4j y mostrarlo en una consola de texto con entrada por líneas. El menú del CLI es textual y no necesita emulación ANSI completa.
- **Alternativas descartadas:** JediTerm (Swing embebido, más complejo) o pipes sin PTY (no soportan menús interactivos ni resize).
- **Cubre:** REQ-016, NFR-004.

### DEC-008 WatchService para cambios externos

- **Decisión:** Vigilar el proyecto y las carpetas de proveedores con `WatchService`, agrupar eventos y marcar nodos obsoletos; la recarga es explícita.
- **Alternativas descartadas:** recarga automática (pisa ediciones) o no detectar cambios (datos viejos).
- **Cubre:** REQ-011, REQ-017.

### DEC-009 Caché de lecturas en memoria

- **Decisión:** Cachear `status`, `tree` y `describe` por parámetros; invalidar tras escrituras o eventos de cambio.
- **Alternativas descartadas:** ejecutar todo siempre (latencia) o caché sin invalidación (datos obsoletos).
- **Cubre:** NFR-002, REQ-017.

### DEC-010 Errores con acción sugerida

- **Decisión:** Traducir los códigos del contrato a mensajes con ruta y acción sugerida, conservando el código original.
- **Alternativas descartadas:** mostrar solo el mensaje del CLI o cerrar la aplicación.
- **Cubre:** REQ-018.

### DEC-011 Buffers sucios y guardia de navegación

- **Decisión:** Cada artefacto mantiene sus buffers y su estado sucio; cambiar de selección exige guardar, descartar o cancelar.
- **Alternativas descartadas:** autoguardado (escrituras no deseadas) o descartar al navegar (pérdida de trabajo).
- **Cubre:** REQ-007, REQ-010, NFR-005.

### DEC-012 Cabecera y cuerpo separados; diff del CLI

- **Decisión:** La cabecera se guarda con `save --header-file` y el cuerpo con `save`; la previsualización de propagación usa `save --dry-run`.
- **Alternativas descartadas:** editar el fichero completo (rompe composición y hash) o calcular la propagación en el viewer.
- **Cubre:** REQ-008, REQ-009.

### DEC-013 Creación guiada por catálogo y simulación

- **Decisión:** El asistente usa `catalog` para la plantilla y `--dry-run` para mostrar lo que se creará.
- **Alternativas descartadas:** hardcodear plantillas o crear primero y editar después.
- **Cubre:** REQ-012, REQ-013, REQ-014.

### DEC-014 Adopción con el comando `adopt`

- **Decisión:** Volver al grupo de un artefacto legacy o desvinculado se hace con `adopt`, con diff previo y confirmación.
- **Alternativas descartadas:** `create-artifact --overwrite` (reescribe la cabecera) o dejar la adopción fuera de v1.
- **Cubre:** REQ-010.

### DEC-015 Lectura directa de `.sdd` solo en lectura

- **Decisión:** Leer `.sdd/project.md`, `team.md` y `architecture.md` directamente para previsualizarlos, sin escribir.
- **Alternativas descartadas:** añadir un comando de lectura al CLI o no mostrar `.sdd`.
- **Cubre:** REQ-006.

### DEC-016 Empaquetado con jpackage y runtime Java

- **Decisión:** Empaquetar la aplicación con jpackage y un runtime `jlink`; incluir el jar del CLI en `cli/agentic-develop.jar` junto al viewer y resolverlo desde ahí.
- **Alternativas descartadas:** exigir Java instalado (fragilidad) o compilar a nativo (coste y reflexión).
- **Cubre:** NFR-001.

### DEC-017 Una sesión de proyecto por ventana

- **Decisión:** Un proyecto a la vez; cambiar de proyecto reinicia árbol, editor y consola.
- **Alternativas descartadas:** varias pestañas o ventanas (fuera de alcance).
- **Cubre:** REQ-001, REQ-017.

### DEC-018 Seguridad de procesos y rutas

- **Decisión:** `ProcessBuilder` con lista de argumentos, contenido por stdin, sin shell, y lecturas directas limitadas a `.sdd` dentro del proyecto.
- **Alternativas descartadas:** interpolar rutas en una línea de shell o permitir escrituras directas.
- **Cubre:** NFR-006.

### DEC-019 Estado con JavaFX properties

- **Decisión:** Estado de vista con `Property`, `ObservableList` y bindings de JavaFX; sin librerías de estado.
- **Alternativas descartadas:** framework de estado externo (dependencia innecesaria) o variables globales sin observabilidad.
- **Cubre:** NFR-002, REQ-017.

## Riesgos

- **Latencia de la JVM por comando:** se mitiga con caché, ejecución en segundo plano de `status` y `tree`, y sin sondeo.
- **Respuestas grandes de `describe`:** se mitiga con memoria suficiente y un límite de seguridad que muestre error.
- **Editor menos rico que un IDE:** RichTextFX cubre resaltado y edición; no se busca autocompletado.
- **pty4j en Windows y Linux:** se mitiga probando el PTY en ambos sistemas y manejando resize y cierre.
- **WebView en JavaFX:** puede requerir dependencias del sistema; se mitiga con pruebas de empaquetado.
- **Jar del CLI ausente:** el viewer no puede ejecutar comandos. Se muestra un error de aplicación con la ruta esperada (`cli/agentic-develop.jar` junto al viewer) y se mitiga con el empaquetado de TASK-015.
- **jpackage:** el instalador MSI requiere WiX; se puede entregar app-image y añadir MSI después.
- **Watcher ruidoso:** debounce y agrupación por ruta.
- **Cambio entre simulación y escritura:** el guardado real vuelve a comprobar el hash y devuelve conflicto.
- **Deriva del contrato:** comprobar `schemaVersion` al inicio y con pruebas de contrato.
- **Pérdida de cambios al cambiar de proyecto:** guardia de navegación y confirmación.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | `project-session`, `cli-client`, `error-presenter` | DEC-002, DEC-017 |
| REQ-002 | `project-session`, `cli-client`, `viewer-ui` | DEC-002 |
| REQ-003 | `project-session`, `viewer-ui`, `cli-client` | DEC-002 |
| REQ-004 | `viewer-ui`, `cli-client` | DEC-002 |
| REQ-005 | `viewer-ui`, `cli-client` | DEC-002 |
| REQ-006 | `artifact-editor`, `markdown-preview`, `cli-client` | DEC-005, DEC-006, DEC-015 |
| REQ-007 | `artifact-editor`, `cli-client`, `error-presenter` | DEC-002, DEC-011 |
| REQ-008 | `artifact-editor`, `cli-client` | DEC-012 |
| REQ-009 | `diff-review`, `artifact-editor`, `cli-client` | DEC-002, DEC-012 |
| REQ-010 | `artifact-editor`, `diff-review`, `cli-client` | DEC-011, DEC-014 |
| REQ-011 | `diff-review`, `error-presenter`, `cli-client` | DEC-008, DEC-010 |
| REQ-012 | `creation-wizard`, `cli-client`, `viewer-ui` | DEC-013 |
| REQ-013 | `creation-wizard`, `cli-client`, `viewer-ui` | DEC-013 |
| REQ-014 | `creation-wizard`, `cli-client` | DEC-013 |
| REQ-015 | `artifact-editor`, `cli-client` | DEC-002, DEC-005 |
| REQ-016 | `terminal-console`, `pty-host`, `javafx-shell` | DEC-007 |
| REQ-017 | `change-watcher`, `viewer-ui`, `cli-client` | DEC-008, DEC-009 |
| REQ-018 | `error-presenter`, `viewer-ui` | DEC-010 |
| NFR-001 | `javafx-shell`, `packaging`, `pty-host` | DEC-001, DEC-016 |
| NFR-002 | `cli-client`, `viewer-ui`, `javafx-shell` | DEC-004, DEC-009, DEC-019 |
| NFR-003 | `cli-client`, `cli-process` | DEC-002 |
| NFR-004 | `terminal-console`, `pty-host` | DEC-007 |
| NFR-005 | `artifact-editor`, `viewer-ui` | DEC-011 |
| NFR-006 | `cli-client`, `cli-process` | DEC-002, DEC-003, DEC-018 |

## Contratos afectados

Ninguno. El viewer consume `cli-json` v1 y, de forma indirecta, `provider-layout` y `sdd-team`; no crea ni modifica contratos de dominio.

## Cambios sugeridos al mapa

- + `viewer-app | viewer/src/main/java/es/vargontoc/agentic/viewer | Ventana, UI, editor, consola y estado | -`
- + `viewer-cli | viewer/src/main/java/es/vargontoc/agentic/viewer/cli | Cliente y proceso del contrato CLI | -`
- + `viewer-terminal | viewer/src/main/java/es/vargontoc/agentic/viewer/terminal | Consola y PTY con pty4j | -`
- + `viewer-packaging | viewer/pom.xml, viewer/src/main/resources | Empaquetado con jpackage y runtime | -`
- + `Convención: cli_contract: v1`
- + `Convención: viewer_ui: javafx`
- + `Convención: viewer_domain_source: cli-json`

## Estrategia de verificación

- Pruebas JUnit del cliente contra envelopes de referencia, errores y `schemaVersion` desconocida.
- Pruebas del modelo de árbol con módulos, técnicos pendientes, routers compartidos y avisos.
- Pruebas de buffers, guardia de navegación y fallos de guardado.
- Pruebas de simulación y confirmación con `save --dry-run`, `create-module --dry-run`, `add-technician --dry-run` y `adopt`.
- Pruebas de la consola PTY con entrada, salida, resize y cierre.
- Pruebas del watcher con cambios externos y debounce.
- Pruebas de rendimiento con 200 artefactos y un fichero de 1 MB.
- Prueba de empaquetado y arranque en Windows y Linux.
- Prueba sin red en apertura, edición, creación y consola.
