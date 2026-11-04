# Requisitos: cli

Estado: aprobado

## Contexto

El CLI Java existente es la fuente de verdad del dominio: valida proyectos, detecta proveedores, compone agentes y skills, crea módulos y configura entornos. Para que una aplicación de escritorio pueda consumirlo sin duplicar reglas, el CLI deberá incorporar un modo no interactivo con salida estructurada versionada, mantener el modo interactivo actual y resolver las rutas de proveedores en Windows y Linux. Esta feature es la base del viewer y no incluye su interfaz gráfica.

## Fuera de alcance

- Cambiar el comportamiento del menú interactivo actual.
- Routers por módulo: se mantiene el router compartido.
- Soporte de Codex a nivel de proyecto (queda en pausa).
- Edición de los recursos empaquetados en el jar.
- Directorio de overrides de plantillas por proyecto.
- Proceso Java persistente con JSON-RPC (se valora en una versión posterior).
- Restaurar un artefacto a la plantilla empaquetada (propuesta para v2, sujeta a la usabilidad de la aplicación).
- La interfaz gráfica del viewer.
- Operaciones de git, publicación, telemetría y actualización automática.

## Requisitos funcionales

### REQ-001 Modo interactivo sin regresiones

- **Requisito:** Cuando el CLI se ejecute sin argumentos, deberá mantener el menú interactivo actual y sus opciones.
- **Criterios de aceptación:**
  - Dado el CLI sin argumentos, cuando arranque, entonces muestra el mismo menú y acepta las mismas opciones que antes.
  - Dado que el usuario elige salir, entonces el proceso termina con código de salida 0.

### REQ-002 Subcomandos no interactivos

- **Requisito:** Cuando el CLI reciba un comando, deberá ejecutarlo sin mostrar menús ni solicitar entrada por consola.
- **Criterios de aceptación:**
  - Dados los comandos de estado, árbol, inicio de proyecto, creación de módulo, alta de técnico, descripción de artefacto, guardado y configuración de proveedor, cuando se ejecuten, entonces no leen de la entrada estándar.
  - Dado un comando con argumentos incompletos, entonces devuelve un error de uso y no modifica el sistema.

### REQ-003 Separación de datos y logs

- **Requisito:** En modo no interactivo, el CLI deberá escribir únicamente el resultado estructurado en la salida estándar y los logs en la salida de error.
- **Criterios de aceptación:**
  - Dado un comando correcto, cuando se capture la salida estándar, entonces contiene un único documento estructurado parseable.
  - Dado un comando que genere logs, entonces los mensajes humanos aparecen en la salida de error y no contaminan la salida de datos.

### REQ-004 Envelope versionado

- **Requisito:** La salida estructurada deberá incluir versión de esquema, comando ejecutado, indicador de éxito, datos y errores.
- **Criterios de aceptación:**
  - Dado cualquier comando, entonces la salida contiene `schemaVersion`, `command`, `ok`, `data` y `errors`.
  - Dado un error, entonces `ok` es falso y `errors` contiene al menos un código estable.

### REQ-005 Códigos de salida

- **Requisito:** El CLI deberá devolver 0 en éxito, 1 en error de dominio, 2 en uso incorrecto y 3 en error interno.
- **Criterios de aceptación:**
  - Dado un comando correcto, entonces devuelve 0.
  - Dado un proyecto inexistente, entonces devuelve 1.
  - Dado un argumento desconocido o incompleto, entonces devuelve 2.
  - Dado un fallo inesperado, entonces devuelve 3.

### REQ-006 Ayuda y versión

- **Requisito:** El CLI deberá ofrecer ayuda y versión sin ejecutar operaciones ni modificar el sistema.
- **Criterios de aceptación:**
  - Dado `--help`, entonces lista los comandos y sus opciones.
  - Dado `--version`, entonces muestra la versión del CLI.
  - En ambos casos no se escribe ningún fichero.

### REQ-007 Rutas multiplataforma

- **Requisito:** El CLI deberá resolver las rutas de proveedores y de estado de la aplicación en Windows y Linux.
- **Criterios de aceptación:**
  - Dado Windows, entonces usa el perfil del usuario y las rutas de configuración esperadas de cada proveedor.
  - Dado Linux, entonces usa el directorio home y respeta `XDG_CONFIG_HOME` cuando esté definido.
  - Dado un proyecto seleccionado, entonces normaliza su ruta absoluta y rechaza las rutas que no son directorio.

### REQ-008 Estado de proveedores

- **Requisito:** El CLI deberá calcular el estado de cada proveedor como no instalado, instalado sin configurar, parcial o configurado, con el detalle de los elementos de la aplicación que faltan.
- **Criterios de aceptación:**
  - Dado un proveedor con su carpeta pero sin los agentes y skills de la aplicación, entonces devuelve «parcial» y la lista de elementos ausentes.
  - Dado un proveedor sin su carpeta, entonces devuelve «no instalado».
  - Dado un proveedor con todos los agentes y skills esperados, entonces devuelve «configurado».

### REQ-009 Catálogo lógico y pertenencia

- **Requisito:** El CLI deberá identificar los artefactos de la aplicación instalados y excluir los artefactos ajenos.
- **Criterios de aceptación:**
  - Dado un agente propio del usuario en la carpeta del proveedor, entonces no aparece en el catálogo.
  - Dada una skill de la aplicación instalada, entonces aparece con su proveedor y su ámbito.
  - Dado un recurso del jar que no está instalado, entonces no se considera un artefacto instalado.

### REQ-010 Árbol global y de proyecto

- **Requisito:** El CLI deberá devolver el árbol de los ámbitos global y de proyecto.
- **Criterios de aceptación:**
  - Dado el ámbito global, entonces devuelve los proveedores y, bajo cada uno, sus agentes y skills de la aplicación instalados.
  - Dado el ámbito de proyecto, entonces devuelve `.sdd`, los módulos registrados en `.sdd/team.md`, los técnicos de cada módulo y los agentes y skills instalados en el proyecto.
  - Dado un proyecto con routers generados, entonces cada router aparece una sola vez bajo su proveedor y no repetido bajo cada módulo.
  - Dado un `.sdd/team.md` ausente o mal formado, entonces devuelve un aviso y el resto del árbol disponible.

### REQ-011 Descripción de artefactos

- **Requisito:** El CLI deberá describir un artefacto instalado separando cabecera, cuerpo compartido, formato, proveedor, módulo, hash y estado de vinculación.
- **Criterios de aceptación:**
  - Dado un agente Markdown, entonces devuelve cabecera y cuerpo separados.
  - Dado un agente Codex en TOML, entonces devuelve cabecera y cuerpo de forma que el cuerpo pueda volver a componerse como TOML válido.
  - Dado un especialista de proyecto, entonces el cuerpo compartido incluye el placeholder del módulo.
  - Dada una ruta dentro del jar, entonces devuelve un error y no la describe como artefacto editable.
  - Dado un artefacto desvinculado, entonces lo indica en su estado.

### REQ-012 Guardado y propagación

- **Requisito:** Cuando se solicite guardar, el CLI deberá permitir guardar una instancia o propagar el cuerpo al grupo lógico, devolviendo los ficheros afectados y el diff. La propagación solo deberá actualizar artefactos existentes y nunca crear copias nuevas.
- **Criterios de aceptación:**
  - Dado un cuerpo compartido de un especialista de proyecto, cuando se propague, entonces actualiza todos los módulos que lo usan conservando el nombre de módulo correcto en cada uno.
  - Dado un cuerpo compartido de un agente global, cuando se propague, entonces actualiza todos los proveedores instalados que tengan ese agente.
  - Dado que la propagación no produce cambios, entonces no escribe ningún fichero y lo informa.
  - Dado un proveedor que no tiene instalado el artefacto, entonces no lo crea y lo informa como omitido.
  - Dada una escritura, entonces se realiza de forma atómica y deja una copia de seguridad.

### REQ-013 Metadatos de agrupación

- **Requisito:** El CLI deberá conservar la agrupación lógica, el cuerpo compartido, los hashes y el estado de vinculación de los artefactos generados.
- **Criterios de aceptación:**
  - Dados unos artefactos recién generados, entonces sus metadatos de agrupación quedan registrados.
  - Dados `describe` y `save`, entonces usan esos metadatos para resolver el grupo y detectar cambios.
  - Dados los metadatos de la primera versión, entonces se guardan en el home de la aplicación y no se mezclan con los ficheros del proyecto.
  - Dados los metadatos, entonces el usuario no los edita directamente.

### REQ-014 Conflictos por cambios externos

- **Requisito:** Si el hash de un artefacto no coincide con el registrado, el CLI deberá devolver un conflicto y no sobrescribir sin confirmación.
- **Criterios de aceptación:**
  - Dado un artefacto modificado por otra herramienta, cuando se solicite guardar o propagar, entonces devuelve un conflicto con la ruta afectada.
  - Dada una propagación con artefactos en conflicto, entonces no los sobrescribe y los informa como omitidos.
  - Dado que se solicite sobrescribir explícitamente, entonces escribe el nuevo contenido y conserva una copia del anterior.

### REQ-015 Crear módulo

- **Requisito:** Cuando se cree un módulo, el CLI deberá validar el nombre, registrar el módulo en `.sdd/team.md` y generar los artefactos de los técnicos y proveedores indicados.
- **Criterios de aceptación:**
  - Dado un nombre inválido según las reglas actuales, entonces devuelve el error correspondiente y no crea nada.
  - Dado un módulo ya registrado, entonces no lo duplica y lo informa.
  - Dado un módulo válido, entonces actualiza `.sdd/team.md` y devuelve el resumen de creados, omitidos y avisos.
  - Dado un grupo lógico con cuerpo compartido, entonces los especialistas del módulo nuevo usan ese cuerpo en lugar de la plantilla empaquetada.

### REQ-016 Añadir técnico a un módulo existente

- **Requisito:** Cuando se añada un técnico a un módulo existente, el CLI deberá generar únicamente los artefactos de ese técnico y no recrear los de los demás.
- **Criterios de aceptación:**
  - Dado un módulo con un técnico, cuando se añada otro, entonces los ficheros del primero no se modifican y se crean los del nuevo.
  - Dado un técnico que ya existe para ese módulo y proveedor, entonces se omite y se informa sin sobrescribir.

### REQ-017 Iniciar proyecto

- **Requisito:** Cuando se inicie un proyecto sin `.sdd`, el CLI deberá copiar la estructura `.sdd` desde la plantilla y reportar si ya estaba configurado.
- **Criterios de aceptación:**
  - Dado un proyecto sin `.sdd`, entonces crea la estructura y devuelve el resultado.
  - Dado un proyecto con `.sdd`, entonces no sobrescribe ningún fichero y lo informa.

### REQ-018 Configurar proveedor

- **Requisito:** Cuando se configure un proveedor, el CLI deberá copiar sus agentes y skills desde los recursos, sin sobrescribir los existentes, y devolver el resultado.
- **Criterios de aceptación:**
  - Dado un proveedor sin configurar, entonces copia los agentes y skills de la aplicación y devuelve creados y omitidos.
  - Dado un proveedor ya configurado, entonces no sobrescribe y lo informa.
  - Dado un recurso no encontrado, entonces devuelve un error con el recurso afectado.

### REQ-019 Crear agente o skill desde plantilla

- **Requisito:** El CLI deberá permitir crear un agente o una skill a partir de una plantilla y un contenido proporcionado, sin sobrescribir sin confirmación.
- **Criterios de aceptación:**
  - Dado un contenido igual a la plantilla, entonces crea el artefacto con ese contenido.
  - Dado un contenido modificado, entonces crea el artefacto con las modificaciones.
  - Dado un artefacto con el mismo nombre, entonces no lo sobrescribe sin confirmación explícita.

### REQ-020 Skills como directorio

- **Requisito:** El CLI deberá tratar las skills como directorios y soportar su descripción, edición y propagación completa. La propagación deberá limitarse al mismo ámbito.
- **Criterios de aceptación:**
  - Dada una skill instalada, entonces la describe con `SKILL.md` y sus ficheros de apoyo.
  - Dado un cambio en uno de sus ficheros, entonces lo escribe en la ruta correspondiente.
  - Dada una skill presente en varios proveedores del mismo ámbito, entonces la propagación actualiza las copias existentes y conserva los ficheros auxiliares específicos de cada proveedor.
  - Dada una skill en el ámbito global y otra en el de proyecto, entonces la propagación no cruza de un ámbito al otro.

### REQ-021 Migración segura de artefactos existentes

- **Requisito:** Si existen artefactos generados antes de que existan metadatos de agrupación, el CLI deberá marcarlos como personalizados y no propagar sobre ellos sin confirmación.
- **Criterios de aceptación:**
  - Dado un proyecto con agentes generados previamente, entonces los describe como artefactos válidos y marcados como personalizados.
  - Dado un artefacto personalizado, entonces la propagación de su grupo no lo sobrescribe y lo informa.
  - Dado que el usuario adopte el cuerpo compartido, entonces devuelve el diff y actualiza tras confirmación.

### REQ-022 Cabeceras por sistema operativo

- **Requisito:** El CLI deberá generar cabeceras válidas para el sistema operativo de destino.
- **Criterios de aceptación:**
  - Dado Windows, entonces las cabeceras que requieran hooks usan las rutas y el shell de Windows.
  - Dado Linux, entonces no se generan hooks de PowerShell ni referencias a `USERPROFILE`.
  - Dado un mismo agente en ambos sistemas, entonces cada instalación recibe la cabecera correspondiente a su sistema.

### REQ-023 Errores tipados

- **Requisito:** Los errores del modo no interactivo deberán incluir código estable, ruta afectada y mensaje comprensible, sin trazas en la salida de datos.
- **Criterios de aceptación:**
  - Dado un error de dominio, entonces la salida incluye el código estable, la ruta y un mensaje comprensible.
  - Dado un error interno, entonces la salida de datos no contiene una traza y la traza se escribe en la salida de error.

### REQ-024 Operaciones de solo lectura e idempotencia

- **Requisito:** Los comandos de solo lectura no deberán modificar el sistema y los comandos de escritura deberán poder repetirse sin duplicar ni sobrescribir.
- **Criterios de aceptación:**
  - Dados `status`, `tree` y `describe`, entonces no se escribe ningún fichero.
  - Dado `create-module` ejecutado dos veces con el mismo nombre, entonces no duplica el módulo ni los agentes existentes.
  - Dado `configure-provider` ejecutado dos veces, entonces no sobrescribe los artefactos ya instalados.
  - Dado `add-technician` ejecutado dos veces, entonces no sobrescribe los ficheros existentes.

## Requisitos no funcionales

### NFR-001 Plataformas soportadas

- **Requisito:** El CLI deberá funcionar en Windows 10/11 y en Linux x86_64.
- **Criterios de aceptación:**
  - Dado Windows 10 versión 1809 o superior, cuando se ejecute el CLI, entonces todas las operaciones funcionan.
  - Dado Linux x86_64, cuando se ejecute el CLI, entonces todas las operaciones funcionan.

### NFR-002 Tiempo de respuesta

- **Requisito:** Cada comando no interactivo deberá completarse en menos de 3 segundos para un proyecto de hasta 200 artefactos, y `describe` en menos de 1 segundo para un fichero de hasta 1 MB.
- **Criterios de aceptación:**
  - Dado un proyecto de 200 artefactos, cuando se ejecute `tree` o `status`, entonces termina en menos de 3 segundos.
  - Dado un fichero de 1 MB, cuando se ejecute `describe`, entonces termina en menos de 1 segundo.

### NFR-003 Funcionamiento sin red

- **Requisito:** El CLI deberá funcionar sin conexión a red para todas sus operaciones.
- **Criterios de aceptación:**
  - Dado un equipo sin red, cuando se ejecuten estado, árbol, descripción, guardado, creación y configuración, entonces todas funcionan igual.

### NFR-004 Integridad de los datos

- **Requisito:** El CLI deberá escribir de forma atómica, conservar UTF-8, preservar el estilo de fin de línea existente y crear una copia de seguridad antes de sobrescribir.
- **Criterios de aceptación:**
  - Dado un fichero con CRLF, cuando se guarde sin cambiar el fin de línea, entonces sigue teniendo CRLF.
  - Dado un fallo durante la escritura, entonces el fichero original no queda truncado ni corrupto.
  - Dada una sobrescritura, entonces existe una copia de seguridad recuperable.

### NFR-005 Seguridad de rutas y recursos

- **Requisito:** El CLI deberá limitar las escrituras a los ámbitos global y de proyecto seleccionados y no deberá modificar los recursos empaquetados.
- **Criterios de aceptación:**
  - Dada una ruta fuera de los ámbitos permitidos, cuando se intente leer o escribir, entonces se rechaza la operación.
  - Dada una ruta con enlaces simbólicos que apunte fuera del ámbito, entonces no se sigue para escritura.
  - Dada cualquier ruta dentro del jar, cuando se intente editar, entonces se rechaza.

### NFR-006 Compatibilidad y contrato estable

- **Requisito:** El CLI deberá mantener el modo interactivo y un contrato de salida versionado que no rompa a los consumidores existentes.
- **Criterios de aceptación:**
  - Dado el mismo comando y los mismos datos, entonces la estructura de la salida es la misma y solo cambian los valores dependientes del estado.
  - Dado un cambio incompatible del contrato, entonces la versión de esquema cambia.
  - Dado el menú interactivo, entonces sigue funcionando sin cambios.

### NFR-007 Determinismo

- **Requisito:** Dados los mismos datos de entrada y el mismo estado del sistema, el CLI deberá producir el mismo resultado lógico.
- **Criterios de aceptación:**
  - Dado el mismo proyecto, cuando se ejecute `tree` dos veces sin cambios, entonces los nodos y estados son equivalentes.
  - Dada la misma operación de creación, entonces los ficheros generados tienen el mismo contenido lógico.

## Supuestos

- El CLI seguirá siendo la implementación de referencia del dominio; el viewer no replicará sus reglas.
- Los metadatos de agrupación de la primera versión se guardarán en el home de la aplicación.
- La propagación solo actualizará artefactos que ya existan; no creará copias nuevas en proveedores que no los tengan.
- La propagación de skills se hará exclusivamente dentro del mismo ámbito (global con global, proyecto con proyecto).
- La cabecera no se propagará entre módulos; solo el cuerpo compartido.
- Restaurar la plantilla empaquetada queda para v2 y se decidirá según la usabilidad de la aplicación.
- Se mantienen las reglas actuales de validación de nombres de módulo.
- Codex a nivel de proyecto queda fuera hasta revisar su funcionamiento.
- El router de cada rol seguirá siendo compartido entre módulos.
- El formato de la salida estructurada será JSON.
- Se mantiene Java 21 como requisito de ejecución.

## Preguntas abiertas

- ¿Qué versiones mínimas de Linux y qué dependencias de escritorio se consideran soportadas?
- ¿El árbol debe mostrar los recursos del jar como referencia de solo lectura, o solo lo instalado?
