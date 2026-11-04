package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.application.ArtifactSaver.SkillSaveResult;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator.Member;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator.Outcome;
import es.vargontoc.agentic.develop.cli.application.GroupPropagator.Result;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

/** Skills como directorio completo: edición de ficheros y propagación entre copias. */
class SkillDirectoryTest {

    @TempDir
    Path tmp;

    PathResolver paths;
    Path project;
    ArtifactInventory inventory;
    MetadataStore metadata;
    ArtifactDescriptor descriptor;
    ArtifactSaver saver;
    GroupPropagator propagator;

    @BeforeEach
    void setUp() throws IOException {
        paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path appHome = Files.createDirectories(tmp.resolve("app-home"));
        Path backups = appHome.resolve("backups");
        project = Files.createDirectories(tmp.resolve("proyecto"));

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(appHome.resolve("metadata.json"), new TextStore(paths, List.of(appHome), backups));
        inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        descriptor = new ArtifactDescriptor(paths, catalog, inventory, metadata, new ProjectWorkspace(paths, backups), backups);
        saver = new ArtifactSaver(paths, inventory, metadata, backups);
        propagator = new GroupPropagator(metadata, descriptor, saver);
    }

    private Path skill(ProviderType provider, String... filesAndContents) throws IOException {
        Path dir = paths.providerHome(provider).resolve("skills/sdd-design");
        for(int i = 0; i < filesAndContents.length; i += 2) {
            Path file = dir.resolve(filesAndContents[i]);
            Files.createDirectories(file.getParent());
            Files.writeString(file, filesAndContents[i + 1]);
        }
        return dir;
    }

    private ArtifactLocation at(Path dir) {
        return descriptor.resolve(dir.toString(), null);
    }

    private void track(String group, Path dir) {
        metadata.putArtifactHash(dir, group, inventory.inspect(at(dir).entry(), at(dir).root()).hash(), LinkState.LINKED);
    }

    // --- edición de una skill ---

    @Test
    void editingOneFileWritesItAndLeavesTheRestAlone() throws IOException {
        Path dir = skill(ProviderType.CLAUDE, "SKILL.md", "uno\n", "assets/plantilla.md", "apoyo\n");
        Files.write(dir.resolve("assets/logo.bin"), new byte[] {0, (byte) 0xFF, 1});
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        track("sdd-design", dir);

        SkillSaveResult result = saver.saveSkillFiles(at(dir), Map.of("SKILL.md", "dos\n"), false);

        assertTrue(result.written());
        assertTrue(result.tracked());
        assertEquals("dos\n", Files.readString(dir.resolve("SKILL.md")));
        assertEquals("apoyo\n", Files.readString(dir.resolve("assets/plantilla.md")));
        assertArrayEquals(new byte[] {0, (byte) 0xFF, 1}, Files.readAllBytes(dir.resolve("assets/logo.bin")));
        assertEquals(Map.of("SKILL.md", "-uno\n+dos".replace("\n+", "\n+")), result.diffs());
        assertEquals(1, result.backups().size());
        // El hash registrado sigue al directorio, así que no queda como modificado fuera de la aplicación
        assertFalse(descriptor.describe(dir.toString(), null).modifiedExternally());
    }

    @Test
    void aNewAuxiliaryFileIsCreatedInsideTheSkill() throws IOException {
        Path dir = skill(ProviderType.CLAUDE, "SKILL.md", "uno\n");

        SkillSaveResult result = saver.saveSkillFiles(at(dir), Map.of("assets/nuevo.md", "hola\n"), false);

        assertTrue(result.written());
        assertFalse(result.tracked());
        assertEquals("hola\n", Files.readString(dir.resolve("assets/nuevo.md")));
        assertEquals(List.of(), result.backups());
    }

    @Test
    void savingTheSameContentWritesNothing() throws IOException {
        Path dir = skill(ProviderType.CLAUDE, "SKILL.md", "uno\n");

        SkillSaveResult result = saver.saveSkillFiles(at(dir), Map.of("SKILL.md", "uno\n"), false);

        assertFalse(result.written());
        assertEquals(Map.of(), result.diffs());
    }

    @Test
    void filesOutsideTheSkillAreRejectedBeforeWritingAnything() throws IOException {
        Path dir = skill(ProviderType.CLAUDE, "SKILL.md", "uno\n");
        ArtifactLocation location = at(dir);

        for(String bad : List.of("../otra/SKILL.md", "/etc/passwd", "..", "", "assets/../../fuera.md")) {
            PathDomainException e = assertThrows(PathDomainException.class,
                () -> saver.saveSkillFiles(location, Map.of("SKILL.md", "cambiado\n", bad, "x"), false), bad);
            assertEquals(AppErrorCode.PATH_OUT_OF_SCOPE, e.code(), bad);
        }
        assertEquals("uno\n", Files.readString(dir.resolve("SKILL.md")));
        assertFalse(Files.exists(dir.resolveSibling("otra")));
    }

    @Test
    void anExternalChangeToTheSkillIsAConflictUnlessOverwritten() throws IOException {
        Path dir = skill(ProviderType.CLAUDE, "SKILL.md", "uno\n");
        track("sdd-design", dir);
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        Files.writeString(dir.resolve("SKILL.md"), "otra herramienta\n");

        PathDomainException e = assertThrows(PathDomainException.class,
            () -> saver.saveSkillFiles(at(dir), Map.of("SKILL.md", "dos\n"), false));
        assertEquals(AppErrorCode.ARTIFACT_CONFLICT, e.code());
        assertEquals("otra herramienta\n", Files.readString(dir.resolve("SKILL.md")));

        SkillSaveResult forced = saver.saveSkillFiles(at(dir), Map.of("SKILL.md", "dos\n"), true);
        assertTrue(forced.written());
        assertEquals("otra herramienta\n", Files.readString(forced.backups().get(0)));
    }

    @Test
    void anAgentIsNotASkillAndAMissingSkillIsAnError() throws IOException {
        Path agent = paths.providerHome(ProviderType.CLAUDE).resolve("agents/sdd-agent.md");
        Files.createDirectories(agent.getParent());
        Files.writeString(agent, "---\nname: x\n---\n\ncuerpo\n");
        Path missing = paths.providerHome(ProviderType.CLAUDE).resolve("skills/sdd-design");

        assertEquals(AppErrorCode.ARTIFACT_KIND_MISMATCH, assertThrows(PathDomainException.class,
            () -> saver.saveSkillFiles(at(agent), Map.of("x.md", "x"), false)).code());
        assertEquals(AppErrorCode.ARTIFACT_NOT_FOUND, assertThrows(PathDomainException.class, () -> at(missing)).code());
    }

    // --- propagación ---

    @Test
    void propagationUpdatesExistingCopiesAndKeepsEachProvidersOwnFiles() throws IOException {
        Path claude = skill(ProviderType.CLAUDE, "SKILL.md", "viejo\n", "assets/solo-claude.md", "mio\n");
        Path gemini = skill(ProviderType.GEMINI, "SKILL.md", "viejo\n", "assets/solo-gemini.md", "suyo\n");
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        track("sdd-design", claude);
        track("sdd-design", gemini);

        Result result = propagator.propagateFiles("sdd-design", Map.of("SKILL.md", "nuevo\n"), null, false);

        assertEquals(List.of(Outcome.UPDATED, Outcome.UPDATED), result.members().stream().map(Member::outcome).toList());
        assertEquals("nuevo\n", Files.readString(claude.resolve("SKILL.md")));
        assertEquals("nuevo\n", Files.readString(gemini.resolve("SKILL.md")));
        assertEquals("mio\n", Files.readString(claude.resolve("assets/solo-claude.md")));
        assertEquals("suyo\n", Files.readString(gemini.resolve("assets/solo-gemini.md")));
        assertFalse(Files.exists(claude.resolve("assets/solo-gemini.md")));
        assertTrue(result.members().get(0).diff().startsWith("@@ SKILL.md\n-viejo"));
    }

    @Test
    void aProviderWithoutTheSkillIsSkippedAndNeverCreated() throws IOException {
        Path claude = skill(ProviderType.CLAUDE, "SKILL.md", "viejo\n");
        Path gemini = skill(ProviderType.GEMINI, "SKILL.md", "viejo\n");
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        track("sdd-design", claude);
        track("sdd-design", gemini);
        try(var files = Files.walk(gemini)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
        }

        Result result = propagator.propagateFiles("sdd-design", Map.of("SKILL.md", "nuevo\n"), null, false);

        assertEquals(Outcome.MISSING, result.members().stream().filter(m -> m.path().equals(gemini)).findFirst().orElseThrow().outcome());
        assertFalse(Files.exists(gemini));
    }

    @Test
    void aGlobalSkillIsNeverPropagatedToTheProjectAndViceVersa() throws IOException {
        Path global = skill(ProviderType.CLAUDE, "SKILL.md", "viejo\n");
        Path inProject = project.resolve(".claude/skills/team-changes");
        Files.createDirectories(inProject);
        Files.writeString(inProject.resolve("SKILL.md"), "viejo proyecto\n");
        Files.createDirectories(project.resolve(".sdd"));
        Files.writeString(project.resolve(".sdd/team.md"), "## Módulos\n\n");
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        track("sdd-design", global);
        metadata.putArtifactHash(inProject, "sdd-design", "x", LinkState.LINKED);

        Result result = propagator.propagateFiles("sdd-design", Map.of("SKILL.md", "nuevo\n"), project, false);

        Member projectCopy = result.members().stream().filter(m -> m.path().equals(inProject)).findFirst().orElseThrow();
        assertEquals(Outcome.OUT_OF_SCOPE, projectCopy.outcome());
        assertEquals("viejo proyecto\n", Files.readString(inProject.resolve("SKILL.md")));
        assertEquals("nuevo\n", Files.readString(global.resolve("SKILL.md")));
    }

    @Test
    void anUnchangedPropagationWritesNothing() throws IOException {
        Path claude = skill(ProviderType.CLAUDE, "SKILL.md", "igual\n");
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        track("sdd-design", claude);

        Result result = propagator.propagateFiles("sdd-design", Map.of("SKILL.md", "igual\n"), null, false);

        assertFalse(result.changed());
        assertEquals(Outcome.UNCHANGED, result.members().get(0).outcome());
        assertEquals("", result.members().get(0).diff());
    }

    @Test
    void aConflictingCopyIsSkippedAndTheOthersAreUpdated() throws IOException {
        Path claude = skill(ProviderType.CLAUDE, "SKILL.md", "viejo\n");
        Path gemini = skill(ProviderType.GEMINI, "SKILL.md", "viejo\n");
        metadata.putGroup(new GroupRecord("sdd-design", ArtifactScope.GLOBAL, ""));
        track("sdd-design", claude);
        track("sdd-design", gemini);
        Files.writeString(gemini.resolve("SKILL.md"), "editado fuera\n");

        Result result = propagator.propagateFiles("sdd-design", Map.of("SKILL.md", "nuevo\n"), null, false);

        assertEquals(Outcome.CONFLICT, result.members().stream().filter(m -> m.path().equals(gemini)).findFirst().orElseThrow().outcome());
        assertEquals("editado fuera\n", Files.readString(gemini.resolve("SKILL.md")));
        assertEquals("nuevo\n", Files.readString(claude.resolve("SKILL.md")));
    }
}
