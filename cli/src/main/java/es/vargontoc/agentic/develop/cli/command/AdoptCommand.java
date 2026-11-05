package es.vargontoc.agentic.develop.cli.command;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.ArtifactLocation;
import es.vargontoc.agentic.develop.cli.application.MigrationService;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupIds;

/**
 * {@code adopt --path <ruta> [--group <id>] [--project <ruta>] [--dry-run] [--confirm]}: hace que un artefacto legacy
 * (sin registro) o desvinculado vuelva a su grupo. En un agente sustituye su cuerpo por el cuerpo compartido del grupo
 * (con su módulo y proveedor); en una skill la vincula tal cual, salvo que se pase contenido para uno de sus ficheros.
 * <p>
 * Sin {@code --confirm} (o con {@code --dry-run}) solo devuelve el diff que se aplicaría. Con {@code --confirm} escribe,
 * deja una copia del contenido anterior y registra el artefacto como vinculado. Sin {@code --group} se usa el grupo que
 * ya tenía registrado y, si no, el que le corresponde por el catálogo.
 */
final class AdoptCommand implements Command {

    private static final String PATH = "path";
    private static final String GROUP = "group";
    private static final String CONFIRM = "confirm";
    private static final String DRY_RUN = "dry-run";
    private static final String FILE = "file";
    private static final String SKILL_FILE = "SKILL.md";

    record AdoptView(String path, String groupId, boolean dryRun, boolean applied, String diff, String backup) {}

    private final Supplier<Services> services;
    private final ContentInput input;

    AdoptCommand(Supplier<Services> services, Supplier<InputStream> input) {
        this.services = services;
        this.input = new ContentInput(input);
    }

    @Override
    public String name() {
        return "adopt";
    }

    @Override
    public String description() {
        return "Vuelve a vincular un artefacto legacy o desvinculado a su grupo";
    }

    @Override
    public List<OptionSpec> options() {
        List<OptionSpec> options = new ArrayList<>(List.of(
            OptionSpec.value(PATH, true, "Ruta del agente o de la skill"),
            OptionSpec.value(GROUP, false, "Grupo al que se vincula (por defecto el que le corresponde)"),
            ProjectOption.spec(false),
            OptionSpec.flag(DRY_RUN, "Solo devuelve el diff (es lo que ocurre sin --confirm)"),
            OptionSpec.flag(CONFIRM, "Aplica el cambio y deja una copia del contenido anterior"),
            OptionSpec.value(FILE, false, "Solo skills: fichero al que se aplica el contenido (por defecto SKILL.md)")));
        options.addAll(input.options());
        return options;
    }

    @Override
    public Object execute(ParsedArgs args) {
        if(args.flag(CONFIRM) && args.flag(DRY_RUN))
            throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, "--" + DRY_RUN);
        boolean confirm = args.flag(CONFIRM);

        Services s = services.get();
        Path project = ProjectOption.resolve(s, args);
        ArtifactLocation location = s.descriptor().resolve(args.required(PATH), project);
        String groupId = args.optional(GROUP).orElseGet(() ->
            s.descriptor().groupOf(location).orElseGet(() -> GroupIds.of(location.entry())));

        MigrationService.Adoption adoption;
        if(location.entry().isDirectory()) {
            Map<String, String> files = input.read(args, false)
                .map(content -> Map.of(args.optional(FILE).orElse(SKILL_FILE), content))
                .orElse(Map.of());
            adoption = s.migration().adoptSkill(location, groupId, files, confirm);
        } else {
            if(args.optional(FILE).isPresent() || input.given(args))
                throw new UsageException(UsageError.UNEXPECTED_ARGUMENT, args.optional(FILE).isPresent() ? "--" + FILE : "--content-file");
            adoption = s.migration().adopt(location, groupId, confirm);
        }
        return new AdoptView(Views.text(adoption.path()), adoption.groupId(), !confirm, adoption.applied(), adoption.diff(),
            Views.text(adoption.backup()));
    }
}
