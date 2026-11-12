# Tareas: cli

Estado: aprobado

## Resumen

El trabajo se divide en cuatro bloques: fundamentos de dominio e infraestructura, escrituras y propagación, adaptadores de comandos y cierre de contratos. Cada tarea deja un resultado verificable y depende solo de tareas anteriores.

## Tareas

### [X] TASK-001 Resolvedor de rutas multiplataforma

- **Cubre:** REQ-007, NFR-001, NFR-005, DEC-011
- **Componente:** `path-resolver | por definir (analista técnico)`
- **Depende de:** -
- **Alcance:** Resolver las rutas de proveedores y del home de la aplicación en Windows y Linux, normalizarlas y rechazar rutas inválidas o fuera de ámbito.
- **Hecho cuando:** se cumplen los criterios de REQ-007 y el resolvedor devuelve rutas correctas para Claude, OpenCode, Codex y Gemini en ambos sistemas.
- **Pruebas automáticas:**
  - Caso feliz: con homes simulados de Windows (`USERPROFILE`) y Linux (`HOME` y `XDG_CONFIG_HOME`), devuelve las rutas esperadas de los cuatro proveedores y del home de la aplicación.
  - Caso de error: ruta inexistente, ruta que es un fichero y enlace simbólico que escapa del ámbito; se rechazan con un error tipado.
- **Prueba manual:** no aplica: componente interno sin interfaz propia; se verifica con las pruebas automáticas y se expone en TASK-022.

### [X] TASK-002 Almacén de texto con escritura atómica

- **Cubre:** NFR-004, NFR-005, DEC-010
- **Componente:** `text-store | por definir (analista técnico)`
- **Depende de:** TASK-001
- **Alcance:** Leer y escribir UTF-8 preservando el fin de línea, con escritura atómica y copia de seguridad antes de sobrescribir.
- **Hecho cuando:** se cumplen los criterios de NFR-004 y una escritura fallida no deja el fichero original truncado.
- **Pruebas automáticas:**
  - Caso feliz: un fichero con CRLF se guarda conservando CRLF y se crea una copia de seguridad recuperable.
  - Caso de error o límite: fallo simulado durante la escritura y ruta fuera de los ámbitos permitidos; el original queda intacto y la ruta se rechaza `(propuesto)`.
- **Prueba manual:** no aplica: componente interno; se verifica con las pruebas automáticas y se usa desde TASK-012 en adelante.

### [X] TASK-003 Catálogo de proveedores y estados

- **Cubre:** REQ-008, DEC-015
- **Componente:** `provider-catalog | no consta en el mapa`
- **Depende de:** TASK-001
- **Alcance:** Calcular el estado de cada proveedor como no instalado, instalado sin configurar, parcial o configurado, con el detalle de los artefactos de la aplicación que faltan.
- **Hecho cuando:** se cumplen los criterios de REQ-008 y el estado se obtiene sin listar artefactos ajenos.
- **Pruebas automáticas:**
  - Caso feliz: proveedor con todos los artefactos esperados devuelve «configurado»; proveedor con algunos devuelve «parcial» y la lista de ausentes.
  - Caso de error o límite: proveedor sin carpeta devuelve «no instalado»; carpeta con solo artefactos del usuario devuelve «instalado sin configurar» `(propuesto)`.
- **Prueba manual:** no aplica: componente interno; se expone a través de `status` en TASK-022.

### [X] TASK-004 Catálogo de artefactos de la aplicación

- **Cubre:** REQ-009, DEC-015
- **Componente:** `artifact-catalog | por definir (analista técnico)`
- **Depende de:** TASK-001
- **Alcance:** Definir los agentes y skills de la aplicación esperados por proveedor, con su formato, convención de nombres y cabecera.
- **Hecho cuando:** se cumplen los criterios de REQ-009 y el catálogo cubre Claude, OpenCode, Codex y Gemini en el ámbito global, y los proveedores con soporte de proyecto en el ámbito de proyecto.
- **Pruebas automáticas:**
  - Caso feliz: un artefacto de la aplicación se identifica con su proveedor, tipo y formato; una skill instalada se reconoce como directorio.
  - Caso de error o límite: un agente propio del usuario no entra en el catálogo; un recurso del jar no instalado tampoco; un proveedor desconocido devuelve error `(propuesto)`.
- **Prueba manual:** no aplica: componente interno; se verifica con pruebas automáticas y se expone en TASK-022.

### [X] TASK-005 Formato de artefactos Markdown y TOML

- **Cubre:** REQ-011, REQ-012, NFR-007, DEC-008
- **Componente:** `artifact-format | por definir (analista técnico)`
- **Depende de:** -
- **Alcance:** Separar y componer cabecera y cuerpo para Markdown y para Codex TOML, conservando el texto original de la cabecera.
- **Hecho cuando:** se cumplen los criterios de REQ-011 para ambos formatos y una separación seguida de composición devuelve el mismo contenido lógico.
- **Pruebas automáticas:**
  - Caso feliz: Markdown con frontmatter y Codex TOML con cuerpo multilínea se separan y recomponen sin cambios; un cuerpo con `"""` se escapa correctamente.
  - Caso de error o límite: fichero sin cabecera reconocible o con delimitadores incompletos devuelve un error de formato `(propuesto)`; la cabecera no se reserializa ni pierde comentarios.
- **Prueba manual:** no aplica: componente interno; se verifica con pruebas automáticas y se expone en TASK-022.

### [X] TASK-006 Almacén de metadatos de agrupación

- **Cubre:** REQ-013, DEC-005
- **Componente:** `metadata-store | por definir (analista técnico)`
- **Depende de:** TASK-001, TASK-002
- **Alcance:** Persistir en el home de la aplicación la agrupación lógica, el cuerpo compartido, los hashes y el estado de vinculación, sin escribir metadatos dentro del proyecto.
- **Hecho cuando:** se cumplen los criterios de REQ-013 y el almacén puede reconstruirse sin tocar los artefactos.
- **Pruebas automáticas:**
  - Caso feliz: se crea, lee y actualiza un grupo con varios artefactos y hashes; la versión de esquema queda registrada.
  - Caso de error o límite: almacén corrupto o ausente se trata como vacío y los artefactos pasan a personalizados `(propuesto)`; no aparece ningún fichero de metadatos bajo el proyecto.
- **Prueba manual:** no aplica: componente interno; se verifica con pruebas automáticas y se expone indirectamente en TASK-025.

### [X] TASK-007 Inventario de artefactos instalados

- **Cubre:** REQ-009, REQ-010, DEC-015
- **Componente:** `artifact-inventory | por definir (analista técnico)`
- **Depende de:** TASK-003, TASK-004, TASK-006
- **Alcance:** Descubrir los artefactos instalados, mapearlos a las entradas del catálogo y calcular su hash y estado.
- **Hecho cuando:** el inventario coincide con lo instalado y excluye los artefactos ajenos y los recursos del jar.
- **Pruebas automáticas:**
  - Caso feliz: un home con agentes y skills de varios proveedores produce el inventario esperado con hashes estables.
  - Caso de error o límite: artefacto ilegible devuelve un error con la ruta `(propuesto)`; artefacto del usuario no aparece; artefacto sin metadatos se marca como legacy.
- **Prueba manual:** no aplica: componente interno; se expone en el árbol de TASK-022.

### [X] TASK-008 Descripción de artefactos

- **Cubre:** REQ-011, REQ-014, DEC-004, DEC-008, DEC-009
- **Componente:** `artifact-descriptor | por definir (analista técnico)`
- **Depende de:** TASK-005, TASK-007
- **Alcance:** Construir la descripción editable de un artefacto con cabecera, cuerpo, cuerpo compartido, formato, proveedor, módulo, hash y estado de vinculación.
- **Hecho cuando:** se cumplen los criterios de REQ-011 y una ruta dentro del jar se rechaza como artefacto editable.
- **Pruebas automáticas:**
  - Caso feliz: agente Markdown y agente Codex TOML devuelven cabecera y cuerpo; un especialista de proyecto devuelve el placeholder de módulo; un artefacto desvinculado lo indica.
  - Caso de error o límite: ruta del jar, artefacto inexistente y artefacto sin permisos de lectura devuelven error con la ruta.
- **Prueba manual:** no aplica: componente interno; se expone en `describe` dentro de TASK-022.

### [X] TASK-009 Espacio de trabajo del proyecto y team.md

- **Cubre:** REQ-017, REQ-010, DEC-017
- **Componente:** `project-workspace | no consta en el mapa`
- **Depende de:** TASK-001, TASK-002
- **Alcance:** Inicializar la estructura `.sdd` de un proyecto y leer los módulos y técnicos registrados en `team.md`.
- **Hecho cuando:** se cumplen los criterios de REQ-017 y el lector devuelve módulos, identificadores y rutas aunque el fichero tenga líneas mal formadas.
- **Pruebas automáticas:**
  - Caso feliz: proyecto sin `.sdd` se inicializa; proyecto con `.sdd` no se sobrescribe; `team.md` válido devuelve sus módulos.
  - Caso de error o límite: ruta que no es directorio, `team.md` ausente, mal formado o con módulos repetidos devuelve aviso y resultado parcial.
- **Prueba manual:** no aplica: componente interno; se expone en `init` dentro de TASK-023.

### [X] TASK-010 Árbol global y de proyecto

- **Cubre:** REQ-010, DEC-015, DEC-017
- **Componente:** `project-tree | por definir (analista técnico)`
- **Depende de:** TASK-007, TASK-009
- **Alcance:** Componer el árbol de los ámbitos global y de proyecto con proveedores, agentes, skills, `.sdd`, módulos y técnicos.
- **Hecho cuando:** se cumplen los criterios de REQ-010, los routers aparecen una sola vez por proveedor y los técnicos pendientes se distinguen de los existentes.
- **Pruebas automáticas:**
  - Caso feliz: árbol global con varios proveedores y árbol de proyecto con dos módulos y sus técnicos.
  - Caso de error o límite: `team.md` mal formado devuelve aviso y árbol parcial; módulo sin técnicos aparece vacío pero visible.
- **Prueba manual:** no aplica: componente interno; se expone en `tree` dentro de TASK-022.

### [X] TASK-011 Servicios de aplicación sin impresión

- **Cubre:** REQ-001, REQ-002, REQ-024, DEC-001
- **Componente:** `application-services | no consta en el mapa`
- **Depende de:** TASK-003, TASK-007, TASK-008, TASK-009, TASK-010
- **Alcance:** Extraer la orquestación de estado, árbol, descripción e inicialización a servicios que devuelvan resultados, sin imprimir ni terminar el proceso.
- **Hecho cuando:** los servicios devuelven resultados tipados y ni el dominio ni la aplicación escriben en la salida estándar ni llaman al cierre del proceso.
- **Pruebas automáticas:**
  - Caso feliz: cada servicio devuelve su resultado y las operaciones de solo lectura no escriben ficheros.
  - Caso de error o límite: un error de dominio se propaga como resultado tipado y no como impresión `(propuesto)`; no hay salida por consola desde la capa de aplicación.
- **Prueba manual:** no aplica: refactor interno; la regresión se comprueba en TASK-028.

### [X] TASK-012 Guardado de instancia y conflictos

- **Cubre:** REQ-012, REQ-014, NFR-004, DEC-009, DEC-010
- **Componente:** `propagation-engine | por definir (analista técnico)`
- **Depende de:** TASK-002, TASK-006, TASK-008
- **Alcance:** Guardar un único artefacto, comprobar su hash, detectar cambios externos y escribir de forma atómica con copia de seguridad.
- **Hecho cuando:** se cumplen los criterios de REQ-014 y un guardado sin cambios no escribe ningún fichero.
- **Pruebas automáticas:**
  - Caso feliz: guardado de una instancia actualiza contenido, hash y copia de seguridad.
  - Caso de error o límite: cambio externo devuelve conflicto; sobrescritura explícita actualiza y conserva copia; guardado sin diferencias no escribe.
- **Prueba manual:** no aplica: componente interno; se expone en `save` dentro de TASK-025.

### [X] TASK-013 Propagación de cuerpo por grupo

- **Cubre:** REQ-012, REQ-006, DEC-006, DEC-007
- **Componente:** `propagation-engine | por definir (analista técnico)`
- **Depende de:** TASK-005, TASK-012
- **Alcance:** Propagar un cuerpo compartido a los artefactos existentes del mismo grupo y ámbito, conservando el nombre de módulo de cada uno y devolviendo el diff.
- **Hecho cuando:** se cumplen los criterios de REQ-012, no se crean artefactos ausentes y la propagación no cruza entre global y proyecto.
- **Pruebas automáticas:**
  - Caso feliz: un especialista compartido actualiza dos módulos con nombres correctos; un agente global actualiza todos los proveedores instalados.
  - Caso de error o límite: proveedor sin el artefacto se omite; grupo sin cambios no escribe; skill de otro ámbito no se propaga.
- **Prueba manual:** no aplica: componente interno; se expone en `save` dentro de TASK-025.

### [X] TASK-014 Migración de artefactos legacy y desvinculación

- **Cubre:** REQ-021, DEC-013
- **Componente:** `migration-service | por definir (analista técnico)`
- **Depende de:** TASK-006, TASK-012, TASK-013
- **Alcance:** Detectar artefactos sin metadatos, marcarlos como personalizados, impedir propagaciones sobre ellos y permitir una adopción explícita del cuerpo compartido.
- **Hecho cuando:** se cumplen los criterios de REQ-021 y ningún artefacto legacy se sobrescribe sin confirmación.
- **Pruebas automáticas:**
  - Caso feliz: artefacto legacy se muestra como personalizado; la propagación lo omite; la adopción muestra diff y actualiza tras confirmación.
  - Caso de error o límite: metadatos corruptos no borran artefactos y se tratan como legacy `(propuesto)`; artefacto desvinculado sigue omitido en propagaciones posteriores.
- **Prueba manual:** no aplica: componente interno; se expone en `save` dentro de TASK-025.

### [X] TASK-015 Skills como directorio

- **Cubre:** REQ-020, DEC-014
- **Componente:** `artifact-service | por definir (analista técnico)`
- **Depende de:** TASK-002, TASK-006, TASK-008, TASK-013
- **Alcance:** Describir, editar y propagar skills como directorios completos, conservando `SKILL.md` y los ficheros de apoyo, solo dentro del mismo ámbito.
- **Hecho cuando:** se cumplen los criterios de REQ-020 y una skill no pierde ficheros auxiliares al propagarse.
- **Pruebas automáticas:**
  - Caso feliz: se listan `SKILL.md` y assets; se edita un fichero; la propagación actualiza las copias existentes del mismo ámbito.
  - Caso de error o límite: skill inexistente devuelve error; una skill global no se propaga a proyecto ni al contrario; un asset no afectado se conserva.
- **Prueba manual:** no aplica: componente interno; se expone en `create-artifact` y en la edición de skills de TASK-027.

### [X] TASK-016 Creación de módulos

- **Cubre:** REQ-015, DEC-017, DEC-007
- **Componente:** `module-service | no consta en el mapa`
- **Depende de:** TASK-004, TASK-005, TASK-006, TASK-009, TASK-013
- **Alcance:** Crear un módulo validando el nombre, registrándolo en `team.md`, generando routers y especialistas para los técnicos y proveedores indicados, y devolviendo el resultado.
- **Hecho cuando:** se cumplen los criterios de REQ-015 y el módulo nuevo usa el cuerpo compartido del grupo si existe.
- **Pruebas automáticas:**
  - Caso feliz: módulo válido con dos técnicos y dos proveedores genera los artefactos, registra el módulo y devuelve creados, omitidos y avisos.
  - Caso de error o límite: nombre inválido no crea nada; módulo duplicado no duplica; fallo a mitad de la generación devuelve resultado parcial con lo creado y lo omitido `(propuesto)`.
- **Prueba manual:** no aplica: componente interno; se expone en `create-module` dentro de TASK-026.

### [X] TASK-017 Alta de técnicos

- **Cubre:** REQ-016, DEC-017
- **Componente:** `module-service | no consta en el mapa`
- **Depende de:** TASK-016
- **Alcance:** Añadir un técnico a un módulo existente generando solo sus artefactos y sin recrear los de los demás.
- **Hecho cuando:** se cumplen los criterios de REQ-016 y los ficheros de otros técnicos permanecen intactos.
- **Pruebas automáticas:**
  - Caso feliz: se añade un rol a un módulo y aparecen sus artefactos para los proveedores indicados.
  - Caso de error o límite: técnico ya existente se omite sin sobrescribir; módulo inexistente devuelve error.
- **Prueba manual:** no aplica: componente interno; se expone en `add-technician` dentro de TASK-026.

### [X] TASK-018 Creación de agentes y skills desde plantilla

- **Cubre:** REQ-019, DEC-018
- **Componente:** `artifact-service | por definir (analista técnico)`
- **Depende de:** TASK-004, TASK-005, TASK-006, TASK-015
- **Alcance:** Crear un agente o una skill a partir de una plantilla con contenido tal cual o modificado, sin sobrescribir sin confirmación.
- **Hecho cuando:** se cumplen los criterios de REQ-019 y el artefacto creado aparece en el catálogo y en el árbol.
- **Pruebas automáticas:**
  - Caso feliz: creación con la plantilla sin cambios y con cambios produce el contenido esperado en el ámbito y proveedor elegidos.
  - Caso de error o límite: artefacto con el mismo nombre devuelve conflicto y no sobrescribe; ámbito o proveedor inválido devuelve error `(propuesto)`.
- **Prueba manual:** no aplica: componente interno; se expone en `create-artifact` dentro de TASK-027.

### [X] TASK-019 Cabeceras por sistema operativo

- **Cubre:** REQ-022, NFR-001, DEC-012
- **Componente:** `resource-repository | no consta en el mapa`
- **Depende de:** TASK-004
- **Alcance:** Definir variantes de cabecera por sistema operativo y seleccionar la del sistema actual al instalar o crear un artefacto.
- **Hecho cuando:** se cumplen los criterios de REQ-022 y Linux no recibe hooks de PowerShell ni referencias a `USERPROFILE`.
- **Pruebas automáticas:**
  - Caso feliz: la misma entrada de catálogo produce cabecera de Windows en Windows y de Linux en Linux.
  - Caso de error o límite: variante ausente para el sistema actual devuelve error con la entrada afectada `(propuesto)`.
- **Prueba manual:** no aplica: componente interno; se verifica de extremo a extremo en TASK-020 y TASK-024.

### [X] TASK-020 Configuración de proveedores

- **Cubre:** REQ-018, DEC-012, DEC-015
- **Componente:** `provider-configurator | no consta en el mapa`
- **Depende de:** TASK-001, TASK-003, TASK-004, TASK-006, TASK-015, TASK-019
- **Alcance:** Instalar agentes, skills y perfiles globales con la cabecera del sistema operativo, sin sobrescribir lo existente, y devolver el resultado.
- **Hecho cuando:** se cumplen los criterios de REQ-018 y los artefactos instalados quedan registrados en los metadatos.
- **Pruebas automáticas:**
  - Caso feliz: proveedor sin configurar recibe agentes y skills y devuelve creados y omitidos; la cabecera corresponde al sistema.
  - Caso de error o límite: proveedor ya configurado no se sobrescribe; recurso ausente devuelve error con su ruta; ejecución repetida es idempotente.
- **Prueba manual:** no aplica: componente interno; se expone en `configure-provider` dentro de TASK-024.

### [X] TASK-021 Contrato de comandos y envelope

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-006, REQ-023, NFR-006, DEC-002, DEC-003
- **Componente:** `command-contract | por definir (analista técnico)`
- **Depende de:** TASK-011
- **Alcance:** Interpretar comandos y opciones, emitir el envelope versionado, separar datos de logs, mapear errores y aplicar los códigos de salida y la ayuda/versión.
- **Hecho cuando:** se cumplen los criterios de REQ-003, REQ-004, REQ-005, REQ-006 y REQ-023.
- **Pruebas automáticas:**
  - Caso feliz: un comando correcto produce un único documento JSON con `schemaVersion`, `command`, `ok`, `data` y `errors`; `--help` y `--version` no escriben ficheros.
  - Caso de error o límite: error de dominio devuelve código 1, uso incorrecto 2 y fallo interno 3; los logs van a stderr y la salida de datos sigue siendo parseable.
- **Prueba manual:**
  1. Ejecutar `--help` y comprobar que lista comandos sin modificar el proyecto.
  2. Ejecutar `--version` y comprobar que muestra una versión.
  3. Ejecutar un comando inexistente y comprobar el código de salida 2 y el error estructurado.

### [X] TASK-022 Comandos de lectura: status, tree y describe

- **Cubre:** REQ-002, REQ-010, REQ-011, REQ-024, NFR-003
- **Componente:** `command-router | por definir (analista técnico)`; interfaces con `application-services`, `project-tree` y `artifact-descriptor`
- **Depende de:** TASK-021, TASK-008, TASK-010
- **Alcance:** Exponer `status`, `tree` y `describe` con salida estructurada, sin escribir en el sistema y funcionando sin red.
- **Hecho cuando:** los tres comandos devuelven los datos de sus servicios, no modifican ficheros y fallan con error tipado ante un proyecto inexistente.
- **Pruebas automáticas:**
  - Caso feliz: `status` devuelve proveedores y proyecto; `tree` devuelve el árbol global y el de proyecto; `describe` devuelve cabecera y cuerpo de un agente Markdown y de uno TOML.
  - Caso de error o límite: proyecto inexistente devuelve error; ninguna de las tres operaciones escribe ficheros `(propuesto)`; las tres funcionan sin red.
- **Prueba manual:**
  1. Ejecutar `status` y comprobar el estado de proveedores en el JSON.
  2. Ejecutar `tree` sobre un proyecto con `.sdd` y comprobar módulos y técnicos.
  3. Ejecutar `describe` sobre un agente y comprobar cabecera, cuerpo y hash.

### [X] TASK-023 Comando init

- **Cubre:** REQ-002, REQ-017, REQ-024
- **Componente:** `command-router | por definir (analista técnico)`; `project-workspace`
- **Depende de:** TASK-021, TASK-009
- **Alcance:** Exponer el comando `init` para crear la estructura `.sdd` de un proyecto.
- **Hecho cuando:** se cumplen los criterios de REQ-017 a través del comando y una segunda ejecución no sobrescribe.
- **Pruebas automáticas:**
  - Caso feliz: proyecto sin `.sdd` recibe la estructura y el resultado indica creado; proyecto con `.sdd` no cambia.
  - Caso de error o límite: ruta inexistente o que no es directorio devuelve error; ejecución repetida es idempotente.
- **Prueba manual:**
  1. Crear un directorio temporal vacío.
  2. Ejecutar `init` sobre él y comprobar que aparece `.sdd`.
  3. Ejecutar `init` de nuevo y comprobar que no se sobrescribe nada.

### [X] TASK-024 Comando configure-provider

- **Cubre:** REQ-002, REQ-018, REQ-024
- **Componente:** `command-router | por definir (analista técnico)`; `provider-configurator`
- **Depende de:** TASK-021, TASK-020
- **Alcance:** Exponer el comando `configure-provider` para instalar agentes y skills globales en un proveedor.
- **Hecho cuando:** se cumplen los criterios de REQ-018 a través del comando y una segunda ejecución no sobrescribe.
- **Pruebas automáticas:**
  - Caso feliz: proveedor sin configurar recibe los artefactos y el resultado lista creados y omitidos.
  - Caso de error o límite: proveedor ya configurado no cambia; recurso ausente devuelve error; ejecución repetida es idempotente.
- **Prueba manual:**
  1. Usar un home temporal con un proveedor vacío.
  2. Ejecutar `configure-provider` y comprobar agentes y skills instalados.
  3. Ejecutar de nuevo y comprobar que no se sobrescribe.

### [X] TASK-025 Comando save

- **Cubre:** REQ-002, REQ-012, REQ-014, REQ-024
- **Componente:** `command-router | por definir (analista técnico)`; `propagation-engine`
- **Depende de:** TASK-021, TASK-012, TASK-013
- **Alcance:** Exponer el comando `save` con modos de instancia y propagación, contenido por fichero o entrada estándar, diff y conflictos.
- **Hecho cuando:** se cumplen los criterios de REQ-012 y REQ-014 a través del comando y un guardado sin cambios no escribe.
- **Pruebas automáticas:**
  - Caso feliz: guardado de instancia y propagación de grupo devuelven ficheros afectados y diff; el contenido se escribe en las rutas correctas.
  - Caso de error o límite: conflicto exige confirmación; artefacto ausente se omite; sin cambios no se escribe; ruta fuera de ámbito devuelve error.
- **Prueba manual:**
  1. Editar el cuerpo de un especialista y ejecutar `save` en modo propagación.
  2. Comprobar que los módulos del grupo reciben el cuerpo con su nombre correcto.
  3. Modificar un fichero por fuera y comprobar que `save` devuelve conflicto.

### [X] TASK-026 Comandos create-module y add-technician

- **Cubre:** REQ-002, REQ-015, REQ-016, REQ-024
- **Componente:** `command-router | por definir (analista técnico)`; `module-service`
- **Depende de:** TASK-021, TASK-016, TASK-017
- **Alcance:** Exponer los comandos `create-module` y `add-technician`, con modo de simulación que no escribe.
- **Hecho cuando:** se cumplen los criterios de REQ-015 y REQ-016 a través de los comandos y el modo de simulación devuelve los ficheros previstos sin crearlos.
- **Pruebas automáticas:**
  - Caso feliz: alta de módulo y alta de técnico devuelven el resumen y actualizan `.sdd/team.md`; la simulación no escribe.
  - Caso de error o límite: nombre inválido, módulo duplicado y técnico duplicado devuelven error o omisión; ejecución repetida es idempotente.
- **Prueba manual:**
  1. Ejecutar `create-module` con simulación y comprobar la lista de ficheros previstos sin cambios en disco.
  2. Ejecutar sin simulación y comprobar módulo, técnicos y `team.md`.
  3. Ejecutar `add-technician` y comprobar que los técnicos previos no cambian.

### [X] TASK-027 Comando create-artifact

- **Cubre:** REQ-002, REQ-019, REQ-020, REQ-024
- **Componente:** `command-router | por definir (analista técnico)`; `artifact-service`
- **Depende de:** TASK-021, TASK-015, TASK-018
- **Alcance:** Exponer el comando `create-artifact` para crear un agente o una skill desde plantilla con contenido tal cual o modificado.
- **Hecho cuando:** se cumplen los criterios de REQ-019 y REQ-020 a través del comando y un nombre existente no se sobrescribe.
- **Pruebas automáticas:**
  - Caso feliz: se crea un agente y una skill con la plantilla y con contenido modificado; ambos aparecen en el catálogo.
  - Caso de error o límite: nombre existente devuelve conflicto; ámbito o proveedor inválido devuelve error; skill con assets se crea completa.
- **Prueba manual:**
  1. Ejecutar `create-artifact` con la plantilla sin modificar y comprobar el fichero creado.
  2. Ejecutar de nuevo con contenido modificado y comprobar el cambio.
  3. Repetir con el mismo nombre y comprobar que no se sobrescribe.

### [X] TASK-028 Adaptación del menú interactivo

- **Cubre:** REQ-001, NFR-006, DEC-001
- **Componente:** `interactive-cli | no consta en el mapa`; `cli-entrypoint | no consta en el mapa`
- **Depende de:** TASK-011, TASK-009, TASK-016, TASK-020
- **Alcance:** Hacer que el menú actual use los servicios de aplicación, conserve sus opciones y salida y no contenga lógica de dominio ni impresiones desde la aplicación.
- **Hecho cuando:** se cumplen los criterios de REQ-001 y las opciones de configuración, inicio de proyecto y creación de módulo siguen funcionando.
- **Pruebas automáticas:**
  - Caso feliz: cada opción del menú se mapea al servicio correspondiente y salir termina con código 0.
  - Caso de error o límite: opción inválida muestra error y vuelve al menú; no hay impresiones de dominio fuera del adaptador `(propuesto)`.
- **Prueba manual:**
  1. Arrancar el CLI sin argumentos y comprobar que aparece el menú de siempre.
  2. Configurar un proveedor de prueba y comprobar el resultado.
  3. Iniciar un proyecto, crear un módulo y salir; comprobar que no hay regresiones.

### [X] TASK-029 Medición de rendimiento

- **Cubre:** NFR-002, DEC-016
- **Componente:** `command-contract | por definir (analista técnico)`; `artifact-inventory`
- **Depende de:** TASK-022, TASK-023, TASK-024, TASK-025, TASK-026, TASK-027
- **Alcance:** Medir los comandos sobre un proyecto de 200 artefactos y un fichero de 1 MB, y documentar si se necesita un daemon en v2.
- **Hecho cuando:** se cumplen los umbrales de NFR-002 o queda registrada la desviación con su causa.
- **Pruebas automáticas:**
  - Caso feliz: `tree` y `status` sobre 200 artefactos terminan en menos de 3 segundos; `describe` de 1 MB termina en menos de 1 segundo.
  - Caso de límite: proyecto con artefactos grandes o muchos proveedores no bloquea ni supera el umbral sin informe `(propuesto)`.
- **Prueba manual:**
  1. Preparar un proyecto de prueba con 200 artefactos.
  2. Cronometrar `status`, `tree` y `describe`.
  3. Anotar los tiempos y si se requiere el daemon de v2.

### [X] TASK-030 Contrato: cli-json - envelope y comandos

- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-023, DEC-002, DEC-003
- **Componente:** `command-contract | por definir (analista técnico)`; contrato en `docs/contracts/cli-json/` con nombre `por definir (analista técnico)`
- **Depende de:** TASK-027
- **Alcance:** Formalizar el contrato del envelope, los comandos, las opciones, los errores y los códigos de salida tal como quedaron implementados.
- **Hecho cuando:** el contrato existe en `docs/contracts/cli-json/` y cada comando implementado aparece con su entrada, salida y errores.
- **Pruebas automáticas:**
  - Caso feliz: el documento de contrato es válido según su formato y cubre todos los comandos implementados.
  - Caso de error: el contrato recoge los errores de uso, dominio e internos que exigen los requisitos.
- **Prueba manual:**
  1. Recorrer la tabla de interfaces del diseño y comprobar que cada comando aparece en el contrato.
  2. Ejecutar un comando y comparar su salida con el contrato.

### [X] TASK-031 Contrato: artifact-metadata - agrupación y hashes

- **Cubre:** REQ-013, REQ-014, REQ-021, DEC-005, DEC-009
- **Componente:** `metadata-store | por definir (analista técnico)`; contrato en `docs/contracts/artifact-metadata/` con nombre `por definir (analista técnico)`
- **Depende de:** TASK-014
- **Alcance:** Formalizar la agrupación, el cuerpo compartido, los hashes, la desvinculación y la versión de esquema del almacén.
- **Hecho cuando:** el contrato existe y coincide con lo que el almacén escribe y lee.
- **Pruebas automáticas:**
  - Caso feliz: el documento es válido y contiene todos los estados de vinculación.
  - Caso de error: recoge almacén ausente, corrupto y versión no soportada.
- **Prueba manual:**
  1. Comparar un almacén real con el contrato.
  2. Comprobar que los estados de vinculación y los hashes documentados coinciden.

### [X] TASK-032 Contrato: provider-layout - rutas, formatos y cabeceras

- **Cubre:** REQ-007, REQ-008, REQ-022, NFR-001, DEC-011, DEC-012
- **Componente:** `provider-configurator | no consta en el mapa`; contrato en `docs/contracts/provider-layout/` con nombre `por definir (analista técnico)`
- **Depende de:** TASK-020
- **Alcance:** Formalizar las rutas de proveedores, los formatos de artefacto y las variantes de cabecera por sistema operativo.
- **Hecho cuando:** el contrato existe y coincide con las rutas y cabeceras que genera la implementación.
- **Pruebas automáticas:**
  - Caso feliz: el documento es válido y cubre Windows y Linux para los cuatro proveedores.
  - Caso de error: recoge proveedor desconocido, variante de cabecera ausente y ruta no válida.
- **Prueba manual:**
  1. Comparar las rutas resueltas en Windows y Linux con las del contrato.
  2. Revisar que las cabeceras por sistema operativo están documentadas.

### [X] TASK-033 Contrato: sdd-team - lectura de módulos y técnicos

- **Cubre:** REQ-010, REQ-015, REQ-016, DEC-017
- **Componente:** `project-workspace | no consta en el mapa`; contrato en `docs/contracts/sdd-team/` con nombre `por definir (analista técnico)`
- **Depende de:** TASK-017
- **Alcance:** Formalizar la lectura de módulos y técnicos de `.sdd/team.md`, sin cambiar su formato actual.
- **Hecho cuando:** el contrato existe y coincide con el parser y con las líneas que escribe la creación de módulos.
- **Pruebas automáticas:**
  - Caso feliz: el documento describe una línea de módulo válida y su correspondencia con técnicos.
  - Caso de error: recoge `team.md` ausente, mal formado y módulos duplicados.
- **Prueba manual:**
  1. Crear un módulo y comparar la línea escrita en `team.md` con el contrato.
  2. Revisar que los técnicos pendientes y existentes se derivan como indica el contrato.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-028 |
| REQ-002 | TASK-022, TASK-023, TASK-024, TASK-025, TASK-026, TASK-027 |
| REQ-003 | TASK-021 |
| REQ-004 | TASK-021 |
| REQ-005 | TASK-021 |
| REQ-006 | TASK-021 |
| REQ-007 | TASK-001 |
| REQ-008 | TASK-003 |
| REQ-009 | TASK-004, TASK-007 |
| REQ-010 | TASK-010, TASK-022 |
| REQ-011 | TASK-005, TASK-008 |
| REQ-012 | TASK-012, TASK-013, TASK-025 |
| REQ-013 | TASK-006 |
| REQ-014 | TASK-012, TASK-025 |
| REQ-015 | TASK-016, TASK-026 |
| REQ-016 | TASK-017, TASK-026 |
| REQ-017 | TASK-009, TASK-023 |
| REQ-018 | TASK-020, TASK-024 |
| REQ-019 | TASK-018, TASK-027 |
| REQ-020 | TASK-015, TASK-027 |
| REQ-021 | TASK-014 |
| REQ-022 | TASK-019 |
| REQ-023 | TASK-021 |
| REQ-024 | TASK-011, TASK-022, TASK-023, TASK-024, TASK-025, TASK-026, TASK-027 |
| NFR-001 | TASK-001, TASK-019 |
| NFR-002 | TASK-029 |
| NFR-003 | TASK-022 |
| NFR-004 | TASK-002, TASK-006, TASK-012 |
| NFR-005 | TASK-001, TASK-002 |
| NFR-006 | TASK-021, TASK-028 |
| NFR-007 | TASK-005, TASK-013 |

## Avisos

- No existe `.sdd/architecture.md`; los componentes existentes se han marcado como `no consta en el mapa` y los nuevos como `por definir (analista técnico)`.
- No existe `.sdd/task-types.md`; no se han generado tipos de tarea adicionales.
- Se han propuesto casos de error o límite en TASK-002, TASK-003, TASK-004, TASK-005, TASK-006, TASK-007, TASK-011, TASK-014, TASK-018, TASK-022, TASK-028 y TASK-029, porque los requisitos no los detallaban.
- Los cambios sugeridos al mapa en `design.md` no se han aplicado; las tareas usan las rutas provisionales indicadas.
- Las tareas de contrato dependen de la implementación correspondiente y dejan el nombre exacto del fichero a la capa técnica.
