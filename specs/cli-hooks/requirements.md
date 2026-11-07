# Requisitos: cli-hooks

Estado: aprobado

## Contexto

Los agentes SDD invocan el hook `sdd-specs-only` desde la carpeta `hooks/` global de cada proveedor, pero hoy el usuario debe aportarlo a mano. El CLI deberá instalarlo y mantenerlo actualizado sin pisar los cambios del usuario, para quien instala el entorno agéntico en Windows o Linux.

## Fuera de alcance

- Instalar hooks en el ámbito de proyecto.
- Instalar hooks distintos de `sdd-specs-only`.
- Adoptar o sobrescribir a la fuerza un hook modificado o sin registro (no hay opción de confirmación en esta feature).
- Desinstalar el hook.
- Instalar la variante de otro sistema operativo distinto del del equipo donde se ejecuta el CLI.

## Requisitos funcionales

### REQ-001 Instalación del hook en el ámbito global

- **Requisito:** Cuando el usuario instala el entorno en el ámbito global para un proveedor, el CLI deberá instalar el hook `sdd-specs-only` en la carpeta `hooks/` global de ese proveedor.
- **Criterios de aceptación:**
  - Dado un proveedor sin hook instalado, cuando se instala en ámbito global, entonces existe `hooks/sdd-specs-only` en la carpeta global de ese proveedor.
  - Dados varios proveedores seleccionados, cuando se instala en ámbito global, entonces el hook existe en la carpeta `hooks/` de cada uno de ellos.

### REQ-002 Variante según el sistema operativo

- **Requisito:** El CLI deberá instalar el hook con extensión `.bat` en Windows y con extensión `.sh` en Linux, según el sistema del equipo donde se ejecuta.
- **Criterios de aceptación:**
  - Dado un equipo Windows, cuando se instala el hook, entonces se crea `hooks/sdd-specs-only.bat` y no `.sh`.
  - Dado un equipo Linux, cuando se instala el hook, entonces se crea `hooks/sdd-specs-only.sh`, con permiso de ejecución, y no `.bat`.

### REQ-003 Solo ámbito global

- **Requisito:** Cuando el usuario instala o crea artefactos en el ámbito de proyecto, el CLI no deberá instalar ni modificar ningún hook.
- **Criterios de aceptación:**
  - Dado un proyecto, cuando se crea un módulo, un técnico o se instala en ámbito de proyecto, entonces no aparece ninguna carpeta `hooks/` ni fichero del hook en la carpeta de proyecto del proveedor.

### REQ-004 Registro del hash

- **Requisito:** Cuando el CLI escribe el hook, deberá registrar el hash del contenido que ha quedado en disco.
- **Criterios de aceptación:**
  - Dado un hook recién instalado, cuando se consulta el registro de hashes, entonces existe una entrada para ese fichero con el hash de su contenido en disco.
  - Dado un hook recién actualizado, cuando se consulta el registro, entonces el hash registrado coincide con el del nuevo contenido en disco.

### REQ-005 Actualización de un hook no modificado

- **Requisito:** Si el hook instalado tiene registro y su hash en disco coincide con el registrado, entonces el CLI deberá sustituirlo por la versión que lleva incorporada cuando esta difiera.
- **Criterios de aceptación:**
  - Dado un hook con registro y sin cambios del usuario, cuando el CLI incorpora una versión nueva del hook y se instala, entonces el fichero en disco pasa a tener el contenido nuevo y el registro se actualiza.
  - Dado un hook con registro y sin cambios cuyo contenido ya es igual al incorporado, cuando se instala, entonces el fichero no se reescribe y no se emite aviso.

### REQ-006 Hook modificado por el usuario

- **Requisito:** Si el hash del hook en disco difiere del registrado, entonces el CLI no deberá sobrescribirlo y deberá avisar al usuario de que está modificado y no se ha actualizado.
- **Criterios de aceptación:**
  - Dado un hook con registro al que el usuario ha cambiado una línea, cuando se instala, entonces el contenido en disco permanece idéntico y la salida contiene un aviso que nombra el fichero y el motivo.
  - Dado el caso anterior, cuando termina la instalación, entonces el resto de artefactos se instalan con normalidad.

### REQ-007 Instalación antigua sin registro

- **Requisito:** Si el hook existe en disco y no tiene registro de hash, entonces el CLI no deberá sobrescribirlo y deberá avisar al usuario de que no puede comprobar si fue modificado.
- **Criterios de aceptación:**
  - Dado un hook en disco sin entrada en el registro (o con el registro ausente o ilegible), cuando se instala, entonces el contenido en disco permanece idéntico y la salida contiene un aviso que nombra el fichero y el motivo.
  - Dado el caso anterior, cuando se instala de nuevo, entonces el aviso se repite y el registro sigue sin crearse para ese fichero.

### REQ-008 Aviso sin fallo

- **Requisito:** Cuando el CLI omite el hook por estar modificado o sin registro, deberá terminar la operación sin error por ese motivo.
- **Criterios de aceptación:**
  - Dado un hook modificado, cuando se instala, entonces el código de salida es el mismo que en una instalación sin avisos y el aviso figura entre los avisos de la salida (en JSON, en la lista de avisos del contrato `cli-json`).

### REQ-009 Actualización del contrato provider-layout

- **Requisito:** El contrato `docs/contracts/provider-layout/provider-layout.md` deberá dejar de indicar que el usuario aporta el hook y deberá describir que el CLI lo instala en la carpeta `hooks/` global de cada proveedor, solo en ámbito global, con actualización por hash y aviso si está modificado o sin registro.
- **Criterios de aceptación:**
  - Dado el contrato actualizado, cuando se lee la sección «Cabeceras por sistema operativo», entonces no contiene la frase de que el hook lo aporta el usuario ni que la aplicación no lo instala.
  - Dado el contrato actualizado, cuando se lee, entonces recoge la ruta del hook (`hooks/sdd-specs-only.bat` o `.sh`), el ámbito solo global y el comportamiento ante hook modificado o sin registro.

## Requisitos no funcionales

### NFR-001 Seguridad de los cambios del usuario

- **Requisito:** El CLI no deberá modificar ni borrar ningún fichero de `hooks/` distinto del hook `sdd-specs-only` en ninguna ejecución.
- **Criterios de aceptación:**
  - Dado un directorio `hooks/` con otros scripts del usuario, cuando se instala en ámbito global, entonces el contenido y la fecha de modificación de esos scripts no cambian.

### NFR-002 Hash coherente con el almacén existente

- **Requisito:** El CLI deberá calcular el hash del hook con el mismo método que el contrato `artifact-metadata` define para ficheros (SHA-256 con fines de línea normalizados).
- **Criterios de aceptación:**
  - Dado un hook cuyo contenido solo difiere en CRLF frente a LF respecto al registrado, cuando se instala, entonces se considera no modificado.

## Supuestos

- El hook se instala dentro de la operación existente de instalación global de agentes y skills, sin un comando nuevo.
- El registro de hashes es el almacén de `artifact-metadata`; el diseño decidirá cómo se representa el hook ahí (puede requerir ampliar ese contrato).
- Una instalación en la que no se pueda leer el registro se trata como «sin registro» (REQ-007), coherente con la regla de `artifact-metadata`.
- Los proveedores con carpeta global son los cuatro de `provider-layout`; el hook se instala en cada uno de los seleccionados.
- Sin registro, siempre se avisa (REQ-007) si el fichero existe, aunque su contenido coincida con el incorporado.
- Un hook borrado por el usuario con registro vigente se reinstala como si fuera nuevo.

## Preguntas abiertas

- ¿Debe existir en el futuro una forma explícita de adoptar/forzar el hook (equivalente a `adopt --confirm`)? Por ahora queda fuera de alcance.
