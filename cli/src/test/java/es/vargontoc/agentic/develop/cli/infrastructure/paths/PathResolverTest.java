package es.vargontoc.agentic.develop.cli.infrastructure.paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver.Os;

class PathResolverTest {

    @TempDir
    Path tmp;

    private PathResolver windows(Map<String, String> extra) {
        return new PathResolver(merge(Map.of("USERPROFILE", tmp.toString()), extra), Os.WINDOWS);
    }

    private PathResolver linux(Map<String, String> extra) {
        return new PathResolver(merge(Map.of("HOME", tmp.toString()), extra), Os.LINUX);
    }

    private static Map<String, String> merge(Map<String, String> a, Map<String, String> b) {
        var all = new java.util.HashMap<>(a);
        all.putAll(b);
        return all;
    }

    private static void assertCode(AppErrorCode expected, Runnable action) {
        DomainException e = assertThrows(DomainException.class, action::run);
        assertEquals(expected, e.code());
    }

    // --- proveedores y home de la aplicación ---

    @Test
    void windowsResolvesProvidersFromUserProfile() {
        PathResolver paths = windows(Map.of());

        assertEquals(tmp.resolve(".claude"), paths.providerHome(ProviderType.CLAUDE));
        assertEquals(tmp.resolve(".codex"), paths.providerHome(ProviderType.CODEX));
        assertEquals(tmp.resolve(".gemini").resolve("config"), paths.providerHome(ProviderType.GEMINI));
        assertEquals(tmp.resolve(".config").resolve("opencode"), paths.providerHome(ProviderType.OPENCODE));
        assertEquals(tmp.resolve("AppData").resolve("Local").resolve("agentic-develop"), paths.appHome());
    }

    @Test
    void windowsUsesLocalAppDataForTheApplicationHomeWhenDefined() {
        Path local = tmp.resolve("local");

        assertEquals(local.resolve("agentic-develop"), windows(Map.of("LOCALAPPDATA", local.toString())).appHome());
    }

    @Test
    void linuxResolvesProvidersFromHome() {
        PathResolver paths = linux(Map.of());

        assertEquals(tmp.resolve(".claude"), paths.providerHome(ProviderType.CLAUDE));
        assertEquals(tmp.resolve(".config").resolve("opencode"), paths.providerHome(ProviderType.OPENCODE));
        assertEquals(tmp.resolve(".local").resolve("state").resolve("agentic-develop"), paths.appHome());
    }

    @Test
    void linuxHonoursXdgVariablesWhenAbsolute() {
        Path xdgConfig = tmp.resolve("xdg-config");
        Path xdgState = tmp.resolve("xdg-state");
        PathResolver paths = linux(Map.of("XDG_CONFIG_HOME", xdgConfig.toString(), "XDG_STATE_HOME", xdgState.toString()));

        assertEquals(xdgConfig.resolve("opencode"), paths.providerHome(ProviderType.OPENCODE));
        assertEquals(xdgState.resolve("agentic-develop"), paths.appHome());
        assertEquals(tmp.resolve(".claude"), paths.providerHome(ProviderType.CLAUDE));
    }

    @Test
    void relativeXdgValuesAreIgnoredAndWindowsIgnoresXdg() {
        assertEquals(tmp.resolve(".config").resolve("opencode"),
            linux(Map.of("XDG_CONFIG_HOME", "relativo/config")).providerHome(ProviderType.OPENCODE));
        assertEquals(tmp.resolve(".config").resolve("opencode"),
            windows(Map.of("XDG_CONFIG_HOME", tmp.resolve("xdg").toString())).providerHome(ProviderType.OPENCODE));
    }

    @Test
    void missingHomeIsAnIncompatibleEnvironment() {
        PathResolver noHome = new PathResolver(Map.of(), Os.LINUX);

        assertFalse(noHome.hasHome());
        assertCode(AppErrorCode.INCOMPATIBLE_ENVIRONMENT, noHome::home);
        assertCode(AppErrorCode.INCOMPATIBLE_ENVIRONMENT, () -> noHome.providerHome(ProviderType.CLAUDE));
    }

    // --- rutas de proyecto ---

    @Test
    void projectPathIsNormalizedAndQuotesAreRemoved() throws IOException {
        Path project = Files.createDirectories(tmp.resolve("proyecto"));
        PathResolver paths = linux(Map.of());

        assertEquals(project, paths.resolveProject(project.toString()));
        assertEquals(project, paths.resolveProject("\"" + project + "\""));
        assertEquals(project, paths.resolveProject(tmp.resolve("otro").resolve("..").resolve("proyecto").toString()));
    }

    @Test
    void emptyProjectPathMeansTheCurrentDirectory() {
        assertEquals(Path.of("").toAbsolutePath(), linux(Map.of()).resolveProject("  "));
    }

    @Test
    void invalidMissingAndNonDirectoryProjectPathsAreRejected() throws IOException {
        Path file = Files.writeString(tmp.resolve("fichero.txt"), "x");
        PathResolver paths = linux(Map.of());

        assertCode(AppErrorCode.PROJECT_PATH_NOT_FOUND, () -> paths.resolveProject(tmp.resolve("no-existe").toString()));
        assertCode(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, () -> paths.resolveProject(file.toString()));
        assertCode(AppErrorCode.PROJECT_PATH_INVALID, () -> paths.resolveProject("mal\0nombre"));
    }

    // --- ámbito ---

    @Test
    void pathsInsideTheScopeAreAcceptedEvenIfTheyDoNotExistYet() throws IOException {
        Path scope = Files.createDirectories(tmp.resolve("scope"));

        linux(Map.of()).requireWithin(scope, scope.resolve("nuevo").resolve("fichero.md"));
    }

    @Test
    void pathsEscapingTheScopeWithDotDotAreRejected() throws IOException {
        Path scope = Files.createDirectories(tmp.resolve("scope"));

        assertCode(AppErrorCode.PATH_OUT_OF_SCOPE,
            () -> linux(Map.of()).requireWithin(scope, scope.resolve("..").resolve("fuera.md")));
    }

    @Test
    void symlinkPointingOutsideTheScopeIsRejected() throws IOException {
        Path scope = Files.createDirectories(tmp.resolve("scope"));
        Path outside = Files.createDirectories(tmp.resolve("outside"));
        Path link = scope.resolve("enlace");
        try {
            Files.createSymbolicLink(link, outside);
        } catch(IOException | UnsupportedOperationException | SecurityException e) {
            assumeTrue(false, "No se pueden crear enlaces simbólicos en este entorno");
        }

        assertCode(AppErrorCode.PATH_OUT_OF_SCOPE,
            () -> linux(Map.of()).requireWithin(scope, link.resolve("dentro.md")));
    }
}
