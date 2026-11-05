package es.vargontoc.agentic.develop.cli.command;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.ArtifactLocation;
import es.vargontoc.agentic.develop.cli.application.ArtifactSaver;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code save --path <ruta> (--content-file <ruta> | --stdin) [--header-file <ruta> | --header-stdin]
 * [--mode instance|group] [--file <ruta en la skill>] [--project <ruta>] [--overwrite] [--dry-run]}.
 * <ul>
 *   <li>{@code instance} (por defecto): guarda el cuerpo de ese único artefacto.</li>
 *   <li>{@code group}: guarda y propaga el cuerpo compartido a las copias existentes del grupo; el contenido lleva los
 *       placeholders sin resolver (los que muestra {@code describe} en {@code sharedBody}).</li>
 * </ul>
 * En una skill el contenido es el de un fichero suyo ({@code SKILL.md} salvo que se indique {@code --file}). Un artefacto
 * modificado fuera de la aplicación es un conflicto salvo que se pida {@code --overwrite}. La entrada estándar solo se
 * lee con {@code --stdin} o {@code --header-stdin}, y no con las dos a la vez.
 * <p>
 * {@code --header-file}/{@code --header-stdin} sustituyen la cabecera de un agente conservando su cuerpo (la cabecera
 * es propia de cada proveedor, así que solo vale en modo {@code instance}); sin contenido, solo cambia la cabecera.
 * {@code --dry-run} devuelve los miembros, los diffs y los conflictos sin escribir nada.
 */
final class SaveCommand implements Command {

    private static final String PATH = "path";
    private static final String MODE = "mode";
    private static final String FILE = "file";
    private static final String OVERWRITE = "overwrite";
    private static final String DRY_RUN = "dry-run";
    private static final String HEADER_FILE = "header-file";
    private static final String HEADER_STDIN = "header-stdin";
    private static final String SKILL_FILE = "SKILL.md";
    private static final String INSTANCE = "instance";
    private static final String GROUP = "group";

    record MemberView(String path, String provider, String module, String outcome, String diff, List<String> backups) {}

    record SaveView(String mode, boolean dryRun, String path, String groupId, boolean written, boolean tracked,
                    boolean groupUpdated, String diff, List<String> backups, List<MemberView> members) {}

    private final Supplier<Services> services;
    private final ContentInput input;
    private final ContentInput headerInput;

    SaveCommand(Supplier<Services> services, Supplier<InputStream> input) {
        this.services = services;
        this.input = new ContentInput(input);
        this.headerInput = new ContentInput(input, HEADER_FILE, HEADER_STDIN, "la cabecera");
    }

    @Override
    public String name() {
        return "save";
    }

    @Override
    public String description() {
        return "Guarda el contenido de un artefacto o lo propaga a su grupo";
    }

    @Override
    public List<OptionSpec> options() {
        List<OptionSpec> options = new ArrayList<>(List.of(OptionSpec.value(PATH, true, "Ruta del agente o de la skill")));
        options.addAll(input.options());
        options.addAll(headerInput.options());
        options.addAll(List.of(
            OptionSpec.value(MODE, false, "instance (por defecto) o group"),
            OptionSpec.value(FILE, false, "Fichero de la skill que se guarda (por defecto SKILL.md)"),
            ProjectOption.spec(false),
            OptionSpec.flag(OVERWRITE, "Sobrescribe aunque haya cambios externos"),
            OptionSpec.flag(DRY_RUN, "Devuelve miembros, diffs y conflictos sin escribir nada")));
        return options;
    }

    @Override
    public Object execute(ParsedArgs args) {
        boolean group = isGroup(args);
        boolean overwrite = args.flag(OVERWRITE);
        boolean dryRun = args.flag(DRY_RUN);
        boolean withHeader = headerInput.given(args);
        if(withHeader && input.fromStdin(args) && headerInput.fromStdin(args))
            throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + HEADER_STDIN);

        // Sin cabecera nueva el contenido es obligatorio; con ella puede faltar y entonces solo cambia la cabecera
        String content = input.read(args, !withHeader).orElse(null);
        String header = withHeader ? headerInput.read(args, true).orElseThrow() : null;

        Services s = services.get();
        Path project = ProjectOption.resolve(s, args);
        ArtifactLocation location = s.descriptor().resolve(args.required(PATH), project);

        if(location.entry().isDirectory()) {
            if(withHeader)
                throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + HEADER_FILE);
            Map<String, String> files = Map.of(args.optional(FILE).orElse(SKILL_FILE), content);
            if(group)
                return of(location, s.propagator().propagateFiles(groupOf(s, location), files, project, overwrite, dryRun), dryRun);
            return instanceFiles(s, location, files, overwrite, dryRun);
        }

        if(args.optional(FILE).isPresent())
            throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + FILE);
        if(group) {
            if(withHeader)
                throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + HEADER_FILE);
            return of(location, s.propagator().propagate(groupOf(s, location), content, project, overwrite, dryRun), dryRun);
        }
        return instanceBody(s, location, header, content, overwrite, dryRun);
    }

    private static boolean isGroup(ParsedArgs args) {
        String mode = args.optional(MODE).orElse(INSTANCE);
        if(!mode.equals(INSTANCE) && !mode.equals(GROUP))
            throw new UsageException(UsageError.INVALID_VALUE, "--" + MODE + " " + mode);
        return mode.equals(GROUP);
    }

    private static SaveView instanceBody(Services s, ArtifactLocation location, String header, String body,
                                         boolean overwrite, boolean dryRun) {
        ArtifactSaver.SaveResult saved = s.saver().saveParts(location, header, body, overwrite, dryRun);
        return new SaveView(INSTANCE, dryRun, Views.text(saved.path()), null, saved.written(), saved.tracked(), false,
            saved.diff(), backups(saved.backup() == null ? List.of() : List.of(saved.backup())), List.of());
    }

    private static SaveView instanceFiles(Services s, ArtifactLocation location, Map<String, String> files,
                                          boolean overwrite, boolean dryRun) {
        ArtifactSaver.SkillSaveResult saved = s.saver().saveSkillFiles(location, files, overwrite, dryRun);
        StringBuilder diff = new StringBuilder();
        saved.diffs().forEach((file, d) -> {
            if(!diff.isEmpty())
                diff.append('\n');
            diff.append("@@ ").append(file);
            if(!d.isEmpty())
                diff.append('\n').append(d);
        });
        return new SaveView(INSTANCE, dryRun, Views.text(saved.path()), null, saved.written(), saved.tracked(), false,
            diff.toString(), backups(saved.backups()), List.of());
    }

    // Solo un artefacto con registro pertenece a un grupo; un legacy no se propaga sin adoptarlo antes
    private static String groupOf(Services s, ArtifactLocation location) {
        return s.descriptor().groupOf(location)
            .orElseThrow(() -> new PathDomainException(AppErrorCode.GROUP_NOT_FOUND, location.path()));
    }

    private static SaveView of(ArtifactLocation location, GroupPropagator.Result result, boolean dryRun) {
        List<MemberView> members = result.members().stream()
            .map(m -> new MemberView(Views.text(m.path()), m.provider() == null ? null : m.provider().id(), m.module(),
                m.outcome().name().toLowerCase(Locale.ROOT), m.diff(), backups(m.backups())))
            .toList();
        return new SaveView(GROUP, dryRun, Views.text(location.path()), result.groupId(), result.changed(), true,
            result.groupUpdated(), "", List.of(), members);
    }

    private static List<String> backups(List<Path> paths) {
        return paths.stream().map(Views::text).toList();
    }
}
