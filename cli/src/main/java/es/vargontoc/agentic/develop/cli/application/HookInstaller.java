package es.vargontoc.agentic.develop.cli.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.metadata.ArtifactRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.ContentHash;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.HookResource;

/**
 * Política de instalación del hook {@code sdd-specs-only} en la carpeta global de un proveedor (DEC-003).
 * Solo toca la ruta exacta del hook: el resto de {@code hooks/} es del usuario.
 * <p>
 * <ul>
 *   <li>No existe: lo escribe con la variante del sistema (con permiso de ejecución en Linux) y registra el hash de lo escrito.</li>
 *   <li>Existe, con registro y sin cambios del usuario (hash en disco igual al registrado): lo sustituye por la versión
 *       incorporada si difiere, o no lo reescribe si es igual. Sin aviso.</li>
 *   <li>Existe y su hash difiere del registrado: no lo toca y avisa de que está modificado por el usuario.</li>
 *   <li>Existe sin registro (también con el almacén ausente o ilegible): no lo toca, no crea registro y avisa de que
 *       no se puede comprobar si fue modificado, aunque su contenido coincida con el incorporado.</li>
 * </ul>
 * La comparación usa el hash del almacén, que normaliza los fines de línea (NFR-002).
 */
public class HookInstaller {

    private static final Logger log = LoggerFactory.getLogger(HookInstaller.class);

    // Solo el usuario lo ejecuta; ni el grupo ni otros necesitan leerlo o ejecutarlo
    private static final String EXECUTABLE = "rwx------";

    private final PathResolver paths;
    private final MetadataStore metadata;

    public HookInstaller(PathResolver paths, MetadataStore metadata) {
        this.paths = paths;
        this.metadata = metadata;
    }

    /** Las rutas del resultado son relativas a la carpeta del proveedor ({@code hooks/sdd-specs-only.bat}). */
    public void install(Provider provider, OperationResult result) {
        TargetOs os = paths.os() == PathResolver.Os.WINDOWS ? TargetOs.WINDOWS : TargetOs.LINUX;
        String relative = "hooks/" + HookResource.fileName(os);

        try {
            Path hook = paths.hookPath(provider.getPath());
            byte[] bundled = HookResource.content(os);

            if(Files.exists(hook)) {
                handleExisting(hook, relative, bundled, os, result);
                return;
            }

            Files.createDirectories(hook.getParent());
            Files.write(hook, bundled, StandardOpenOption.CREATE_NEW);
            finish(hook, relative, os, result);
        } catch(IOException | DomainException e) {
            fail(result, relative + ": could not be installed: " + reason(e), e);
        }
    }

    // El fichero ya existe: el registro decide si es nuestro y está sin tocar (se puede actualizar) o no (se respeta)
    private void handleExisting(Path hook, String relative, byte[] bundled, TargetOs os, OperationResult result)
            throws IOException {
        Optional<ArtifactRecord> registered = metadata.hook(hook);
        if(registered.isEmpty()) {
            // Sin registro (sin entrada, almacén ausente o ilegible) no se puede saber si es del usuario: no se adopta
            skip(result, relative, "no se puede comprobar si fue modificado (sin registro), no se ha actualizado");
            return;
        }

        String onDisk = ContentHash.ofUtf8(Files.readAllBytes(hook));
        if(!onDisk.equals(registered.get().hash())) {
            skip(result, relative, "modificado por el usuario, no se ha actualizado");
            return;
        }

        // Sin cambios del usuario: solo se reescribe si la versión incorporada es distinta
        if(onDisk.equals(ContentHash.ofUtf8(bundled))) {
            result.skipped().add(relative);
            return;
        }

        replace(hook, bundled);
        finish(hook, relative, os, result);
    }

    // Permiso de ejecución (Linux), registro del hash de lo escrito y constancia en el resultado
    private void finish(Path hook, String relative, TargetOs os, OperationResult result) {
        if(os == TargetOs.LINUX)
            makeExecutable(hook, relative, result);
        register(hook, relative, result);
        result.created().add(relative);
    }

    // Omisión con aviso: es un resultado normal, no un error (DEC-004)
    private void skip(OperationResult result, String relative, String reason) {
        result.skipped().add(relative);
        result.warnings().add(relative + ": " + reason);
    }

    // Se escribe a un temporal de la misma carpeta y se mueve encima: nunca queda un hook a medias
    private void replace(Path hook, byte[] content) throws IOException {
        Path temp = Files.createTempFile(hook.getParent(), hook.getFileName().toString(), ".tmp");
        try {
            Files.write(temp, content);
            Files.move(temp, hook, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // El permiso de ejecución es parte del requisito en Linux: si no se puede fijar, queda como aviso
    private void makeExecutable(Path hook, String relative, OperationResult result) {
        try {
            Files.setPosixFilePermissions(hook, PosixFilePermissions.fromString(EXECUTABLE));
        } catch(IOException | UnsupportedOperationException e) {
            fail(result, relative + ": installed but could not be made executable: " + reason(e), e);
        }
    }

    // Se registra el hash de lo que quedó en disco; un fallo de registro no deshace la instalación
    private void register(Path hook, String relative, OperationResult result) {
        try {
            metadata.putHook(hook, Files.readAllBytes(hook));
        } catch(IOException | RuntimeException e) {
            fail(result, relative + ": installed but could not be registered: " + reason(e), e);
        }
    }

    private void fail(OperationResult result, String message, Exception cause) {
        log.error(message, cause);
        result.warnings().add(message);
    }

    // Algunas excepciones (UnsupportedOperationException) no traen mensaje
    private static String reason(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
