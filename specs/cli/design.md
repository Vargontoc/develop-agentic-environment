# Diseño: cli

Estado: aprobado

## Resumen

El CLI se reorganiza en dos adaptadores de entrada —el menú interactivo actual y un nuevo adaptador de comandos con salida estructurada— sobre los mismos servicios de dominio. El dominio se amplía con un modelo de artefactos compuestos (cabecera por proveedor y cuerpo compartido), un catálogo explícito de artefactos de la aplicación, un motor de propagación y un almacén de metadatos en el home de la aplicación. La salida estructurada se convierte en el contrato que consumirá el viewer.

## Supuestos sobre el sistema actual

- No existe `.sdd/architecture.md` ni `.sdd/project.md` en la raíz; el diseño se apoya en `specs/cli/requirements.md` y en la estructura observada del CLI.
- El CLI actual no interpreta argumentos: el arranque construye el menú interactivo y toda la salida va a la consola.
- La lógica de creación y configuración está acoplada a la impresión por consola y no devuelve resultados completos.
- El catálogo de proveedores solo comprueba la existencia de carpetas; no distingue instalado, parcial o configurado.
- Los recursos del jar son la única fuente de plantillas; no existe un repositorio editable.
- No hay pruebas automatizadas en el repositorio.
- Se asume que los formatos de agentes y skills de los proveedores no cambian durante la feature.
- Se asume que el usuario tiene permisos de escritura en los directorios de proveedores y en el home de la aplicación.

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| `cli-entrypoint` | Decidir entre menú interactivo y comando, y mapear el resultado a código de salida. | existente, se modifica |
| `interactive-cli` | Menú, preguntas y presentación humana actuales. | existente |
| `command-contract` | Interpretar comandos y opciones, serializar la salida estructurada y aplicar el envelope versionado. | nuevo |
| `command-router` | Dirigir cada comando al servicio de aplicación correspondiente. | nuevo |
| `application-services` | Orquestar estado, árbol, descripción, guardado, creación, configuración e inicialización sin imprimir. | existente, se modifica |
| `provider-catalog` | Resolver proveedores instalados y calcular su estado. | existente, se modifica |
| `path-resolver` | Resolver rutas de proveedores y de estado en Windows y Linux. | nuevo |
| `artifact-catalog` | Definir los artefactos de la aplicación esperados por proveedor, su formato y su cabecera. | nuevo |
| `artifact-inventory` | Descubrir artefactos instalados y mapearlos a entradas del catálogo. | nuevo |
| `artifact-format` | Separar y componer cabecera y cuerpo para Markdown y TOML. | nuevo |
| `artifact-descriptor` | Construir la descripción editable de un artefacto y su grupo lógico. | nuevo |
| `artifact-service` | Crear agentes y skills desde plantilla y gestionar skills como directorio. | nuevo |
| `propagation-engine` | Calcular y aplicar cambios sobre un grupo lógico, detectar conflictos y marcar desvinculaciones. | nuevo |
| `metadata-store` | Persistir agrupación, cuerpo compartido, hashes y estado de vinculación en el home. | nuevo |
| `text-store` | Leer y escribir UTF-8, preservar fin de línea, escribir de forma atómica y crear copias. | nuevo |
| `resource-repository` | Acceder a los recursos empaquetados y a las cabeceras por sistema operativo. | existente, se modifica |
| `project-workspace` | Inicializar `.sdd`, leer módulos de `team.md` y registrar cambios. | existente, se modifica |
| `project-tree` | Componer el árbol global y de proyecto a partir del catálogo y del inventario. | nuevo |
| `module-service` | Crear módulos y añadir técnicos. | existente, se modifica |
| `provider-configurator` | Instalar agentes, skills y perfiles globales. | existente, se modifica |
| `migration-service` | Detectar artefactos legacy y marcarlos como personalizados. | nuevo |
| `error-model` | Códigos estables, categorías y mapeo a códigos de salida. | existente, se modifica |

## Flujo

1. Arranque: `cli-entrypoint` inspecciona los argumentos. Sin argumentos delega en `interactive-cli`; con un comando delega en `command-contract`.
2. Comando: `command-contract` valida las opciones, `command-router` invoca el servicio de aplicación y el resultado se serializa en el envelope.
3. Estado y árbol: `provider-catalog` resuelve rutas y estado; `artifact-inventory` cruza el catálogo con lo instalado; `project-tree` compone el árbol global y el de proyecto.
4. Descripción: `artifact-inventory` localiza el artefacto; `artifact-format` separa cabecera y cuerpo; `metadata-store` aporta grupo, hashes y desvinculación; `artifact-descriptor` devuelve la descripción.
5. Guardado: `artifact-descriptor` recibe el contenido editado; `propagation-engine` calcula el cambio, consulta `metadata-store`, escribe con `text-store` y actualiza los hashes.
6. Creación de módulo: `module-service` valida, actualiza `team.md`, compone artefactos con `resource-repository` y `artifact-format`, y registra metadatos.
7. Configuración de proveedor: `provider-configurator` copia recursos, selecciona la cabecera según el sistema operativo y registra metadatos.
8. Migración: `migration-service` detecta artefactos sin metadatos y los marca como personalizados; no propaga sobre ellos sin adopción explícita.

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| `cli-entrypoint` | `interactive-cli` | Ejecución del menú | en proceso |
| `cli-entrypoint` | `command-contract` | Comando y opciones | en proceso |
| `command-contract` | `command-router` | Comando validado | en proceso |
| `command-router` | `application-services` | Intención de negocio | en proceso |
| `command-contract` | Viewer | Envelope estructurado | stdout, JSON |
| `application-services` | `provider-catalog` | Consulta de proveedores y estado | en proceso |
| `application-services` | `artifact-inventory` | Consulta de artefactos instalados | en proceso |
| `application-services` | `artifact-format` | Separación y composición de artefactos | en proceso |
| `application-services` | `propagation-engine` | Cambio a aplicar y grupo afectado | en proceso |
| `propagation-engine` | `metadata-store` | Grupos, hashes y desvinculaciones | en proceso |
| `propagation-engine` | `text-store` | Escritura atómica y copias | en proceso |
| `project-tree` | `project-workspace` | Módulos y técnicos de `.sdd` | en proceso |
| `provider-configurator` | `resource-repository` | Plantillas y cabeceras por sistema operativo | en proceso |
| `artifact-descriptor` | `command-contract` | Descripción de artefacto | en proceso |
| `command-router` | `error-model` | Error de dominio o de uso | en proceso |

## Superficie lógica de comandos

| Comando | Propósito | Ámbito |
|---|---|---|
| `status` | Estado de proveedores y del proyecto. | global y proyecto |
| `tree` | Árbol de proveedores, agentes, skills, módulos y técnicos. | global y proyecto |
| `init` | Iniciar la estructura `.sdd` de un proyecto. | proyecto |
| `create-module` | Crear un módulo con sus técnicos y proveedores. | proyecto |
| `add-technician` | Añadir un técnico a un módulo existente. | proyecto |
| `describe` | Describir un artefacto y su grupo lógico. | global y proyecto |
| `save` | Guardar una instancia o propagar un cuerpo compartido. | global y proyecto |
| `configure-provider` | Instalar agentes y skills de la aplicación en un proveedor. | global |
| `create-artifact` | Crear un agente o una skill desde plantilla. | global y proyecto |
| `--help` / `--version` | Ayuda y versión sin efectos secundarios. | — |

Los nombres y opciones definitivos, así como la forma exacta de los datos, se formalizan en el contrato `cli-json`. Los comandos de escritura admiten un modo de simulación que devuelve los cambios previstos sin aplicarlos.

## Modelo de datos lógico

- **Proveedor:** entorno instalado de una herramienta. Relaciones: tiene artefactos y un estado calculado.
- **Ámbito:** global o de proyecto. Relaciones: acota proveedores, artefactos y grupos.
- **Artefacto:** fichero o directorio físico instalado. Relaciones: pertenece a un proveedor y a un ámbito; tiene formato, cabecera, cuerpo, hash y estado de vinculación.
- **Grupo lógico:** identidad compartida de varios artefactos. Relaciones: agrupa artefactos de varios proveedores y módulos; contiene el cuerpo compartido y la plantilla de módulo.
- **Entrada de catálogo:** definición esperada de un artefacto de la aplicación. Relaciones: determina nombre, tipo, formato y cabecera por proveedor y sistema operativo.
- **Módulo:** unidad registrada en `.sdd/team.md`. Relaciones: tiene técnicos y artefactos asociados.
- **Técnico:** combinación de rol y módulo. Relaciones: produce uno o varios artefactos por proveedor.
- **Skill:** artefacto de tipo directorio. Relaciones: contiene `SKILL.md` y ficheros de apoyo.
- **Conjunto de cambios:** resultado de un guardado o propagación. Relaciones: contiene artefactos afectados, diferencias y conflictos.
- **Metadatos de agrupación:** información persistida sobre grupos, hashes, cuerpo compartido y desvinculaciones. Relaciones: indexa artefactos y grupos.
- **Copia de seguridad:** contenido anterior de un artefacto sobrescrito. Relaciones: pertenece a un artefacto y a una operación.
- **Resultado de comando:** envoltorio común con éxito, datos y errores. Relaciones: lo produce todo comando.

## Decisiones

### DEC-001 Dos adaptadores sobre servicios compartidos

- **Decisión:** Mantener el menú interactivo y añadir un adaptador de comandos; ambos usan los mismos servicios de aplicación y dominio.
- **Alternativas descartadas:** sustituir el menú por comandos (rompe REQ-001) o duplicar la lógica en el adaptador JSON (riesgo de divergencia).
- **Cubre:** REQ-001, REQ-002.

### DEC-002 Contrato estructurado versionado como interfaz del viewer

- **Decisión:** El viewer consume el CLI mediante un envelope JSON con versión de esquema, comando, éxito, datos y errores.
- **Alternativas descartadas:** texto plano parseable (frágil) o un daemon con JSON-RPC (complejidad innecesaria en v1).
- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-023, NFR-006.

### DEC-003 Separación estricta de datos, logs y códigos de salida

- **Decisión:** La salida estándar contiene solo el documento estructurado; los logs van a la salida de error; los códigos de salida distinguen éxito, dominio, uso y fallo interno.
- **Alternativas descartadas:** mezclar mensajes humanos con datos o usar un único código de salida.
- **Cubre:** REQ-003, REQ-004, REQ-005, REQ-006, REQ-023.

### DEC-004 Modelo de grupo lógico y artefacto físico

- **Decisión:** La unidad editable es el artefacto físico, pero la propagación se calcula sobre un grupo lógico que comparte cuerpo y plantilla.
- **Alternativas descartadas:** editar solo ficheros completos (no permite propagar con módulos) o un repositorio de plantillas editable (descartado para v1).
- **Cubre:** REQ-011, REQ-012, REQ-019.

### DEC-005 Metadatos de agrupación en el home de la aplicación

- **Decisión:** Los grupos, hashes, cuerpos compartidos y desvinculaciones se guardan en el home de la aplicación, separados del proyecto.
- **Alternativas descartadas:** guardarlos en `.sdd` (ensucia el repositorio y mezcla responsabilidades) o junto a cada artefacto (difícil de descubrir y propenso a pérdida).
- **Cubre:** REQ-013, REQ-014, REQ-021, NFR-004.

### DEC-006 Propagación solo de artefactos existentes y mismo ámbito

- **Decisión:** La propagación actualiza únicamente artefactos ya instalados y nunca cruza entre global y proyecto.
- **Alternativas descartadas:** crear copias ausentes o propagar entre ámbitos; ambas amplían el alcance y el riesgo sin necesidad.
- **Cubre:** REQ-012, REQ-020.

### DEC-007 Cuerpo compartido con placeholder; cabecera no propagada

- **Decisión:** El cuerpo compartido de un especialista de proyecto conserva el placeholder de módulo; la cabecera es específica de cada proveedor y artefacto y no se propaga entre módulos.
- **Alternativas descartadas:** sustituir el nombre de módulo por heurísticas al propagar (frágil) o hacer merge a tres bandas (complejidad alta para v1).
- **Cubre:** REQ-011, REQ-012, REQ-015.

### DEC-008 Separación y composición textual por formato, sin reserializar cabeceras

- **Decisión:** Markdown se separa por el bloque de frontmatter y TOML por el marcador de instrucciones; la cabecera se conserva como texto y no se reconstruye.
- **Alternativas descartadas:** parsear y volver a serializar YAML/TOML (pierde comentarios y orden) o usar expresiones regulares sobre el fichero completo (frágil).
- **Cubre:** REQ-011, REQ-012, NFR-007.

### DEC-009 Detección de conflictos por hash

- **Decisión:** Cada artefacto registra el hash de la última composición conocida; un cambio externo se detecta comparando el hash actual con el registrado.
- **Alternativas descartadas:** fechas de modificación (poco fiables) o bloqueos de fichero (pesados para el caso de uso).
- **Cubre:** REQ-014, NFR-004.

### DEC-010 Escritura atómica y copias de seguridad

- **Decisión:** Toda escritura se hace sobre un temporal y se mueve de forma atómica; antes de sobrescribir se guarda una copia recuperable.
- **Alternativas descartadas:** escritura directa (riesgo de corrupción) o confiar en el sistema de ficheros del usuario.
- **Cubre:** REQ-012, NFR-004.

### DEC-011 Resolución de rutas multiplataforma

- **Decisión:** Un único resolvedor centraliza las rutas de proveedores y de estado para Windows y Linux, con canonicalización y rechazo de rutas no válidas.
- **Alternativas descartadas:** builds específicos por sistema o depender solo de variables de entorno sin valores por defecto.
- **Cubre:** REQ-007, NFR-001, NFR-005.

### DEC-012 Cabeceras por sistema operativo

- **Decisión:** El catálogo de artefactos define variantes de cabecera por sistema operativo; la instalación elige la variante del sistema actual.
- **Alternativas descartadas:** eliminar los hooks específicos de Windows (pierde funcionalidad) o usar una cabecera neutra que no aproveche las capacidades de cada sistema.
- **Cubre:** REQ-022, NFR-001.

### DEC-013 Artefactos legacy como personalizados, sin inferencia automática

- **Decisión:** Un artefacto sin metadatos de agrupación se marca como personalizado y no se propaga sobre él; la adopción del cuerpo compartido es explícita.
- **Alternativas descartadas:** inferir la plantilla sustituyendo el nombre de módulo (puede corromper personalizaciones).
- **Cubre:** REQ-021.

### DEC-014 Skills como directorio

- **Decisión:** Una skill se describe, edita y propaga como directorio completo, no solo como `SKILL.md`.
- **Alternativas descartadas:** tratar solo el fichero principal (pierde assets) o copiar sin control (pierde cambios locales).
- **Cubre:** REQ-020, REQ-019.

### DEC-015 Catálogo explícito de artefactos de la aplicación

- **Decisión:** El árbol y el inventario se basan en un catálogo de artefactos esperados, no en un escaneo indiscriminado de las carpetas del proveedor.
- **Alternativas descartadas:** mostrar todo lo instalado (incluye agentes y skills ajenos) o inferir pertenencia por nombre sin catálogo (ambigüedad).
- **Cubre:** REQ-008, REQ-009, REQ-010, REQ-018.

### DEC-016 Sin daemon persistente en v1

- **Decisión:** Cada comando es un proceso independiente; no hay servidor Java persistente.
- **Alternativas descartadas:** daemon JSON-RPC desde el inicio (complejidad operativa y de ciclo de vida) o un servidor local (abre puertos y requiere seguridad adicional).
- **Cubre:** REQ-002, NFR-002.

### DEC-017 Módulos derivados de `.sdd/team.md`

- **Decisión:** La lista de módulos y sus técnicos se deriva de `.sdd/team.md` y de los artefactos instalados, sin un registro paralelo.
- **Alternativas descartadas:** mantener un índice propio de módulos (duplica la fuente de verdad y se desincroniza).
- **Cubre:** REQ-010, REQ-015, REQ-016.

### DEC-018 Creación de agentes y skills por la misma pipeline de composición

- **Decisión:** Crear un agente o una skill usa el mismo catálogo, formato y escritura que la configuración y la propagación.
- **Alternativas descartadas:** copiar plantillas directamente (evita validación y metadatos) o generar contenido ad hoc en el adaptador JSON.
- **Cubre:** REQ-019, REQ-020.

## Riesgos

- **Latencia de arranque de la JVM:** cada comando paga el arranque del proceso. Se mitiga con el umbral de NFR-002 y, si no se cumple, con un daemon en v2.
- **Pérdida del almacén de metadatos:** si se borra el home de la aplicación, los artefactos siguen existiendo pero pasan a personalizados. Se mitiga tratándolos como legacy y no borrando contenido.
- **Divergencia de cabeceras por sistema operativo:** una cabecera de Windows en Linux puede ser inválida. Se mitiga con variantes por SO y pruebas de contrato por plataforma.
- **Adopción de artefactos legacy:** requiere que el usuario revise el cuerpo compartido; una adopción automática podría pisar personalizaciones. Se mitiga con el estado personalizado y confirmación explícita.
- **Cambios externos entre la comprobación y la escritura:** puede colarse una edición. Se mitiga comprobando de nuevo el hash antes de escribir y conservando copia.
- **Fugas en la composición TOML:** un cuerpo con delimitadores puede romper el fichero. Se mitiga con el adaptador de formato y pruebas de ida y vuelta.
- **Rutas y enlaces simbólicos:** Linux distingue mayúsculas y los enlaces pueden escapar del ámbito. Se mitiga con canonicalización, rechazo de rutas fuera de ámbito y pruebas con homes simulados.
- **Propagación de skills con assets:** una copia completa puede sobrescribir ficheros auxiliares. Se mitiga con hashes por fichero, copia de seguridad y confirmación.
- **`team.md` mal formado:** puede ocultar módulos. Se mitiga con lectura tolerante, avisos y árbol parcial.
- **Deriva del contrato con el viewer:** un cambio de esquema puede romper al consumidor. Se mitiga con versión de esquema y pruebas de contrato.

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | `cli-entrypoint`, `interactive-cli`, `application-services` | DEC-001 |
| REQ-002 | `command-contract`, `command-router`, `application-services` | DEC-001, DEC-016 |
| REQ-003 | `command-contract`, `error-model` | DEC-003 |
| REQ-004 | `command-contract` | DEC-002, DEC-003 |
| REQ-005 | `command-contract`, `error-model` | DEC-003 |
| REQ-006 | `command-contract` | DEC-003 |
| REQ-007 | `path-resolver`, `provider-catalog` | DEC-011 |
| REQ-008 | `provider-catalog`, `artifact-catalog`, `artifact-inventory` | DEC-015 |
| REQ-009 | `artifact-catalog`, `artifact-inventory` | DEC-015 |
| REQ-010 | `project-tree`, `artifact-inventory`, `project-workspace` | DEC-015, DEC-017 |
| REQ-011 | `artifact-descriptor`, `artifact-inventory`, `artifact-format`, `metadata-store` | DEC-004, DEC-008 |
| REQ-012 | `propagation-engine`, `artifact-format`, `text-store`, `metadata-store` | DEC-004, DEC-006, DEC-007, DEC-010 |
| REQ-013 | `metadata-store` | DEC-005 |
| REQ-014 | `propagation-engine`, `metadata-store` | DEC-009 |
| REQ-015 | `module-service`, `project-workspace`, `artifact-format`, `metadata-store` | DEC-007, DEC-017 |
| REQ-016 | `module-service`, `project-workspace` | DEC-017 |
| REQ-017 | `project-workspace`, `resource-repository` | — |
| REQ-018 | `provider-configurator`, `resource-repository`, `metadata-store` | DEC-012, DEC-015 |
| REQ-019 | `artifact-service`, `resource-repository`, `artifact-format`, `text-store` | DEC-018 |
| REQ-020 | `artifact-service`, `artifact-format`, `text-store`, `metadata-store` | DEC-014 |
| REQ-021 | `migration-service`, `metadata-store` | DEC-013 |
| REQ-022 | `resource-repository`, `artifact-catalog`, `provider-configurator` | DEC-012 |
| REQ-023 | `error-model`, `command-contract` | DEC-003 |
| REQ-024 | `command-router`, `application-services`, `text-store` | — |
| NFR-001 | `path-resolver`, `resource-repository`, `text-store` | DEC-011, DEC-012 |
| NFR-002 | `command-contract`, `artifact-inventory`, `provider-catalog` | DEC-016 |
| NFR-003 | `application-services`, `provider-catalog`, `artifact-inventory` | — |
| NFR-004 | `text-store`, `metadata-store` | DEC-010 |
| NFR-005 | `path-resolver`, `text-store` | DEC-011 |
| NFR-006 | `command-contract`, `interactive-cli` | DEC-002, DEC-003 |
| NFR-007 | `artifact-format`, `propagation-engine` | DEC-008 |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| `cli-json` | `docs/contracts/cli-json/contract.md` | Nuevo: envelope, comandos, opciones, errores y códigos de salida. |
| `artifact-metadata` | `docs/contracts/artifact-metadata/contract.md` | Nuevo: agrupación, cuerpo compartido, hashes y desvinculación. |
| `provider-layout` | `docs/contracts/provider-layout/contract.md` | Nuevo: rutas, formatos y cabeceras por proveedor y sistema operativo. |
| `sdd-team` | `docs/contracts/sdd-team/contract.md` | Existente: lectura de módulos y técnicos; sin cambio de formato. |

## Cambios sugeridos al mapa

- ~ `cli-entrypoint | cli/src/main/java/es/vargontoc/agentic/develop/cli/App.java | Arranque interactivo o por comandos | cli-ui, cli-json`
- ~ `cli-ui | cli/src/main/java/es/vargontoc/agentic/develop/cli/ui | Menú interactivo sin lógica de dominio | cli-application`
- ~ `cli-application | cli/src/main/java/es/vargontoc/agentic/develop/cli/application | Servicios de dominio sin impresión | cli-domain, cli-infrastructure, artifact-format, propagation, project-tree`
- ~ `cli-domain | cli/src/main/java/es/vargontoc/agentic/develop/cli/domain | Proveedores, artefactos, grupos y errores | —`
- ~ `cli-infrastructure | cli/src/main/java/es/vargontoc/agentic/develop/cli/infrastructure | Rutas, recursos, ficheros y metadatos | cli-domain`
- + `cli-json | cli/src/main/java/es/vargontoc/agentic/develop/cli/json | Contrato no interactivo y salida estructurada | cli-application`
- + `artifact-format | cli/src/main/java/es/vargontoc/agentic/develop/cli/format | Composición y separación de Markdown y TOML | cli-domain`
- + `propagation | cli/src/main/java/es/vargontoc/agentic/develop/cli/propagation | Propagación, conflictos y desvinculación | cli-domain, cli-infrastructure`
- + `project-tree | cli/src/main/java/es/vargontoc/agentic/develop/cli/tree | Árbol global y de proyecto | cli-application, cli-domain`
- + `cli-json | docs/contracts/cli-json/contract.md | cli-json -> viewer`
- + `artifact-metadata | docs/contracts/artifact-metadata/contract.md | cli-infrastructure -> cli-application`
- + `provider-layout | docs/contracts/provider-layout/contract.md | cli-infrastructure -> cli-application`
- ~ `sdd-team | docs/contracts/sdd-team/contract.md | cli-application -> .sdd/team.md`
- + `Convención: header_variants: windows, linux`
- + `Convención: artifact_formats: markdown, codex-toml`
- + `Convención: metadata_location: home de la aplicación`

## Estrategia de verificación

- Pruebas de ida y vuelta de separación y composición para Markdown y TOML, incluyendo cuerpos con delimitadores.
- Pruebas del resolvedor de rutas con homes simulados de Windows y Linux, incluyendo `XDG_CONFIG_HOME`.
- Pruebas del árbol con `.sdd/team.md` válido, ausente, mal formado y con módulos repetidos.
- Pruebas de propagación con varios módulos y proveedores, verificando nombres de módulo y cuerpos no afectados.
- Pruebas de conflicto, migración, artefactos desvinculados y adopción explícita.
- Pruebas de contrato con documentos de referencia del envelope y de los resultados de cada comando.
- Prueba de regresión del menú interactivo y de sus opciones actuales.
