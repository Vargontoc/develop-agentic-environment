package es.vargontoc.agentic.develop.cli.command;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.CreateArtifact;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code create-artifact --provider <id> --scope global|project --kind agent|skill --name <nombre>
 * [--content-file <ruta> | --stdin] [--project <ruta>] [--overwrite]}: crea un agente o una skill del catálogo a partir
 * de su plantilla. Sin contenido usa la plantilla tal cual; con contenido, este sustituye al cuerpo del agente o al
 * {@code SKILL.md} de la skill. Un artefacto existente no se sobrescribe sin {@code --overwrite}.
 */
final class CreateArtifactCommand implements Command {

    private static final String PROVIDER = "provider";
    private static final String SCOPE = "scope";
    private static final String KIND = "kind";
    private static final String NAME = "name";
    private static final String OVERWRITE = "overwrite";

    record CreatedView(String provider, String scope, String kind, String name, String path, boolean replaced,
                       boolean custom, String backup) {}

    private final Supplier<Services> services;
    private final ContentInput input;

    CreateArtifactCommand(Supplier<Services> services, Supplier<InputStream> input) {
        this.services = services;
        this.input = new ContentInput(input);
    }

    @Override
    public String name() {
        return "create-artifact";
    }

    @Override
    public String description() {
        return "Crea un agente o una skill a partir de su plantilla";
    }

    @Override
    public List<OptionSpec> options() {
        List<OptionSpec> options = new ArrayList<>(List.of(
            OptionSpec.value(PROVIDER, true, "Proveedor: claude, codex, gemini u opencode"),
            OptionSpec.value(SCOPE, true, "Ámbito: global o project"),
            OptionSpec.value(KIND, true, "Tipo: agent o skill"),
            OptionSpec.value(NAME, true, "Nombre del artefacto en el catálogo"),
            ProjectOption.spec(false)));
        options.addAll(ContentInput.specs());
        options.add(OptionSpec.flag(OVERWRITE, "Sobrescribe un artefacto existente (se conserva una copia)"));
        return options;
    }

    @Override
    public Object execute(ParsedArgs args) {
        ArtifactScope scope = choice(args, SCOPE, ArtifactScope.class);
        ArtifactKind kind = choice(args, KIND, ArtifactKind.class);
        if(kind != ArtifactKind.AGENT && kind != ArtifactKind.SKILL)
            throw new UsageException(UsageError.INVALID_VALUE, "--" + KIND + " " + args.required(KIND));
        String name = args.required(NAME);
        String content = input.read(args, false).orElse(null);

        Services s = services.get();
        ProviderType type = provider(args.required(PROVIDER));
        Path project = ProjectOption.resolve(s, args);

        CreateArtifact.Created created;
        try {
            created = s.createArtifact().execute(type, scope, kind, name, project, content, args.flag(OVERWRITE));
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), name, e);
        }
        return new CreatedView(type.id(), id(scope), id(kind), name, Views.text(created.path()), created.replaced(),
            created.custom(), Views.text(created.backup()));
    }

    private static ProviderType provider(String raw) {
        try {
            return ProviderType.fromId(raw);
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), raw, e);
        }
    }

    private static <E extends Enum<E>> E choice(ParsedArgs args, String option, Class<E> type) {
        String raw = args.required(option);
        for(E value : type.getEnumConstants()) {
            if(id(value).equals(raw))
                return value;
        }
        throw new UsageException(UsageError.INVALID_VALUE, "--" + option + " " + raw);
    }

    private static String id(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
