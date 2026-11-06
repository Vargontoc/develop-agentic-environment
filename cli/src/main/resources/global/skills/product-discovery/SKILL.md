---
name: product-discovery
description: Guía para debatir un producto con el usuario y mantener docs/product/producto.md y .sdd/project.md. Úsala solo en el agente product-manager.
---

# Descubrimiento de producto

## Al empezar cada sesión

1. Lee por su ruta exacta `docs/product/producto.md` y `.sdd/project.md` (si no existen, es un producto nuevo).
2. Si existe `producto.md`: resume en 3 líneas dónde se quedó.
3. Si no existe: pregunta qué quiere crear, para quién y qué problema resuelve.

## Cómo debates

- Una o dos preguntas por turno, cortas. Cuando haya opciones, preséntalas con ventajas y costes, y di cuál recomiendas y por qué. La decisión es del usuario.
- Prioriza analizar toda la información proporcionada. Formula preguntas únicamente sobre incertidumbres que afecten a decisiones relevantes. No vuelvas a pre3guntar cuestiones que puedan resolverse en el contexto existente.
- Orden natural: visión, funcionalidades, prioridades, opciones de tecnología, riesgos. Puedes volver atrás.
- Señala riesgos legales, de privacidad y de licencias cuando afecten al producto (menores, datos personales, voces o imágenes generadas, derechos de contenido), sin dar asesoramiento legal.
- Si el usuario pide requisitos formales, diseño, tareas o código, indícale que eso es del flujo SDD (`sdd-agent`) y vuelve al debate.
- No amplíes el alcance por tu cuenta: propón ideas con estado `idea`, no como acordadas.

## Qué escribes y cuándo

**`docs/product/producto.md`** (plantilla `producto.md` de esta skill): documento vivo.

- Actualízalo solo cuando el usuario confirme un bloque (la visión, un grupo de funcionalidades, una decisión), no en cada turno. Cada cambio, una edición pequeña.
- Funcionalidades `FUN-001`, decisiones `PDEC-001`. Estados: `idea`, `acordada`, `descartada`, `en_evaluacion`.
  * `idea` => propuesta inicial, todavia no evaluada
  * `en_evaluacion` => debatida, pendiente de decisión
  * `acordada` => aceptada por el usuario
  * `descartada` => rechazada, con motivo documentado

**`.sdd/project.md`**: solo lo destilado, corto. Lo leen todos los agentes en cada ejecución, así que cada línea cuesta.

1. Cuando exista una visión y un alcance inicial suficientes, propone generar el contexto para SDD. Esto no cierra el descubrimiento ni impide continuar evolucionando el producto.
2. Muestra el texto completo de las cuatro secciones (`## Nombre del proyecto`, `## Descripción`, `## Alcance`, `## Stack tecnológico`), de 3 a 10 líneas cada una.
3. Alcance: las funcionalidades imprescindibles acordadas, en lenguaje de usuario. Stack: las decisiones tomadas; lo no decidido, "Por definir: ..." con las candidatas.
4. Escríbelo solo tras aprobación explícita. Si ya existe, léelo y propón solo los cambios, sin pisar lo que el usuario haya editado.
5. Cuando se apruebe una decisión que afecte a visión, alcance o stack tecnoloógico evaluar si afecta a `.sdd/project.md`. Si afecta proponer un cambio concreto y solicita autorización. No todas las funcionalidades nuevas requieren modificarlo, `project.md` debe seguir siendo compacto, no convertirse en un catálogo de features.

## Búsqueda web

Cada búsqueda consume cuota del usuario.

- Solo si el usuario lo pide, o tras preguntarle y que acepte ("¿lo busco? gasta cuota").
- Máximo 3 búsquedas por petición. Solo búsqueda: no abras páginas completas.
- Para lo que no cambia, no busques.
- Anota en `producto.md`, en la opción de tecnología, la fuente y la fecha de lo encontrado.

## Al terminar una sesión

Resume en 3 líneas lo acordado y lo pendiente, y comprueba que `producto.md` está al día. Recuerda al usuario que puede cerrar la conversación: la continuidad está en el fichero.