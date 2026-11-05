# Requisitos: viewer

Estado: aceptado

## Contexto

Se quiere una aplicación de escritorio que permita seleccionar un proyecto, comprobar si su entorno agéntico está configurado, mostrar el estado global y del proyecto, previsualizar y editar agentes y skills instalados, crear módulos y técnicos, y operar el CLI desde una terminal integrada. El viewer consume el modo no interactivo del CLI y no reimplementa reglas de dominio. Esta feature se implementa después de la feature `cli`.

## Dependencias

- La feature `cli` debe estar aprobada e implementada antes de empezar esta.
- El viewer deberá obtener el estado, el árbol, las descripciones, el guardado, la propagación y las operaciones de creación a través del contrato no interactivo del CLI.
- El viewer no deberá escribir directamente artefactos de dominio; la composición y la escritura son responsabilidad del CLI.

## Fuera de alcance

- La implementación del modo no interactivo del CLI (feature `cli`).
- Soporte de Codex a nivel de proyecto (queda en pausa; solo se contempla en el ámbito global).
- Routers por módulo: se mantiene el router compartido actual.
- Edición de los recursos empaquetados en el jar.
- Directorio de overrides de plantillas por proyecto.
- Edición estructurada de los ficheros de `.sdd`; en esta versión solo se previsualizan.
- Proceso Java persistente con JSON-RPC.
- Restaurar un artefacto a la plantilla empaquetada (propuesta para v2, sujeta a la usabilidad de la aplicación).
- Gestión de agentes o skills ajenos a la aplicación.
- Operaciones de git, publicación, telemetría y actualización automática.
- Varios proyectos abiertos simultáneamente.

## Requisitos funcionales

### REQ-001 Selección y validación del proyecto

- **Requisito:** Cuando el usuario seleccione un directorio, el viewer deberá comprobar que existe y que es un directorio, y mostrar si el proyecto está configurado.
- **Criterios de aceptación:**
  - Dado un directorio existente sin `.sdd`, cuando se seleccione, entonces se muestra el estado «no configurado» y la acción «iniciar proyecto».
  - Dado un directorio inexistente o una ruta que no es un directorio, cuando se seleccione, entonces se muestra un error con la ruta y no se escribe nada.

### REQ-002 Inicio de proyecto

- **Requisito:** Cuando el usuario confirme iniciar un proyecto sin `.sdd`, el viewer deberá solicitarlo al CLI y mostrar el resultado.
- **Criterios de aceptación:**
  - Dado un proyecto sin `.sdd`, cuando se confirme el inicio, entonces el CLI crea la estructura, el viewer actualiza el estado y el proyecto pasa a configurado.
  - Dado un proyecto con `.sdd`, cuando se solicite iniciar, entonces no se sobrescribe ningún fichero y se informa de que ya estaba configurado.

### REQ-003 Estado de proveedores

- **Requisito:** El viewer deberá mostrar cada proveedor soportado con uno de estos estados: no instalado, instalado sin configurar, parcial o configurado, junto con el detalle de los elementos de la aplicación que faltan.
- **Criterios de aceptación:**
  - Dado un proveedor declarado como parcial por el CLI, cuando se abra el ámbito Global, entonces se muestra como «parcial» y se listan los elementos ausentes.
  - Dado un proveedor declarado como no instalado, entonces se muestra como «no instalado».
  - Dado un proveedor declarado como configurado, entonces se muestra como «configurado».

### REQ-004 Árbol global

- **Requisito:** El viewer deberá mostrar en el ámbito global, por proveedor, únicamente los agentes y skills de la aplicación que estén instalados.
- **Criterios de aceptación:**
  - Dado un agente ajeno al catálogo de la aplicación, cuando se abra el ámbito Global, entonces no aparece en el árbol.
  - Dado un agente de la aplicación instalado, entonces aparece bajo su proveedor.
  - Dada una skill no instalada, entonces no aparece en el árbol.

### REQ-005 Árbol de proyecto

- **Requisito:** Cuando el usuario seleccione un proyecto, el viewer deberá mostrar un árbol con `.sdd`, los módulos registrados en `.sdd/team.md`, los técnicos de cada módulo y los agentes y skills de la aplicación instalados en el proyecto.
- **Criterios de aceptación:**
  - Dado un proyecto con los módulos `auth` y `billing`, cuando se abra el ámbito Project, entonces aparecen ambos módulos con sus técnicos existentes y con los técnicos pendientes claramente diferenciados.
  - Dado un proyecto con routers generados, entonces cada router aparece una sola vez bajo su proveedor y no repetido bajo cada módulo.
  - Dado un `.sdd/team.md` ausente o mal formado, entonces se muestra un aviso y el resto del árbol sigue disponible.

### REQ-006 Previsualización de contenido

- **Requisito:** Cuando el usuario seleccione un agente, una skill o un fichero de `.sdd`, el viewer deberá mostrar su contenido y sus metadatos: ámbito, proveedor, ruta relativa, formato y estado.
- **Criterios de aceptación:**
  - Dado un fichero Markdown, cuando se seleccione, entonces se ofrece vista de texto y vista renderizada, y el frontmatter se muestra como metadatos separados del cuerpo.
  - Dado un fichero TOML o YAML, cuando se seleccione, entonces se muestra con resaltado de sintaxis y sin interpretarlo como Markdown.
  - Dado un fichero de `.sdd`, cuando se seleccione, entonces se muestra su contenido y se indica que es de solo lectura en esta versión.

### REQ-007 Edición de artefactos instalados

- **Requisito:** El usuario deberá poder editar los agentes y skills instalados en el ámbito global o de proyecto. El viewer no deberá permitir editar los recursos empaquetados en el jar.
- **Criterios de aceptación:**
  - Dado un agente instalado, cuando el usuario modifique su contenido y guarde, entonces el cambio persiste en la ruta del artefacto y se refleja al volver a abrirlo.
  - Dada una ruta dentro de los recursos del jar, cuando el viewer construya el árbol o reciba una petición de edición, entonces no la lista y rechaza la operación.
  - Dado un artefacto sin permisos de escritura, cuando el usuario intente guardar, entonces se muestra un error con la ruta y no se pierde el contenido editado en memoria.

### REQ-008 Editor de cabecera y cuerpo

- **Requisito:** Para los agentes, el viewer deberá mostrar la cabecera específica del proveedor y el cuerpo compartido que devuelve el CLI, y permitir editar ambos.
- **Criterios de aceptación:**
  - Dado un agente global instalado en varios proveedores, cuando el usuario edite la cabecera de un proveedor y guarde, entonces solo cambia la cabecera de ese proveedor.
  - Dado un especialista de proyecto, cuando el usuario abra su cuerpo, entonces ve el placeholder del módulo resaltado y puede alternar una previsualización con el nombre del módulo resuelto.
  - Dado un fichero Codex en formato TOML, cuando el usuario edite su cuerpo y guarde, entonces el fichero resultante sigue siendo TOML válido.

### REQ-009 Propagación de cuerpo

- **Requisito:** Cuando el usuario guarde un cuerpo en modo propagar, el viewer deberá mostrar los artefactos afectados y el diff que devuelve el CLI y, tras la confirmación, solicitar la propagación.
- **Criterios de aceptación:**
  - Dado un cuerpo compartido de un especialista de proyecto, cuando se propague, entonces se actualizan todos los módulos que lo usan conservando el nombre de módulo correcto en cada uno.
  - Dado un cuerpo compartido de un agente global, cuando se propague, entonces se actualizan todos los proveedores instalados que tengan ese agente.
  - Dado que la propagación no produce cambios, entonces no se escribe ningún fichero y se informa de que no había diferencias.
  - Dado un proveedor que no tiene instalado el artefacto, entonces no se crea y se informa de que se ha omitido.

### REQ-010 Guardado local y desvinculación

- **Requisito:** El viewer deberá permitir guardar un artefacto sin propagar y marcarlo como personalizado, de forma que quede excluido de futuras propagaciones.
- **Criterios de aceptación:**
  - Dado un especialista de proyecto, cuando el usuario guarde solo su instancia, entonces solo cambia ese fichero y queda marcado como «personalizado».
  - Dado un artefacto personalizado, cuando se propague el cuerpo compartido de su grupo, entonces no se sobrescribe y se avisa de que está desvinculado.
  - Dado un artefacto personalizado, cuando el usuario decida volver al cuerpo compartido, entonces se le muestra el diff y se actualiza tras confirmación.

### REQ-011 Conflictos por cambios externos

- **Requisito:** Si el CLI devuelve un conflicto porque un artefacto cambió fuera del viewer desde la última lectura, el viewer deberá avisar antes de guardar y ofrecer recargar, comparar o sobrescribir.
- **Criterios de aceptación:**
  - Dado un fichero modificado por otra herramienta, cuando el usuario intente guardar, entonces se muestra el aviso de conflicto y no se sobrescribe automáticamente.
  - Dado el aviso de conflicto, cuando el usuario elija recargar, entonces se descartan los cambios locales de ese fichero y se muestra el contenido actual.
  - Dado el aviso de conflicto, cuando el usuario elija sobrescribir, entonces se escribe su versión y se conserva una copia de seguridad de la anterior.

### REQ-012 Crear módulo

- **Requisito:** Cuando el usuario cree un módulo, el viewer deberá solicitar nombre, técnicos y proveedores, validar el nombre con las reglas del CLI y mostrar antes los ficheros que se crearán.
- **Criterios de aceptación:**
  - Dado un nombre inválido según las reglas del CLI, cuando se confirme, entonces se muestra el error correspondiente y no se crea nada.
  - Dado un módulo ya registrado, cuando se intente crear de nuevo, entonces no se duplica y se informa de que ya existía.
  - Dado un módulo válido, cuando se confirme, entonces se actualiza `.sdd/team.md`, se generan los artefactos y se muestra el resumen de creados, omitidos y avisos.
  - Dado un grupo lógico con cuerpo compartido, entonces el módulo nuevo usa ese cuerpo en lugar de la plantilla empaquetada.

### REQ-013 Añadir técnico a un módulo existente

- **Requisito:** Cuando el usuario añada un técnico a un módulo existente, el viewer deberá solicitar únicamente los datos de ese técnico y no recrear los de los demás.
- **Criterios de aceptación:**
  - Dado un módulo con un técnico, cuando se añada otro, entonces los ficheros del primero no se modifican y se crean los del nuevo.
  - Dado un técnico que ya existe para ese módulo y proveedor, entonces se omite y se informa sin sobrescribir.

### REQ-014 Crear agente o skill desde plantilla

- **Requisito:** Cuando el usuario cree un agente o una skill, el viewer deberá mostrar una plantilla editable y permitir guardarla tal cual o modificada en el ámbito y proveedor seleccionados.
- **Criterios de aceptación:**
  - Dado el diálogo de creación, cuando se abra, entonces se muestra el contenido de la plantilla con los placeholders resueltos y un campo para el nombre.
  - Dado que el usuario no modifique la plantilla, cuando guarde, entonces se crea el artefacto con el contenido mostrado.
  - Dado que el usuario modifique la plantilla, cuando guarde, entonces se crea el artefacto con sus cambios.
  - Dado que ya existe un artefacto con ese nombre, entonces no se sobrescribe sin confirmación.

### REQ-015 Skills como directorio

- **Requisito:** El viewer deberá tratar cada skill como un directorio, mostrando y permitiendo editar `SKILL.md` y sus ficheros de apoyo, y solicitando al CLI la propagación de la carpeta completa.
- **Criterios de aceptación:**
  - Dada una skill instalada, cuando se seleccione, entonces se listan `SKILL.md` y sus ficheros de apoyo.
  - Dado un cambio en un fichero de la skill, cuando se guarde, entonces se escribe en la ruta correspondiente.
  - Dada una skill presente en varios proveedores del mismo ámbito, cuando se propague, entonces se actualizan las copias existentes y se conservan los ficheros auxiliares específicos de cada proveedor.
  - Dada una skill en el ámbito global y otra en el de proyecto, entonces la propagación no cruza de un ámbito al otro.

### REQ-016 Terminal integrada

- **Requisito:** El viewer deberá ofrecer una terminal embebida que ejecute el CLI interactivo con el directorio del proyecto como directorio de trabajo.
- **Criterios de aceptación:**
  - Dada la terminal abierta, cuando el usuario escriba una opción del menú del CLI, entonces el CLI recibe la entrada y responde como en una terminal del sistema.
  - Dada una operación en curso, cuando el usuario envíe la interrupción habitual, entonces el proceso la recibe y la terminal sigue utilizable.
  - Dado un cambio de tamaño de la ventana, cuando la terminal esté abierta, entonces el proceso recibe el nuevo tamaño y el texto se reajusta.
  - Dado que el CLI termina, entonces se muestra su código de salida y se ofrece reiniciarlo.

### REQ-017 Actualización tras operaciones

- **Requisito:** Después de una operación del CLI o de un cambio externo, el viewer deberá actualizar el árbol y el estado sin reiniciar la aplicación.
- **Criterios de aceptación:**
  - Dado que se crea un módulo desde el viewer, cuando termine la operación, entonces el nuevo módulo aparece en el árbol.
  - Dado un fichero modificado por otra herramienta, cuando el viewer detecte el cambio, entonces marca el nodo como desactualizado y permite recargarlo.
  - Dado que se configura un proveedor, cuando termine la operación, entonces su estado y sus hijos se actualizan.

### REQ-018 Gestión de errores sin cierre

- **Requisito:** El viewer deberá mostrar los errores del CLI con código, ruta afectada y acción sugerida, sin cerrar la aplicación.
- **Criterios de aceptación:**
  - Dado un error de dominio del CLI, cuando se produzca, entonces el viewer muestra el código, la ruta y una acción sugerida.
  - Dado un fallo inesperado del CLI, entonces la aplicación sigue abierta y permite reintentar la operación.

## Requisitos no funcionales

### NFR-001 Plataformas soportadas

- **Requisito:** El viewer deberá funcionar en Windows 10/11 y en Linux x86_64.
- **Criterios de aceptación:**
  - Dado Windows 10 versión 1809 o superior, cuando se instale la aplicación, entonces puede abrir un proyecto y usar la terminal.
  - Dado Linux x86_64 con las dependencias de escritorio instaladas, cuando se instale la aplicación, entonces puede abrir un proyecto y usar la terminal.

### NFR-002 Tiempo de apertura y navegación

- **Requisito:** El viewer deberá mostrar el árbol de un proyecto de hasta 200 artefactos en menos de 2 segundos desde que se selecciona el directorio.
- **Criterios de aceptación:**
  - Dado un proyecto de 200 artefactos en disco, cuando se seleccione, entonces el árbol y el estado están visibles en menos de 2 segundos.
  - Dado un fichero de hasta 1 MB, cuando se seleccione, entonces su contenido se muestra en menos de 1 segundo.

### NFR-003 Funcionamiento sin red

- **Requisito:** El viewer deberá funcionar sin conexión a red para todas las operaciones de inspección, edición, creación y terminal.
- **Criterios de aceptación:**
  - Dado un equipo sin red, cuando se abra un proyecto, se edite un artefacto y se cree un módulo, entonces todas las operaciones funcionan igual.

### NFR-004 Fluidez de la terminal

- **Requisito:** La terminal integrada deberá procesar la salida del proceso sin bloquear la interfaz de usuario.
- **Criterios de aceptación:**
  - Dado un proceso que escribe de forma continua, cuando la terminal esté mostrando la salida, entonces la interfaz sigue respondiendo a la interacción.
  - Dado un proceso que genera salida voluminosa, entonces la terminal no pierde caracteres de control necesarios para interpretar la salida.

### NFR-005 Protección de cambios sin guardar

- **Requisito:** El viewer no deberá perder los cambios en memoria cuando falle una operación de guardado o cuando el usuario cambie de artefacto.
- **Criterios de aceptación:**
  - Dado un editor con cambios sin guardar, cuando el usuario seleccione otro artefacto, entonces se le pide confirmación antes de descartarlos.
  - Dado un fallo de guardado, entonces el contenido editado permanece en el editor y se muestra el error.

### NFR-006 Fuente única de verdad

- **Requisito:** El viewer deberá obtener y modificar el estado del dominio exclusivamente a través del contrato no interactivo del CLI, sin escribir artefactos directamente.
- **Criterios de aceptación:**
  - Dado un proyecto, cuando se consulte su árbol desde el viewer y desde el CLI, entonces ambos devuelven la misma estructura y los mismos estados.
  - Dada una creación de módulo lanzada desde el viewer, entonces el resultado y los ficheros generados coinciden con los de ejecutar el mismo comando en el CLI.
  - Dada una operación de escritura, entonces el viewer no accede directamente al fichero de dominio.

## Supuestos

- La feature `cli` estará implementada y su contrato será estable antes de empezar el viewer.
- El viewer usará el modo no interactivo del CLI para todas las operaciones de dominio.
- Los metadatos de agrupación gestionados por el CLI vivirán en el home de la aplicación en la primera versión.
- La terminal integrada ejecutará el CLI interactivo; el shell del sistema quedará como opción posterior.
- Los ficheros de `.sdd` serán de solo lectura en esta versión.
- La propagación solo actualizará artefactos existentes; no creará copias nuevas en proveedores que no los tengan.
- La propagación de skills se hará exclusivamente dentro del mismo ámbito (global con global, proyecto con proyecto).
- La cabecera no se propagará entre módulos; solo el cuerpo compartido.
- Restaurar la plantilla empaquetada queda para v2 y se decidirá según la usabilidad de la aplicación.
- Se abrirá un solo proyecto a la vez.
- Solo se mostrarán los artefactos de la aplicación instalados, no los ajenos ni los recursos del jar.
- El usuario revisará los supuestos del CLI sobre propagación y metadatos antes de implementar el viewer.

## Preguntas abiertas

- ¿La terminal debe ofrecer también el shell del sistema además del CLI?
- ¿Debe el usuario poder editar los ficheros de `.sdd` como texto en una versión posterior?
- ¿Debe el viewer mostrar los recursos del jar como referencia de solo lectura, o solo lo instalado?
- ¿La aplicación debe incluir su propio runtime de Java o exigir Java 21 instalado?
- ¿Qué versiones mínimas de Linux y qué dependencias de escritorio se consideran soportadas?
