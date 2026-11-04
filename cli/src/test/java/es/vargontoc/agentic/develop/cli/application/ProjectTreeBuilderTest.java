package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.team.TeamWarning;
import es.vargontoc.agentic.develop.cli.domain.tree.ProjectTree;
import es.vargontoc.agentic.develop.cli.domain.tree.TreeNode;
import es.vargontoc.agentic.develop.cli.domain.tree.TreeNode.NodeType;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.providers.LocalProviderCatalog;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class ProjectTreeBuilderTest {

    @TempDir
    Path tmp;

    Path project;
    ProjectTreeBuilder builder;

    @BeforeEach
    void setUp() throws IOException {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path appHome = Files.createDirectories(tmp.resolve("app-home"));
        Path backups = appHome.resolve("backups");
        project = Files.createDirectories(tmp.resolve("proyecto"));

        var catalog = new ResourceArtifactCatalog();
        var metadata = new MetadataStore(appHome.resolve("metadata.json"), new TextStore(paths, List.of(appHome), backups));
        var inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        builder = new ProjectTreeBuilder(new LocalProviderCatalog(paths, catalog), inventory, new ProjectWorkspace(paths, backups));
    }

    private Path file(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content);
    }

    private void team(String modules) throws IOException {
        file(project.resolve(".sdd/team.md"), "## Módulos\n\n" + modules + "\n## Opciones\n");
    }

    private static TreeNode child(List<TreeNode> nodes, NodeType type, String name) {
        return nodes.stream().filter(n -> n.type() == type && n.name().equals(name)).findFirst().orElseThrow();
    }

    private static int count(List<TreeNode> nodes, String name) {
        int total = 0;
        for(TreeNode node : nodes)
            total += (node.name().equals(name) ? 1 : 0) + count(node.children(), name);
        return total;
    }

    // --- ámbito global ---

    @Test
    void globalTreeListsInstalledProvidersWithTheirApplicationArtifacts() throws IOException {
        file(tmp.resolve(".claude/agents/sdd-agent.md"), "x");
        file(tmp.resolve(".claude/agents/mi-agente-propio.md"), "mio");
        file(tmp.resolve(".claude/skills/sdd-design/SKILL.md"), "s");
        file(tmp.resolve(".codex/agents/sdd_agent.toml"), "x");

        ProjectTree tree = builder.build(null);

        assertEquals(List.of("claude", "codex"), tree.global().stream().map(TreeNode::name).toList());
        TreeNode claude = child(tree.global(), NodeType.PROVIDER, "claude");
        assertEquals(List.of("sdd-agent", "sdd-design"), claude.children().stream().map(TreeNode::name).toList());
        assertEquals(NodeType.SKILL, child(claude.children(), NodeType.SKILL, "sdd-design").type());
        assertTrue(tree.project().isEmpty());
    }

    @Test
    void globalTreeIsEmptyWhenNoProviderIsInstalled() {
        ProjectTree tree = builder.build(null);

        assertTrue(tree.global().isEmpty());
        assertTrue(tree.warnings().isEmpty());
    }

    // --- ámbito de proyecto ---

    @Test
    void projectTreeShowsModulesTechniciansAndSharedRoutersOnlyOnce() throws IOException {
        team("- alfa | - | -\n- beta | - | -\n");
        file(project.resolve(".claude/agents/analista-router.md"), "r");
        file(project.resolve(".claude/agents/analista-alfa.md"), "a");
        file(project.resolve(".claude/agents/desarrollador-alfa.md"), "d");
        file(project.resolve(".claude/agents/analista-beta.md"), "a");
        file(project.resolve(".claude/skills/team-sprints/SKILL.md"), "s");

        ProjectTree tree = builder.build(project);

        List<TreeNode> nodes = tree.project();
        assertEquals(List.of(NodeType.SDD, NodeType.MODULE, NodeType.MODULE, NodeType.PROVIDER),
            nodes.stream().map(TreeNode::type).toList());
        assertEquals(1, count(nodes, "analista-router"));
        TreeNode claude = child(nodes, NodeType.PROVIDER, "claude");
        assertEquals(List.of("team-sprints", "analista-router"), claude.children().stream().map(TreeNode::name).toList());

        TreeNode alfa = child(nodes, NodeType.MODULE, "alfa");
        assertFalse(child(alfa.children(), NodeType.TECHNICIAN, "analista").pending());
        assertFalse(child(alfa.children(), NodeType.TECHNICIAN, "desarrollador").pending());
        assertTrue(child(alfa.children(), NodeType.TECHNICIAN, "documentador").pending());
        TreeNode beta = child(nodes, NodeType.MODULE, "beta");
        assertFalse(child(beta.children(), NodeType.TECHNICIAN, "analista").pending());
        assertTrue(child(beta.children(), NodeType.TECHNICIAN, "desarrollador").pending());
    }

    @Test
    void existingTechnicianListsItsArtifactWithItsProvider() throws IOException {
        team("- alfa | - | -\n");
        file(project.resolve(".claude/agents/analista-alfa.md"), "a");
        // Aún no hay cabeceras de proyecto para OpenCode, así que no es un artefacto de la aplicación
        file(project.resolve(".opencode/agents/analista-alfa.md"), "a");

        TreeNode alfa = child(builder.build(project).project(), NodeType.MODULE, "alfa");
        TreeNode analyst = child(alfa.children(), NodeType.TECHNICIAN, "analista");

        assertEquals(List.of(ProviderType.CLAUDE), analyst.children().stream().map(TreeNode::provider).toList());
    }

    @Test
    void moduleWithoutTechniciansIsVisibleWithEveryTechnicianPending() throws IOException {
        team("- vacio | - | -\n");

        TreeNode module = child(builder.build(project).project(), NodeType.MODULE, "vacio");

        assertEquals(3, module.children().size());
        assertTrue(module.children().stream().allMatch(TreeNode::pending));
        assertTrue(module.children().stream().allMatch(t -> t.children().isEmpty()));
    }

    @Test
    void specialistsOfUnregisteredModulesDoNotAppear() throws IOException {
        team("- alfa | - | -\n");
        file(project.resolve(".claude/agents/analista-ajeno.md"), "a");

        assertEquals(0, count(builder.build(project).project(), "analista-ajeno"));
    }

    @Test
    void projectWithoutSddMarksItPending() {
        ProjectTree tree = builder.build(project);

        TreeNode sdd = child(tree.project(), NodeType.SDD, ".sdd");
        assertTrue(sdd.pending());
        assertEquals(List.of(new TeamWarning(TeamWarning.Type.TEAM_MISSING, 0)), tree.warnings());
    }

    @Test
    void malformedTeamFileGivesWarningsAndAPartialTree() throws IOException {
        team("- alfa | - | -\nbasura sin formato\n- alfa | - | -\n- beta | - | -\n");

        ProjectTree tree = builder.build(project);

        assertEquals(List.of("alfa", "beta"),
            tree.project().stream().filter(n -> n.type() == NodeType.MODULE).map(TreeNode::name).toList());
        assertEquals(2, tree.warnings().size());
        assertFalse(child(tree.project(), NodeType.SDD, ".sdd").pending());
    }

    @Test
    void unreadableArtifactsAreReportedAsProblemsWithoutBreakingTheTree() throws IOException {
        team("- alfa | - | -\n");
        Path broken = project.resolve(".claude/agents/analista-alfa.md");
        file(broken, "x");
        Files.write(broken, new byte[] {(byte) 0xC3, (byte) 0x28});

        ProjectTree tree = builder.build(project);

        assertEquals(1, tree.problems().size());
        assertEquals(broken, tree.problems().get(0).path());
        assertTrue(child(child(tree.project(), NodeType.MODULE, "alfa").children(), NodeType.TECHNICIAN, "analista").pending());
    }

    @Test
    void missingOrNonDirectoryProjectIsAnErrorWithItsPath() throws IOException {
        Path file = file(tmp.resolve("fichero"), "x");
        Path missing = tmp.resolve("nada");

        PathDomainException notFound = assertThrows(PathDomainException.class, () -> builder.build(missing));
        PathDomainException notDir = assertThrows(PathDomainException.class, () -> builder.build(file));

        assertEquals(AppErrorCode.PROJECT_PATH_NOT_FOUND, notFound.code());
        assertEquals(missing.toString(), notFound.location());
        assertEquals(AppErrorCode.PROJECT_PATH_NOT_DIRECTORY, notDir.code());
    }

    @Test
    void treeIsDeterministicAndDoesNotWriteAnything() throws IOException {
        team("- alfa | - | -\n");
        file(project.resolve(".claude/agents/analista-alfa.md"), "a");
        List<Path> before = new ArrayList<>(listAll(tmp));

        ProjectTree first = builder.build(project);
        ProjectTree second = builder.build(project);

        assertEquals(first, second);
        assertEquals(before, listAll(tmp));
    }

    private static List<Path> listAll(Path root) throws IOException {
        try(var files = Files.walk(root)) {
            return files.sorted().toList();
        }
    }
}
