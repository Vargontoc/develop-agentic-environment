# Plan de acción: release 1.0.0

## Fases

### Fase 0 — Preparación del repositorio

- [x] Decidir la opción del framework: **C** (GitHub Packages, repositorio privado), versión `0.1.0`. En CI hay que dar
      acceso al paquete (o un secreto con `read:packages`).
- [x] Comprobar la visibilidad del repositorio de GitHub: **público** (el framework es privado).
- [x] Elegir y añadir `LICENSE` (MIT). `vendor` de `jpackage` cambiado a «Agentic Development Environment».
- [x] Crear en GitHub el milestone `1.0.0`.
- [X] Registrar en él los bugs del Viewer como issues (Fase 1).

### Fase 1 — Estabilizar el Viewer

- [X] Revisar el Viewer y registrar **cada bug** (usabilidad / visual) como issue, con etiqueta y prioridad.
- [X] Marcar cuáles bloquean la 1.0 (los que impiden usarlo o dan datos incorrectos) y cuáles pueden esperar a 1.0.x.
- [X] Corregir los bloqueantes. Definir qué se considera «Viewer terminado» para cerrar esta fase.
- [X] Arreglar `CliClientTest.parseaTreeDelCliRealSiElJarEstaDisponible`: depende de lo que haya instalado en el `HOME`
      de quien ejecuta (falla en una máquina limpia). Hacerlo independiente (`HOME` temporal o `assumeTrue`).
- [X] Revisar que no haya otros tests que dependan del estado de la máquina.

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

- [x] Poner CLI y Viewer en la misma versión (`1.0.0-SNAPSHOT` en desarrollo; ambos pom).
- [x] Los scripts de empaquetado leen la versión de `viewer/pom.xml` (jar y `APP_VERSION`); `PackagingTest` usa
      `ViewerVersion`. `verify-package.ps1` no tenía versiones fijas.
- [x] `jpackage` exige una versión numérica en `--app-version`: los scripts descartan el sufijo (`1.0.0-rc.1` →
      `1.0.0`). El instalador de una `rc` lleva la misma versión que la final; la `rc` se distingue por el nombre del
      fichero y por la etiqueta de la Release.
- [ ] Quitar `-SNAPSHOT` solo en la rama de la release.

### Fase 5 — Integración continua

Crear `.github/workflows/`:

- [x] **`ci.yml`** (hecho; usa el secreto `FRAMEWORK_READ_TOKEN`): en cada push y pull request, `mvn test` de la CLI y del viewer con matriz `ubuntu-latest` +
      `windows-latest`. Debe instalar primero el framework (según la opción elegida). Aquí se detectan los fallos que
      hoy solo se ven al probar a mano en WSL.
- [~] **`release.yml`** (escrito, sin ejecutar; se valida con la `rc.1`): al subir una etiqueta `v*`, construir en cada sistema y subir a la GitHub Release:
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
