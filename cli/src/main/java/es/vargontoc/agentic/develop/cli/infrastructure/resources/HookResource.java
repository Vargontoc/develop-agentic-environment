package es.vargontoc.agentic.develop.cli.infrastructure.resources;

import java.io.IOException;
import java.io.InputStream;

import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;

/**
 * Hook {@code sdd-specs-only} incorporado en el CLI, en una variante por sistema operativo ({@code .bat} en Windows,
 * {@code .sh} en Linux). El contenido se devuelve tal cual está en el recurso, sin marcadores ni sustituciones, para
 * que el hash del fichero instalado sea el del recurso (DEC-002).
 */
public final class HookResource {

    public static final String NAME = "sdd-specs-only";

    private static final String HOOKS_RESOURCE = "/global/hooks";

    private HookResource() {}

    /** Extensión de la variante del sistema, con el punto. */
    public static String extension(TargetOs os) {
        return os == TargetOs.WINDOWS ? ".bat" : ".sh";
    }

    /** Nombre del fichero del hook para el sistema, por ejemplo {@code sdd-specs-only.bat}. */
    public static String fileName(TargetOs os) {
        return NAME + extension(os);
    }

    /**
     * Contenido de la variante del sistema indicado, byte a byte.
     *
     * @throws IOException si el recurso falta o no se puede leer (es un error interno del empaquetado)
     */
    public static byte[] content(TargetOs os) throws IOException {
        String resource = HOOKS_RESOURCE + "/" + fileName(os);
        try(InputStream in = HookResource.class.getResourceAsStream(resource)) {
            if(in == null)
                throw new IOException("Resource '" + resource + "' not found");
            return in.readAllBytes();
        }
    }
}
