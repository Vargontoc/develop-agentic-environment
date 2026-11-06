# Propuesta de evolución del sistema SDD — Development Kit v1.1

Diseño propuesto · Sin implementación

He contrastado las siete mejoras con los archivos reales de `develop-kit.zip`, incluyendo las instrucciones de los agentes, las skills y `sdd-specs-only.bat`.

Mi propuesta es evolucionar el comportamiento existente, sin cambiar la arquitectura de cuatro agentes ni añadir subagentes. Mantendría el SDD global, la aprobación humana entre fases, las especificaciones Markdown y la separación respecto al equipo técnico del proyecto.

Hay tres principios que aplicaría a todas las modificaciones:

1. Compatibilidad: las features existentes deben seguir siendo utilizables.
2. Intervención mínima: cada mejora debe modificar únicamente las responsabilidades que le corresponden.
3. Consistencia: ningún documento puede considerarse listo para la siguiente fase si depende de información aprobada que ha cambiado.

## 1. Mapa de archivos afectados

| Archivo                            | Cambios                                                       |
| ---------------------------------- | ------------------------------------------------------------- |
| `agents/sdd-agent.md`              | Propagación, aprobaciones, validación, `BLOQUEADO`            |
| `agents/sdd-requirements.md`       | Contrato de respuesta, verificación y supuestos               |
| `agents/sdd-design.md`             | Contrato de respuesta, verificación y escenarios alternativos |
| `agents/sdd-tasklist.md`           | Contrato de respuesta, verificación y actualización de tareas |
| `skills/sdd-requirements/SKILL.md` | Supuestos y verificaciones                                    |
| `skills/sdd-design/SKILL.md`       | Escenarios, trazabilidad y verificaciones                     |
| `skills/sdd-tasklist/SKILL.md`     | Pruebas, contratos y verificaciones                           |
| `hooks/sdd-specs-only.bat`         | Restricciones de escritura por agente                         |

La skill `sdd-project-context` no necesita una modificación funcional para estas siete mejoras.

## 2. Mejora 1 — Propagación de cambios entre fases

Prioridad crítica

Problema actual: `sdd-agent` identifica la fase por el estado de los documentos, pero no comprueba si un documento aprobado sigue siendo compatible con las versiones de las que depende.

### Propuesta: revisiones explícitas y dependencias

Mantendría `Estado: borrador/aprobado` y añadiría metadatos ligeros.

En `requirements.md`:

```
Estado: aprobado
Revisión: 2
```

En `design.md`:

```
Estado: aprobado
Revisión: 3
Basado en: requirements@2
```

En `tasks.md`:

```
Estado: aprobado
Revisión: 1
Basado en: requirements@2, design@3
```

Estos campos permiten distinguir aprobación de vigencia sin introducir nuevos estados.

La revisión empieza en 1, aumenta únicamente cuando cambia el contenido sustantivo del documento y permanece inalterada cuando solo se aprueba un borrador.

El subagente que modifica el documento incrementa su revisión y actualiza las referencias de origen. El router sigue siendo el único responsable de cambiar `Estado:` tras la aprobación del usuario.

### Reglas nuevas de `sdd-agent`

- Antes de avanzar, comprueba los estados y las revisiones de las dependencias.
- Un diseño aprobado cuyo `requirements@N` no coincide con la revisión actual no está vigente.
- Un `tasks.md` aprobado cuyas referencias no coinciden tampoco está vigente.
- Una discrepancia impide iniciar una nueva implementación basada en esa especificación, aunque no impide revisar los documentos.
- Los cambios en Requirements requieren revisar Design y, si el impacto alcanza sus tareas, TaskList.
- Los cambios en Design requieren revisar TaskList.
- No se modifica automáticamente ningún documento posterior.

Ejemplo: cambio en Requirements

requirements.md · rev. 3

Aprobado

design.md · basado en rev. 2

Revisión necesaria

tasks.md · basado en design rev. 1

Impacto pendiente

Los ficheros siguen existiendo y conservan su contenido. El router controla qué puede utilizarse como entrada vigente.

### Compatibilidad con features existentes

Los documentos que no contienen `Revisión:` se interpretan inicialmente como revisión 1. Al volver a editarlos, se incorporan los metadatos.

Si una dependencia carece de `Basado en:`, el router no debe afirmar que está sincronizada: solicitará una revisión de compatibilidad antes de reutilizarla en un flujo nuevo.

Matiz importante: una modificación de Requirements no tiene por qué obligar a reescribir Design y TaskList. Si la revisión demuestra que siguen siendo compatibles, el subagente puede actualizar únicamente la referencia de dependencia, dejando constancia de la revisión efectuada. Eso también produce una nueva revisión del documento porque cambia su base de aprobación.

## 3. Mejora 2 — Verificación estructurada antes de `LISTO`

Prioridad alta

Propongo incorporar una regla común a los tres subagentes:

> Antes de responder `LISTO`, verifica la integridad, trazabilidad y coherencia del documento producido. No devuelvas `LISTO` si existe una incoherencia bloqueante.

No crearía una skill adicional. Incluiría una sección `## Verificación previa a LISTO` dentro de cada skill existente.

### Verificaciones de Requirements

- Los IDs `REQ-XXX` y `NFR-XXX` son únicos.
- Cada requisito expresa un comportamiento identificable.
- Cada requisito tiene al menos un criterio de aceptación verificable.
- Los NFR contienen umbrales medibles.
- No existen contradicciones evidentes entre requisitos.
- Los supuestos que afectan al comportamiento están identificados para revisión.

### Verificaciones de Design

- Todos los REQ y NFR figuran en la tabla de trazabilidad.
- Los componentes referenciados existen en el diseño.
- Las decisiones `DEC-XXX` tienen identificadores únicos.
- Todas las referencias a requisitos son válidas.
- Los flujos no contradicen los criterios de aceptación.
- Los contratos afectados tienen protocolo y propósito identificables.
- No se introducen comportamientos fuera de los requisitos aprobados.

### Verificaciones de TaskList

- Todos los requisitos están cubiertos.
- Las referencias a `REQ`, `NFR` y `DEC` existen.
- Las dependencias entre tareas no forman ciclos.
- Las tareas no dependen de sí mismas.
- Los IDs de tareas completadas se conservan.
- Las tareas nuevas tienen resultados verificables.
- Los contratos afectados tienen tareas asociadas.
- No se generan tareas sin respaldo en Requirements, Design o las reglas explícitas de `.sdd/task-types.md`.

### ¿Cómo informar al router?

Mantendría el contrato de respuesta simple:

```
LISTO
Fichero: specs/feature/design.md
Resumen: ...
Verificación: correcta
Avisos: ...
```

No incorporaría una tabla de validación dentro de cada documento. Sería documentación repetitiva que no aporta valor a los agentes técnicos.

La autoverificación semántica continúa siendo responsabilidad del modelo. Las comprobaciones deterministas, como IDs duplicados y ciclos, pueden convertirse posteriormente en validadores ejecutables, sin necesitar otro agente.

## 4. Mejora 3 — Restricciones reales de escritura

Prioridad crítica

Aquí modificaría `sdd-specs-only.bat` y las declaraciones de hooks de los cuatro agentes.

Actualmente, todos los agentes SDD llaman al hook con la política `specs`, que permite escritura dentro del directorio completo.

### Propuesta: políticas explícitas por rol

| Agente             | Escritura permitida                                          |
| ------------------ | ------------------------------------------------------------ |
| `sdd-agent`        | Exclusivamente la línea `Estado:` de los tres documentos SDD |
| `sdd-requirements` | `specs/<feature>/requirements.md`                            |
| `sdd-design`       | `specs/<feature>/design.md`                                  |
| `sdd-tasklist`     | `specs/<feature>/tasks.md`                                   |

Introduciría políticas identificadas por agente, en lugar de reutilizar el permiso genérico `specs`.

El router debe comunicar la feature activa y cada hook debe validar tanto la ruta como el rol que realiza la escritura.

### Dos aspectos técnicos importantes

Primero: el hook actual valida rutas, no contenido.

Por tanto, permitir que `sdd-agent` escriba en `requirements.md` no demuestra que únicamente cambie la línea `Estado:`. Esa restricción requeriría inspeccionar la operación propuesta y sus cambios, o trasladar la aprobación a una operación especializada y controlada.

Para v1.1 recomendaría una operación de aprobación limitada a la transición de estado, que compruebe previamente las dependencias y el contenido que va a modificar.

Segundo: hay que garantizar que el control no dependa de instrucciones del modelo.

El rol y la feature autorizados deben derivarse de un contexto fiable de ejecución, no de una ruta o parámetro que el propio agente pueda declarar arbitrariamente.

Además:

- Normalizar rutas antes de compararlas.
- Bloquear rutas fuera de la raíz del proyecto.
- Considerar enlaces simbólicos y rutas indirectas.
- Rechazar una escritura cuando no pueda determinarse la identidad autorizada.
- Validar por separado los mecanismos de permisos de cada herramienta compatible.

Conservaría los registros del hook, pero incorporaría un motivo específico para cada denegación.

Alcance: no cambiaría los permisos de Product Manager ni de los routers técnicos en esta iteración. Son responsabilidades distintas.

## 5. Mejora 4 — Escenarios alternativos en `sdd-design`

Prioridad media-alta

El diseño actual contempla un flujo principal, interfaces, decisiones y riesgos. Lo ampliaría para contemplar comportamientos alternativos relevantes, sin convertirlo en documentación de implementación.

### Cambios en `skills/sdd-design/SKILL.md`

Sustituiría la sección `## Flujo` por dos secciones:

```
## Flujo principal

1. <Componente A inicia operación>
2. <Componente B procesa petición>
3. <Componente A recibe resultado>

## Flujos alternativos y de fallo

### ALT-001 <Nombre del escenario>

- **Condición:** <situación que desencadena el flujo>
- **Comportamiento:** <cómo responde el sistema>
- **Responsable:** <componente responsable>
- **Resultado:** <estado observable>
- **Cubre:** REQ-003
```

`ALT-XXX` identificaría escenarios, no nuevas decisiones arquitectónicas. Mantendría identificadores estables durante las revisiones.

### Cuándo deben generarse

Solo cuando los requisitos, las restricciones o el diseño impliquen escenarios alternativos significativos.

Por ejemplo, en Framework-Vargontoc:

- Operaciones que pueden fallar parcialmente.
- Validaciones que interrumpen un flujo.
- Dependencias externas que no responden.
- Operaciones asíncronas.
- Situaciones que requieren compensación o recuperación.

No exigiría escenarios alternativos en funcionalidades triviales. En esos casos, `Ninguno.` sería válido.

### Delimitación de responsabilidades

El SDD debe determinar qué comportamiento arquitectónico se espera y qué componente asume la responsabilidad.

El analista técnico decide posteriormente cómo implementarlo: excepciones, transacciones, mecanismos de reintento, librerías, etc.

Además, `sdd-design` no debe inventar una política de recuperación ausente en Requirements. Si es necesaria para completar el diseño, debe solicitar una decisión o identificar el requisito que falta.

Cambio en `sdd-tasklist`: los escenarios `ALT-XXX` relevantes deben quedar reflejados en el alcance o las pruebas de las tareas correspondientes, sin tener que crear una tarea independiente por escenario.

## 6. Mejora 5 — Flexibilizar pruebas y contratos de TaskList

Prioridad media-alta

Aquí modificaría dos apartados de `skills/sdd-tasklist/SKILL.md`.

### A. Pruebas proporcionales a cada tarea

Sustituiría la obligación de generar un camino feliz y otro de error en todas las tareas por esta regla:

> Cada tarea debe definir las verificaciones necesarias para demostrar sus condiciones de finalización. Los escenarios positivos, negativos y de límite se incluirán cuando correspondan al comportamiento especificado. No se inventarán errores ni nuevos requisitos para completar una estructura de pruebas.

Formato propuesto:

```
- **Pruebas automáticas:**
  - <escenario y resultado esperado>
  - <escenario y resultado esperado>

- **Prueba manual:**
  1. <acción y resultado esperado>
```

Cuando no procedan pruebas automáticas o manuales, permitiría `No aplica: <justificación>`.

Mantendría como regla que cualquier comportamiento observable debe contar con alguna verificación apropiada. No se debe convertir la flexibilidad en una excusa para omitir pruebas.

### B. Contratos según la estrategia del proyecto

Actualmente la tarea de contrato depende obligatoriamente de la implementación de la interfaz.

Lo cambiaría por una estrategia configurable mediante `.sdd/task-types.md` o una convención explícita del mapa arquitectónico.

Estrategias admitidas

Contract-first

Definir contrato

Implementar

Implementation-first

Implementar

Formalizar contrato

Propondría una convención opcional:

```
## Convenciones

- estrategia-contratos: contract-first
```

Si no existe, mantendría `implementation-first` como comportamiento heredado, evitando alterar las dependencias de las features actuales.

Si se especifican estrategias incompatibles en varios documentos de contexto, `sdd-tasklist` devolvería `BLOQUEADO` hasta resolver la contradicción.

También respetaría las dependencias adicionales definidas en `.sdd/task-types.md`, siempre que no generen ciclos.

## 7. Mejora 6 — Nuevo estado de respuesta `BLOQUEADO`

Prioridad media-alta

En este punto conviene distinguir entre el estado del documento y el resultado de ejecución del subagente.

No añadiría `bloqueado` a `Estado: borrador/aprobado`. Lo incorporaría exclusivamente al protocolo de respuesta de los agentes.

### Contrato común

| Respuesta     | Significado                                          | Escritura                  |
| ------------- | ---------------------------------------------------- | -------------------------- |
| `PREGUNTAS`   | Falta una decisión funcional o arquitectónica        | No                         |
| `LISTO`       | Documento generado y verificado                      | Sí                         |
| `SIN CAMBIOS` | No hay cambios necesarios o autorizados              | No                         |
| `BLOQUEADO`   | Existe una condición que impide completar el trabajo | No se considera completado |

Ejemplo:

```
BLOQUEADO
Código: DEPENDENCIA_INVALIDA
Motivo: design.md referencia REQ-009,
que no existe en requirements.md.
Acción requerida: revisar la trazabilidad
de design.md.
Fichero: specs/example/design.md
```

### Comportamiento del router

Cuando recibe `BLOQUEADO`:

1. Detiene la transición entre fases.
2. Comunica al usuario la causa concreta.
3. Distingue qué componente debe resolverla.
4. No aprueba ningún documento.
5. No delega repetidamente la misma operación sin que cambie la condición bloqueante.

La regla debería aparecer tanto en `sdd-agent.md` como en las tres skills.

### Una precisión sobre las escrituras

Actualmente `PREGUNTAS` implica que el subagente no ha escrito nada.

Mantendría esa garantía.

Para `BLOQUEADO`, intentaría aplicar la misma regla: ejecutar las comprobaciones previas antes de modificar el fichero.

Pero si falla una operación durante la escritura, el agente debe comunicarlo expresamente y no afirmar que se ha preservado el fichero sin comprobarlo.

No asumiría que todas las herramientas soportan escrituras transaccionales.

## 8. Mejora 7 — Formalizar la revisión de supuestos

Prioridad media

Ya existen supuestos en Requirements y Design. Lo que falta es definir cuándo dejan de ser una hipótesis y pasan a ser una decisión aceptada.

No convertiría todos los supuestos en requisitos formales. Introduciría una clasificación mínima.

### Dos categorías

| Categoría  | Ejemplo                                                         | Tratamiento                   |
| ---------- | --------------------------------------------------------------- | ----------------------------- |
| Contextual | Se presupone la existencia de un componente descrito en el mapa | Puede permanecer documentado  |
| Decisional | Se presupone que una operación se reintentará automáticamente   | Requiere validación explícita |

Formato propuesto en Requirements:

```
## Supuestos

### SUP-001 <Título>

- **Tipo:** contextual | decisional
- **Descripción:** <supuesto>
- **Impacto:** <qué depende de él>
- **Estado:** pendiente | validado | descartado
```

### Reglas

- Los supuestos no deben contradecir requisitos aprobados.
- Los supuestos decisionales pendientes que alteran el comportamiento no pueden pasar inadvertidos durante la aprobación.
- Si afectan significativamente al diseño o a los criterios de aceptación, deben convertirse en una decisión explícita o en una pregunta bloqueante.
- Los supuestos contextuales pueden permanecer si están identificados y su incertidumbre no compromete el diseño.
- Un supuesto descartado conserva su identificador y debe revisarse cualquier contenido derivado de él.

No crearía un registro global nuevo. Cada feature conserva sus supuestos dentro de sus documentos.

### Cambio en la aprobación de `sdd-agent`

Antes de solicitar aprobación, el router debe mostrar los supuestos decisionales pendientes.

Por ejemplo:

> El documento está preparado para revisión. Contiene dos supuestos: uno contextual y otro sobre la política de reintentos, que requiere tu decisión antes de aprobar.

Esto evita que un simple «aprobado» confirme inadvertidamente una decisión importante escondida entre varias secciones.

## 9. Contrato final de los cuatro agentes

Con las mejoras, el sistema quedaría así:

| Agente             | Responsabilidad nueva                                                                               |
| ------------------ | --------------------------------------------------------------------------------------------------- |
| `sdd-agent`        | Comprobar vigencia, gestionar revisiones, aprobar, interpretar bloqueos                             |
| `sdd-requirements` | Verificar requisitos, clasificar supuestos, conservar IDs                                           |
| `sdd-design`       | Verificar trazabilidad, contemplar escenarios alternativos, revisar compatibilidad                  |
| `sdd-tasklist`     | Verificar cobertura y dependencias, aplicar estrategia de contratos, conservar tareas implementadas |

No cambiaría sus modelos por estas mejoras. Primero evaluaría el comportamiento del sistema actualizado y solo después decidiría si `sdd-tasklist`, actualmente configurado con Haiku, necesita un modelo distinto para verificaciones más complejas.

## 10. Criterios de aceptación de la evolución

Para considerar cerrada la mejora, propondría estas pruebas funcionales del propio kit:

| Caso                                        | Resultado esperado                                                      |
| ------------------------------------------- | ----------------------------------------------------------------------- |
| Nueva feature desde cero                    | El flujo actual funciona sin pasos adicionales innecesarios             |
| Cambio en Requirements aprobado             | Se detectan las fases posteriores potencialmente desactualizadas        |
| Design compatible tras cambio               | Se permite confirmar compatibilidad sin rediseñar contenido innecesario |
| ID duplicado en Requirements                | No devuelve `LISTO`                                                     |
| Requisito sin cobertura                     | Design o TaskList no se consideran completos                            |
| Escenario de fallo relevante                | Design identifica comportamiento y responsable                          |
| Tarea sin caso negativo aplicable           | No inventa una prueba artificial                                        |
| Proyecto contract-first                     | El contrato precede a la implementación                                 |
| Agente Design intenta editar Requirements   | La escritura se rechaza                                                 |
| Inconsistencia documental irresoluble       | Devuelve `BLOQUEADO` con causa y acción                                 |
| Supuesto decisional pendiente               | La aprobación exige resolverlo explícitamente                           |
| Tarea `[x]` afectada por un nuevo requisito | Se conserva la tarea original y se propone trabajo adicional            |

## 11. Orden recomendado de desarrollo

No implementaría las siete modificaciones simultáneamente. Las agruparía en tres iteraciones:

Iteración A — Consistencia del workflow

Revisiones y propagación de cambios, `BLOQUEADO` y validación previa a `LISTO`.

Son modificaciones relacionadas: primero hay que garantizar que el sistema sepa cuándo puede avanzar.

Iteración B — Calidad documental

Escenarios alternativos, pruebas proporcionadas, estrategias de contrato y gestión de supuestos.

Aquí mejoramos la utilidad de los documentos sin cambiar su propósito.

Iteración C — Restricciones y pruebas de integración

Permisos efectivos, controles de escritura y validación del funcionamiento completo con las herramientas soportadas.

Durante las iteraciones anteriores mantendría las restricciones actuales y evitaría considerarlas garantías de aislamiento hasta reforzarlas.

## 12. Una decisión adicional que dejaría registrada

Hay un caso que merece especial atención: modificar una especificación cuando su `tasks.md` ya contiene tareas implementadas.

Tu sistema actual establece correctamente que una tarea `[x]` no se modifica ni elimina. Mantendría esa regla.

Sin embargo, si el nuevo diseño invalida el comportamiento de una tarea ya implementada, no basta con crear una tarea nueva y continuar. El router debería advertir explícitamente que existe trabajo implementado potencialmente incompatible.

No haría que SDD inspeccionara Git ni el código; esa responsabilidad seguiría siendo del analista técnico. El SDD solo debe identificar el impacto documental y comunicarlo al equipo encargado de la implementación.

## Veredicto

Considero que estas siete mejoras son apropiadas para una v1.1 del Development Kit.

La más importante es la propagación de cambios. La más delicada técnicamente es el control efectivo de escritura. Y la que probablemente más notarás en la calidad de las especificaciones será el refuerzo de `sdd-design`.

Solo pondría una condición antes de dar el diseño por cerrado: que el mecanismo de revisiones y supuestos no haga más pesado el trabajo cotidiano. En features sencillas, el agente debería seguir siendo tan ágil como ahora.

El resultado buscado no es un proceso SDD más burocrático, sino uno que pueda evolucionar durante meses de desarrollo manteniendo coherencia, trazabilidad y responsabilidades bien delimitadas.