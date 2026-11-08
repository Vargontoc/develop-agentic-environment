---
name: example-conventions
description: Convenciones del fichero specs/<feature>/<modulo>/example.md, donde el desarrollador deja un ejemplo de uso por cada componente nuevo para que el documentador redacte la guía sin leer código. Úsala solo en el desarrollador especialista.
---

# Ejemplos de uso de componentes

Cada componente nuevo o modificado de forma visible para quien lo usa se documenta con un ejemplo en `specs/<feature>/<modulo>/example.md`. El documentador redacta la guía de uso (`guide-conventions`) solo a partir de este fichero, sin leer código: lo que no esté aquí no llegará a la guía.

## Cuándo

- Por cada componente nuevo, tras marcar su tarea como hecha.
- Si modificas un componente existente de forma que cambia cómo se usa, actualiza su sección.
- No lo hagas para clases internas que nadie más usa.

## Reglas

- Crea el fichero si no existe; si existe, añade o actualiza solo la sección del componente, sin reordenar ni borrar las demás.
- Un ejemplo es mínimo, compilable y fiel al código real: usa los nombres, firmas y paquetes que existen. No inventes API.
- Sin lógica de negocio ajena al ejemplo ni dependencias que el lector no necesite.
- Mantén el código sencillo (KISS, YAGNI): el documentador lo copiará a la guía.

## Formato

```markdown
# Ejemplos: <feature> - <modulo>

## <NombreDelComponente>

**Ruta:** `<ruta real del componente>`
**Para qué sirve:** <1 o 2 frases, en lenguaje llano.>

```<lenguaje>
<código de ejemplo de uso>
```

**Notas:** <límites, errores que puede devolver o requisitos previos; omite la línea si no hay.>
```

Una sección por componente, en el orden en que los has implementado. Informa en `Avisos:` de los componentes cuyo ejemplo has creado o actualizado.
