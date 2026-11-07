# Development Kit — Especificación de mejoras de la capa técnica v1.1

Propuesta de diseño · Sin implementación

He contrastado las diez mejoras con los archivos de `tecnicos.zip` y con la estructura de `develop-kit.zip`.

Mi propuesta es mantener la arquitectura actual:

- Tres routers: `analista-router`, `desarrollador-router` y `documentador-router`.
- Tres tipos de especialistas locales por módulo.
- Skills compartidas que establecen las convenciones de trabajo.
- SDD global como fuente de las especificaciones aprobadas.
- Usuario como responsable de aprobar planes, verificar integraciones y gestionar Git.

No añadiría más agentes, no introduciría una base de datos de estados y no convertiría cada tarea técnica en una nueva fase SDD.

Antes de entrar en cada archivo, hay tres reglas que considero fundamentales para esta evolución:

1. Una sola fuente de verdad por responsabilidad. Las convenciones compartidas no deberían repetirse con variantes contradictorias en cada agente.
2. La aprobación no implica vigencia permanente. Un sprint aprobado debe seguir siendo compatible con las especificaciones en las que se basa.
3. Un router coordina; el especialista ejecuta. Evitaría trasladar decisiones técnicas a los routers simplemente porque controlan el flujo.

## 1. Arquitectura objetivo

SDD global

Requirements → Design → TaskList

Especificaciones aprobadas y revisiones

Analista Router

Analista especialista

Plan técnico y dependencias

Desarrollador Router

Desarrollador especialista

Código, pruebas y resultados

Documentador Router

Documentador especialista

Contratos, guías, mapa y README

Validación y aprobación del usuario

Confirmación técnica, aceptación y operaciones Git

## 2. Resumen de archivos afectados

| Archivo                       | Modificación principal                                    |
| ----------------------------- | --------------------------------------------------------- |
| `team-conventions/SKILL.md`   | Contrato común de módulos, estados, permisos y validación |
| `team-conventions/team.md`    | Plantilla única de equipo y opciones                      |
| `analista-router.md`          | Dependencias, vigencia de sprints y modo multimódulo      |
| `analista.md`                 | Planificación fundamentada en código existente            |
| `team-sprints/SKILL.md`       | Revisiones SDD, lotes y criterios de planificación        |
| `desarrollador-router.md`     | Política Git, secuencia de tareas y validación            |
| `desarrollador.md`            | Adaptaciones internas, pruebas y resultados               |
| `team-development/SKILL.md`   | Definición de terminado y edición segura                  |
| `documentador-router.md`      | README, contratos y actualización del mapa                |
| `documentador.md`             | Cuatro modos de documentación                             |
| `team-documentation/SKILL.md` | Contract-first, mapa y coherencia documental              |
| `team-changes/SKILL.md`       | Coordinación de fixes y mejoras multimódulo               |
| `docs-readme/SKILL.md`        | Traslado desde SDD y adaptación a documentación real      |
| Generador del kit             | Provisioning, nombres y permisos de los especialistas     |

No veo necesario modificar la responsabilidad funcional de `sdd-requirements`, `sdd-design` ni `sdd-tasklist` por estas diez mejoras. Sí habrá que consumir correctamente los metadatos y convenciones acordados en la revisión SDD anterior.

# 3. Contrato único de `.sdd/team.md`

Archivos: `team-conventions/SKILL.md` y `team-conventions/team.md`

Esta es la primera modificación que aplicaría.

Actualmente la plantilla utiliza cuatro columnas (`layer`, `modulo`, identificador arquitectónico y rutas), mientras la skill describe tres. Los nombres de especialistas tampoco son completamente uniformes.

## 3.1. Formato obligatorio

```
# Equipo

## Módulos

- framework | core | CORE | framework/core
- framework | persistence | PERSISTENCE | framework/persistence
- framework | ai | AI | framework/ai

## Opciones

- sprints: si
- git-policy: balanced
- contract-strategy: implementation-first
- readme-owner: framework/core
```

Es un ejemplo de configuración, no una representación de los identificadores actuales de tu repositorio.

| Campo         | Regla                                                 |
| ------------- | ----------------------------------------------------- |
| `layer`       | Identificador lógico de la capa                       |
| `modulo`      | Identificador único dentro de la capa                 |
| `id del mapa` | Identificador arquitectónico o `-`                    |
| `rutas`       | Rutas de código autorizadas, relativas al repositorio |

Las rutas múltiples se separarían mediante espacios, siguiendo la plantilla actual.

### Validaciones obligatorias

- No pueden existir dos registros con la misma combinación `layer + modulo`.
- Si existe un ID arquitectónico distinto de `-`, debe ser único e identificable en el mapa.
- Una ruta debe pertenecer al repositorio.
- Los prefijos de rutas deben compararse por segmentos de directorio, no por coincidencia textual.
- Las rutas compartidas entre módulos deben declararse expresamente o provocar un bloqueo de escritura hasta resolver quién es responsable.
- Las opciones desconocidas deben notificarse; no deben interpretarse silenciosamente.

La validación de la existencia del mapa será condicional: un proyecto puede comenzar sin `.sdd/architecture.md`.

## 3.2. Nombres de especialistas

Establecería un único identificador lógico:

`<rol>-<layer>-<modulo>`

Ejemplos:

```
analista-framework-core
desarrollador-framework-core
documentador-framework-core
```

El identificador lógico será independiente del formato físico que utilice cada plataforma.

Por ejemplo, si Codex necesita una adaptación de nombres, esa transformación corresponde al generador y no a la lógica de los routers.

No permitiría que cada router construya los nombres de una forma distinta.

## 3.3. Fuente de verdad

`team-conventions/SKILL.md` debe contener la definición del contrato.

Los routers y especialistas solo deben indicar que consumen ese contrato, sin reproducir sus reglas completas.

Esto evitará que una modificación futura obligue a localizar cinco definiciones distintas de la misma convención.

# 4. Vigencia de sprints respecto a SDD

Archivos: `analista-router.md`, `team-sprints/SKILL.md`, `desarrollador-router.md`

Esta mejora debe alinearse con las revisiones que ya propusimos para Requirements, Design y TaskList.

## 4.1. Metadatos del sprint

Mantendría la tercera línea `Estado:`, porque forma parte del contrato actual.

Añadiría:

```
# Sprint 01: persistence-audit - core
Feature: persistence-audit
Estado: aprobado

## Origen SDD

- Requirements: 2
- Design: 3
- Tasks: 2

## Alcance

- Tareas: TASK-001, TASK-003
```

No modificaría el significado de `Estado: aprobado`.

El sprint puede estar aprobado y, aun así, requerir una revisión de compatibilidad debido a cambios posteriores en SDD.

## 4.2. Responsabilidad del analista-router

Antes de delegar:

1. Lee las revisiones actuales de la feature.
2. Lee las revisiones de origen de cada sprint aplicable.
3. Compara los valores.
4. Si coinciden, continúa.
5. Si no coinciden, solicita revisión de impacto al analista especialista.

No debe crear otro sprint automáticamente por encontrar una revisión diferente.

## 4.3. Responsabilidad del analista especialista

Cuando encuentra un sprint desactualizado:

- Identifica las tareas o decisiones que han cambiado.
- Revisa únicamente la parte afectada.
- Conserva las tareas y referencias que siguen siendo válidas.
- Propone una nueva aprobación si hay cambios sustantivos.
- No elimina trabajo previamente completado.

Si únicamente hay que confirmar compatibilidad, debe dejar evidencia de esa revisión y actualizar las referencias de origen mediante una operación autorizada, sin rediseñar el plan.

## 4.4. Responsabilidad del desarrollador-router

No podrá iniciar trabajo pendiente desde un sprint que no sea compatible con las revisiones SDD vigentes.

Pero tampoco debe bloquear retroactivamente las tareas ya completadas ni modificar su estado.

Si existe trabajo anterior potencialmente afectado, deberá comunicarlo al usuario y derivar su análisis al especialista correspondiente.

### Compatibilidad

Los sprints existentes sin metadatos de revisión se consideran de vigencia desconocida, no automáticamente inválidos.

En su siguiente utilización se revisará su compatibilidad y se incorporarán los metadatos.

# 5. Dependencias entre tareas, no únicamente entre módulos

Archivos: `analista-router.md`, `desarrollador-router.md`, `team-sprints/SKILL.md`

Este punto requiere una precisión importante: un sprint es un lote técnico de una feature para un módulo, no necesariamente una unidad que deba ejecutarse entera de una sola vez.

## 5.1. Algoritmo de planificación

Los routers deberán:

1. Leer las tareas pendientes de la feature.
2. Resolver el módulo responsable de cada tarea mediante `.sdd/team.md`.
3. Construir las dependencias a partir de `Depende de`.
4. Detectar ciclos y referencias inexistentes.
5. Obtener las tareas ejecutables cuyas dependencias están satisfechas.
6. Agrupar tareas ejecutables por módulo cuando sea posible.
7. Delegar secuencialmente al especialista correspondiente.
8. Recalcular las tareas disponibles cuando termine cada delegación.

Ejemplo:

TASK-001 · Core

Definir abstracción compartida

TASK-002 · Persistence

Integrar la abstracción en persistencia

TASK-003 · Core

Completar integración del módulo

Orden válido: Core → Persistence → Core. Agrupar permanentemente todas las tareas por módulo produciría un falso ciclo.

## 5.2. Cambios en `team-sprints`

El sprint conservará su formato por módulo, pero deberá distinguir:

- Tareas incluidas.
- Dependencias internas.
- Dependencias externas.
- Orden propuesto dentro del módulo.
- Condiciones que habilitan cada tarea.

No obligaría a crear un sprint distinto por cada visita al mismo módulo.

Un sprint puede ejecutarse en varios lotes.

## 5.3. Dependencias satisfechas

Una tarea no está habilitada simplemente porque la anterior figure `[x]`.

Debe cumplirse la condición de entrega que corresponda a esa dependencia.

Por ejemplo, si un módulo depende de un contrato aprobado, una implementación todavía no validada no debe considerarse suficiente.

No introduciría un motor complejo de dependencias: utilizaría los estados y evidencias ya existentes, aplicando la política de validación definida en la siguiente sección.

# 6. Definición de tarea terminada y validación

Archivos: `team-conventions/SKILL.md`, `team-development/SKILL.md`, `desarrollador.md`, `desarrollador-router.md`

Aquí corregiría una contradicción real: `team-conventions` exige que pasen todas las pruebas del módulo para marcar una tarea, mientras `team-development` solo permite al especialista ejecutar las pruebas específicas.

## 6.1. Mantener `[x]`, pero precisar su significado

Propongo:

| Nivel                            | Responsable   | Evidencia                                 |
| -------------------------------- | ------------- | ----------------------------------------- |
| Implementación de tarea          | Desarrollador | Código y pruebas específicas correctas    |
| Validación del módulo            | Usuario       | Suite completa satisfactoria              |
| Validación de integración/manual | Usuario       | Resultado de las comprobaciones acordadas |

`### [x] TASK-XXX` significará:

> La tarea ha sido implementada, su compilación y verificaciones específicas han terminado satisfactoriamente y se han satisfecho sus condiciones de finalización dentro del alcance del especialista.

No significará que toda la feature ha superado las pruebas de integración.

Una prueba fallida que sea necesaria para completar la tarea impide marcarla. En particular, la prueba deliberadamente roja de un fix de regresión se trata según el contrato especial de `team-changes`.

## 6.2. Persistencia de la validación

No añadiría un estado a cada tarea. Utilizaría una sección en cada sprint:

```
## Validación

- Suite del módulo: pendiente
- Integración: pendiente
- Validación manual: pendiente
```

Tras recibir los resultados, la operación responsable de registrar la validación actualizaría únicamente los campos correspondientes.

Los estados admitidos serían:

`pendiente | correcta | fallida | no aplica`

Una validación `correcta` debe conservar como mínimo el origen de la confirmación y el alcance comprobado. Si se modifica posteriormente el código afectado, no puede reutilizarse automáticamente esa validación como evidencia del nuevo estado.

### ¿Quién escribe este resultado?

El `desarrollador-router` seguirá sin tener permiso general de edición.

Por tanto, propongo que delegue el registro al especialista técnico mediante una operación expresamente autorizada sobre la sección de validación, o mediante una utilidad restringida.

No concedería permisos generales de escritura al router.

### Caso `sprints: no`

Aquí necesitamos una alternativa, porque no existe fichero de sprint.

Para la v1.1 utilizaría un registro mínimo de validación por feature y módulo, creado únicamente cuando se produzca una validación:

`docs/validation/<feature>/<layer>-<modulo>.md`

No lo convertiría en otro plan de trabajo ni exigiría aprobación adicional.

## 6.3. Cambios precisos en `team-development`

Sustituiría la regla de pruebas por:

> Ejecuta las pruebas específicas necesarias para validar las tareas asignadas. No ejecutes la suite completa del módulo salvo autorización expresa prevista en la configuración del proyecto. Registra los comandos, su resultado y las verificaciones que no has podido realizar.

Mantendría el límite de no ejecutar Git.

También permitiría que el especialista adapte clases o métodos propuestos por el analista cuando la modificación sea interna y no cambie contratos, requisitos ni decisiones arquitectónicas.

Las desviaciones relevantes deben comunicarse en `Avisos:`; las incompatibilidades con el diseño aprobado deben bloquearse y devolverse al analista.

# 7. Política Git configurable

Archivos: `desarrollador-router.md`, `team-conventions/SKILL.md` y plantilla `team.md`

Eliminaría la obligación de ejecutar siempre `git fetch` y bloquear por cualquier diferencia con el remoto.

## 7.1. Opciones

Propondría dos políticas:

| Política   | Comportamiento                                                      |
| ---------- | ------------------------------------------------------------------- |
| `balanced` | Permite trabajo local y solicita decisión ante riesgos relevantes   |
| `strict`   | Exige comprobación de sincronización y bloquea estados no conformes |

La política predeterminada sería `balanced`.

Ejemplo:

```
## Opciones

- git-policy: balanced
```

## 7.2. Comportamiento por situación

| Situación                  | Balanced             | Strict               |
| -------------------------- | -------------------- | -------------------- |
| Rama normal y limpia       | Continuar            | Continuar            |
| Sin upstream               | Continuar con aviso  | Bloquear             |
| Remoto inaccesible         | Avisar               | Bloquear             |
| Rama detrás del remoto     | Solicitar decisión   | Bloquear             |
| Rama divergida             | Solicitar decisión   | Bloquear             |
| Detached HEAD              | Bloquear por defecto | Bloquear             |
| Cambios locales sin commit | Comprobar conflictos | Comprobar conflictos |

Añadiría un detalle importante: la comprobación de Git debe ser informativa y de solo lectura por defecto.

Usaría `git status`, `git branch` y las consultas pertinentes de referencias, pero no ejecutaría `git fetch` salvo autorización expresa del usuario. Sin una actualización de referencias no se puede afirmar que la rama esté sincronizada con el remoto en tiempo real; debe indicarse que la evaluación utiliza la información local disponible.

El especialista no necesita saber cómo se ejecutan estas comprobaciones. El router recibe el resultado y decide si puede delegar.

## 7.3. Cambios locales existentes

No bloquearía por defecto un repositorio con modificaciones sin commit.

Pero antes de escribir, el desarrollador debe comprobar que no va a sobrescribir cambios ajenos a su encargo.

Si existe riesgo de colisión, devuelve `BLOQUEADO` con los ficheros afectados.

El usuario conserva completamente las operaciones de commit, merge, pull, rebase y push.

# 8. Integración del README en el documentador

Archivos: `documentador-router.md`, `documentador.md`, `team-documentation/SKILL.md`, `docs-readme/SKILL.md`

Esta es la decisión que cerramos anteriormente: el README deja de pertenecer al flujo SDD y pasa a la documentación técnica del proyecto.

## 8.1. Cuarto modo del documentador

El router admitirá:

```
contratos
mapa
guia
readme
```

En modo README:

1. Identifica el responsable en `.sdd/team.md`.
2. Comprueba que el especialista existe.
3. Delega la creación o actualización.
4. Transmite los cambios y advertencias.
5. Recomienda al usuario revisar el resultado antes del commit.

No necesita que exista una feature activa, ni `tasks.md` aprobado.

Esta excepción es importante: actualizar un README puede ser una operación de mantenimiento general del proyecto.

## 8.2. Selección del responsable

Introduciría en la plantilla:

```
- readme-owner: framework/core
```

La opción debe identificar una combinación `layer/modulo` existente.

Si no está configurada, el router solicitará seleccionar un responsable; no elegirá arbitrariamente el primer módulo.

El responsable podrá leer la documentación general y las evidencias técnicas necesarias para redactar el README. Su permiso adicional de escritura será exclusivamente sobre `README.md` o los recursos documentales expresamente autorizados para esa tarea.

## 8.3. Evolución de `docs-readme/SKILL.md`

La conservaría como skill reutilizable, con estas modificaciones:

| Regla actual                             | Nueva regla                                     |
| ---------------------------------------- | ----------------------------------------------- |
| Solo lectura de SDD                      | SDD, documentación y evidencias del repositorio |
| Un bloque fijo                           | Secciones adaptadas al tipo de proyecto         |
| Español obligatorio                      | Idioma configurado o existente                  |
| Todas las TASK `[x]` = feature terminada | Evidencia técnica y validación correspondiente  |
| Solo mantenimiento                       | Creación y mantenimiento                        |
| Invocación desde SDD                     | Invocación desde documentador                   |

### Protección del README

Conservaría la filosofía de los delimitadores.

No permitiría que una actualización ordinaria reescribiera libremente todo el README.

El modo creación puede proponer una estructura completa; el modo mantenimiento modifica únicamente las secciones autorizadas.

Si el contenido manual y el contenido generado se solapan, se solicita una decisión.

## 8.4. Evitar afirmaciones no verificadas

Un README público no debería presentar como operativa una capacidad que solo está especificada o que todavía no ha superado la validación necesaria.

El documentador distinguirá:

- Implementado y validado.
- Implementado, pendiente de validación.
- Planificado.

Por defecto, la sección de funcionalidades disponibles se basará en las capacidades implementadas cuya evidencia sea suficiente, sin confundirlas con las features simplemente aprobadas por SDD.

# 9. Contratos y actualización progresiva del mapa

Archivos: `team-documentation/SKILL.md`, `documentador-router.md`, `documentador.md`, `analista-router.md`

## 9.1. Contratos: dos estrategias

La estrategia se obtiene de `.sdd/team.md`:

```
- contract-strategy: implementation-first
```

Los dos valores serán:

`contract-first | implementation-first`

### Contract-first

El documentador redacta el contrato a partir del Design aprobado y las convenciones del proyecto.

El desarrollador implementa contra ese contrato.

Posteriormente, el documentador comprueba su coherencia con el código real.

### Implementation-first

El desarrollador implementa la interfaz conforme al Design y las tareas aprobadas.

Después, el documentador formaliza el contrato y verifica el comportamiento implementado.

En ambos casos, el documento del contrato debe indicar si describe un diseño pendiente de implementación o una interfaz ya contrastada con el código.

No marcaría un contrato como verificado antes de que exista evidencia de implementación.

## 9.2. Modificaciones concretas

En `analista-router.md`, eliminaría la instrucción que obliga a realizar siempre las tareas de contrato al final.

En `team-documentation/SKILL.md`, sustituiría la exigencia incondicional de implementación previa por la estrategia configurada.

En `documentador-router.md`, comprobaría las dependencias reales de cada tarea de contrato, en lugar de aplicar un orden fijo por tipo de tarea.

En `documentador.md`, permitiría los dos estados documentales del contrato, manteniendo el mismo especialista.

## 9.3. Actualización del mapa arquitectónico

Actualmente se requiere que todas las tareas de la feature estén terminadas antes de actualizar `.sdd/architecture.md`.

Cambiaría esa condición:

> Un componente podrá incorporarse al mapa cuando su existencia y responsabilidad estén verificadas, aunque existan otras tareas pendientes en la feature.

El documentador:

1. Lee los cambios sugeridos en Design.
2. Comprueba qué componentes existen realmente.
3. Actualiza únicamente las entradas del mapa que corresponden a su módulo.
4. No incorpora componentes meramente planificados.
5. Comunica las discrepancias con Design.

No permitiría modificar las entradas de otros módulos sin delegación al responsable correspondiente.

### Conflictos con SDD

Si el código contradice un requisito aprobado o una decisión arquitectónica, el documentador no debe corregir silenciosamente el documento para adaptarlo al código.

Debe devolver `BLOQUEADO` o `PREGUNTAS`, según requiera corregir una inconsistencia objetiva o tomar una decisión.

# 10. Modo cambios multimódulo

Archivos: `team-changes/SKILL.md`, `analista-router.md`, `desarrollador-router.md`, `analista.md`

Mantendría la principal decisión actual: un documento de fix o mejora técnica pertenece a un solo módulo.

No crearía un documento gigante que autorice a un especialista a modificar varios módulos.

## 10.1. Introducir un identificador de cambio compartido

Actualmente los documentos se identifican mediante números locales por carpeta.

Conservaría su numeración y añadiría un identificador de agrupación, únicamente cuando el cambio afecte a varios módulos.

Ejemplo:

```
# Fix 04: Corregir propagación del contexto

Módulo: core
Estado: aprobado
Cambio relacionado: CHG-2026-001

## Dependencias externas

- FIX-002 de persistence: requiere la interfaz corregida en core
```

Y en otro módulo:

```
# Fix 02: Adaptar propagación del contexto

Módulo: persistence
Estado: aprobado
Cambio relacionado: CHG-2026-001
```

El identificador `CHG-2026-001` es ilustrativo; deberá generarse sin colisionar con los existentes. No sustituye los IDs `FIX` o `IMP`.

## 10.2. Responsabilidad del analista-router

Cuando el usuario solicita un cambio:

1. Identifica los módulos posiblemente afectados.
2. Solicita a los analistas responsables que revisen su parte.
3. Cada analista clasifica el cambio como fix, mejora técnica o modificación que requiere SDD.
4. Recopila las dependencias entre los documentos.
5. Presenta el alcance completo para aprobación.

Si una parte del cambio requiere modificar requisitos, no la camufla como refactor para evitar SDD.

Deriva esa parte al flujo correspondiente y bloquea las operaciones técnicas que dependan de la nueva decisión.

## 10.3. Responsabilidad del desarrollador-router

El router debe ejecutar los cambios aprobados según sus dependencias, del mismo modo que las tareas de una feature.

Puede delegar varias veces al mismo especialista cuando sea necesario.

Una dependencia entre módulos no concede permiso para modificar el módulo ajeno.

## 10.4. Regla de terminación

Un cambio multimódulo estará terminado únicamente cuando:

- Todas las tareas de los documentos relacionados estén completas.
- Sus dependencias estén satisfechas.
- Se hayan realizado las validaciones aplicables.
- No queden discrepancias documentales bloqueantes.

No añadiría un estado global persistente al grupo: su situación puede derivarse de los documentos relacionados y de sus evidencias de validación.

# 11. Permisos y edición de archivos compartidos

Archivos: `team-conventions/SKILL.md`, los seis agentes técnicos y adaptadores del kit

Este apartado requiere separar dos aspectos: permisos declarados y controles efectivos.

## 11.1. Matriz objetivo

| Rol                        | Escritura autorizada                                                                        |
| -------------------------- | ------------------------------------------------------------------------------------------- |
| Analista especialista      | Sprints y documentos de cambio de su módulo                                                 |
| Desarrollador especialista | Código y pruebas de su módulo; estados y rutas autorizadas de tareas                        |
| Documentador especialista  | Contratos, guías, entradas propias del mapa, tareas documentales y README si es responsable |
| Analista-router            | Exclusivamente aprobación de planes                                                         |
| Desarrollador-router       | Sin escritura general; delega el registro de validación                                     |
| Documentador-router        | Sin escritura general                                                                       |

Mantendría los routers con los mínimos permisos posibles.

## 11.2. Problema especial: `tasks.md`

`tasks.md` es compartido por todos los módulos.

El desarrollador no debería tener autorización para editar libremente todo ese documento solo porque necesita marcar una tarea.

Propondría una operación de modificación acotada:

```
Feature: persistence-audit
Tarea: TASK-004
Estado esperado: [ ]
Estado nuevo: [x]
```

Antes de escribir debe comprobarse:

- Que la tarea existe una sola vez.
- Que pertenece al encargo y rol autorizado.
- Que no se ha modificado desde su lectura.
- Que el cambio afecta únicamente a los campos permitidos.

Si alguna condición falla, se devuelve `BLOQUEADO` y se conserva el contenido.

### Implementación futura

Para que sea una garantía real, no bastará con instrucciones en el prompt.

El kit necesitará una operación controlada o un mecanismo de validación de ediciones. Su implementación podrá ser una utilidad local pequeña, pero debe respetar las capacidades reales de cada plataforma.

No propondría una base de datos de bloqueos ni coordinación distribuida.

## 11.3. Otros archivos compartidos

Aplicaría el mismo criterio a:

- `.sdd/architecture.md`: únicamente entradas del módulo.
- `README.md`: únicamente secciones autorizadas.
- Sprints: únicamente campos de aprobación o validación permitidos.
- Documentos de cambio: únicamente tareas asignadas y sus metadatos autorizados.

La edición debe volver a leer el estado vigente antes de escribir para reducir el riesgo de sobrescribir modificaciones de otra sesión.

## 11.4. No confundir aislamiento con nombres de carpeta

Si dos módulos comparten una carpeta, el router no puede asumir que toda la carpeta pertenece exclusivamente al especialista seleccionado.

En ese caso, la responsabilidad debe resolverse mediante rutas más específicas o una declaración explícita de propiedad.

Si no puede resolverse, se bloquea la modificación conflictiva y se comunica al usuario.

# 12. Modelos según responsabilidad

Archivos: los seis agentes y las plantillas del generador

Aquí distinguiría entre la configuración lógica y el modelo concreto que ofrece cada plataforma.

No impondría los mismos identificadores de modelos a Claude, OpenCode y Codex.

## 12.1. Perfiles propuestos

| Agente                 | Perfil recomendado | Justificación                                 |
| ---------------------- | ------------------ | --------------------------------------------- |
| `analista-router`      | Ligero             | Resolver tareas, dependencias y delegación    |
| `analista`             | Alto razonamiento  | Examinar código y planificar                  |
| `desarrollador-router` | Ligero/medio       | Coordinar, comprobar y delegar                |
| `desarrollador`        | Alto razonamiento  | Implementación, depuración y pruebas          |
| `documentador-router`  | Ligero             | Selección de modo y responsable               |
| `documentador`         | Ligero/medio       | Redacción, revisión de contratos y coherencia |

Mi propuesta no es bajar automáticamente todos los routers a modelos pequeños.

Un router que resuelve dependencias complejas puede necesitar más capacidad de la que parece.

Por eso utilizaría perfiles lógicos:

`light | balanced | reasoning`

El generador traduciría cada perfil al modelo disponible en la herramienta seleccionada.

## 12.2. Configuración recomendada

Para la v1.1:

```
analista-router       balanced
analista              reasoning

desarrollador-router  balanced
desarrollador         reasoning

documentador-router   light
documentador          balanced
```

Este sería el punto de partida, no una configuración definitiva.

Mantendría una opción para sobrescribir el modelo por proyecto.

## 12.3. Qué mediría

Antes de optimizar costes, probaría las tres rutas con casos conocidos y observaría:

- Delegaciones al especialista correcto.
- Dependencias detectadas correctamente.
- Errores de interpretación del contexto.
- Preguntas innecesarias.
- Cambios fuera de alcance.
- Resultados de pruebas.
- Consumo de tokens y latencia, cuando la plataforma lo permita.

No seleccionaría modelos únicamente por el nombre del rol.

# 13. Contrato de respuesta unificado

Aprovecharía estas modificaciones para alinear los agentes técnicos con la evolución que ya propusimos para SDD.

En `team-conventions/SKILL.md` definiría:

| Respuesta     | Significado                                          |
| ------------- | ---------------------------------------------------- |
| `PREGUNTAS`   | Falta una decisión del usuario                       |
| `LISTO`       | Encargo completado y verificado dentro de su alcance |
| `PARCIAL`     | Parte del encargo se ha completado                   |
| `SIN CAMBIOS` | No hay modificaciones necesarias                     |
| `BLOQUEADO`   | Una condición objetiva impide continuar              |
| `DERIVAR`     | El analista detecta que el cambio pertenece a SDD    |

`DERIVAR` seguiría siendo exclusivo del analista en modo cambio.

Añadiría a `BLOQUEADO` una respuesta estructurada:

```
BLOQUEADO
Código: SPRINT_DESACTUALIZADO
Motivo: El sprint está basado en tasks@2,
pero la revisión actual es tasks@3.
Acción requerida: revisar impacto con analista-router.
Archivos afectados: ...
```

Y establecería una regla común: ningún especialista debe responder `LISTO` si no ha comprobado las condiciones de finalización que le corresponden.

# 14. Matriz definitiva de cambios por fichero

Esta sería la distribución que entregaría al desarrollador encargado de actualizar el kit.

| Fichero                                     | Cambios obligatorios                                                                                         |
| ------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `team-conventions/SKILL.md`                 | Registro uniforme de módulos, protocolo `BLOQUEADO`, definición de `[x]`, validaciones, permisos compartidos |
| `team-conventions/team.md`                  | Cuatro columnas, nombres lógicos y opciones de Git, contratos y README                                       |
| `analista-router.md`                        | Vigencia SDD, dependencias por tarea, clasificación multimódulo, retirada de contratos-al-final              |
| `analista.md`                               | Comprobación de código existente, compatibilidad de planes y declaración de dependencias                     |
| `team-sprints/SKILL.md`                     | Revisiones de origen, lotes ejecutables, compatibilidad, pruebas proporcionadas                              |
| `desarrollador-router.md`                   | Git configurable, selección de tareas pendientes, validación persistente, modo cambio independiente          |
| `desarrollador.md`                          | Permitir adaptaciones internas justificadas y registrar evidencias de pruebas                                |
| `team-development/SKILL.md`                 | Nueva definición de terminado, edición limitada, pruebas, resultados y reanudación                           |
| `documentador-router.md`                    | Modo README, responsable transversal, contratos según estrategia, mapa progresivo                            |
| `documentador.md`                           | Habilitar skill README, lectura contextual y coherencia entre código y diseño                                |
| `team-documentation/SKILL.md`               | Contract-first, implementation-first, mapa incremental, discrepancias y validaciones                         |
| `team-changes/SKILL.md`                     | Cambios relacionados, dependencias externas y coordinación multimódulo                                       |
| `docs-readme/SKILL.md`                      | Creación/mantenimiento, fuentes reales, secciones protegidas y presentación del repositorio                  |
| `create-project.ps1` y plantillas asociadas | Generación coherente de agentes, opciones, permisos, perfiles y skills                                       |

Hay dos detalles adicionales del ZIP que incluiría en la misma revisión: la referencia a `guide-conventions` necesita garantizar que esa skill se instala realmente, y deben unificarse las rutas de ejemplos utilizadas por desarrollador y documentador. Son correcciones de consistencia, no nuevas funcionalidades.

# 15. Pruebas de aceptación del Development Kit v1.1

No daría por terminada la evolución sin probar estos escenarios:

| Escenario                                      | Resultado esperado                                |
| ---------------------------------------------- | ------------------------------------------------- |
| Dos módulos con el mismo ID lógico             | Configuración rechazada                           |
| Especialista inexistente                       | Router identifica el agente faltante              |
| Tasks cambia después de aprobar un sprint      | Se exige revisión de impacto                      |
| Sprint antiguo sin metadatos                   | Compatibilidad evaluada antes de reutilizar       |
| Core → Persistence → Core                      | Ejecución válida                                  |
| Dependencias cíclicas entre TASK               | Bloqueo con referencias concretas                 |
| Pruebas específicas correctas                  | Tarea implementada marcada `[x]`                  |
| Suite completa pendiente                       | Validación del módulo permanece pendiente         |
| Proyecto local sin remoto                      | Trabajo permitido en política balanced            |
| Rama divergida                                 | Solicitud de decisión al usuario                  |
| README sin feature seleccionada                | Documentador puede actualizarlo                   |
| Contract-first                                 | Contrato precede a implementación                 |
| Implementación parcial de una feature          | Mapa incorpora solo componentes verificados       |
| Fix que afecta dos módulos                     | Documentos relacionados y dependencias respetadas |
| Dos especialistas editando `tasks.md`          | No se sobrescriben cambios ajenos                 |
| Especialista intenta escribir fuera del módulo | Operación rechazada                               |

# 16. Orden de implementación recomendado

No ejecutaría todas las modificaciones simultáneamente porque algunas dependen de decisiones anteriores.

Fase A — Contratos y coherencia

Unificar `team.md`, nombres, estados, permisos y protocolo de respuesta.

También establecer el contrato de revisiones SDD y la definición de validación.

Fase B — Análisis y desarrollo

Vigencia de sprints, dependencias entre tareas, Git configurable, cambios multimódulo y registro de validación.

Fase C — Documentación

README local, estrategias de contratos, mapas incrementales y coherencia documental.

Fase D — Adaptadores y pruebas

Permisos efectivos por plataforma, generación de agentes, perfiles de modelos y pruebas de regresión del kit.

## 17. Tres decisiones que requieren especial cuidado

Aunque las diez mejoras son compatibles con tu diseño actual, hay tres en las que evitaría añadir complejidad innecesaria.

Primera: el registro de validación. Lo he propuesto para conservar el estado entre sesiones, pero debe seguir siendo un mecanismo sencillo. Si se convierte en otro workflow completo de aprobaciones, estaríamos duplicando las responsabilidades que ya tiene el usuario.

Segunda: las dependencias. El planificador no necesita convertirse en un sistema de gestión de proyectos. Basta con resolver correctamente el grafo de tareas, sus módulos responsables y las condiciones para ejecutarlas.

Tercera: los permisos. Es la parte donde más distinguiría entre una regla escrita en una skill y una restricción realmente garantizada por la plataforma. No afirmaría que existe aislamiento técnico hasta comprobarlo.

## Conclusión

Mi propuesta mantiene intacta la filosofía original de tu Development Kit.

El analista sigue planificando, el desarrollador implementando y el documentador describiendo el producto real. Los routers siguen coordinando, sin convertirse en especialistas.

Las mejoras se concentran en continuidad entre sesiones, coherencia documental, ejecución controlada y reducción de bloqueos artificiales.

Para mí, el cambio más importante respecto a la implementación actual es este:

> El Development Kit debe poder interrumpir, revisar y retomar una feature sin perder el contexto de qué se aprobó, qué se implementó, qué se verificó y qué queda pendiente.

Conservaría esta propuesta como especificación de referencia para la v1.1, y antes de implementarla cerraría especialmente los contratos de `.sdd/team.md`, revisiones y validación, porque el resto depende de ellos.