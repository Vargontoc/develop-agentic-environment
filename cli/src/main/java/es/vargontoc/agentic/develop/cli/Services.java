package es.vargontoc.agentic.develop.cli;

import java.nio.file.Path;
import java.util.List;

import es.vargontoc.agentic.develop.cli.application.ArtifactDescriptor;
import es.vargontoc.agentic.develop.cli.application.ArtifactInventory;
import es.vargontoc.agentic.develop.cli.application.ArtifactSaver;
import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.application.CreateModule;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator;
import es.vargontoc.agentic.develop.cli.application.InitProject;
import es.vargontoc.agentic.develop.cli.application.ProjectTreeBuilder;
import es.vargontoc.agentic.develop.cli.application.ProjectWorkspace;
import es.vargontoc.agentic.develop.cli.application.StatusService;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactCatalog;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.providers.LocalProviderCatalog;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

/**
 * Raíz de composición: los servicios de aplicación ya montados y compartidos por los dos adaptadores de entrada
 * (menú interactivo y comandos). Ninguno imprime ni termina el proceso; devuelven resultados o lanzan errores tipados.
 */
public record Services(
        PathResolver paths,
        ProviderCatalog providers,
        ArtifactCatalog artifacts,
        ProjectWorkspace workspace,
        InitProject initProject,
        StatusService status,
        ProjectTreeBuilder tree,
        ArtifactDescriptor descriptor,
        ArtifactSaver saver,
        GroupPropagator propagator,
        ConfigureProvider configureProvider,
        CreateModule createModule) {

    public static Services system() {
        return of(PathResolver.system());
    }

    public static Services of(PathResolver paths) {
        Path appHome = paths.appHome();
        Path backups = appHome.resolve("backups");

        ArtifactCatalog artifacts = new ResourceArtifactCatalog();
        ProviderCatalog providers = new LocalProviderCatalog(paths, artifacts);
        MetadataStore metadata = new MetadataStore(appHome.resolve("metadata.json"),
            new TextStore(paths, List.of(appHome), backups));
        ProjectWorkspace workspace = new ProjectWorkspace(paths, backups);
        ArtifactInventory inventory = new ArtifactInventory(paths, artifacts, metadata, backups);
        ArtifactDescriptor descriptor = new ArtifactDescriptor(paths, artifacts, inventory, metadata, workspace, backups);
        ArtifactSaver saver = new ArtifactSaver(paths, inventory, metadata, backups);

        return new Services(paths, providers, artifacts, workspace,
            new InitProject(paths, workspace),
            new StatusService(providers, workspace),
            new ProjectTreeBuilder(providers, inventory, workspace),
            descriptor,
            saver,
            new GroupPropagator(metadata, descriptor, saver),
            new ConfigureProvider(),
            new CreateModule());
    }
}
