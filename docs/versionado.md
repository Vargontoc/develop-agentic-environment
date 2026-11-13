# Versionado y actualizaciones

## Numeración

El proyecto usa [versionado semántico](https://semver.org/lang/es/): `MAJOR.MINOR.PATCH`.

- **La CLI y el visor llevan siempre el mismo número** y se publican juntos en la misma Release.
- Una etiqueta con sufijo (`v1.0.0-rc.1`) es una **pre-release** para probar. Los instaladores de una pre-release
  llevan internamente la versión numérica sin sufijo (`1.0.0`), porque `jpackage` no admite sufijos; se distinguen por
  el nombre del fichero y por la etiqueta de la Release.
- Cada Release adjunta `SHA256SUMS.txt`. Comprueba las sumas antes de instalar.

## Qué cuenta como cambio incompatible

Un cambio incompatible sube el **MAJOR** (por ejemplo, de `1.x` a `2.0`).

**Es incompatible:**

- Quitar o renombrar un comando de la CLI o una de sus opciones, o cambiar el significado de una opción existente.
- Quitar, renombrar o cambiar el tipo de un campo del JSON de salida, o cambiar un código de error o de salida.
  Estos cambios suben también la `schemaVersion` del contrato [`cli-json`](contracts/cli-json/cli-json.md).
- Cambiar el formato de un fichero que el usuario conserva de forma que una versión nueva no pueda leer lo que escribió
  una anterior: `.sdd/team.md`, los metadatos de artefactos, o la estructura de carpetas de cada proveedor
  (contratos [`sdd-team`](contracts/sdd-team/sdd-team.md),
  [`artifact-metadata`](contracts/artifact-metadata/artifact-metadata.md) y
  [`provider-layout`](contracts/provider-layout/provider-layout.md)) sin migración automática.
- Exigir una versión de Java o un sistema operativo que antes no se exigía, cuando se usa el jar de la CLI.

**No es incompatible** (sube MINOR o PATCH):

- Añadir un comando, una opción o un campo nuevo a la salida JSON.
- Nuevas plantillas de agentes o skills, o cambios en el texto de las existentes.
- Cambios visuales y de usabilidad del visor.
- Correcciones de errores.

| Cambio | Sube |
|---|---|
| Incompatible | MAJOR |
| Funcionalidad nueva compatible | MINOR |
| Corrección de errores | PATCH |

## Compatibilidad entre el visor y la CLI

El visor comprueba al arrancar la `schemaVersion` de la CLI que usa y avisa si no coincide con la que entiende.
La aplicación empaquetada trae la CLI correcta, así que esto solo importa si usas una CLI distinta de la incluida.

## Cómo actualizar

Tus datos no se pierden al actualizar: los agentes, skills y ficheros `.sdd` viven en las carpetas de tus proveedores y
de tus proyectos, no en la carpeta de instalación.

- **Windows:** descarga el `.msi` nuevo y ejecútalo. Sustituye a la versión anterior.
- **Linux (Debian/Ubuntu):** `sudo apt install ./agentic-viewer-<versión>-amd64.deb`. Sustituye a la versión anterior.
- **Solo la CLI:** descarga el nuevo `agentic-develop.jar` y sustituye al anterior.

Después de actualizar, los agentes y skills que ya tenías no cambian solos. Para llevarlos a las plantillas de la nueva
versión, en el visor usa **Crear → Artefactos** con **Reemplazar los artefactos existentes** activado: guarda una copia
de seguridad de cada fichero que reemplaza. Un hook que hayas modificado a mano no se sobrescribe.

## Política de mantenimiento

- Se corrigen errores en la última versión publicada de cada `MAJOR`.
- Los cambios incompatibles se anuncian en el [changelog](../CHANGELOG.md) y se publican solo en un `MAJOR` nuevo.
