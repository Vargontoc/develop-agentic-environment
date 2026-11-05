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

### [ ] TASK-003 Cliente del contrato y presentación de errores

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

### [ ] TASK-004 Selección de proyecto y estado inicial

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

### [ ] TASK-005 Árbol lateral global y de proyecto

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

### [ ] TASK-006 Previsualización de artefactos y `.sdd`

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

### [ ] TASK-007 Edición de cabecera y cuerpo

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

### [ ] TASK-008 Diff y propagación de grupo

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

### [ ] TASK-009 Guardado local, desvinculación y adopción

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
  2. Abrir un artefacto legacy y pulsar “volver al grupo”.
  3. Revisar el diff, confirmar y comprobar el estado vinculado.

### [ ] TASK-010 Creación de módulos y técnicos

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

### [ ] TASK-011 Creación de agentes y skills desde plantilla

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

### [ ] TASK-012 Edición de skills como directorio

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

### [ ] TASK-013 Consola PTY con pty4j

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

### [ ] TASK-014 Watcher y refresco tras cambios

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

### [ ] TASK-015 Empaquetado con jpackage y runtime Java

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

### [ ] TASK-016 Rendimiento y caché

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

### [ ] TASK-017 Errores, reintento y funcionamiento sin red

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
| NFR-001 | TASK-001, TASK-015 |
| NFR-002 | TASK-016 |
| NFR-003 | TASK-015, TASK-017 |
| NFR-004 | TASK-013 |
| NFR-005 | TASK-007 |
| NFR-006 | TASK-002, TASK-003 |

## Avisos

- El stack se cambió de Tauri/React/Rust a JavaFX 21 + pty4j para evitar Rust y MSVC. El diseño y las tareas se han reescrito con los mismos IDs.
- TASK-001 está completada y verificada con `mvn test` y `mvn javafx:run`.
- TASK-002 está completada.
- TASK-003 está implementada y verificada con 28 pruebas, incluidas `status`, `tree` y `describe` del CLI real; la casilla queda sin marcar hasta la confirmación del usuario.
- El viewer exige `viewer/cli/agentic-develop.jar`. El jar no se versiona (`.gitignore`) y se copia tras construir el CLI; si falta, la aplicación muestra un error.
- No existe `.sdd/architecture.md`; los componentes nuevos se marcan como `por definir (analista técnico)`.
- No existe `.sdd/task-types.md`; no se han generado tipos de tarea adicionales.
- `specs/viewer/requirements.md` y `specs/viewer/design.md` siguen en `borrador`.
- Se han propuesto casos de error o límite en TASK-006 y TASK-016 porque los requisitos no los detallaban.
