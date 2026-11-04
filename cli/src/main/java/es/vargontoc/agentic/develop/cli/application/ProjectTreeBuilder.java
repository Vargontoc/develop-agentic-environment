package es.vargontoc.agentic.develop.cli.application;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.artifact.InstalledArtifact;
import es.vargontoc.agentic.develop.cli.domain.artifact.Inventory;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.team.ModuleEntry;
import es.vargontoc.agentic.develop.cli.domain.team.TeamInfo;
import es.vargontoc.agentic.develop.cli.domain.tree.ProjectTree;
import es.vargontoc.agentic.develop.cli.domain.tree.TreeNode;
import es.vargontoc.agentic.develop.cli.domain.tree.TreeNode.NodeType;

/**
 * Compone el árbol de los ámbitos global y de proyecto a partir del catálogo, el inventario y {@code team.md}.
 * Solo lee: no crea ni modifica nada.
 */
public class ProjectTreeBuilder {

    private final ProviderCatalog providers;
    private final ArtifactInventory inventory;
    private final ProjectWorkspace workspace;

    public ProjectTreeBuilder(ProviderCatalog providers, ArtifactInventory inventory, ProjectWorkspace workspace) {
        this.providers = providers;
        this.inventory = inventory;
        this.workspace = workspace;
    }

    /** @param project proyecto a incluir, o null para devolver solo el ámbito global */
    public ProjectTree build(Path project) {
        if(project != null)
            requireDirectory(project);

        List<Inventory.Problem> problems = new ArrayList<>();
        List<TreeNode> global = globalNodes(problems);

        if(project == null)
            return new ProjectTree(global, List.of(), List.of(), problems);

        TeamInfo team = workspace.readTeam(project);
        return new ProjectTree(global, projectNodes(project, team, problems), team.warnings(), problems);
    }

    private List<TreeNode> globalNodes(List<Inventory.Problem> problems) {
        List<TreeNode> nodes = new ArrayList<>();
        for(Provider provider : providers.availableProviders()) {
            Inventory inv = inventory.scanGlobal(provider.getType());
            problems.addAll(inv.problems());
            nodes.add(providerNode(provider.getType(), provider.getPath(), inv.artifacts()));
        }
        return nodes;
    }

    private List<TreeNode> projectNodes(Path project, TeamInfo team, List<Inventory.Problem> problems) {
        Map<ProviderType, Inventory> inventories = new EnumMap<>(ProviderType.class);
        for(ProviderType type : ProviderType.values()) {
            if(type.supportsProject()) {
                Inventory inv = inventory.scanProject(project, type, team.moduleNames());
                problems.addAll(inv.problems());
                inventories.put(type, inv);
            }
        }

        List<TreeNode> nodes = new ArrayList<>();
        Path sdd = project.resolve(ProjectWorkspace.SDD_DIR);
        nodes.add(new TreeNode(NodeType.SDD, ProjectWorkspace.SDD_DIR, sdd, null, null, !Files.exists(sdd), List.of()));

        for(ModuleEntry module : team.modules())
            nodes.add(moduleNode(project, module, inventories));

        inventories.forEach((type, inv) -> {
            if(Files.isDirectory(project.resolve(type.projectDir())))
                nodes.add(providerNode(type, project.resolve(type.projectDir()), providerLevel(inv)));
        });
        return nodes;
    }

    // Bajo el proveedor solo van lo compartido (skills y routers); los especialistas cuelgan de su módulo
    private static List<InstalledArtifact> providerLevel(Inventory inv) {
        return inv.artifacts().stream().filter(a -> a.entry().module() == null).toList();
    }

    private TreeNode moduleNode(Path project, ModuleEntry module, Map<ProviderType, Inventory> inventories) {
        List<TreeNode> technicians = new ArrayList<>();
        for(Technician technician : Technician.values()) {
            List<TreeNode> instances = new ArrayList<>();
            inventories.forEach((type, inv) -> inv.artifacts().stream()
                .filter(a -> module.name().equals(a.entry().module()) && a.entry().technician() == technician)
                .forEach(a -> instances.add(artifactNode(a))));
            technicians.add(new TreeNode(NodeType.TECHNICIAN, technician.slug(), null, null, null, instances.isEmpty(), instances));
        }
        return new TreeNode(NodeType.MODULE, module.name(), null, null, null, false, technicians);
    }

    private TreeNode providerNode(ProviderType type, Path path, List<InstalledArtifact> artifacts) {
        return new TreeNode(NodeType.PROVIDER, type.id(), path, type, null, false,
            artifacts.stream().map(ProjectTreeBuilder::artifactNode).toList());
    }

    private static TreeNode artifactNode(InstalledArtifact artifact) {
        NodeType type = switch(artifact.entry().kind()) {
            case AGENT -> NodeType.AGENT;
            case SKILL -> NodeType.SKILL;
            case PROFILE -> NodeType.PROFILE;
        };
        return new TreeNode(type, artifact.entry().name(), artifact.path(), artifact.entry().provider(),
            artifact.status(), false, List.of());
    }

    private static void requireDirectory(Path project) {
        if(!Files.exists(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_FOUND, project);
        if(!Files.isDirectory(project))
            throw new PathDomainException(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, project);
    }
}
