# Plan de acción: release 1.0.0

Estado de partida (comprobado en el repositorio el 2026-11-08):

- No hay nada en producción, así que no hay compatibilidad hacia atrás que mantener antes de la 1.0.
- `cli/pom.xml` está en `1.0.0-SNAPSHOT` y `viewer/pom.xml` en `0.1.0-SNAPSHOT`. Los scripts de empaquetado repiten
  `0.1.0` (`APP_VERSION`) y `viewer-0.1.0-SNAPSHOT.jar` a mano.
- La CLI depende de `framework-core` y `framework-json` de `vargontoc-framework` en `0.1.0-SNAPSHOT`, que se instalan a
  mano en `~/.m2` desde `cli/external-libs/`. El viewer no depende del framework.
- No hay `.github/` (ni CI), ni `LICENSE`, ni `CHANGELOG.md`.
- Los scripts de empaquetado ya generan la app-image y, con `--installer` / `-Installer`, un `.deb` / `.msi`
  (**el instalador no se ha probado todavía**).

## Decisiones tomadas

| Tema | Decisión |
|---|---|
| Alcance de la 1.0 | CLI **y** Viewer. El Viewer aún tiene bugs de usabilidad y visuales que hay que revisar antes. |
| Canal de distribución | GitHub Releases, con los instaladores adjuntos. |
| Versionado | Semántico. Un cambio incompatible en el JSON de la CLI, en `metadata.json` (`schemaVersion`) o en el reparto de carpetas por proveedor obliga a subir la versión mayor. |

## Decisión pendiente: qué hacer con `vargontoc-framework`

Es un framework interno y privado, no una librería pública. El jar de la CLI es un jar con las dependencias incluidas
(`maven-shade-plugin`), así que **quien instale la aplicación no necesita el framework**: solo lo necesita quien
compile. Opciones:

| Opción | Cómo funciona | Pros | Contras |
|---|---|---|---|
| **A. Mantenerlo incluido en el repositorio** (como ahora) con versión fija, sin `-SNAPSHOT` | Los jars siguen en `cli/external-libs/`; CI y personas los instalan con un script. | Sin infraestructura nueva. Compila desde cualquier clon. | El repositorio lleva binarios. Si el repositorio es público, también publicas ese código. |
| **B. Compilarlo desde su repositorio** | CI clona `vargontoc-framework` (con una clave de acceso o token) y lo instala con `mvn install` antes de compilar. | Siempre es el código fuente real. | Necesita un secreto en CI; un clon limpio no compila sin acceso. |
| **C. GitHub Packages (registro Maven privado)** | El framework se publica en GitHub Packages y la CLI lo descarga como dependencia normal. | Es la forma estándar para un framework interno; versiones reales. | Requiere credenciales en `settings.xml` también para el desarrollo local. |

**Recomendación para la 1.0:** opción **A**, pero con la versión del framework fijada sin `-SNAPSHOT` (por ejemplo
`0.1.0`) y un script que instale los jars. Es lo mínimo que hace reproducible la release. Pasar a C si el framework
empieza a tener más consumidores. **Antes de elegir A, comprobar si el repositorio de GitHub es público**, porque
entonces los jars del framework ya son públicos.

Sea cual sea la opción, **no mantener el mismo número de versión con un jar que cambia** (es el problema actual del
`-SNAPSHOT` copiado a mano).

## Fases

### Fase 0 — Preparación del repositorio

- [ ] Decidir la opción del framework (A, B o C) y fijar su versión sin `-SNAPSHOT`.
- [ ] Comprobar la visibilidad del repositorio de GitHub (público o privado).
- [ ] Elegir y añadir `LICENSE`. Revisar el `vendor` («Agentic Development») que usan `jpackage` y los instaladores.
- [ ] Crear en GitHub un milestone `1.0.0` y registrar ahí los bugs del Viewer como issues.

### Fase 1 — Estabilizar el Viewer

- [ ] Revisar el Viewer y registrar **cada bug** (usabilidad / visual) como issue, con etiqueta y prioridad.
- [ ] Marcar cuáles bloquean la 1.0 (los que impiden usarlo o dan datos incorrectos) y cuáles pueden esperar a 1.0.x.
- [ ] Corregir los bloqueantes. Definir qué se considera «Viewer terminado» para cerrar esta fase.
- [ ] Arreglar `CliClientTest.parseaTreeDelCliRealSiElJarEstaDisponible`: depende de lo que haya instalado en el `HOME`
      de quien ejecuta (falla en una máquina limpia). Hacerlo independiente (`HOME` temporal o `assumeTrue`).
- [ ] Revisar que no haya otros tests que dependan del estado de la máquina.

### Fase 2 — Verificación en Linux

- [ ] Ejecutar la suite de la CLI en Ubuntu (ya hecha en WSL: los 5 tests saltados en Windows deben ejecutarse).
- [ ] Empaquetar en Ubuntu con `--installer`, instalar el `.deb` y comprobar que arranca y que el Viewer encuentra el
      jar de la CLI en su ubicación instalada.
- [ ] Comprobar el hook instalado: `-rwx------`, y que no se sobrescribe un hook modificado.
- [ ] Decidir si el reemplazo del hook debe dejar copia de seguridad (queda documentado como excepción).
- [ ] Probar la terminal integrada (PTY) del Viewer en Linux.

### Fase 3 — Verificación en Windows del instalador

- [ ] Instalar WiX Toolset 3.x y construir el `.msi` con `-Installer`.
- [ ] Instalar el `.msi` en una máquina limpia (o VM) y comprobar acceso directo, arranque y CLI.
- [ ] Comprobar la **actualización**: instalar una versión y luego otra encima (el UUID de actualización de
      `package-windows.ps1` es fijo y no debe cambiarse nunca).
- [ ] Anotar que el `.msi` irá **sin firmar** y que Windows mostrará el aviso de SmartScreen. Decidir si se firma
      (cuesta dinero) o se documenta en las notas de la release.

### Fase 4 — Una única fuente de versión

- [ ] Poner CLI y Viewer en la misma versión (`1.0.0`).
- [ ] Hacer que los scripts de empaquetado lean la versión del `pom.xml` en vez de repetirla (`APP_VERSION`,
      `viewer-0.1.0-SNAPSHOT.jar`). Revisar también `verify-package.ps1`.
- [ ] `jpackage` exige una versión numérica en `--app-version` (por ejemplo `1.0.0`); no admite `-rc.1`. Para las
      pre-releases, decidir cómo se refleja en el instalador.
- [ ] Quitar `-SNAPSHOT` solo en la rama de la release.

### Fase 5 — Integración continua

Crear `.github/workflows/`:

- [ ] **`ci.yml`**: en cada push y pull request, `mvn test` de la CLI y del viewer con matriz `ubuntu-latest` +
      `windows-latest`. Debe instalar primero el framework (según la opción elegida). Aquí se detectan los fallos que
      hoy solo se ven al probar a mano en WSL.
- [ ] **`release.yml`**: al subir una etiqueta `v*`, construir en cada sistema y subir a la GitHub Release:
      - `agentic-develop.jar` (CLI);
      - `.deb` (job en `ubuntu-latest`, con `dpkg-dev` y `fakeroot`);
      - `.msi` (job en `windows-latest`, con WiX 3.x instalado en el paso previo);
      - un fichero de sumas SHA-256 de todo lo anterior.
- [ ] Comprobar que los runners tienen JDK 21 (con `jlink` y `jpackage`) y que el script de empaquetado descarga bien
      los jmods de JavaFX en CI.

### Fase 6 — Documentación

- [ ] `CHANGELOG.md` (formato «Keep a Changelog») con la 1.0.0.
- [ ] Guía de instalación por sistema en el README: `.deb`, `.msi` y uso del jar de la CLI.
- [ ] Revisar que `docs/contracts/*` y el README describen exactamente lo que hace la 1.0.
- [ ] Documentar la política de actualización y qué cuenta como cambio incompatible (ver «Versionado»).

### Fase 7 — Release candidate y publicación

- [ ] Etiqueta `v1.0.0-rc.1` → la Release se marca como **pre-release**. Instalar los artefactos descargados de
      GitHub (no los construidos en local) en una máquina limpia de cada sistema.
- [ ] Corregir lo que salga y repetir con `rc.2` si hace falta.
- [ ] Etiqueta `v1.0.0` desde `main`, con las notas de la release (a partir del changelog) y las sumas SHA-256.
- [ ] Tras publicar, pasar `main` a la siguiente versión de desarrollo (`1.0.1-SNAPSHOT` o `1.1.0-SNAPSHOT`).

## Criterios para etiquetar `v1.0.0`

- La CI pasa en Ubuntu y Windows en el commit de la etiqueta.
- El `.deb` y el `.msi` descargados de la `rc` se instalan, arrancan y configuran un proveedor en máquinas limpias.
- No quedan issues abiertos del milestone `1.0.0` que bloqueen.
- La CLI y el Viewer están en la misma versión, sin `-SNAPSHOT`, y ningún artefacto depende de un `-SNAPSHOT`.
- README, contratos y changelog coinciden con lo publicado.

## Riesgos y cosas a vigilar

- **Instaladores sin probar:** las opciones de `jpackage` de `--installer` están escritas pero no se han ejecutado. Es
  lo primero que hay que validar (fases 2 y 3).
- **Viewer sin fecha:** el alcance de la 1.0 depende del número de bugs. Si se alarga, valorar sacar primero la CLI
  como 1.0 y el Viewer como 1.1.
- **WiX 3.x:** `jpackage` de JDK 21 no soporta WiX 4 o posterior. Fijar la versión en la CI.
- **Arquitectura:** los runners construyen para x64. No hay instalador para ARM.
- **Prompts de SDD:** los cambios de las iteraciones A y B (`mejoras-sdd.md`) solo son texto sin probar en ejecución.
  Decidir si se verifican antes de la 1.0 o se declaran fuera del alcance.
- **Hook de Linux:** el comportamiento real con permisos y enlaces simbólicos ya está probado en WSL (suite de la
  CLI), pero no a través del Viewer instalado.

## Orden recomendado

1. Fase 0 (decisiones) → 2. Fase 5, solo `ci.yml` (para dejar de depender de pruebas manuales) → 3. Fases 1, 2 y 3 en
paralelo según avance el Viewer → 4. Fase 4 → 5. `release.yml` y Fase 6 → 6. Fase 7.
