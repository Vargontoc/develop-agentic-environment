package es.vargontoc.agentic.develop.cli.command;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.CreateArtifact;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;

/**
 * {@code catalog --provider <id> --scope global|project --kind agent|skill [--name <nombre>] [--project <ruta>]}:
 * lo que se puede crear con {@code create-artifact}. Sin {@code --name} lista las entradas; con {@code --name}
 * devuelve esa entrada con su plantilla compuesta (cabecera y cuerpo de un agente, ficheros de una skill), exactamente
 * la que escribiría {@code create-artifact} sin contenido. Solo lee: no crea nada.
 */
final class CatalogCommand implements Command {

    private static final String PROVIDER = "provider";
    private static final String SCOPE = "scope";
    private static final String KIND = "kind";
    private static final String NAME = "name";

    record FileView(String path, String content) {}

    record TemplateView(String header, String body, String content, List<FileView> files) {}

    record EntryView(String name, String relativePath, String path, String module, String technician, boolean exists,
                     TemplateView template) {}

    record CatalogView(String provider, String scope, String kind, List<EntryView> entries) {}

    private final Supplier<Services> services;

    CatalogCommand(Supplier<Services> services) {
        this.services = services;
    }

    @Override
    public String name() {
        return "catalog";
    }

    @Override
    public String description() {
        return "Lista lo que se puede crear y devuelve su plantilla compuesta";
    }

    @Override
    public List<OptionSpec> options() {
        return List.of(
            OptionSpec.value(PROVIDER, true, "Proveedor: claude, codex, gemini u opencode"),
            OptionSpec.value(SCOPE, true, "Ámbito: global o project"),
            OptionSpec.value(KIND, true, "Tipo: agent o skill"),
            OptionSpec.value(NAME, false, "Nombre de la entrada, para obtener su plantilla"),
            ProjectOption.spec(false));
    }

    @Override
    public Object execute(ParsedArgs args) {
        ArtifactScope scope = EnumOption.of(args, SCOPE, ArtifactScope.class);
        ArtifactKind kind = EnumOption.of(args, KIND, ArtifactKind.class);
        if(kind != ArtifactKind.AGENT && kind != ArtifactKind.SKILL)
            throw new UsageException(UsageError.INVALID_VALUE, "--" + KIND + " " + args.required(KIND));

        ProviderType type = EnumOption.provider(args.required(PROVIDER));
        Services s = services.get();
        Path project = ProjectOption.resolve(s, args);
        String name = args.optional(NAME).orElse(null);

        List<CreateArtifact.CatalogItem> items;
        try {
            items = name == null
                ? s.createArtifact().catalog(type, scope, kind, project)
                : List.of(s.createArtifact().template(type, scope, kind, name, project));
        } catch(PathDomainException e) {
            throw e;
        } catch(DomainException e) {
            throw new PathDomainException(e.code(), name == null ? type.id() : name, e);
        }
        return new CatalogView(type.id(), EnumOption.id(scope), EnumOption.id(kind), items.stream().map(CatalogCommand::of).toList());
    }

    private static EntryView of(CreateArtifact.CatalogItem item) {
        var entry = item.entry();
        CreateArtifact.Template template = item.template();
        TemplateView view = template == null ? null : new TemplateView(template.header(), template.body(), template.content(),
            template.files().stream().map(f -> new FileView(f.path(), f.content())).toList());
        return new EntryView(entry.name(), entry.relativePath(), Views.text(item.path()), entry.module(),
            entry.technician() == null ? null : entry.technician().slug(), item.exists(), view);
    }
}
