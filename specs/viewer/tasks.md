# Tareas: viewer

Estado: borrador

## Resumen

Plan incremental sobre JavaFX 21: primero la ventana mínima, después el proceso del CLI, el cliente del contrato, el árbol, el editor, la propagación, la creación, la consola PTY y el empaquetado. Un solo módulo Maven, estado con JavaFX properties y sin capas ni librerías de estado.

## Tareas

### [x] TASK-001 Esqueleto JavaFX y ventana mínima

- **Cubre:** NFR-001, DEC-001
- **Componente:** `javafx-shell | por definir (analista técnico)`
- **Depende de:** -
- **Alcance:** Crear el módulo Maven JavaFX, una ventana con estilos, un campo, un botón y una lógica mínima testeable.
- **Hecho cuando:** `mvn test` pasa, `mvn javafx:run` abre la ventana y el botón muestra el saludo o el error de nombre vacío.
- **Pruebas automáticas:**
  - Caso feliz: `Greeting.greet("mundo")` devuelve el saludo esperado y `mvn test` pasa.
  - Caso de error o límite: nombre vacío o solo espacios lanza `IllegalArgumentException`.
- **Prueba manual:**
  1. Ejecutar `mvn javafx:run` y comprobar que la ventana abre.
  2. Pulsar “Probar” y comprobar el saludo.
  3. Borrar el nombre y comprobar el error.

### [x] TASK-002 Proceso del CLI y resolución de Java/jar

- **Cubre:** REQ-003, REQ-005, NFR-003, NFR-006, DEC-003, DEC-016
- **Componente:** `cli-process | por definir (analista técnico)`
- **Depende de:** TASK-001
- **Alcance:** Ejecutar el CLI con `ProcessBuilder` y lista de argumentos, resolver `cli/agentic-develop.jar` dentro del directorio del viewer, enviar stdin y capturar stdout, stderr, código de salida y timeout. Si el jar falta, mostrar un error de aplicación.
- **Hecho cuando:** con `viewer/cli/agentic-develop.jar` presente se ejecutan `--version` y `status` desde la aplicación, con rutas con espacios y sin shell; si falta, la aplicación muestra el error y no ejecuta nada.
- **Pruebas automáticas:**
  - Caso feliz: un proceso falso devuelve JSON y código 0; se conservan stdout, stderr y código.
  - Caso de error o límite: código distinto de cero, timeout, ruta con espacios y jar ausente devuelven un resultado controlado sin bloquear la interfaz.
- **Prueba manual:**
  1. Construir `cli/target/agentic-develop.jar` y copiarlo a `viewer/cli/agentic-develop.jar`.
  2. Ejecutar `--version` y `status` desde la aplicación.
  3. Probar una ruta de proyecto con espacios y comprobar el error si se retira el jar.

### [x] TASK-003 Cliente del contrato y presentación de errores

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-018, NFR-006, DEC-002, DEC-010
- **Componente:** `cli-client | por definir (analista técnico)`; `error-presenter | por definir (analista técnico)`
- **Depende de:** TASK-002
- **Alcance:** Parsear el envelope de una línea, validar `schemaVersion`, exponer `status`, `tree` y `describe` tipados y traducir errores a código, ruta y acción sugerida.
- **Hecho cuando:** las lecturas devuelven datos tipados, un JSON inválido o una versión desconocida se tratan como error y ningún error cierra la aplicación.
- **Pruebas automáticas:**
  - Caso feliz: envelopes de referencia de `status`, `tree` y `describe` se parsean y conservan sus datos.
  - Caso de error o límite: stdout vacío, JSON mal formado, `schemaVersion` desconocida y error de dominio producen un aviso con código y ruta.
- **Prueba manual:**
  1. Ejecutar un comando con un proyecto inexistente y comprobar el aviso.
  2. Comprobar que la aplicación sigue usable y permite reintentar.

### [x] TASK-004 Selección de proyecto y estado inicial

- **Cubre:** REQ-001, REQ-002, REQ-003, DEC-002, DEC-017
- **Componente:** `project-session | por definir (analista técnico)`; `viewer-ui | por definir (analista técnico)`
- **Depende de:** TASK-003
- **Alcance:** `DirectoryChooser`, validación con `status --project`, acción de iniciar proyecto cuando falte `.sdd`, sesión de un solo proyecto y estados de vacío, cargando y error.
- **Hecho cuando:** un directorio sin `.sdd` muestra “no configurado” e inicia el proyecto; uno válido muestra su estado; uno inexistente muestra error sin escribir.
- **Pruebas automáticas:**
  - Caso feliz: proyecto sin `.sdd` permite iniciar y, tras `init`, pasa a configurado; proyecto válido muestra proveedores y proyecto.
  - Caso de error o límite: ruta inexistente, ruta que es fichero y error de `status` se muestran sin crear ficheros.
- **Prueba manual:**
  1. Seleccionar un directorio temporal vacío y comprobar la acción de iniciar.
  2. Iniciar el proyecto y comprobar que aparece `.sdd`.
  3. Seleccionar un fichero y comprobar el error.

### [x] TASK-005 Árbol lateral global y de proyecto

- **Cubre:** REQ-004, REQ-005, DEC-002
- **Componente:** `viewer-ui | por definir (analista técnico)`
- **Depende de:** TASK-004
- **Alcance:** Pedir `tree`, mostrar las secciones Global y Project, módulos y técnicos, técnicos pendientes, routers una sola vez y avisos de `team.md`.
- **Hecho cuando:** el árbol refleja la respuesta del CLI, permite expandir y contraer y distingue pendientes, personalizados y avisos.
- **Pruebas automáticas:**
  - Caso feliz: un fixture con dos módulos y varios técnicos se convierte en el árbol esperado; cada router aparece una vez.
  - Caso de error o límite: `team.md` mal formado muestra aviso y el resto del árbol; un proyecto sin módulos muestra el estado vacío.
- **Prueba manual:**
  1. Abrir un proyecto con módulos y comprobar las dos secciones.
  2. Expandir un módulo y comprobar técnicos existentes y pendientes.
  3. Abrir un proyecto sin módulos y comprobar el estado vacío.

### [x] TASK-006 Previsualización de artefactos y `.sdd`

- **Cubre:** REQ-006, DEC-005, DEC-006, DEC-015
- **Componente:** `artifact-editor | por definir (analista técnico)`; `markdown-preview | por definir (analista técnico)`; `viewer-ui | por definir (analista técnico)`
- **Depende de:** TASK-005
- **Alcance:** Al seleccionar un nodo, pedir `describe` para agentes y skills; mostrar metadatos, texto y vista renderizada; mostrar TOML/YAML con resaltado; leer `.sdd` directamente en solo lectura.
- **Hecho cuando:** Markdown ofrece texto y render con frontmatter separado; TOML/YAML no se interpretan como Markdown; `.sdd` se ve con aviso de solo lectura.
- **Pruebas automáticas:**
  - Caso feliz: `describe` de un agente Markdown, uno TOML y una skill muestran los paneles esperados; un `.sdd` se lee sin escribir.
  - Caso de error o límite: ruta fuera de `.sdd`, fichero inexistente y contenido no UTF-8 muestran error controlado `(propuesto)`.
- **Prueba manual:**
  1. Seleccionar un agente y alternar texto/render.
  2. Seleccionar un agente Codex y comprobar el resaltado TOML.
  3. Seleccionar `team.md` y comprobar que se indica solo lectura.

### [x] TASK-007 Edición de cabecera y cuerpo

- **Cubre:** REQ-007, REQ-008, NFR-005, DEC-011, DEC-012
- **Componente:** `artifact-editor | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-006
- **Alcance:** Buffers separados de cabecera y cuerpo, marca de cambios, guardia al cambiar de selección, guardado de instancia y guardado de cabecera con `save`.
- **Hecho cuando:** el cuerpo se guarda y persiste; la cabecera editada solo cambia el proveedor elegido; un fallo de guardado conserva el contenido; un especialista muestra el placeholder del módulo.
- **Pruebas automáticas:**
  - Caso feliz: guardar cuerpo y cabecera actualiza el contenido; un Codex TOML sigue siendo válido tras guardar.
  - Caso de error o límite: fallo de guardado mantiene el buffer; cambiar de artefacto con cambios pide confirmación; cabecera inválida devuelve error sin escribir.
- **Prueba manual:**
  1. Editar el cuerpo de un agente y guardar; volver a abrirlo.
  2. Editar la cabecera de un proveedor y comprobar que otro proveedor no cambia.
  3. Intentar cambiar de artefacto sin guardar y comprobar la confirmación.

### [x] TASK-008 Diff y propagación de grupo

- **Cubre:** REQ-009, REQ-011, DEC-002, DEC-008, DEC-012
- **Componente:** `diff-review | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-007
- **Alcance:** Ejecutar `save --dry-run` antes de escribir, mostrar miembros, diffs, conflictos y estados; confirmar con el mismo comando sin simulación; tratar sin cambios, ausentes y conflictos.
- **Hecho cuando:** el usuario ve el diff antes de escribir, la confirmación actualiza solo los miembros existentes del mismo ámbito y un conflicto no se sobrescribe sin decisión.
- **Pruebas automáticas:**
  - Caso feliz: un grupo de dos módulos muestra sus diffs y, al confirmar, ambos se actualizan con el nombre correcto.
  - Caso de error o límite: sin cambios no se escribe; miembro ausente se muestra omitido; conflicto se muestra y no se pisa sin confirmación.
- **Prueba manual:**
  1. Editar el cuerpo compartido de un especialista y pulsar propagar.
  2. Revisar el diff y los módulos afectados.
  3. Confirmar y comprobar que los módulos se actualizan.

### [x] TASK-009 Guardado local, desvinculación y adopción

- **Cubre:** REQ-010, DEC-011, DEC-014
- **Componente:** `artifact-editor | por definir (analista técnico)`; `diff-review | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-008
- **Alcance:** Guardar solo la instancia, mostrar estados personalizado y desvinculado, y adoptar un artefacto legacy o desvinculado con `adopt` y confirmación.
- **Hecho cuando:** una instancia personalizada no se sobrescribe al propagar; la adopción muestra el diff y, al confirmar, vincula el artefacto al grupo.
- **Pruebas automáticas:**
  - Caso feliz: guardado de instancia no toca otras copias; `adopt` sin confirmar devuelve diff y con confirmación aplica y registra.
  - Caso de error o límite: artefacto ya vinculado devuelve conflicto; grupo inexistente devuelve error comprensible.
- **Prueba manual:**
  1. Guardar solo una instancia y comprobar que las demás no cambian.
  2. Abrir un artefacto legacy y pulsar "volver al grupo".
  3. Revisar el diff, confirmar y comprobar el estado vinculado.
- **Bug fix:** Corregido problema donde seleccionar un artefacto sin editarlo marcaba el estado como "dirty". Se añadió bandera `updatingFromModel` en `PreviewPanel` para evitar que los listeners de texto se disparen durante la carga inicial.

### [x] TASK-010 Creación de módulos y técnicos

- **Cubre:** REQ-012, REQ-013, DEC-013
- **Componente:** `creation-wizard | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`; `viewer-ui | por definir (analista técnico)`
- **Depende de:** TASK-005
- **Alcance:** Formulario de módulo con nombre, técnicos y proveedores; simulación con `create-module --dry-run`; creación; formulario para añadir técnicos con `add-technician --dry-run`.
- **Hecho cuando:** un nombre inválido o un módulo duplicado muestran el error del CLI; la simulación no escribe; tras crear, el árbol se refresca.
- **Pruebas automáticas:**
  - Caso feliz: simulación devuelve ficheros previstos; creación devuelve resumen y actualiza el árbol; alta de técnico no recrea los demás.
  - Caso de error o límite: nombre inválido, módulo duplicado y técnico duplicado muestran error u omisión sin sobrescribir.
- **Prueba manual:**
  1. Crear un módulo en simulación y comprobar que no cambia el disco.
  2. Crearlo de verdad y comprobar `.sdd/team.md` y los agentes.
  3. Añadir un técnico y comprobar que los existentes no cambian.

### [x] TASK-011 Creación de agentes y skills desde plantilla

- **Cubre:** REQ-014, DEC-013
- **Componente:** `creation-wizard | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-006, TASK-010
- **Alcance:** Elegir proveedor, ámbito, tipo y nombre; pedir `catalog --name`; mostrar la plantilla; permitir editarla; crear con `create-artifact`; tratar conflicto y sobrescritura.
- **Hecho cuando:** la plantilla se muestra antes de escribir; se puede crear tal cual o modificada; un nombre existente no se sobrescribe sin confirmación.
- **Pruebas automáticas:**
  - Caso feliz: catálogo devuelve plantilla de agente y de skill; creación sin cambios y con cambios genera el contenido esperado.
  - Caso de error o límite: nombre existente devuelve conflicto; proveedor sin ámbito de proyecto devuelve error; ámbito de proyecto sin proyecto devuelve error.
- **Prueba manual:**
  1. Crear un agente sin modificar la plantilla.
  2. Crear otro modificando el cuerpo.
  3. Repetir el mismo nombre y comprobar el conflicto.
- **Implementación:**
  - Modelos: `CatalogData`, `CreateArtifactData`
  - Cliente CLI: `catalog()`, `createArtifact()`
  - Diálogos: `CreateArtifactDialog` (selección), `EditTemplateDialog` (edición)
  - Flujo completo en `App.createArtifact()` con manejo de conflictos y sobrescritura

### [x] TASK-012 Edición de skills como directorio

- **Cubre:** REQ-015, DEC-002, DEC-005
- **Componente:** `artifact-editor | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-007
- **Alcance:** Listar los ficheros de una skill, editar `SKILL.md` y sus ficheros de apoyo, guardar con `save --file` y propagar la skill dentro del mismo ámbito.
- **Hecho cuando:** se listan los ficheros, se edita y guarda uno y la propagación actualiza las copias existentes sin cruzar de ámbito.
- **Pruebas automáticas:**
  - Caso feliz: skill con `SKILL.md` y un asset lista ambos; guardar un fichero lo actualiza; propagar conserva los assets no tocados.
  - Caso de error o límite: fichero inexistente y ruta fuera de la skill devuelven error; skill de otro ámbito no se propaga.
- **Prueba manual:**
  1. Abrir una skill y comprobar la lista de ficheros.
  2. Editar `SKILL.md` y guardar.
  3. Propagar y comprobar las copias del mismo ámbito.
- **Implementación:**
  - Modelo: `SkillEditorModel` para manejar múltiples ficheros con buffers independientes
  - Cliente CLI: `saveBody()` y `saveGroup()` con parámetro `file` para skills
  - UI: `PreviewPanel` con selector de fichero y botones de guardar/propagar por fichero
  - Flujo completo en `App.loadSkill()`, `saveSkillFile()` y `propagateSkillFile()`

### [x] TASK-013 Consola PTY con pty4j

- **Cubre:** REQ-016, NFR-004, DEC-007
- **Componente:** `terminal-console | por definir (analista técnico)`; `pty-host | por definir (analista técnico)`; `javafx-shell | por definir (analista técnico)`
- **Depende de:** TASK-002
- **Alcance:** Ejecutar el CLI interactivo en un PTY de pty4j con el proyecto como directorio de trabajo; mostrar salida, enviar líneas, resize, cierre y reinicio.
- **Hecho cuando:** el menú del CLI responde, el resize se aplica, la interrupción llega al proceso y al terminar se muestra el código y se puede reiniciar.
- **Pruebas automáticas:**
  - Caso feliz: un proceso falso recibe entrada y devuelve salida; resize actualiza el tamaño; al terminar se informa del código.
  - Caso de error o límite: salida continua no bloquea la interfaz; proceso que termina con error muestra el código y permite reiniciar.
- **Prueba manual:**
  1. Abrir la consola y usar una opción del menú del CLI.
  2. Redimensionar la ventana y comprobar el reajuste.
  3. Enviar la interrupción y comprobar que el proceso responde.
- **Implementación:**
  - Interfaz: `PtySession` para abstracción y testabilidad
  - Implementación: `Pty4jSession` con pty4j (PtyProcess, PtyProcessBuilder, WinSize)
  - Utilidad: `AnsiStripper` para limpiar secuencias ANSI antes de mostrar en TextArea
  - UI: `PtyConsolePanel` con TextArea de salida, TextField de entrada, botones de enviar/interrupt/reiniciar/cerrar/limpiar
  - Hilos virtuales para lectura de salida y espera de finalización sin bloquear la UI (NFR-004)
  - Integración en `App.java` con sección de consola en el panel principal
  - Pruebas: `FakePtySession`, `AnsiStripperTest`, `FakePtySessionTest`

### [x] TASK-014 Watcher y refresco tras cambios

- **Cubre:** REQ-017, NFR-002, DEC-008, DEC-009
- **Componente:** `change-watcher | por definir (analista técnico)`; `viewer-ui | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-005, TASK-009
- **Alcance:** Vigilar el proyecto y las carpetas de proveedores con `WatchService`, agrupar eventos, marcar nodos obsoletos, invalidar caché y permitir recargar sin pisar editores sucios.
- **Hecho cuando:** un cambio externo marca el nodo como obsoleto; recargar actualiza árbol y editor; un editor con cambios pide confirmación antes de recargar.
- **Pruebas automáticas:**
  - Caso feliz: una modificación externa emite un evento agrupado y el nodo queda obsoleto; recargar actualiza los datos.
  - Caso de error o límite: varios eventos seguidos se agrupan; editor sucio no se recarga automáticamente; ruta borrada se refleja como ausente.
- **Prueba manual:**
  1. Editar un agente fuera de la aplicación.
  2. Comprobar que el nodo se marca obsoleto.
  3. Recargar y comprobar el contenido nuevo.
- **Implementación:**
  - Watcher: `ProjectWatcher` con `WatchService`, registra recursivamente, agrupa eventos con debounce de 500ms
  - Caché: `CacheStore` con `getOrLoad()` e invalidación por clave, prefijo o total
  - Obsolescencia: `StaleTracker` registra rutas cambiadas y permite consultar si un nodo está obsoleto
  - UI: botón "Recargar" con guarda de editores sucios, etiqueta `staleWarning` con aviso, estilo `tree-stale` en nodos obsoletos
  - Integración: watcher se inicia al cargar proyecto, se detiene al cerrar; `invalidateAfterWrite()` tras cada escritura
  - Pruebas: `CacheStoreTest` (6 tests), `StaleTrackerTest` (7 tests), `ProjectWatcherTest` (6 tests)

### [x] TASK-015 Empaquetado con jpackage y runtime Java

- **Cubre:** NFR-001, NFR-003, DEC-016
- **Componente:** `packaging | por definir (analista técnico)`; `cli-process | por definir (analista técnico)`
- **Depende de:** TASK-002, TASK-013
- **Alcance:** Incluir el jar del CLI en `cli/agentic-develop.jar` junto al viewer, más un runtime `jlink`, resolver la ruta del ejecutable, generar una app-image de Windows y Linux y comprobar el arranque sin Java del sistema.
- **Hecho cuando:** la aplicación instalada abre un proyecto, ejecuta comandos y usa la consola sin depender de un Java del sistema, también sin red.
- **Pruebas automáticas:**
  - Caso feliz: el empaquetado contiene runtime y jar; `--version` funciona desde el recurso empaquetado; la app-image se genera.
  - Caso de error o límite: runtime ausente muestra un error claro; ruta configurada inválida cae al recurso empaquetado.
- **Prueba manual:**
  1. Generar la app-image en Windows y comprobar apertura, comandos y consola.
  2. Generar la app-image en Linux con las dependencias de escritorio y repetir.
  3. Desconectar la red y comprobar que todo sigue funcionando.
- **Implementación:**
  - Scripts de empaquetado: `scripts/package-windows.ps1` y `scripts/package-linux.sh`
  - Crean fat JAR con todas las dependencias (Jackson, CommonMark, Pty4J, JavaFX)
  - Generan runtime Java personalizado con `jlink` (módulos base + JavaFX)
  - Empaquetan con `jpackage` tipo `app-image` para Windows y Linux
  - Incluyen el jar del CLI en `cli/agentic-develop.jar`
  - Script de verificación: `scripts/verify-package.ps1` comprueba estructura y funcionamiento
  - Pruebas: `PackagingTest` verifica MANIFEST y dependencias del fat JAR
  - Funciona sin Java instalado en el sistema y sin conexión a internet

### [X] TASK-016 Rendimiento y caché

- **Cubre:** NFR-002, DEC-004, DEC-009
- **Componente:** `cli-client | por definir (analista técnico)`; `viewer-ui | por definir (analista técnico)`; `javafx-shell | por definir (analista técnico)`
- **Depende de:** TASK-012, TASK-014, TASK-015
- **Alcance:** Medir la apertura de un proyecto de 200 artefactos y la previsualización de 1 MB, ajustar la caché y confirmar que no hace falta daemon.
- **Hecho cuando:** el árbol y el estado se muestran en menos de 2 segundos y un fichero de 1 MB en menos de 1 segundo, con caché válida y sin sondeo continuo.
- **Pruebas automáticas:**
  - Caso feliz: con un fixture de 200 artefactos, la apertura y el árbol cumplen el umbral; con 1 MB, la previsualización cumple.
  - Caso de límite: repetir la apertura usa la caché y no relanza los comandos de lectura `(propuesto)`.
- **Prueba manual:**
  1. Preparar un proyecto de 200 artefactos.
  2. Cronometrar la apertura y la selección de un fichero de 1 MB.
  3. Anotar los tiempos y comprobar que no hay sondeo.

### [X] TASK-017 Errores, reintento y funcionamiento sin red

- **Cubre:** REQ-018, NFR-003, DEC-010
- **Componente:** `error-presenter | por definir (analista técnico)`; `viewer-ui | por definir (analista técnico)`; `cli-client | por definir (analista técnico)`
- **Depende de:** TASK-003, TASK-008, TASK-010, TASK-011
- **Alcance:** Cubrir los códigos de error del contrato con mensaje, ruta y acción sugerida; permitir reintentar; comprobar que la aplicación no se cierra y que todo funciona sin red.
- **Hecho cuando:** cada familia de errores del contrato tiene una acción comprensible, un fallo inesperado permite reintentar y las operaciones funcionan sin conexión.
- **Pruebas automáticas:**
  - Caso feliz: los códigos de proyecto, módulo, artefacto, conflicto, formato y uso muestran el mensaje y la acción esperados.
  - Caso de error o límite: código desconocido muestra mensaje genérico con el código original; sin red, apertura, edición y creación siguen funcionando.
- **Prueba manual:**
  1. Provocar un error de proyecto inexistente y comprobar la acción sugerida.
  2. Provocar un conflicto y comprobar las opciones.
  3. Desconectar la red y repetir apertura, edición y creación.

### [x] TASK-018 Configurar proveedores desde el visor

- **Cubre:** Mejora de usabilidad (no estaba en requisitos originales)
- **Componente:** `viewer-ui | setup`; `cli-client`
- **Depende de:** TASK-003 (CliClient), TASK-015 (empaquetado)
- **Alcance:** Añadir un diálogo de configuración de proveedores accesible desde Archivo → Configurar proveedores y mostrarlo automáticamente en el primer arranque si no hay proveedores configurados. El diálogo lista los 4 proveedores soportados (Claude, Codex, Gemini, OpenCode), muestra su estado actual (configurado/no configurado), permite seleccionar cuáles instalar y ejecuta `configure-provider` para cada uno, mostrando los resultados.
- **Hecho cuando:** el usuario puede configurar proveedores sin salir al CLI, y el primer arranque sin proveedores muestra un asistente automáticamente.
- **Pruebas automáticas:**
  - Caso feliz: el diálogo lista los 4 proveedores, marca correctamente los configurados, selecciona por defecto los no configurados, y la configuración genera resultados.
  - Caso de error: un proveedor que falla muestra el error en los resultados sin interrumpir la configuración de los demás.
- **Prueba manual:**
  1. Borrar la configuración de un proveedor y comprobar que aparece como "No configurado" en el diálogo.
  2. Ejecutar Archivo → Configurar proveedores, seleccionar uno y comprobar que se instala correctamente.
  3. Borrar todas las configuraciones y arrancar de nuevo para comprobar el asistente de primer arranque.
- **Implementación:**
  - `ConfigureProvidersDialog`: diálogo con checkboxes para los 4 proveedores, estados coloreados (verde/rojo), selección inteligente por defecto, resultados con conteo de creados/omitidos/advertencias.
  - `App.configureProviders()`: método invocado desde el menú Archivo.
  - `App.loadGlobalStatus()`: detección de primer arranque con flag `initialProviderCheckDone`, muestra el asistente si todos los proveedores están `not_installed` o `unconfigured`.
  - `ConfigureProvidersDialogTest`: 5 pruebas unitarias que verifican la construcción de la lista de proveedores, marcado de configurados, y estructura de resultados.

### [x] TASK-019 Visibilidad e instalación del hook desde el visor

- **Cubre:** Mejora de usabilidad (no estaba en requisitos originales); NFR-006 (el CLI sigue siendo la única fuente de escritura)
- **Componente:** `cli-tree`; `viewer-ui | preview`; `cli-client`
- **Depende de:** TASK-005 (árbol), TASK-018 (configuración de proveedores), TASK-015 (empaquetado)
- **Alcance:** El tipo `hook` ya existía en el visor (icono, etiqueta y grupo) pero el CLI nunca lo emitía. Se añade el nodo `hook` al `tree` global bajo Claude (único proveedor con hook), con estado (`pending` si falta, `linked` si está registrado y sin cambios, `legacy` si está modificado o sin registro), y el visor muestra su ficha con ruta, estado y acción de instalación/actualización.
- **Hecho cuando:** el hook `sdd-specs-only` aparece en el árbol global de Claude, se ve su estado y se puede instalar o actualizar desde el visor sin salir al CLI. Los demás proveedores no muestran ni instalan hook.
- **Pruebas automáticas:**
  - Caso feliz: nodo hook pendiente cuando falta; `linked` cuando hay registro y el hash coincide; `legacy` cuando está modificado o sin registro.
  - Caso de error: ruta de hook fuera del ámbito global se anota como problema del árbol y el nodo queda pendiente sin romper el resto.
- **Prueba manual:**
  1. Configurar un proveedor y comprobar que el hook aparece como vinculado bajo su nodo global.
  2. Modificar el hook a mano y comprobar que pasa a «personalizado» (legacy).
  3. Seleccionar el nodo hook, pulsar «Reinstalar / actualizar hook» y comprobar el resultado y el refresco del árbol.
- **Implementación:**
  - CLI: `TreeNode.NodeType.HOOK`; `ProjectTreeBuilder` añade el nodo bajo Claude usando `PathResolver.hookPath`, `MetadataStore.hook` y `ContentHash`; `Services` cablea `PathResolver` y `MetadataStore`.
  - CLI: `ConfigureProvider` instala el hook solo cuando el proveedor es Claude; el resto no toca `hooks/`.
  - Contratos: `cli-json` documenta el tipo `hook` y su semántica; `artifact-metadata` y `provider-layout` dejan de decir que el hook no aparece en `tree` y aclaran que solo existe para Claude.
  - Viewer: `PreviewPanel.showNodeInfo(...)` muestra una ficha con estado, ruta y acción; `App.loadHook(...)` la compone y `App.installHook(...)` reutiliza `configure-provider` (idempotente) y refresca el árbol.
  - Pruebas CLI: `ProjectTreeBuilderTest` cubre hook pendiente, vinculado, legacy y ausencia en proveedores distintos de Claude; `ConfigureProviderTest` y `ConfigureProviderCommandTest` verifican que solo Claude instala el hook.

### [x] TASK-020 Resolver conflictos externos con guía

- **Cubre:** Mejora de usabilidad; REQ-007, REQ-008, REQ-015 (guardado de artefactos y ficheros de skill); NFR-005 (no perder cambios sin confirmación)
- **Componente:** `viewer-ui | diff`; `viewer-ui | preview`; `cli-client`
- **Depende de:** TASK-007 (edición), TASK-012 (skills), TASK-014 (watcher/cambios externos)
- **Alcance:** Cuando el CLI rechaza un guardado con `conflict.artifact.modified_externally`, mostrar la versión del usuario y la del disco (lado a lado y diff) y permitir **Conservar mi versión** (`save --overwrite`, con copia de seguridad), **Usar la versión del disco** (descarta los cambios de esa parte) o **Cancelar**. Cubre cabecera, cuerpo y ficheros de skill, incluido «guardar todo».
- **Hecho cuando:** el usuario puede resolver un conflicto sin salir del visor, viendo qué cambia, y el guardado con `--overwrite` deja copia de seguridad.
- **Pruebas automáticas:**
  - Caso feliz: el diff local marca líneas eliminadas y añadidas; el cliente envía `--overwrite` en cuerpo y cabecera.
  - Caso de error o límite: un fichero que vuelve a cambiar tras elegir «conservar mi versión» vuelve a mostrar el conflicto; un fichero de skill ilegible informa y no escribe.
- **Prueba manual:**
  1. Abrir un agente, modificarlo sin guardar y cambiar el fichero por fuera.
  2. Guardar: comprobar el diálogo con lado a lado/diff; elegir «Conservar mi versión» y verificar la copia de seguridad.
  3. Repetir eligiendo «Usar la versión del disco» y comprobar que el editor queda limpio.
  4. Repetir con un fichero de skill y con «guardar todo».
- **Implementación:**
  - `CliClient.saveBody`/`saveHeader` aceptan `overwrite` y añaden `--overwrite`.
  - `TextDiff`: diff de líneas local (LCS con límite de celdas) para no depender de un dry-run que hoy devuelve conflicto.
  - `ConflictResolutionDialog`: pestañas «Lado a lado» y «Diff», botones de conservar/usar disco/cancelar.
  - `PreviewPanel.refreshAfterExternalResolution()`: refleja en los editores la versión del disco.
  - `App`: `saveEditor` sin diálogos (apto para hilo virtual), `resolveEditorConflicts`/`resolveEditorPart` en JavaFX, resolución por fichero de skill y por lote; `SaveOutcome` transporta la parte en conflicto y el mensaje de copia de seguridad.
  - Pruebas: `TextDiffTest` y casos de `--overwrite` en `CliClientTest`.

### [x] TASK-021 Proyectos recientes y reapertura

- **Cubre:** Mejora de usabilidad; REQ-001, REQ-002 (selección y validación de proyecto)
- **Componente:** `viewer-ui | welcome`; `runtime`
- **Depende de:** TASK-004 (selección de proyecto)
- **Alcance:** Persistir el último proyecto y una lista de recientes (máximo 8). Al arrancar, reabrir el más reciente que siga existiendo; si no hay ninguno válido, mostrar la bienvenida. Añadir un submenú **Archivo → Abrir recientes** y enlaces de recientes en la pantalla de bienvenida.
- **Hecho cuando:** cerrar y volver a abrir el visor recupera el último proyecto sin pasar por el selector, y se puede cambiar a otro reciente desde el menú o la bienvenida.
- **Pruebas automáticas:**
  - Caso feliz: recordar mueve al principio, sin duplicados y con tope; `lastExisting` devuelve el más reciente existente.
  - Caso de error o límite: proyectos borrados se ocultan de la lista; un JSON corrupto se trata como lista vacía.
- **Prueba manual:**
  1. Abrir un proyecto, cerrarlo y reiniciar el visor: debe reabrirse solo.
  2. Abrir otro proyecto y comprobar que ambos aparecen en Archivo → Abrir recientes y en la bienvenida.
  3. Borrar la carpeta de un reciente y comprobar que desaparece de las listas y no se intenta reabrir.
  4. Usar «Limpiar lista» y comprobar que la bienvenida deja de mostrar recientes.
- **Implementación:**
  - `RecentProjectsStore`: JSON `recent-projects.json` en la carpeta de estado del visor, orden reciente→antiguo, tope de 8, escritura atómica, filtrado de rutas inexistentes y tolerancia a JSON corrupto.
  - `AppPaths.recentProjectsFile()`.
  - `App`: `reopenLastProject()` al arrancar, `rememberRecentProject()` tras validar un proyecto, submenú «Abrir recientes» con entradas y «Limpiar lista», y `openRecentProject()` con guardia de cambios sin guardar.
  - `WelcomePane`: sección de proyectos recientes con enlaces (máximo 5) y aviso si la carpeta ya no existe.
  - Pruebas: `RecentProjectsStoreTest` (orden, tope, rutas borradas, eliminar/limpiar, JSON corrupto).

### [x] TASK-022 Edición de `.sdd/team.md` con formulario

- **Cubre:** Mejora de usabilidad; NFR-006 (el cliente no escribe `.sdd`); REQ-012 (módulos)
- **Componente:** `cli-team`; `viewer-ui | team`; `cli-client`
- **Depende de:** TASK-004 (selección de proyecto), TASK-010 (validación de módulos), TASK-012 (skills, por el patrón de edición)
- **Alcance:** Sustituir la edición a mano de `team.md` por un formulario que gestione módulos (`nombre`, `mapId`, rutas) y las opciones `sprints`, `git-policy`, `contract-strategy` y `readme-owner`. El CLI lee el estado con `team` y lo guarda con `team-update` recibiendo el estado completo por `--stdin`, con hash esperado, validación, `--dry-run`, copia de seguridad y conservación del resto del fichero. El visor mantiene el texto en solo lectura y añade «Editar equipo…».
- **Hecho cuando:** se pueden editar módulos y opciones desde el visor sin tocar Markdown, se ve el diff antes de guardar y un cambio externo concurrente se detecta sin sobrescribir.
- **Pruebas automáticas:**
  - Caso feliz: `team` devuelve módulos y opciones efectivas; `team-update` reescribe solo las listas, conserva comentarios/otras secciones y deja copia; el visor envía el JSON esperado.
  - Caso de error o límite: hash viejo da `conflict.team.modified_externally`; módulos duplicados o `readme-owner` inexistente dan error de validación; `--dry-run` no escribe; CRLF se conserva.
- **Prueba manual:**
  1. Abrir `.sdd/team.md`, pulsar «Editar equipo…», añadir un módulo y cambiar las cuatro opciones.
  2. Guardar, revisar el diff y comprobar el fichero y la copia de seguridad.
  3. Editar `team.md` a mano entre abrir y guardar el formulario y comprobar el conflicto.
  4. Quitar un módulo y verificar que sus artefactos no se borran.
- **Implementación:**
  - CLI: `TeamOptions`, `ModuleName` (validación compartida con `create-module`), `TeamParser.parseDocument` (módulos + opciones + `unknown_option`), `TeamService` (lectura, hash, validación y reescritura preservando comentarios/CRLF, `TextStore` para copia atómica), comandos `team` y `team-update`.
  - Contratos: `cli-json` documenta ambos comandos, códigos de error y `unknown_option`; `sdd-team` documenta `## Opciones` y la escritura completa.
  - Viewer: modelos `TeamData`/`TeamUpdateRequest`/`TeamUpdateData`, métodos `CliClient.team`/`teamUpdate`, `TeamEditorDialog` (módulos, opciones, avisos, validación básica), `PreviewPanel.showTeamFile` y flujo `App.openTeamEditor`/`saveTeam` con dry-run + diff + backup.
  - Pruebas: `TeamParserTest`, `TeamServiceTest`, `TeamCommandsTest` y casos de `CliClientTest`.

### [x] TASK-023 Asistente único de creación de artefactos

- **Cubre:** REQ-014, DEC-013; mejora de usabilidad
- **Componente:** `viewer-ui | creation`; `cli-client`
- **Depende de:** TASK-011 (creación desde plantilla)
- **Alcance:** Sustituir la cadena de diálogos por un único asistente con tres pasos: (1) ámbito, proveedores, tipo y reemplazo; (2) catálogo con búsqueda y selección múltiple; (3) vista previa editable por artefacto. La creación sigue delegando en `create-artifact`; no se reimplementan reglas ni se escribe desde el visor.
- **Hecho cuando:** se pueden buscar, seleccionar, previsualizar y crear varios agentes/skills en un solo flujo, con plantilla editable y aviso de existentes.
- **Pruebas automáticas:**
  - Caso feliz: la clave de catálogo identifica proveedor/ámbito/tipo/nombre; la petición copia selección y contenidos y el cliente envía `create-artifact` con el contenido editado.
  - Caso de error o límite: sin proveedor o sin tipo no se avanza; los existentes no se seleccionan sin marcar reemplazo; un conflicto al crear se informa y no se cuenta como creado.
- **Prueba manual:**
  1. Abrir «Crear artefactos», buscar en el catálogo, seleccionar varios y revisar sus plantillas.
  2. Editar el contenido de uno y crear el lote.
  3. Repetir con un artefacto existente sin reemplazo y comprobar que no se toca; activar reemplazo y comprobar la copia.
- **Implementación:**
  - `CreateArtifactWizard`: pasos con navegación, validación, búsqueda, selección múltiple, vista previa editable por entrada y `Request` final.
  - Carga de catálogo y plantillas en hilos virtuales con token de versión para evitar carreras; los cargadores los aporta `App` mediante `CliClient.catalog`.
  - `App.createArtifact`/`applyArtifactCreation`: crea cada selección con `create-artifact`, cuenta creados/reemplazados/omitidos, trata conflictos y refresca el árbol.
  - Reset de plantilla, selección de visibles y bloqueo de existentes sin reemplazo; menú y bienvenida renombrados a «Crear artefactos».
  - Pruebas: `CreateArtifactWizardTest`.

### [x] TASK-024 Avisos no modales para éxitos

- **Cubre:** Mejora de usabilidad; DEC-019 (estado de vista JavaFX)
- **Componente:** `viewer-ui | notifications`
- **Depende de:** TASK-010, TASK-011, TASK-023
- **Alcance:** Sustituir los `Alert` de éxito (módulo, técnicos, artefactos, hook y equipo) por toasts no modales apilados en la esquina inferior derecha y un mensaje en la barra de estado. Los errores y las confirmaciones que exigen decisión siguen siendo modales.
- **Hecho cuando:** crear un módulo, técnico, artefacto, hook o guardar el equipo no bloquea la ventana; el resultado se ve en un toast que desaparece solo y en la barra de estado.
- **Pruebas automáticas:**
  - Caso feliz: los toasts no capturan el ratón y se limitan a tres visibles; los avisos con incidencias duran más.
  - Caso de error o límite: los errores y las confirmaciones de destrucción/reemplazo siguen usando diálogos modales.
- **Prueba manual:**
  1. Crear artefactos y comprobar que aparece un toast sin bloquear la ventana.
  2. Encadenar varias operaciones y ver que los toasts se apilan y desaparecen.
  3. Provocar una incidencia y comprobar que el toast de aviso dura más.
- **Implementación:**
  - `ToastHost`: capa transparente sobre la raíz, sin captura de ratón, tarjetas apiladas (máximo 3), fundido de entrada/salida, duración 6 s (9 s con aviso) y truncado seguro de mensajes largos.
  - `App`: raíz envuelta en `StackPane` con `ToastHost`; `notifySuccess`/`notifyWarning` escriben también en la barra de estado.
  - Sustituidos los `showInfoAlert` de éxito en creación de módulos, técnicos, artefactos, hook y guardado del equipo; los errores y confirmaciones no cambian.
  - Estilos `toast`, `toast-title`, `toast-message` y `toast-warning` en `styles.css`.

### [x] TASK-025 Detalle real de módulos y técnicos

- **Cubre:** Mejora de usabilidad; REQ-005 (árbol global y de proyecto), REQ-012
- **Componente:** `viewer-ui | preview`; `viewer-ui | tree`
- **Depende de:** TASK-005 (árbol), TASK-022 (`team.md` estructurado)
- **Alcance:** Sustituir los mensajes genéricos de módulo y técnico por una ficha con secciones: para el módulo, nombre, identificador del mapa, rutas, técnicos y artefactos; para el técnico, rol, módulo y artefactos. Cada artefacto muestra proveedor, contexto, estado y ruta.
- **Hecho cuando:** al seleccionar un módulo o técnico se ve su información real y la de sus artefactos, sin tener que recorrer el árbol.
- **Pruebas automáticas:**
  - Caso feliz: la ficha de módulo incluye `mapId`, rutas, técnicos y artefactos; la de técnico incluye módulo y artefactos con estado y ruta.
  - Caso de error o límite: sin `team.md` legible o sin padre conocido se muestra la ficha con lo disponible y valores como «sin datos»/«—».
- **Prueba manual:**
  1. Seleccionar un módulo y comprobar identificador, rutas, técnicos y artefactos.
  2. Seleccionar un técnico y comprobar su módulo y especialistas por proveedor.
  3. Alternar entre artefactos y volver al módulo para comprobar que la ficha se refresca.
- **Implementación:**
  - `NodeDetail`: modelo de ficha con secciones e ítems (título, detalle, estado, ruta).
  - `NodeDetails`: construcción pura a partir de `TreeData` y, para módulos, `TeamData`; estados legibles con `TreePresentation`.
  - `PreviewPanel.showNodeDetail`: sección desplazable con estilos `detail-*`; los artefactos muestran chip de estado y ruta monoespaciada.
  - `App.loadModuleDetail`: carga `team` en segundo plano con guarda de selección; `showTechnicianDetail` resuelve el módulo padre desde el árbol.
  - Pruebas: `NodeDetailsTest`.

### [x] TASK-026 Versiones visibles y compatibilidad del contrato

- **Cubre:** REQ-019 (nueva), REQ-018
- **Componente:** `cli-client`; `viewer-ui | shell`; `viewer-ui | status`
- **Depende de:** TASK-003 (cliente del CLI)
- **Alcance:** Mostrar la versión del visor, la del CLI y la del contrato `cli-json` en la barra de estado y en la bienvenida. Al arrancar (y al pedirlo desde Ayuda) se ejecuta `--version`, se lee `schemaVersion` del envelope y, si no coincide con la que entiende el visor, se muestra un aviso persistente no modal con la acción recomendada. «Acerca de» detalla ambas versiones y el resultado de la comprobación.
- **Hecho cuando:** las versiones son visibles sin abrir diálogos y una incompatibilidad de contrato se advierte antes de que fallen las operaciones.
- **Pruebas automáticas:**
  - Caso feliz: la sonda devuelve versión y `schemaVersion`; con el mismo contrato el estado es compatible y el resumen incluye ambas versiones.
  - Caso de error o límite: un `schemaVersion` distinto no lanza la excepción habitual de la sonda y produce un aviso de incompatibilidad; un CLI que no responde o devuelve error produce «CLI no disponible».
- **Prueba manual:**
  1. Arrancar el visor y comprobar la línea de versiones en la barra de estado y en la bienvenida.
  2. Abrir Ayuda → Acerca de y revisar visor, CLI, contrato y compatibilidad.
  3. Simular un contrato distinto y comprobar el aviso persistente y el toast; después Ayuda → Comprobar versiones.
- **Implementación:**
  - `viewer-version.properties` filtrado por Maven y `ViewerVersion.current()`.
  - `CliClient.version()` devuelve `CliVersionInfo` (versión + `schemaVersion`) sin aplicar la validación habitual del contrato.
  - `VersionStatus`: estados compatible, contrato incompatible y CLI no disponible; resumen, aviso y detalle para «Acerca de».
  - `StatusBarView.setVersions`, `WelcomePane.setVersionInfo`, banner `version-banner` y nuevos elementos en Ayuda.
  - Pruebas: `VersionStatusTest`, casos de versión en `CliClientTest`.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-004 |
| REQ-002 | TASK-004 |
| REQ-003 | TASK-003, TASK-004 |
| REQ-004 | TASK-005 |
| REQ-005 | TASK-005 |
| REQ-006 | TASK-006 |
| REQ-007 | TASK-007 |
| REQ-008 | TASK-007 |
| REQ-009 | TASK-008 |
| REQ-010 | TASK-009 |
| REQ-011 | TASK-008 |
| REQ-012 | TASK-010 |
| REQ-013 | TASK-010 |
| REQ-014 | TASK-011 |
| REQ-015 | TASK-012 |
| REQ-016 | TASK-013 |
| REQ-017 | TASK-014 |
| REQ-018 | TASK-003, TASK-017 |
| REQ-019 | TASK-026 |
| NFR-001 | TASK-001, TASK-015 |
| NFR-002 | TASK-016 |
| NFR-003 | TASK-015, TASK-017 |
| NFR-004 | TASK-013 |
| NFR-005 | TASK-007 |
| NFR-006 | TASK-002, TASK-003 |

## Avisos

- El stack se cambió de Tauri/React/Rust a JavaFX 21 + pty4j para evitar Rust y MSVC. El diseño y las tareas se han reescrito con los mismos IDs.
- TASK-001 a TASK-009 están completadas.
- TASK-010 está implementada y verificada con 86 pruebas, incluidos los métodos reales del CLI `create-module` y `add-technician`; la casilla queda sin marcar hasta la confirmación del usuario.
- Se retiró la tarjeta de demostración de TASK-001; la ventana empieza por la navegación y la sesión de proyecto.
- La previsualización usa commonmark-java (Markdown) y resaltado propio de TOML/YAML en WebView; la cabecera se muestra en un panel aparte y `.sdd` se lee directamente en solo lectura.
- La edición guarda el cuerpo con `save --stdin` y la cabecera con `save --header-stdin`; un cambio sin guardar pide confirmación antes de cambiar de selección.
- La propagación simula con `save --mode group --dry-run`, muestra miembros, diffs y conflictos y, al confirmar, escribe con el mismo comando sin simulación.
- Un artefacto legacy o desvinculado muestra «Volver al grupo»: `adopt` sin confirmar devuelve el diff y, al confirmar, vincula el artefacto y refresca el árbol.
- La creación de módulos y técnicos usa diálogos con checkboxes para técnicos (analista, desarrollador, documentador) y proveedores; simula con `--dry-run` y muestra preview antes de confirmar.
- El viewer exige `viewer/cli/agentic-develop.jar`. El jar no se versiona (`.gitignore`) y se copia tras construir el CLI; si falta, la aplicación muestra un error.
- No existe `.sdd/architecture.md`; los componentes nuevos se marcan como `por definir (analista técnico)`.
- No existe `.sdd/task-types.md`; no se han generado tipos de tarea adicionales.
- `specs/viewer/requirements.md` y `specs/viewer/design.md` siguen en `borrador`.
- Se han propuesto casos de error o límite en TASK-006 y TASK-016 porque los requisitos no los detallaban.
- TASK-018 (Configurar proveedores desde el visor) es una mejora de usabilidad añadida a petición del usuario; no estaba en el spec original. Incluye asistente de primer arranque y menú Archivo → Configurar proveedores.
- Corrección: `addTechnician()` ya no usa siempre `modules().getFirst()`. `AddTechnicianDialog` muestra un selector de módulo (ChoiceBox) cuando hay varios y `Result` incluye el módulo elegido; con un solo módulo se muestra deshabilitado. Añadidas pruebas en `AddTechnicianDialogTest`.
- TASK-019: el hook `sdd-specs-only` ya aparece en el `tree` global bajo Claude (único proveedor con hook) con estado (`pending`/`linked`/`legacy`) y se puede instalar/actualizar desde el visor. Implica cambios en el CLI (`NodeType.HOOK`, `ProjectTreeBuilder`, `ConfigureProvider` solo para Claude), en los contratos (`cli-json`, `artifact-metadata`, `provider-layout`) y en el visor (`PreviewPanel.showNodeInfo`, `App.loadHook`/`installHook`).
- TASK-020: los conflictos por cambios externos ya no se limitan a un mensaje de error. El visor muestra «Tu versión» frente a «Versión en disco» (lado a lado y diff) y permite conservar la propia (con `--overwrite` y copia), usar la del disco o cancelar, tanto en cabecera/cuerpo como en ficheros de skill y «guardar todo». El diff local (`TextDiff`) evita depender de `--dry-run`, que en instancia sigue devolviendo el conflicto.
- TASK-021: el último proyecto y hasta 8 recientes se guardan en `recent-projects.json` (carpeta de estado del visor). Al arrancar se reabre el más reciente existente; también se puede elegir otro desde Archivo → Abrir recientes o desde los enlaces de la pantalla de bienvenida. Las rutas borradas se ocultan y un JSON corrupto se ignora.
- TASK-022: `.sdd/team.md` se edita con formulario (módulos + `sprints`, `git-policy`, `contract-strategy`, `readme-owner`). El visor no escribe `.sdd`: usa los nuevos comandos `team` (lectura con hash) y `team-update` (estado completo por `--stdin`, `--expect-hash`, `--dry-run`), que preservan comentarios, secciones y CRLF y dejan copia de seguridad. La vista de texto sigue en solo lectura.
- TASK-023: la creación de artefactos pasa a un asistente único de tres pasos (ámbito/proveedor/tipo, catálogo con búsqueda y multiselección, vista previa editable). Sustituye «Aplicar artefactos» por «Crear artefactos» y crea cada entrada con `create-artifact`; los perfiles y el hook siguen en «Configurar proveedores».
- TASK-024: los éxitos de creación de módulos, técnicos, artefactos, hook y guardado de equipo ya no usan `Alert`. Se muestran como toasts no modales apilados (máximo 3, 6 s o 9 s con avisos) y como mensaje en la barra de estado. Errores y confirmaciones siguen siendo modales.
- TASK-025: módulos y técnicos muestran una ficha real: identificador y rutas del módulo (desde `team`), técnicos con número de artefactos, y artefactos con proveedor, contexto, estado y ruta. Sin `team.md` legible se muestra lo que aporta el árbol.
- TASK-026: la barra de estado y la bienvenida muestran «Visor · CLI · contrato». La comprobación usa `--version` y la `schemaVersion` del envelope sin bloquear la UI; si no coincide aparece un banner persistente, un toast y el detalle en «Acerca de». También está en Ayuda → Comprobar versiones.
