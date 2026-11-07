# Diseño: cli-hooks

Estado: aprobado

## Resumen

La instalación global existente incorpora un paso más: para cada proveedor seleccionado, una política de hook decide si instala, actualiza u omite con aviso el script `sdd-specs-only`, comparando el hash en disco con el del almacén `artifact-metadata`. El hook se trata como un artefacto de tipo fichero con su propio grupo en el almacén, sin tocar nada más de `hooks/`.

## Supuestos sobre el sistema actual

- No existe `.sdd/architecture.md`: los nombres de componentes existentes se deducen de los contratos `provider-layout` y `artifact-metadata`, no del código.
- Existe un resolvedor de rutas (`PathResolver`, citado en `provider-layout`) que da la carpeta global de cada proveedor y valida el ámbito.
- Existe un almacén de metadatos (`artifact-metadata`) con cálculo de hash SHA-256 con fines de línea normalizados, lectura tolerante (ilegible = vacío) y escritura atómica con copia.
- Existe una operación de instalación global de agentes y skills que ya recibe la lista de proveedores y acumula avisos para la salida (`cli-json`); el hook se engancha ahí y no hay comando nuevo.
- El CLI ya sabe si corre en Windows o Linux (lo usa para las cabeceras).
- Los recursos del CLI (`cli/src/main/resources`) admiten añadir dos ficheros del hook, uno por sistema operativo; el hook no lleva marcadores que sustituir.
- La lista de avisos de `cli-json` admite un aviso con fichero y motivo sin cambiar el contrato.

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| install-global | Orquesta la instalación global por proveedor e invoca la política de hook al final de cada proveedor. | existente |
| hook-resource | Aporta el contenido incorporado del hook para el sistema operativo del equipo (`.bat` o `.sh`). | nuevo |
| hook-installer | Decide instalar, actualizar u omitir el hook de un proveedor según presencia, registro y hash, y devuelve el resultado. | nuevo |
| path-resolver | Resuelve y valida la carpeta `hooks/` global de cada proveedor. | existente |
| metadata-store | Guarda y lee el hash registrado del hook y calcula hashes normalizados. | existente |
| warning-report | Recoge los avisos de hook modificado o sin registro en la salida, sin alterar el código de salida. | existente |

## Flujo

1. `install-global` termina de instalar agentes y skills de un proveedor y pasa el proveedor a `hook-installer` (solo en ámbito global; el ámbito de proyecto nunca lo invoca).
2. `hook-installer` pide a `hook-resource` el contenido incorporado según el sistema operativo y a `path-resolver` la ruta destino en `hooks/` con la extensión correspondiente.
3. Si el fichero no existe: lo escribe (en Linux con permiso de ejecución) y registra su hash en disco en `metadata-store`. Si existía registro, lo trata igual que nuevo.
4. Si existe y hay registro: compara el hash en disco con el registrado.
   - Coincide y el contenido incorporado difiere: sustituye el fichero y actualiza el registro.
   - Coincide y es igual al incorporado: no hace nada ni avisa.
   - Difiere: no escribe y emite aviso de «modificado por el usuario, no actualizado».
5. Si existe y no hay registro (incluido almacén ilegible o ausente): no escribe, no crea registro y emite aviso de «no se puede comprobar si fue modificado».
6. `warning-report` incorpora los avisos a la salida; `install-global` continúa con el resto de proveedores y termina con el mismo código de salida que sin avisos.

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| install-global | hook-installer | Proveedor a procesar en ámbito global. | - |
| hook-installer | hook-resource | Petición del contenido del hook para el sistema operativo actual. | - |
| hook-installer | path-resolver | Petición de la ruta del hook en la carpeta global del proveedor. | - |
| hook-installer | metadata-store | Consulta del hash registrado y registro del hash tras escribir. | artifact-metadata |
| hook-installer | warning-report | Aviso con el fichero y el motivo cuando se omite. | cli-json |

## Modelo de datos lógico

- Hook: fichero `sdd-specs-only` (variante del sistema operativo) en la carpeta `hooks/` global de un proveedor. Relaciones: pertenece a un proveedor; tiene como máximo un registro en el almacén.
- Registro del hook: entrada del almacén con el hash de la última composición escrita. Relaciones: pertenece al grupo de hook; se identifica por la ruta absoluta del fichero.
- Grupo de hook: grupo global único que abarca el hook de todos los proveedores, con cuerpo compartido vacío. Relaciones: agrupa los registros del hook.

## Decisiones

### DEC-001 El hook se registra en el almacén de `artifact-metadata`

- **Decisión:** el hook es un artefacto de tipo fichero con grupo propio de ámbito global, estado `LINKED` y hash del fichero completo (método de agentes y perfiles). Se amplía el contrato con ese tipo de artefacto.
- **Alternativas descartadas:** un almacén o fichero de hashes aparte (duplica lectura tolerante, atomicidad y copias); comparar con el contenido incorporado sin registro (no distingue «versión antigua nuestra» de «modificado por el usuario»).
- **Cubre:** REQ-004, REQ-005, REQ-006, REQ-007, NFR-002

### DEC-002 Un recurso por sistema operativo, sin marcadores

- **Decisión:** el CLI lleva el hook en dos variantes (`.bat` y `.sh`) y instala solo la del equipo; el contenido se escribe tal cual, así el hash en disco es el del recurso.
- **Alternativas descartadas:** un único recurso con marcadores (el contenido en disco no sería el incorporado y complica la comparación); instalar ambas variantes (viola REQ-002).
- **Cubre:** REQ-002, REQ-004, REQ-005

### DEC-003 Componente de política separado de la instalación global

- **Decisión:** `hook-installer` encapsula toda la lógica de decisión y `install-global` solo lo invoca por proveedor, solo en ámbito global. Opera únicamente sobre la ruta exacta del hook.
- **Alternativas descartadas:** tratar el hook como un artefacto más del catálogo de agentes y skills (arrastra propagación, desvinculación y `adopt`, fuera de alcance); sincronizar la carpeta `hooks/` completa (pondría en riesgo scripts del usuario, NFR-001).
- **Cubre:** REQ-001, REQ-003, REQ-008, NFR-001

### DEC-004 Omitir con aviso es un resultado normal

- **Decisión:** hook modificado o sin registro se devuelve como resultado con aviso, no como error, y no interrumpe al resto de proveedores ni artefactos.
- **Alternativas descartadas:** fallar la operación (contradice REQ-008); omitir en silencio (contradice REQ-006 y REQ-007).
- **Cubre:** REQ-006, REQ-007, REQ-008

## Riesgos

- Un almacén ilegible hace que un hook ya instalado por el CLI se trate como sin registro y avise siempre: es coherente con la regla de `artifact-metadata` y queda documentado; sin opción de adopción en esta feature el aviso persiste.
- El hook de un proveedor puede quedar desactualizado indefinidamente si el usuario lo modificó: se mitiga con el aviso en cada instalación.
- Extender `artifact-metadata` con un tipo nuevo puede chocar con la validación estricta del almacén («identificador conocido»): el contrato debe definir el identificador de grupo del hook y la capa técnica ajustar la validación.
- Ruta del hook con enlaces simbólicos fuera del ámbito: se mitiga con la validación de ámbito existente de `path-resolver`.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | install-global, hook-installer, hook-resource, path-resolver | DEC-003 |
| REQ-002 | hook-resource, hook-installer | DEC-002 |
| REQ-003 | install-global, hook-installer | DEC-003 |
| REQ-004 | hook-installer, metadata-store | DEC-001, DEC-002 |
| REQ-005 | hook-installer, hook-resource, metadata-store | DEC-001, DEC-002 |
| REQ-006 | hook-installer, metadata-store, warning-report | DEC-001, DEC-004 |
| REQ-007 | hook-installer, metadata-store, warning-report | DEC-001, DEC-004 |
| REQ-008 | hook-installer, warning-report, install-global | DEC-003, DEC-004 |
| REQ-009 | (documentación de contratos) | DEC-001 |
| NFR-001 | hook-installer, path-resolver | DEC-003 |
| NFR-002 | metadata-store, hook-installer | DEC-001 |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| provider-layout | docs/contracts/provider-layout/provider-layout.md | Sustituir la frase de que el hook lo aporta el usuario por la instalación por el CLI: ruta `hooks/sdd-specs-only.bat` o `.sh`, solo ámbito global, actualización por hash y aviso si está modificado o sin registro; añadir el hook a «Artefactos instalados». |
| artifact-metadata | docs/contracts/artifact-metadata/artifact-metadata.md | Añadir el hook como tipo de artefacto (fichero) con su identificador de grupo global, hash del fichero completo y la regla de que sin registro no se adopta. |

## Cambios sugeridos al mapa

- ~ provider-layout | docs/contracts/provider-layout/provider-layout.md | incluye instalación del hook global
- ~ artifact-metadata | docs/contracts/artifact-metadata/artifact-metadata.md | incluye registro de hash del hook
- hook-installer: módulo nuevo en el CLI, dependiente de path-resolver y metadata-store (el mapa aún no existe; añadir con su ruta real).
