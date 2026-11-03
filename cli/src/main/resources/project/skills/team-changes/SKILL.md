---
name: team-changes
description: Guía para tratar fixes y mejoras técnicas sin pasar por el flujo SDD, documentos de cambio en docs/changes/, compuerta de clasificación, análisis del analista e implementación del desarrollador. Úsala solo en el analista y el desarrollador, en modo cambio.
---

# Cambios: fixes y mejoras técnicas

Un cambio es un **fix** (el código no cumple un comportamiento ya definido) o una **mejora técnica** (mejora el código sin cambiar lo que hace). No pasa por requisitos, diseño ni `tasks.md`: se analiza en un documento de cambio, el usuario lo aprueba y el desarrollador lo implementa.

## Compuerta: ¿es de verdad un cambio?

- Es un **fix** solo si el comportamiento esperado ya está definido (en `specs/`, en un contrato o es evidente) y el código no lo cumple.
- Es una **mejora** solo si no cambia lo que ve el usuario ni un contrato público: rendimiento, limpieza, refactor, deuda técnica, pruebas.

Si no se cumple, no escribas nada y responde `DERIVAR`:

- El comportamiento esperado no está definido, es discutible o hay que cambiar un requisito: pertenece al flujo SDD (`sdd-agent`).
- Cambia el comportamiento observable o un contrato público: pertenece al flujo SDD. Si nace de una idea de producto, pasa antes por el `product-manager`.

## Rutas y nombres

| Tipo | Carpeta | Fichero | Tareas |
|---|---|---|---|
| Fix | `docs/changes/fixes/<modulo>/` | `fix-NN-<slug>.md` | `FIX-001`... |
| Mejora | `docs/changes/improvements/<modulo>/` | `mejora-NN-<slug>.md` | `IMP-001`... |

- `NN`: dos dígitos, el siguiente número libre de esa carpeta (`01` si está vacía). Lista la carpeta según "Búsqueda de ficheros" de `sdd-project-context`.
- `<slug>`: minúsculas, sin acentos, con guiones, máximo 5 palabras.
- Un documento afecta a un solo módulo.
- Primera línea, el título; segunda, `Módulo: <modulo>`; tercera, `Estado: borrador` o `Estado: aprobado`. Solo el router lo pasa a aprobado.

## Plantilla de fix

```markdown
# Fix NN: <título corto>
Módulo: <modulo>
Estado: borrador
Afecta a: <feature o REQ si el usuario lo dijo; si no, "-">

## Problema
- Observado:
- Esperado:
- Cómo reproducirlo:

## Causa probable
Hipótesis a partir del código, con rutas y métodos. No se ha ejecutado nada.

## Cambio propuesto
Qué se cambia y por qué, a nivel técnico, sin código.

## Tareas
### [ ] FIX-001 Prueba que reproduce el fallo
- Qué comprueba:
- Debe fallar con:
### [ ] FIX-002 <corrección>
- Archivos:
- Hecho cuando: la prueba de FIX-001 pasa y no se rompe ninguna otra

## Riesgos y alcance
- Qué más podría verse afectado:
- Contratos o guía afectados (si los hay):
```

## Plantilla de mejora

```markdown
# Mejora NN: <título corto>
Módulo: <modulo>
Estado: borrador
Afecta a: -

## Motivación
## Situación actual
## Cambio propuesto
Sin cambiar el comportamiento observable ni los contratos públicos.

## Tareas
### [ ] IMP-001 <tarea>
- Archivos:
- Hecho cuando:

## Riesgos y alcance
```

## Si eres el analista

1. Aplica la comprobación previa de `team-conventions`.
2. Lee la descripción que te da el router y, del código de la carpeta de trabajo de tu módulo, solo lo necesario para localizar el punto afectado. No explores todo.
3. Aplica la compuerta. Si no es un cambio, `DERIVAR` con el motivo en una frase.
4. Si te falta información para reproducir o clasificar, responde `PREGUNTAS` (máximo 5, cada una con propuesta por defecto).
5. Lista la carpeta de destino. Si ya hay un documento del mismo tema, no lo dupliques: revísalo si está en borrador; si está aprobado, avisa.
6. `Afecta a` solo si el usuario indicó la feature o el REQ. No busques por tu cuenta en `specs/`.
7. Escribe el documento con la plantilla: como mucho una página y de 2 a 5 tareas. La causa es una hipótesis y se marca como tal.
8. Fix: la primera tarea es siempre la prueba que reproduce el fallo y la segunda, la corrección. Mejora: si el código afectado no tiene pruebas, la primera tarea son pruebas de caracterización.

## Si eres el desarrollador

1. Aplica la comprobación previa. Lee el documento de cambio entero: el router ya comprobó que está aprobado.
2. Implementa solo las tareas `[ ]` que te indican, en orden, dentro de tu carpeta de trabajo.
3. **Fix:** en FIX-001, escribe la prueba y ejecútala: debe fallar por el motivo descrito. Si pasa o falla por otro motivo, responde `PARCIAL` con lo observado y no toques más. FIX-001 se marca cuando la prueba existe, compila y falla por ese motivo. Las demás, cuando compila y pasan todas las pruebas.
4. **Mejora:** las pruebas existentes pasan antes y después y el comportamiento observable no cambia. Si necesitas cambiar una prueba existente para que pase, es un cambio de comportamiento: responde `PARCIAL`.
5. Marca `### [x]` en el documento de cambio y, si procede, completa rutas reales. No toques `tasks.md` ni el resto del documento.
6. Si el cambio afecta a un contrato o a la guía, díselo en `Avisos:` para que lo pida el usuario al documentador.
7. Si terminas con una prueba en rojo (porque quedó solo FIX-001), avísalo en `Avisos:`.
8. Respeta los límites y el formato de respuesta de `team-development`.

## Cómo respondes

La primera línea es `PREGUNTAS`, `LISTO`, `PARCIAL`, `SIN CAMBIOS` o, solo el analista, `DERIVAR` (ver `team-conventions`).