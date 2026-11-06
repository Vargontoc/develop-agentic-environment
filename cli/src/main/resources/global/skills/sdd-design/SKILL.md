---
name: sdd-design
description: Guía para redactar specs/<feature>/design.md a nivel de arquitectura (componentes, flujos, interfaces lógicas, decisiones y trazabilidad a REQ-xxx). Úsala solo en la fase de diseño de SDD.
---

# Fase de diseño (SDD)

Produces `specs/<feature>/design.md`. Describe CÓMO se organiza la solución a nivel de arquitectura. Nada de clases, firmas, esquemas SQL, endpoints concretos ni código: eso lo detallan los agentes técnicos de cada proyecto.

## Antes de escribir

1. Comprueba `.sdd/project.md` con la skill `sdd-project-context`. Si falla, sigue sus instrucciones y termina.
2. Lee `specs/<feature>/requirements.md`. Debe existir y tener `Estado: aprobado`. Si no, no escribas nada y responde `PREGUNTAS` indicando ese bloqueo. Anota su `Revisión` (1 si no la tiene): la usarás en `Basado en:`.
3. Si `specs/<feature>/design.md` existe con `Estado: aprobado` y el router no pide rehacerlo de forma explícita, no lo toques y responde `SIN CAMBIOS`.
4. Lee `.sdd/project.md` y, si el router te da su ruta, `.sdd/architecture.md` (el mapa), entero.
5. Reconoce el sistema actual siguiendo "Lectura del mapa".
6. Decide si hay dudas bloqueantes: respuestas distintas darían diseños sustancialmente distintos, los requisitos se contradicen, o no puedes cubrir alguno. Si las hay, no escribas el fichero y responde `PREGUNTAS` (máximo 5, cada una con propuesta por defecto). Las dudas no bloqueantes van como supuestos.

## Lectura del mapa

La fuente de verdad sobre el sistema actual es `.sdd/architecture.md` y lo que referencia. No explores el repositorio por tu cuenta ni busques en él para descubrir módulos.

- Lee el mapa entero e identifica los módulos y contratos que la feature toca.
- Sigue las dependencias dentro del mapa: la columna `depende de` de cada módulo, hasta cubrir lo que la feature toca.
- Solo puedes abrir lo que el mapa referencia para esos módulos y contratos: las rutas de sus líneas y los ficheros de contrato. Úsalo para confirmar responsabilidades e interfaces, no para rediseñar. No abras nada que el mapa no señale.
- Si el mapa no existe, no tiene referencias para lo que la feature toca, o una ruta referenciada no existe o contradice lo que el mapa dice, no lo resuelvas por tu cuenta: avísalo SIEMPRE en `Avisos:` y diseña solo con los requisitos y `.sdd/project.md`, anotando lo asumido en "Supuestos sobre el sistema actual".
- Si los requisitos presuponen algo existente que no encuentras en el mapa y no puedes diseñar sin ello, responde `PREGUNTAS`.
- Propón en "Cambios sugeridos al mapa" lo que el diseño añade o cambia, y lo que el mapa tenga de desactualizado.

## Cómo diseñar

- Reutiliza componentes existentes antes de proponer nuevos. Cada componente tiene una responsabilidad clara; ponle un nombre corto en minúsculas con guiones, que sirve de identificador.
- **Trazabilidad**: cada `REQ-xxx` y `NFR-xxx` de requirements.md debe aparecer en la tabla de trazabilidad con los componentes que lo cubren. Si no puedes cubrir uno, dilo y explica por qué.
- **Decisiones**: `DEC-001`, `DEC-002`... solo para elecciones con alternativas reales; indica las descartadas y el motivo. Si revisas un fichero existente, conserva los IDs.
- **Interfaces**: descríbelas a nivel lógico ("A notifica a B cuando ocurre X"), con el protocolo si procede (`openapi`, `websocket`...). No definas rutas, campos ni esquemas.
- **Modelo de datos**: entidades y relaciones, sin atributos ni tipos.
- **Contratos afectados**: indica qué contratos de `docs/contracts/<protocolo>/` cambian o se crean y qué cambia, siempre con su protocolo. No los redactes: los formaliza la capa técnica del proyecto, y TaskList creará una tarea por cada fila de esa tabla.
- **Escenarios alternativos y de fallo**: `ALT-001`, `ALT-002`... Descríbelos solo cuando los requisitos, las restricciones o el diseño impliquen comportamientos alternativos significativos (operaciones que pueden fallar parcialmente, validaciones que interrumpen el flujo, dependencias externas que no responden, operaciones asíncronas, situaciones que requieren compensación o recuperación). En funcionalidades triviales basta `Ninguno.`. Un `ALT` es un escenario, no una decisión arquitectónica: indica qué comportamiento se espera y qué componente es responsable, no cómo se implementa (excepciones, transacciones, reintentos, librerías). Si revisas un fichero existente, conserva los IDs.
- **No inventes políticas de recuperación.** Si un escenario necesita una política que los requisitos no definen (reintentos, compensación, plazos...), pide la decisión con `PREGUNTAS` o señala el requisito que falta; no la elijas tú.
- No inventes funcionalidad que los requisitos no pidan.

## Formato del fichero

La tercera línea es siempre `Estado: borrador`. Cada vez que crees o modifiques el fichero, déjala en `borrador`: solo el router la pasa a `aprobado`, y solo tras la aprobación explícita del usuario.

Las líneas cuarta y quinta son `Revisión: N` y `Basado en: requirements@N` (la revisión de `requirements.md` con la que has trabajado). `Revisión` empieza en 1 y la incrementas solo cuando cambia el contenido sustantivo, incluido un cambio solo de `Basado en:`. Si el fichero existente no las tiene, `Revisión` se interpreta como 1 y las añades al modificarlo.

```markdown
# Diseño: <feature>

Estado: borrador
Revisión: 1
Basado en: requirements@1

## Resumen

<1 a 3 frases: el enfoque elegido.>

## Supuestos sobre el sistema actual

- <lo asumido sin haberlo visto en el código>

## Componentes

| Componente | Responsabilidad | Existente o nuevo |
|---|---|---|
| <nombre> | <una frase> | existente |

## Flujo principal

1. <paso entre componentes del escenario principal>

## Flujos alternativos y de fallo

### ALT-001 <nombre del escenario>

- **Condición:** <situación que lo desencadena>
- **Comportamiento:** <cómo responde el sistema>
- **Responsable:** <componente de la tabla "Componentes">
- **Resultado:** <estado observable>
- **Cubre:** REQ-001

## Interfaces entre componentes

| Origen | Destino | Qué se comunica | Protocolo |
|---|---|---|---|
| <a> | <b> | <descripción lógica> | <protocolo o ->|

## Modelo de datos lógico

- <Entidad>: <significado>. Relaciones: <con qué entidades>.

## Decisiones

### DEC-001 <título>

- **Decisión:** <qué se elige>
- **Alternativas descartadas:** <cuáles y por qué>
- **Cubre:** REQ-001

## Riesgos

- <riesgo y cómo se mitiga>

## Trazabilidad

| Requisito | Componentes | Decisiones |
|---|---|---|
| REQ-001 | <nombre> | DEC-001 |

## Contratos afectados

| Protocolo | Contrato | Cambio |
|---|---|---|
| <protocolo> | <ruta o nombre> | <qué cambia> |

## Cambios sugeridos al mapa

- <línea en el formato del mapa>
```

Si una sección no tiene contenido, déjala con "Ninguno.".

## Formato del mapa (`.sdd/architecture.md`)

Usa este patrón, una línea por elemento, para las líneas de "Cambios sugeridos al mapa", con el prefijo `+` (añadir), `~` (modificar) o `-` (quitar):

- Módulo: `<id> | <ruta> | <responsabilidad, máx. 12 palabras> | <depende de: ids separados por coma, o ->`
- Contrato: `<protocolo> | <ruta del contrato> | <origen> -> <destino>`
- Convención: `<clave>: <valor>`

No modifiques el mapa: lo mantienen los agentes técnicos del proyecto.

## Cómo respondes al router

## Verificación previa a LISTO

Antes de escribir y de responder `LISTO`, comprueba:

- Todos los `REQ-xxx` y `NFR-xxx` figuran en la tabla de trazabilidad.
- Las referencias a requisitos existen en `requirements.md`.
- Los componentes citados (en flujos, interfaces, decisiones y trazabilidad) existen en la tabla "Componentes".
- Los `DEC-xxx` y `ALT-xxx` son únicos y los de un fichero previo se conservan.
- Cada `ALT-xxx` tiene condición, comportamiento, responsable (un componente existente en el diseño), resultado y al menos un requisito en `Cubre`.
- Ningún `ALT-xxx` inventa una política de recuperación ausente en los requisitos.
- Los flujos no contradicen los criterios de aceptación.
- Cada contrato afectado tiene protocolo y propósito identificables.
- No introduces comportamientos fuera de los requisitos aprobados.

Si una comprobación falla y puedes corregirla, corrígela. Si no puedes, no devuelvas `LISTO`: responde `PREGUNTAS` si falta una decisión, o `BLOQUEADO` si no (por ejemplo, un requisito que el diseño no puede cubrir sin cambiar los requisitos).

## Cómo respondes al router

La primera línea es `PREGUNTAS`, `LISTO`, `SIN CAMBIOS` o `BLOQUEADO`.

- `PREGUNTAS`: lista numerada de dudas, cada una con su propuesta por defecto. No has escrito nada.
- `LISTO`: debajo, estas líneas:
  - `Fichero:` ruta del fichero.
  - `Resumen:` 1 a 3 frases.
  - `Verificación:` `correcta`.
  - `Avisos:` si procede (supuestos a revisar, sin mapa o sin referencias para lo que toca la feature, referencias rotas, requisitos no cubiertos, compatibilidad confirmada).
- `SIN CAMBIOS`: una frase explicando por qué.
- `BLOQUEADO`: hay una condición que impide completar el trabajo. No has escrito nada (haz las comprobaciones antes de escribir). Debajo: `Código:` (por ejemplo `DEPENDENCIA_INVALIDA`, `COBERTURA_INCOMPLETA`), `Motivo:`, `Acción requerida:` y `Fichero:`. Si falla una escritura, dilo expresamente y no afirmes que el fichero se ha conservado sin haberlo comprobado.