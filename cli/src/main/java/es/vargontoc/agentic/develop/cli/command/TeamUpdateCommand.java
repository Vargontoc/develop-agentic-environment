package es.vargontoc.agentic.develop.cli.command;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.command.TeamViews.UpdateRequest;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.framework.json.JsonMapperFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code team-update --project <ruta> --stdin [--expect-hash <hash>] [--dry-run]}:
 * guarda el estado completo de módulos y opciones de {@code .sdd/team.md}.
 */
final class TeamUpdateCommand implements Command {

    static final String DRY_RUN = "dry-run";
    static final String EXPECT_HASH = "expect-hash";

    private final Supplier<Services> services;
    private final ContentInput input;
    private final JsonMapper mapper = JsonMapperFactory.create();

    TeamUpdateCommand(Supplier<Services> services, Supplier<InputStream> input) {
        this.services = services;
        this.input = new ContentInput(input);
    }

    @Override
    public String name() {
        return "team-update";
    }

    @Override
    public String description() {
        return "Guarda módulos y opciones de .sdd/team.md";
    }

    @Override
    public List<OptionSpec> options() {
        List<OptionSpec> options = new java.util.ArrayList<>(List.of(
            ProjectOption.spec(true),
            OptionSpec.value(EXPECT_HASH, false, "Hash esperado de team.md para detectar cambios externos"),
            OptionSpec.flag(DRY_RUN, "Calcula los cambios sin escribir")));
        options.addAll(input.options());
        return options;
    }

    @Override
    public Object execute(ParsedArgs args) {
        String content = input.read(args, true).orElseThrow();
        UpdateRequest request;
        try {
            request = mapper.readValue(content, UpdateRequest.class);
        } catch(RuntimeException e) {
            throw new PathDomainException(AppErrorCode.CONTENT_UNREADABLE, "stdin", e);
        }

        Services s = services.get();
        Path project = ProjectOption.resolve(s, args);
        boolean dryRun = args.flag(DRY_RUN);
        String expectHash = args.optional(EXPECT_HASH).orElse(null);
        return TeamViews.of(s.team().update(project, TeamViews.options(request.options()),
            TeamViews.moduleSpecs(request.modules()), expectHash, dryRun));
    }
}
