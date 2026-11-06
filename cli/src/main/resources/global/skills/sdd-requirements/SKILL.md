---
name: sdd-requirements
description: Guía para redactar specs/<feature>/requirements.md en formato EARS, con IDs trazables y criterios de aceptación verificables. Úsala solo en la fase de requisitos de SDD.
---

# Fase de requisitos (SDD)

Produces `specs/<feature>/requirements.md`. Describe QUÉ debe hacer el sistema y para quién, nunca CÓMO: nada de tecnologías de implementación, clases, tablas, endpoints ni código. Eso es la fase de diseño. El stack de `.sdd/project.md` solo sirve para no contradecirlo.

## Antes de escribir

1. Comprueba `.sdd/project.md` con la skill `sdd-project-context`. Si falla, sigue sus instrucciones y termina.
2. Lee `.sdd/project.md` y los ficheros que el router te indique. No explores el repositorio por tu cuenta.
3. Si `specs/<feature>/requirements.md` existe con `Estado: aprobado` y el router no pide rehacerlo de forma explícita, no lo toques y responde `SIN CAMBIOS`.
4. Decide si hay dudas bloqueantes. Una duda es bloqueante si respuestas distintas darían requisitos sustancialmente distintos, o si la petición queda fuera del `Alcance` de `.sdd/project.md`. Si hay alguna, no escribas el fichero y responde `PREGUNTAS` (máximo 5). Cada pregunta debe ser concreta y llevar una propuesta por defecto, para que el usuario pueda aceptarla con un "ok".
5. Las dudas no bloqueantes no detienen el trabajo: recógelas como supuestos en el fichero, siguiendo "Supuestos".

## Cómo redactar

- **Un requisito, un comportamiento verificable.** Usa "deberá". Evita "y/o", "etc." y adjetivos sin métrica ("rápido", "fácil", "seguro").
- **IDs**: `REQ-001`, `REQ-002`... para funcionales y `NFR-001`... para no funcionales. Si revisas un fichero existente, conserva los IDs; los nuevos continúan la numeración y un ID retirado no se reutiliza. El diseño y las tareas enlazarán a estos IDs.
- **Patrones EARS** (elige el que corresponda):
  - Siempre activo: "El <sistema> deberá <respuesta>."
  - Por evento: "Cuando <evento>, el <sistema> deberá <respuesta>."
  - Por estado: "Mientras <estado>, el <sistema> deberá <respuesta>."
  - Condición no deseada: "Si <condición>, entonces el <sistema> deberá <respuesta>."
  - Opcional: "Donde <característica incluida>, el <sistema> deberá <respuesta>."
- **Criterios de aceptación**: al menos uno por requisito, con la forma "Dado <contexto>, cuando <acción>, entonces <resultado observable>". Deben poder comprobarse sin leer código.
- **No funcionales**: solo con umbral medible. Si el usuario no dio umbral, es una duda (bloqueante si condiciona el diseño, supuesto si no).
- **No inventes funcionalidad** que el usuario no pidió. Lo que descartes expresamente va en "Fuera de alcance".

## Supuestos

Cada supuesto lleva ID `SUP-001`, `SUP-002`... (estable: si revisas un fichero existente, conserva los IDs y no reutilices uno retirado) y se clasifica en:

- **Contextual:** presupone algo del entorno que no cambia el comportamiento (por ejemplo, que existe un componente descrito en el mapa). Puede permanecer documentado.
- **Decisional:** presupone una decisión que altera el comportamiento (por ejemplo, que una operación se reintenta automáticamente). Requiere validación explícita del usuario.

Reglas:

- Un supuesto no puede contradecir un requisito.
- Estado `pendiente`, `validado` o `descartado`. Los supuestos nuevos nacen `pendiente`; solo pasan a `validado` o `descartado` por decisión del usuario, que te llega a través del router.
- Si un supuesto decisional condiciona significativamente el diseño o los criterios de aceptación, no lo dejes como supuesto: es una duda bloqueante (`PREGUNTAS`) o un requisito.
- Un supuesto `descartado` conserva su ID. Revisa y corrige cualquier requisito derivado de él.
- No crees ningún registro global: los supuestos viven en el fichero de su feature.

## Formato del fichero

La tercera línea es siempre `Estado: borrador`. Cada vez que crees o modifiques el fichero, déjala en `borrador`: solo el router la pasa a `aprobado`, y solo tras la aprobación explícita del usuario.

La cuarta línea es `Revisión: N`. Empieza en 1 al crear el fichero y la incrementas solo cuando cambia su contenido sustantivo. Si el fichero existente no la tiene, se interpreta como 1 y la añades al modificarlo.

```markdown
# Requisitos: <feature>

Estado: borrador
Revisión: 1

## Contexto

<1 a 3 frases: qué se quiere conseguir y para quién.>

## Fuera de alcance

- <lo que no se hará en esta feature>

## Requisitos funcionales

### REQ-001 <título corto>

- **Requisito:** Cuando <evento>, el <sistema> deberá <respuesta>.
- **Criterios de aceptación:**
  - Dado <contexto>, cuando <acción>, entonces <resultado observable>.

## Requisitos no funcionales

### NFR-001 <título corto>

- **Requisito:** El <sistema> deberá <respuesta medible>.
- **Criterios de aceptación:**
  - Dado <contexto>, cuando <acción>, entonces <resultado medible>.

## Supuestos

### SUP-001 <título corto>

- **Tipo:** contextual | decisional
- **Descripción:** <supuesto>
- **Impacto:** <qué depende de él>
- **Estado:** pendiente | validado | descartado

## Preguntas abiertas

- <duda no bloqueante pendiente>
```

Si una sección no tiene contenido, déjala con "Ninguno." en lugar de borrarla.

## Cómo respondes al router

## Verificación previa a LISTO

Antes de escribir y de responder `LISTO`, comprueba:

- Los IDs `REQ-xxx` y `NFR-xxx` son únicos y los de un fichero previo se conservan.
- Cada requisito expresa un único comportamiento identificable.
- Cada requisito tiene al menos un criterio de aceptación verificable.
- Cada NFR contiene un umbral medible.
- No hay contradicciones evidentes entre requisitos.
- Los `SUP-xxx` son únicos, tienen tipo, impacto y estado, y no contradicen ningún requisito.
- Todo supuesto que afecta al comportamiento está clasificado como decisional.

Si una comprobación falla y puedes corregirla, corrígela. Si no puedes (por ejemplo, una contradicción que solo el usuario puede resolver), no devuelvas `LISTO`: responde `PREGUNTAS` si falta una decisión, o `BLOQUEADO` si no.

## Cómo respondes al router

La primera línea es `PREGUNTAS`, `LISTO`, `SIN CAMBIOS` o `BLOQUEADO`.

- `PREGUNTAS`: debajo, la lista numerada de dudas, cada una con su propuesta por defecto. No has escrito nada.
- `LISTO`: debajo, estas líneas:
  - `Fichero:` ruta del fichero.
  - `Resumen:` 1 a 3 frases.
  - `Verificación:` `correcta`.
  - `Supuestos decisionales pendientes:` lista de `SUP-xxx` con su título, o `ninguno`.
  - `Avisos:` si procede.
- `SIN CAMBIOS`: debajo, una frase explicando por qué.
- `BLOQUEADO`: hay una condición que impide completar el trabajo. No has escrito nada (haz las comprobaciones antes de escribir). Debajo: `Código:` (por ejemplo `ID_DUPLICADO`, `CONTRADICCION`), `Motivo:`, `Acción requerida:` y `Fichero:`. Si falla una escritura, dilo expresamente y no afirmes que el fichero se ha conservado sin haberlo comprobado.