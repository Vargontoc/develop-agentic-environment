---
name: team-conventions
description: Convenciones de la capa técnica del proyecto (routers y especialistas de analista, desarrollador, tester y documentador): registro .sdd/team.md, nombres, estados y protocolo de respuesta. Úsala antes de actuar en cualquier agente técnico.
---

# Convenciones de la capa técnica

La capa SDD (specs) define QUÉ y CÓMO a alto nivel. La capa técnica lo ejecuta por capa y módulo con especialistas propios del proyecto, coordinados por tres routers: `analista-router`, `desarrollador-router`, y `documentador-router`.

## Comprobación previa

1. `.sdd/project.md`: aplica la comprobación de la skill `sdd-project-context`.
2. `.sdd/team.md`: existe, no está vacío y tiene al menos una línea de módulo bajo `## Módulos`.

Si algo falla, no hagas nada más: no delegues, no crees ni modifiques ficheros. Responde con qué falla y una sugerencia mínima: crear `.sdd/team.md` desde la plantilla `team.md`.

Para comprobar un fichero de un directorio oculto (`.sdd/...`), usa Read con la ruta exacta: si Read dice que no existe, no existe. No uses Glob ni Grep para ello.

## Registro `.sdd/team.md`

Una línea por módulo bajo `## Módulos`, siempre con este patrón:

`-  <modulo> | <id del mapa o -> | <package | ficheros>`

- `modulo`: conjunto de funcionalidades que tiene el framework
- `id del mapa`: el `id` del módulo en `.sdd/architecture.md`; relaciona el `Componente` de una tarea con su módulo.
- `package | ficheros`: paquete java donde se desarrolla el módulo (main, test) y ficheros que tengas que tengas que tocas como pom.xml en caso de necesitar dependencias

Opcional, al final del fichero: `## Opciones` con `- sprints: si` o `- sprints: no` (por defecto `si`). Con `no`, no interviene el analista y el desarrollador trabaja desde `tasks.md` y `design.md`.

## Especialistas

- Cada módulo tiene un especialista por rol, con nombre `<rol>-<modulo>` (roles: `analista`, `desarrollador`, `documentador`). Viven en `.claude/agents/` y `.opencode/agents/` del proyecto.
- Un especialista lee solo las rutas de su módulo en `.sdd/team.md` y lo que su encargo indique, y escribe solo donde su rol lo permite (tabla de abajo). Si algo te impide hacerlo o dudas de si puedes, no lo eludas: informa.

## Estados

Los ficheros que requieren aprobación (los sprints) llevan en la tercera línea `Estado: borrador` o `Estado: aprobado`. Solo el router correspondiente pasa a `aprobado`, y solo tras la aprobación explícita del usuario. Quien crea o modifica el fichero lo deja en `borrador`.

## Protocolo de los especialistas

Los especialistas no pueden hablar con el usuario. Su respuesta empieza con una de estas líneas:

- `PREGUNTAS`: dudas bloqueantes, máximo 5, cada una con propuesta por defecto. No has escrito nada.
- `LISTO`: ruta del fichero, resumen de 1 a 3 frases y una línea `Avisos:` si procede.
- `SIN CAMBIOS`: una frase explicando por qué.
- `PARCIAL` (solo desarrollador y documentador): has hecho una parte. Indica las tareas terminadas, las pendientes, el motivo (un test que falla, una duda, una dependencia sin cumplir) y el último error relevante, resumido.

El router transmite las preguntas al usuario y vuelve a delegar con sus respuestas.

| Rol | Escribe | No hace |
|---|---|---|
| analista | `docs/sprints/<modulo>/` | código |
| desarrollador | código y pruebas unitarias de su módulo; en `specs/<feature>/tasks.md` la casilla `[x]` y la ruta de los componentes nuevos, escribir ejemplos de uso en `specs/<feature>/<module>/examples.md` | cambiar requisitos, diseño ni sprints |
| documentador | `docs/contracts/<protocolo>/`, actualizar guia de uso `docs/guide/<modulo>/manual-uso.md`, las líneas de su módulo en `.sdd/architecture.md`, la casilla `[x]` de las tareas `Contrato:`, la casilla `[x]` de las tareas `Guia:` | código |


## Qué significa "hecho"

Una tarea está hecha cuando el código está implementado, compila y pasan con `mvn test` sus pruebas unitarias y las existentes del módulo. Solo entonces se marca `[x]`. Las pruebas de integración y las manuales las hace el tester, que informa sin marcar. Las tareas cuyo título empieza por `Contrato:` son del documentador.

## Git

Ningún agente hace commit, push, pull, checkout, merge, rebase ni stash: los hace el usuario. Solo `desarrollador-router` consulta el estado de la rama.

## Particularidades de este proyecto

- Seguir el patron SOLID lo mas acotado posible pero sobre todo patron (KISS, Keep It Simple, Stupid)  y YAGNI (You Aren´t Gonna Need It). Buenas practicas pero que el codigo sea sencillo para los ejemplos que va leer el documentador. 