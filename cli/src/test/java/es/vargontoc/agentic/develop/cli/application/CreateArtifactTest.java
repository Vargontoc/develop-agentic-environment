package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.DomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.Technician;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class CreateArtifactTest {

    @TempDir
    Path tmp;

    Path home;
    Path project;
    MetadataStore metadata;
    ArtifactInventory inventory;
    CreateArtifact create;
    CreateModule createModule;

    @BeforeEach
    void setUp() throws IOException {
        home = Files.createDirectories(tmp.resolve("home"));
        project = Files.createDirectories(tmp.resolve("proyecto"));
        PathResolver paths = new PathResolver(Map.of("HOME", home.toString()), PathResolver.Os.LINUX);
        Path backups = tmp.resolve("backups");
        ProjectWorkspace workspace = new ProjectWorkspace(paths, backups);
        workspace.initialize(project);

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(tmp.resolve("app-home/metadata.json"), new TextStore(paths, List.of(tmp), backups));
        inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        create = new CreateArtifact(TargetOs.LINUX, paths, catalog, inventory, metadata, workspace, backups);
        createModule = new CreateModule(TargetOs.LINUX, paths, catalog, inventory, metadata, backups);
    }

    private Path claudeAgent(String name) {
        return home.resolve(".claude/agents/" + name + ".md");
    }

    @Test
    void templateAsIsProducesTheTemplateContentAndIsTracked() throws IOException {
        CreateArtifact.Created created = create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.AGENT,
            "sdd-design", null, null, false);

        assertEquals(claudeAgent("sdd-design"), created.path().toAbsolutePath().normalize());
        assertFalse(created.replaced());
        assertFalse(created.custom());
        String body = ArtifactFormat.MARKDOWN.split(Files.readString(created.path())).body();
        assertEquals(ClasspathResources.readText("/global/agents/shared/sdd-design-body.md").orElseThrow(), body);
        var record = metadata.artifact(created.path().toAbsolutePath().normalize()).orElseThrow();
        assertEquals("global/agent/sdd-design", record.groupId());
        assertEquals(ArtifactStatus.LINKED, inventory.scanGlobal(ProviderType.CLAUDE).artifacts().stream()
            .filter(a -> a.entry().name().equals("sdd-design")).findFirst().orElseThrow().status());
    }

    @Test
    void modifiedContentReplacesTheBodyButKeepsTheTemplateHeader() throws IOException {
        CreateArtifact.Created created = create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.AGENT,
            "sdd-design", null, "Mi cuerpo propio\n", false);

        assertTrue(created.custom());
        var parts = ArtifactFormat.MARKDOWN.split(Files.readString(created.path()));
        assertEquals("Mi cuerpo propio\n", parts.body());
        assertTrue(parts.header().contains("name: sdd-design"));
        assertTrue(parts.header().contains("shell: bash"));
    }

    @Test
    void anExistingArtifactIsNotOverwrittenWithoutConfirmation() throws IOException {
        create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.AGENT, "sdd-design", null, "uno\n", false);
        String before = Files.readString(claudeAgent("sdd-design"));

        DomainException e = assertThrows(DomainException.class, () -> create.execute(ProviderType.CLAUDE,
            ArtifactScope.GLOBAL, ArtifactKind.AGENT, "sdd-design", null, "dos\n", false));

        assertEquals(AppErrorCode.ARTIFACT_EXISTS, e.code());
        assertEquals(before, Files.readString(claudeAgent("sdd-design")));
    }

    @Test
    void confirmedOverwriteReplacesAndKeepsABackup() throws IOException {
        create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.AGENT, "sdd-design", null, "uno\n", false);

        CreateArtifact.Created again = create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.AGENT,
            "sdd-design", null, "dos\n", true);

        assertTrue(again.replaced());
        assertNotNull(again.backup());
        assertTrue(Files.readString(again.backup()).contains("uno"));
        assertEquals("dos\n", ArtifactFormat.MARKDOWN.split(Files.readString(claudeAgent("sdd-design"))).body());
    }

    @Test
    void aSkillIsCopiedAsADirectoryAndItsSkillMdCanBeCustomised() throws IOException {
        CreateArtifact.Created plain = create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.SKILL,
            "sdd-design", null, null, false);
        assertTrue(Files.isRegularFile(plain.path().resolve("SKILL.md")));
        assertNull(plain.backup());

        Path other = home.resolve(".claude/skills/sdd-requirements");
        CreateArtifact.Created custom = create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.SKILL,
            "sdd-requirements", null, "# Propia\n", false);

        assertEquals("# Propia\n", Files.readString(other.resolve("SKILL.md")).replace("\r\n", "\n"));
        assertTrue(custom.custom());
        assertTrue(metadata.artifact(other.toAbsolutePath().normalize()).isPresent());
    }

    @Test
    void anExistingSkillIsAlsoProtected() throws IOException {
        create.execute(ProviderType.CLAUDE, ArtifactScope.GLOBAL, ArtifactKind.SKILL, "sdd-design", null, null, false);

        DomainException e = assertThrows(DomainException.class, () -> create.execute(ProviderType.CLAUDE,
            ArtifactScope.GLOBAL, ArtifactKind.SKILL, "sdd-design", null, "x", false));

        assertEquals(AppErrorCode.ARTIFACT_EXISTS, e.code());
    }

    @Test
    void aProjectSpecialistIsCreatedForARegisteredModule() throws IOException {
        createModule.execute(project, "alfa", List.of(Technician.ANALYST), List.of(new Provider(ProviderType.CLAUDE, Path.of("x"))));
        Files.delete(project.resolve(".claude/agents/desarrollador-alfa.md".replace("desarrollador", "analista")));

        CreateArtifact.Created created = create.execute(ProviderType.CLAUDE, ArtifactScope.PROJECT, ArtifactKind.AGENT,
            "analista-alfa", project, null, false);

        String content = Files.readString(created.path());
        assertFalse(content.contains("{module}"));
        assertTrue(content.contains("alfa"));
    }

    @Test
    void anArtifactOutsideTheCatalogOrWithAnInvalidScopeIsAnError() {
        DomainException unknown = assertThrows(DomainException.class, () -> create.execute(ProviderType.CLAUDE,
            ArtifactScope.GLOBAL, ArtifactKind.AGENT, "no-existe", null, null, false));
        assertEquals(AppErrorCode.ARTIFACT_NOT_CATALOGUED, unknown.code());

        DomainException noProject = assertThrows(DomainException.class, () -> create.execute(ProviderType.CLAUDE,
            ArtifactScope.PROJECT, ArtifactKind.AGENT, "analista-router", null, null, false));
        assertEquals(AppErrorCode.MODULE_NO_PROJECT, noProject.code());

        DomainException unregistered = assertThrows(DomainException.class, () -> create.execute(ProviderType.CLAUDE,
            ArtifactScope.PROJECT, ArtifactKind.AGENT, "analista-beta", project, null, false));
        assertEquals(AppErrorCode.ARTIFACT_NOT_CATALOGUED, unregistered.code());
        assertFalse(Files.exists(project.resolve(".claude/agents/analista-beta.md")));
    }
}
