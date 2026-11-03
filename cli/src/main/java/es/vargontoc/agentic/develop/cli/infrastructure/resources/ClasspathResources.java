package es.vargontoc.agentic.develop.cli.infrastructure.resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Acceso a los recursos del classpath, tanto desde el IDE (ficheros) como desde un jar.
 */
public final class ClasspathResources {

    public record CopyStats(int copied, int skipped) {}

    @FunctionalInterface
    private interface PathAction<T> {
        T apply(Path path) throws IOException;
    }

    private ClasspathResources() {}

    public static Optional<String> readText(String resource) throws IOException {
        try(InputStream in = ClasspathResources.class.getResourceAsStream(resource)) {
            if(in == null)
                return Optional.empty();
            return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** Nombres de los ficheros directos de una carpeta de recursos; vacío si la carpeta no existe. */
    public static Optional<List<String>> listFiles(String resource) throws IOException {
        return list(resource, Files::isRegularFile);
    }

    /** Nombres de las subcarpetas directas de una carpeta de recursos; vacío si la carpeta no existe. */
    public static Optional<List<String>> listDirectories(String resource) throws IOException {
        return list(resource, Files::isDirectory);
    }

    /**
     * Copia el árbol de recursos sin sobrescribir: los ficheros que ya existen en destino se cuentan como omitidos.
     * Devuelve vacío si el recurso no existe.
     */
    public static Optional<CopyStats> copyTreeIfAbsent(String resource, Path dest) throws IOException {
        return withPath(resource, src -> copyTree(src, dest));
    }

    private static Optional<List<String>> list(String resource, Predicate<Path> filter) throws IOException {
        return withPath(resource, dir -> {
            try(Stream<Path> children = Files.list(dir)) {
                return children.filter(filter).map(ClasspathResources::name).sorted().toList();
            }
        });
    }

    // En un jar el sistema de ficheros debe seguir abierto mientras se usa el Path
    private static <T> Optional<T> withPath(String resource, PathAction<T> action) throws IOException {
        URL url = ClasspathResources.class.getResource(resource);
        if(url == null)
            return Optional.empty();

        try {
            URI uri = url.toURI();
            if("jar".equals(uri.getScheme())) {
                try(FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    return Optional.of(action.apply(fs.getPath(resource)));
                }
            }
            return Optional.of(action.apply(Path.of(uri)));
        } catch(URISyntaxException e) {
            throw new IOException(e);
        }
    }

    // En un jar, el nombre de una carpeta puede terminar en "/"
    private static String name(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
    }

    private static CopyStats copyTree(Path src, Path dest) throws IOException {
        int copied = 0;
        int skipped = 0;

        try(Stream<Path> files = Files.walk(src)) {
            for(Path file : files.toList()) {
                Path target = dest.resolve(src.relativize(file).toString());
                if(Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else if(Files.exists(target)) {
                    skipped++;
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target);
                    copied++;
                }
            }
        }
        return new CopyStats(copied, skipped);
    }
}
