# Tareas: cli-hooks

Estado: aprobado

## Resumen

El trabajo se divide en componentes pequeños (recurso del hook, ruta, registro, política de instalación por caso y dos integraciones con la instalación global y con la salida `cli-json`), seguidos de las tareas de contrato `artifact-metadata` y `provider-layout`.

## Tareas

### [ ] TASK-001 Recurso del hook por sistema operativo

- **Cubre:** REQ-002, DEC-002
- **Componente:** hook-resource | por definir (analista técnico)
- **Depende de:** -
- **Alcance:** incorporar en el CLI las dos variantes del hook `sdd-specs-only` (`.bat` y `.sh`) y devolver la que corresponde al sistema operativo del equipo.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-002 y el recurso devuelve el contenido de la variante del sistema operativo tal cual, sin marcadores ni sustituciones.
- **Pruebas automáticas:**
  - Caso feliz: en un equipo Windows el recurso devuelve la variante `.bat`; en Linux, la variante `.sh`. Resultado esperado: el contenido devuelto coincide con el del recurso correspondiente.
  - Caso límite: el contenido devuelto no contiene texto añadido ni marcadores, y nunca se devuelve la variante del otro sistema. Resultado esperado: el contenido es idéntico al del recurso y la otra variante no aparece.
- **Prueba manual:**
  1. Revisar en `cli/src/main/resources` que existen las dos variantes del hook `sdd-specs-only` (`.bat` y `.sh`) y que ninguna contiene marcadores. Resultado esperado: ambas variantes presentes y sin marcadores.

### [ ] TASK-002 Ruta del hook en la carpeta global del proveedor

- **Cubre:** REQ-001, REQ-002, NFR-001, DEC-003
- **Componente:** path-resolver | no consta en el mapa
- **Depende de:** -
- **Alcance:** resolver la ruta exacta `hooks/sdd-specs-only` con la extensión del sistema operativo dentro de la carpeta global de cada proveedor, validando que la ruta queda en el ámbito global.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 y REQ-002 en lo relativo a la ruta, y la ruta resuelta apunta solo al fichero del hook.
- **Pruebas automáticas:**
  - Caso feliz: para cada proveedor con carpeta global, en Windows la ruta termina en `hooks/sdd-specs-only.bat` y en Linux en `hooks/sdd-specs-only.sh`. Resultado esperado: la ruta está dentro de la carpeta global del proveedor.
  - Caso límite: si la carpeta `hooks/` es un enlace simbólico que sale del ámbito global, la resolución se rechaza. Resultado esperado: no se devuelve ruta y se informa del motivo.
- **Prueba manual:**
  1. En un equipo Windows, resolver la ruta del hook para el proveedor Claude (con la herramienta de pruebas del CLI o mediante la instalación de TASK-008). Resultado esperado: la ruta termina en `hooks/sdd-specs-only.bat` y está en la carpeta global de Claude.

### [ ] TASK-003 Registro del hook en el almacén de metadatos

- **Cubre:** REQ-004, NFR-002, DEC-001
- **Componente:** metadata-store | no consta en el mapa
- **Depende de:** -
- **Alcance:** permitir que el almacén guarde el hook como artefacto de tipo fichero, con grupo global único identificado por la ruta absoluta del fichero, y calcule su hash con SHA-256 y fines de línea normalizados.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-004 y NFR-002, y el almacén acepta el grupo del hook sin rechazarlo por identificador desconocido.
- **Pruebas automáticas:**
  - Caso feliz: se registra el hash del hook y se consulta por su ruta absoluta. Resultado esperado: se devuelve el hash del contenido escrito en disco.
  - Caso límite: dos contenidos iguales salvo CRLF frente a LF producen el mismo hash. Resultado esperado: hashes idénticos.
  - Caso de error: un identificador de grupo que no es el del hook ni uno conocido sigue rechazándose por la validación estricta. Resultado esperado: el almacén rechaza la operación y no altera las entradas existentes.
- **Prueba manual:**
  1. Tras una instalación global (TASK-008), abrir el almacén de metadatos y localizar la entrada del hook. Resultado esperado: entrada con la ruta absoluta del fichero y un hash de 64 caracteres hexadecimales.

### [ ] TASK-004 Instalación del hook cuando no existe

- **Cubre:** REQ-001, REQ-002, REQ-004, DEC-002, DEC-003
- **Componente:** hook-installer | por definir (analista técnico)
- **Depende de:** TASK-001, TASK-002, TASK-003
- **Alcance:** implementar la política de hook para el caso en que el fichero no existe: escribe la variante del equipo, con permiso de ejecución en Linux, y registra el hash del contenido escrito.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001, REQ-002 y REQ-004 para un hook nuevo, y tras la escritura existen el fichero y su registro.
- **Pruebas automáticas:**
  - Caso feliz: proveedor sin hook en Windows. Resultado esperado: se crea `hooks/sdd-specs-only.bat`, no `.sh`, y el registro contiene el hash del contenido escrito.
  - Caso límite (Linux): se crea `hooks/sdd-specs-only.sh` con permiso de ejecución y no `.bat`. Resultado esperado: el fichero es ejecutable y el registro existe.
  - Caso límite (hook borrado por el usuario con registro vigente): se trata como nuevo. Resultado esperado: el fichero se reinstala y el registro se actualiza con el nuevo hash.
- **Prueba manual:**
  1. En Windows, con un proveedor sin hook en su carpeta global, ejecutar la instalación global de ese proveedor. Resultado esperado: existe `hooks/sdd-specs-only.bat` y no existe `hooks/sdd-specs-only.sh`.

### [ ] TASK-005 Actualización de un hook no modificado

- **Cubre:** REQ-005, NFR-002, DEC-001, DEC-002
- **Componente:** hook-installer | por definir (analista técnico)
- **Depende de:** TASK-004
- **Alcance:** cuando el hook tiene registro y su hash en disco coincide con el registrado, sustituirlo por la versión incorporada si difiere, o dejarlo sin reescribir si es igual.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-005 y la comparación de hashes respeta NFR-002.
- **Pruebas automáticas:**
  - Caso feliz: hook con registro, sin cambios del usuario, y versión incorporada distinta. Resultado esperado: el fichero pasa a tener el contenido nuevo, el registro coincide con el hash del nuevo contenido y no hay aviso.
  - Caso límite: hook con registro cuyo contenido ya es igual al incorporado. Resultado esperado: el fichero no se reescribe (su fecha de modificación no cambia) y no hay aviso.
  - Caso límite: hook sin cambios del usuario que solo difiere del registrado en CRLF frente a LF. Resultado esperado: se considera no modificado, no hay aviso y se aplica la regla anterior según el contenido incorporado.
- **Prueba manual:**
  1. Instalar el hook, después ejecutar la instalación global con una versión incorporada distinta (por ejemplo, un build de prueba) y sin tocar el fichero. Resultado esperado: el contenido del hook cambia a la nueva versión y no aparece aviso.

### [ ] TASK-006 Omisión con aviso de un hook modificado

- **Cubre:** REQ-006, DEC-004
- **Componente:** hook-installer | por definir (analista técnico)
- **Depende de:** TASK-004
- **Alcance:** cuando el hash en disco difiere del registrado, no escribir el hook y devolver un resultado de omisión que nombra el fichero y el motivo.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-006 para el hook y el resultado es una omisión con aviso, no un error.
- **Pruebas automáticas:**
  - Caso feliz: hook con registro al que se ha cambiado una línea. Resultado esperado: el contenido en disco permanece idéntico y el resultado indica que el fichero está modificado por el usuario y no se ha actualizado.
  - Caso límite: hook modificado cuando la versión incorporada es distinta. Resultado esperado: no se sustituye el fichero y no se modifica el registro.
- **Prueba manual:**
  1. Cambiar una línea del hook instalado y ejecutar la instalación global de ese proveedor. Resultado esperado: el fichero conserva el cambio y la salida lo indica como modificado.

### [ ] TASK-007 Omisión con aviso de un hook sin registro

- **Cubre:** REQ-007, DEC-004
- **Componente:** hook-installer | por definir (analista técnico)
- **Depende de:** TASK-004
- **Alcance:** cuando el fichero existe y no hay registro (sin entrada, almacén ausente o ilegible), no escribir el hook, no crear registro y devolver un resultado de omisión que indica que no se puede comprobar si fue modificado.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-007 y no se crea registro para ese fichero.
- **Pruebas automáticas:**
  - Caso feliz: hook en disco sin entrada en el registro. Resultado esperado: el contenido permanece idéntico, el resultado nombra el fichero y el motivo, y no se crea entrada.
  - Caso límite: almacén ilegible o ausente con el hook presente. Resultado esperado: se trata como sin registro (aviso, sin cambios).
  - Caso límite: hook sin registro cuyo contenido coincide con el incorporado. Resultado esperado: se avisa igualmente.
  - Caso límite: segunda instalación sin resolver el caso. Resultado esperado: el aviso se repite y el registro sigue sin crearse.
- **Prueba manual:**
  1. Eliminar la entrada del hook del almacén de metadatos y ejecutar la instalación global del proveedor. Resultado esperado: el hook no cambia y la salida avisa de que no se puede comprobar si fue modificado.
  2. Repetir la instalación global. Resultado esperado: el aviso aparece otra vez y el almacén no tiene entrada para el hook.

### [ ] TASK-008 Integración de install-global con hook-installer

- **Cubre:** REQ-001, REQ-003, NFR-001, DEC-003
- **Componente:** install-global | no consta en el mapa; hook-installer | por definir (analista técnico)
- **Depende de:** TASK-004, TASK-005, TASK-006, TASK-007
- **Alcance:** la instalación global invoca la política de hook una vez por cada proveedor seleccionado, después de instalar sus agentes y skills, y nunca en ámbito de proyecto.
- **Hecho cuando:** se cumplen los criterios de aceptación de REQ-001 y REQ-003, y la instalación global de varios proveedores deja el hook en la carpeta `hooks/` de cada uno.
- **Pruebas automáticas:**
  - Caso feliz: instalación global con varios proveedores seleccionados. Resultado esperado: el hook existe en la carpeta `hooks/` de cada uno de ellos.
  - Caso límite: crear un módulo, crear un técnico o instalar en ámbito de proyecto. Resultado esperado: no aparece ninguna carpeta `hooks/` ni fichero del hook en la carpeta de proyecto del proveedor.
  - Caso límite (NFR-001): `hooks/` contiene otros scripts del usuario. Resultado esperado: su contenido y fecha de modificación no cambian tras la instalación global.
- **Prueba manual:**
  1. En Windows, crear en la carpeta `hooks/` global de un proveedor un script propio (por ejemplo `mi-script.bat`) y anotar su contenido y fecha.
  2. Ejecutar la instalación global seleccionando dos proveedores. Resultado esperado: ambas carpetas `hooks/` contienen `sdd-specs-only.bat`.
  3. Comprobar que `mi-script.bat` sigue igual. Resultado esperado: mismo contenido y misma fecha.
  4. Crear un módulo en un proyecto. Resultado esperado: no aparece ninguna carpeta `hooks/` en el proyecto.

### [ ] TASK-009 Integración de hook-installer con warning-report

- **Cubre:** REQ-006, REQ-007, REQ-008, DEC-004
- **Componente:** hook-installer | por definir (analista técnico); warning-report | no consta en el mapa
- **Depende de:** TASK-006, TASK-007, TASK-008
- **Alcance:** los avisos de hook modificado o sin registro llegan a la lista de avisos de la salida `cli-json` con el fichero y el motivo, sin alterar el código de salida ni interrumpir la instalación.
- **Hecho cuando:** se cumple REQ-008 y los avisos de REQ-006 y REQ-007 aparecen en la salida JSON.
- **Pruebas automáticas:**
  - Caso feliz: instalación global con un hook modificado. Resultado esperado: la salida JSON contiene un aviso en su lista de avisos con el nombre del fichero y el motivo, y el código de salida es el mismo que sin avisos.
  - Caso límite: un proveedor con hook sin registro y otro con hook correcto. Resultado esperado: el aviso aparece solo para el primero y los agentes, skills y el hook del segundo se instalan con normalidad.
- **Prueba manual:**
  1. Cambiar una línea del hook instalado en un proveedor.
  2. Ejecutar la instalación global de ese proveedor en modo no interactivo con salida JSON. Resultado esperado: la lista de avisos contiene el nombre del fichero y el motivo de modificación.
  3. Comparar el código de salida con el de la misma instalación sin el cambio. Resultado esperado: es el mismo.
  4. Comprobar que los agentes y skills del proveedor se instalaron. Resultado esperado: instalados con normalidad.

### [ ] TASK-010 Contrato artifact-metadata - hook como artefacto de tipo fichero

- **Cubre:** REQ-004, REQ-005, REQ-006, REQ-007, NFR-002, DEC-001
- **Componente:** metadata-store | no consta en el mapa
- **Ubicación del contrato:** `docs/contracts/artifact-metadata/artifact-metadata.md`
- **Depende de:** TASK-003
- **Alcance:** crear o actualizar el contrato `artifact-metadata` para añadir el hook como tipo de artefacto fichero, con su identificador de grupo global, el hash del fichero completo con fines de línea normalizados y la regla de que sin registro no se adopta.
- **Hecho cuando:** el contrato existe en `docs/contracts/artifact-metadata/` y describe el tipo fichero del hook tal como quedó implementado en TASK-003.
- **Pruebas automáticas:**
  - Caso feliz: el contrato contiene el tipo de artefacto del hook, su identificador de grupo global, la regla de hash (SHA-256 del fichero completo con fines de línea normalizados) y la regla de no adopción. Resultado esperado: las cuatro partes aparecen en el contrato.
  - Caso de error: el contrato recoge el comportamiento ante hook modificado o sin registro (no sobrescribir y avisar). Resultado esperado: ambos casos aparecen descritos.
- **Prueba manual:**
  1. Leer el contrato y comparar el identificador de grupo del hook con el que usa el almacén en TASK-003. Resultado esperado: coinciden.
  2. Comparar la regla de hash con NFR-002. Resultado esperado: coincide con SHA-256 y fines de línea normalizados.

### [ ] TASK-011 Contrato provider-layout - instalación del hook por el CLI

- **Cubre:** REQ-001, REQ-002, REQ-003, REQ-006, REQ-007, REQ-009, DEC-003
- **Componente:** path-resolver | no consta en el mapa
- **Ubicación del contrato:** `docs/contracts/provider-layout/provider-layout.md`
- **Depende de:** TASK-002, TASK-008, TASK-009
- **Alcance:** actualizar el contrato para sustituir la frase de que el usuario aporta el hook por la instalación por el CLI (ruta `hooks/sdd-specs-only.bat` o `.sh`, solo ámbito global, actualización por hash y aviso si está modificado o sin registro) y añadir el hook a «Artefactos instalados».
- **Hecho cuando:** el contrato existe en `docs/contracts/provider-layout/` y describe el hook tal como quedó implementado en TASK-002, TASK-008 y TASK-009.
- **Pruebas automáticas:**
  - Caso feliz: la sección «Cabeceras por sistema operativo» no contiene la frase de que el hook lo aporta el usuario ni la de que la aplicación no lo instala, y el contrato recoge la ruta, el ámbito solo global y la actualización por hash. Resultado esperado: se cumplen ambas comprobaciones.
  - Caso de error: el contrato describe que, ante hook modificado o sin registro, no se sobrescribe y se avisa. Resultado esperado: la descripción aparece en el contrato.
- **Prueba manual:**
  1. Leer la sección «Cabeceras por sistema operativo». Resultado esperado: no indica que el usuario aporta el hook.
  2. Comprobar que se citan `hooks/sdd-specs-only.bat` y `hooks/sdd-specs-only.sh` según el sistema operativo y que el ámbito es solo global. Resultado esperado: ambos datos presentes.
  3. Comprobar que el hook aparece en «Artefactos instalados». Resultado esperado: presente.
  4. Comparar ruta y ámbito con lo implementado en TASK-002 y TASK-008. Resultado esperado: coinciden.

## Cobertura

| Requisito | Tareas |
|---|---|
| REQ-001 | TASK-002, TASK-004, TASK-008, TASK-011 |
| REQ-002 | TASK-001, TASK-002, TASK-004, TASK-011 |
| REQ-003 | TASK-008, TASK-011 |
| REQ-004 | TASK-003, TASK-004, TASK-010 |
| REQ-005 | TASK-005, TASK-010 |
| REQ-006 | TASK-006, TASK-009, TASK-010, TASK-011 |
| REQ-007 | TASK-007, TASK-009, TASK-010, TASK-011 |
| REQ-008 | TASK-009 |
| REQ-009 | TASK-011 |
| NFR-001 | TASK-002, TASK-008 |
| NFR-002 | TASK-003, TASK-005, TASK-010 |
