package es.vargontoc.agentic.develop.cli.infrastructure.paths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ProviderLayout;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.HookResource;

/**
 * Único punto que decide dónde viven los proveedores y el estado de la aplicación en Windows y Linux,
 * y que normaliza y valida las rutas que llegan del usuario.
 * Recibe el entorno y el sistema operativo para poder probarse con homes simulados.
 */
public final class PathResolver {

    public enum Os { WINDOWS, LINUX }

    private static final String APP_DIR = "agentic-develop";

    private final Map<String, String> env;
    private final Os os;

    public PathResolver(Map<String, String> env, Os os) {
        this.env = Map.copyOf(env);
        this.os = os;
    }

    public static PathResolver system() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return new PathResolver(System.getenv(), osName.contains("win") ? Os.WINDOWS : Os.LINUX);
    }

    public Os os() {
        return os;
    }

    public boolean hasHome() {
        return value(homeVariable()).isPresent();
    }

    /** Perfil del usuario (Windows) o home (Linux). Sin él la aplicación no puede funcionar. */
    public Path home() {
        return value(homeVariable())
            .map(Path::of)
            .orElseThrow(() -> new DomainException(AppErrorCode.INCOMPATIBLE_ENVIRONMENT));
    }

    /** Carpeta global del proveedor; puede no existir si no está instalado. */
    public Path providerHome(ProviderType type) {
        Path home = home();
        return switch(type) {
            case CLAUDE -> home.resolve(".claude");
            case CODEX -> home.resolve(".codex");
            case GEMINI -> home.resolve(".gemini").resolve("config");
            case OPENCODE -> configHome(home).resolve("opencode");
        };
    }

    /**
     * Base de la que cuelgan los artefactos globales del proveedor (ver {@link ProviderLayout}): su carpeta global
     * (Claude, OpenCode) o el home del usuario (Codex, Gemini).
     */
    public Path globalBase(ProviderType type) {
        return ProviderLayout.globalBase(type, providerHome(type));
    }

    /** Base de la que cuelgan los artefactos del proveedor en el proyecto. */
    public Path projectBase(ProviderType type, Path project) {
        return ProviderLayout.projectBase(type, project);
    }

    /**
     * Ruta exacta del hook {@code sdd-specs-only} en la carpeta global del proveedor: {@code hooks/sdd-specs-only.bat}
     * en Windows y {@code hooks/sdd-specs-only.sh} en Linux. Apunta solo al fichero del hook, nunca a la carpeta.
     * El fichero y la carpeta pueden no existir todavía.
     *
     * @throws DomainException {@code PATH_OUT_OF_SCOPE} si, resueltos {@code ..} y enlaces simbólicos (por ejemplo,
     *                         una carpeta {@code hooks/} que sea un enlace hacia fuera), la ruta sale de la carpeta global
     */
    public Path hookPath(ProviderType type) {
        return hookPath(providerHome(type));
    }

    /** Como {@link #hookPath(ProviderType)} para una carpeta global de proveedor ya conocida. */
    public Path hookPath(Path providerHome) {
        TargetOs target = os == Os.WINDOWS ? TargetOs.WINDOWS : TargetOs.LINUX;
        Path hook = providerHome.resolve("hooks").resolve(HookResource.fileName(target));
        return requireWithin(providerHome, hook);
    }

    /** Carpeta donde la aplicación guarda su estado (metadatos, copias de seguridad). */
    public Path appHome() {
        Path home = home();
        Path base = switch(os) {
            case WINDOWS -> absolute("LOCALAPPDATA").orElse(home.resolve("AppData").resolve("Local"));
            case LINUX -> absolute("XDG_STATE_HOME").orElse(home.resolve(".local").resolve("state"));
        };
        return base.resolve(APP_DIR);
    }

    /**
     * Ruta de proyecto escrita por el usuario (vacía = directorio actual): absoluta, normalizada y que existe como directorio.
     */
    public Path resolveProject(String input) {
        String raw = input == null ? "" : input.trim();
        if(raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\""))
            raw = raw.substring(1, raw.length() - 1).trim();

        Path dir;
        try {
            dir = raw.isEmpty() ? Path.of("").toAbsolutePath() : Path.of(raw).toAbsolutePath().normalize();
        } catch(InvalidPathException e) {
            throw new DomainException(AppErrorCode.PROJECT_PATH_INVALID, e);
        }

        if(!Files.exists(dir))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND);
        if(!Files.isDirectory(dir))
            throw new DomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY);
        return dir;
    }

    /**
     * Comprueba que {@code path} queda dentro de {@code scope} una vez resueltos {@code ..} y enlaces simbólicos.
     * Ni la ruta ni el ámbito tienen por qué existir todavía: se resuelve su ancestro existente más cercano.
     */
    public Path requireWithin(Path scope, Path path) {
        try {
            // El ámbito también puede no existir aún (p. ej. el home de la aplicación en el primer uso)
            Path realScope = realOrNearest(scope.toAbsolutePath().normalize());
            Path realPath = realOrNearest(path.toAbsolutePath().normalize());
            if(!realPath.startsWith(realScope))
                throw new DomainException(AppErrorCode.PATH_OUT_OF_SCOPE);
            return path;
        } catch(IOException e) {
            throw new DomainException(AppErrorCode.PATH_OUT_OF_SCOPE, e);
        }
    }

    private Path realOrNearest(Path path) throws IOException {
        Path existing = path;
        while(existing != null && !Files.exists(existing))
            existing = existing.getParent();
        if(existing == null)
            throw new IOException("No existing ancestor for " + path);
        return existing.toRealPath().resolve(existing.relativize(path)).normalize();
    }

    private Path configHome(Path home) {
        if(os == Os.LINUX)
            return absolute("XDG_CONFIG_HOME").orElse(home.resolve(".config"));
        return home.resolve(".config");
    }

    private String homeVariable() {
        return os == Os.WINDOWS ? "USERPROFILE" : "HOME";
    }

    // Las variables XDG deben ser absolutas; si no, se ignoran
    private Optional<Path> absolute(String name) {
        return value(name).map(Path::of).filter(Path::isAbsolute);
    }

    private Optional<String> value(String name) {
        return Optional.ofNullable(env.get(name)).filter(v -> !v.isBlank());
    }
}
