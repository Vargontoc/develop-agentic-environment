# Rendimiento del CLI (NFR-002)

Umbrales: cada comando no interactivo en menos de 3 s con un proyecto de hasta 200 artefactos, y `describe` en menos
de 1 s con un fichero de hasta 1 MB. Decisión asociada: DEC-016 (sin daemon persistente en v1).

## Cómo se mide

- **En proceso** (`PerformanceTest`, etiqueta `performance`): se monta un home temporal, se configura Claude y se crea
  un proyecto con 64 módulos y los 3 técnicos (más de 200 artefactos en `.claude`). Se ejecuta cada comando por
  `CommandLine`, serialización del envelope incluida, en frío (sin calentamiento). La prueba falla si se supera un umbral.
- **Proceso real**: `java -cp target/classes;<dependencias> ...App <comando>` sobre un proyecto pequeño, con el
  cronómetro del shell. Incluye el arranque de la JVM.

Entorno de las mediciones: Windows 11, Java 21.0.7, disco local.

## Resultados

| Comando | Caso | Tiempo en proceso | Umbral |
|---|---|---|---|
| `status` | proyecto con más de 200 artefactos | ≈ 9 ms | 3 s |
| `tree` | proyecto con más de 200 artefactos | ≈ 170 ms | 3 s |
| `tree` | 64 artefactos de ≈ 100 KB (≈ 6 MB) | ≈ 190 ms | 3 s |
| `describe` | agente de 1 MB | ≈ 20 ms | 1 s |

Tres ejecuciones seguidas dieron valores dentro de ±5 ms.

Proceso real (arranque de la JVM incluido, proyecto con un módulo y tres técnicos):

| Comando | Tiempo |
|---|---|
| `status` | ≈ 350 ms |
| `describe` | ≈ 400 ms |
| `tree` | ≈ 460 ms |
| `configure-provider` | ≈ 510 ms |
| `create-module` | ≈ 550 ms |

El arranque de la JVM y de las clases pesa entre 300 y 400 ms; el trabajo propio del comando es pequeño.

## Conclusión

- Los umbrales de NFR-002 se cumplen con un margen amplio: el peor caso medido en proceso está unas 15 veces por debajo
  del límite de 3 s, y `describe` de 1 MB unas 50 veces por debajo de 1 s. Sumando el arranque de la JVM, el comando más
  lento queda por debajo de 1 s.
- **No se necesita un daemon en v2 por rendimiento.** DEC-016 se mantiene. Convendría reconsiderarlo solo si un consumidor
  llama a los comandos de forma continua (por ejemplo, un visor que refresque el árbol cada pocos cientos de ms), porque
  entonces el coste fijo de la JVM (≈ 0,3-0,4 s) se repite en cada llamada.

## Observaciones

- **Tamaño de las cadenas en el JSON:** `describe` de un fichero de 1 MB devuelve el cuerpo en una sola cadena. El lector
  Jackson por defecto rechaza cadenas de más de 1.000.000 de caracteres (`StreamReadConstraints`). Un consumidor que lea la
  salida con la configuración por defecto fallará con ficheros de ese tamaño; debe elevar ese límite. Debe constar en el
  contrato `cli-json` (TASK-030).
- **Escrituras de metadatos:** cada registro reescribe `metadata.json` y deja una copia en el directorio de copias. La
  creación de los 64 módulos de la prueba (192 especialistas) tardó pocos segundos, pero el número de copias crece con el
  número de artefactos registrados. No afecta a los umbrales (solo a los comandos de escritura), pero conviene vigilarlo
  si se crean cientos de módulos de una vez.
- **Hash de skills:** se calcula sobre el directorio completo en cada inspección. Con skills grandes con binarios sería
  el primer punto a optimizar (por ejemplo, con una caché por fecha y tamaño); con los recursos actuales no es relevante.
- **Repetir la medición:** `mvn test -Dtest=PerformanceTest` imprime los tiempos con el prefijo `[perf]`.
