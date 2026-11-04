package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactDescription;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactKind;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactScope;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.errors.AppErrorCode;
import es.vargontoc.agentic.develop.cli.domain.errors.PathDomainException;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.domain.metadata.GroupRecord;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;

class ArtifactDescriptorTest {

    private static final String MD = "---\nname: sdd-agent\n# nota\n---\n\nEres el agente.\n";

    @TempDir
    Path tmp;

    Path claude;
    Path project;
    MetadataStore metadata;
    ArtifactDescriptor descriptor;

    @BeforeEach
    void setUp() throws IOException {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path appHome = Files.createDirectories(tmp.resolve("app-home"));
        Path backups = appHome.resolve("backups");
        claude = Files.createDirectories(tmp.resolve(".claude"));
        project = Files.createDirectories(tmp.resolve("proyecto"));

        var catalog = new ResourceArtifactCatalog();
        metadata = new MetadataStore(appHome.resolve("metadata.json"), new TextStore(paths, List.of(appHome), backups));
        var inventory = new ArtifactInventory(paths, catalog, metadata, backups);
        descriptor = new ArtifactDescriptor(paths, catalog, inventory, metadata, new ProjectWorkspace(paths, backups), backups);
    }

    private Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    private static PathDomainException failure(Runnable action) {
        return assertThrows(PathDomainException.class, action::run);
    }

    @Test
    void markdownAgentReturnsHeaderAndBodySeparated() throws IOException {
        Path agent = write(claude.resolve("agents/sdd-agent.md"), MD);

        ArtifactDescription d = descriptor.describe(agent.toString(), null);

        assertEquals(ProviderType.CLAUDE, d.provider());
        assertEquals(ArtifactScope.GLOBAL, d.scope());
        assertEquals(ArtifactKind.AGENT, d.kind());
        assertEquals(ArtifactFormat.MARKDOWN, d.format());
        assertEquals("---\nname: sdd-agent\n# nota\n---\n\n", d.header());
        assertEquals("Eres el agente.\n", d.body());
        assertEquals(ArtifactStatus.LEGACY, d.status());
        assertNull(d.sharedBody());
        assertEquals(64, d.hash().length());
    }

    @Test
    void codexTomlAgentReturnsHeaderAndBodyThatCanBeRecomposed() throws IOException {
        Path codex = Files.createDirectories(tmp.resolve(".codex"));
        String toml = "name = \"sdd_agent\"\ndeveloper_instructions = \"\"\"Cuerpo codex\"\"\"";
        Path agent = write(codex.resolve("agents/sdd_agent.toml"), toml);

        ArtifactDescription d = descriptor.describe(agent.toString(), null);

        assertEquals(ArtifactFormat.CODEX_TOML, d.format());
        assertEquals("Cuerpo codex", d.body());
        assertEquals(toml, d.format().compose(new es.vargontoc.agentic.develop.cli.domain.format.ArtifactParts(d.header(), d.body())));
    }

    @Test
    void projectSpecialistExposesItsModuleAndThePlaceholderInTheSharedBody() throws IOException {
        write(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");
        Path agent = write(project.resolve(".claude/agents/analista-alfa.md"), "---\nname: analista-alfa\n---\n\nEres analista-alfa.\n");
        metadata.putGroup(new GroupRecord("project:analista", ArtifactScope.PROJECT, "Eres analista-{module}."));
        metadata.putArtifact(agent, "project:analista", "---\nname: analista-alfa\n---\n\nEres analista-alfa.\n", LinkState.LINKED);

        ArtifactDescription d = descriptor.describe(agent.toString(), project);

        assertEquals(ArtifactScope.PROJECT, d.scope());
        assertEquals("alfa", d.module());
        assertEquals("Eres analista-alfa.\n", d.body());
        assertEquals("Eres analista-{module}.", d.sharedBody());
        assertEquals(ArtifactStatus.LINKED, d.status());
    }

    @Test
    void detachedArtifactSaysSo() throws IOException {
        Path agent = write(claude.resolve("agents/sdd-agent.md"), MD);
        metadata.putArtifact(agent, "global:sdd-agent", MD, LinkState.LINKED);
        metadata.markDetached(agent);

        assertEquals(ArtifactStatus.DETACHED, descriptor.describe(agent.toString(), null).status());
    }

    @Test
    void skillIsDescribedAsADirectoryFromItsFolderOrAnyFileInside() throws IOException {
        Path dir = claude.resolve("skills/sdd-design");
        write(dir.resolve("SKILL.md"), "skill");
        write(dir.resolve("assets/plantilla.md"), "apoyo");

        ArtifactDescription fromDir = descriptor.describe(dir.toString(), null);
        ArtifactDescription fromFile = descriptor.describe(dir.resolve("SKILL.md").toString(), null);

        assertEquals(ArtifactKind.SKILL, fromDir.kind());
        assertEquals(List.of("SKILL.md", "assets/plantilla.md"), fromDir.files());
        assertEquals(fromDir, fromFile);
    }

    @Test
    void jarPathsAreRejectedAsEditableArtifacts() {
        for(String input : List.of("/global/agents/claude/sdd-agent.md", "global/skills/sdd-design",
                "jar:file:/opt/cli.jar!/global/agents/claude/sdd-agent.md", "/opt/cli.jar!/global/x")) {
            PathDomainException e = failure(() -> descriptor.describe(input, null));

            assertEquals(AppErrorCode.ARTIFACT_NOT_EDITABLE, e.code(), input);
            assertEquals(input, e.location());
        }
    }

    @Test
    void missingArtifactIsReportedWithItsPath() {
        Path missing = claude.resolve("agents/sdd-agent.md");

        PathDomainException e = failure(() -> descriptor.describe(missing.toString(), null));

        assertEquals(AppErrorCode.ARTIFACT_NOT_FOUND, e.code());
        assertEquals(missing.toString(), e.location());
    }

    @Test
    void foreignFilesAreNotDescribed() throws IOException {
        Path own = write(claude.resolve("agents/mi-agente-propio.md"), MD);
        Path outside = write(tmp.resolve("suelto.md"), MD);

        assertEquals(AppErrorCode.ARTIFACT_NOT_CATALOGUED, failure(() -> descriptor.describe(own.toString(), null)).code());
        assertEquals(AppErrorCode.ARTIFACT_NOT_CATALOGUED, failure(() -> descriptor.describe(outside.toString(), null)).code());
    }

    @Test
    void unreadableOrMalformedArtifactsFailWithTheirPath() throws IOException {
        Path binary = claude.resolve("agents/sdd-agent.md");
        write(binary, "x");
        Files.write(binary, new byte[] {(byte) 0xC3, (byte) 0x28});
        Path noHeader = write(claude.resolve("agents/sdd-design.md"), "sin frontmatter");

        PathDomainException unreadable = failure(() -> descriptor.describe(binary.toString(), null));
        PathDomainException malformed = failure(() -> descriptor.describe(noHeader.toString(), null));

        assertEquals(AppErrorCode.TEXT_READ_FAILED, unreadable.code());
        assertEquals(binary.toString(), unreadable.location());
        assertEquals(AppErrorCode.ARTIFACT_FORMAT_INVALID, malformed.code());
        assertEquals(noHeader.toString(), malformed.location());
    }

    @Test
    void projectArtifactsOfUnregisteredModulesAreNotDescribed() throws IOException {
        write(project.resolve(".sdd/team.md"), "## Módulos\n\n- alfa | - | -\n");
        Path stranger = write(project.resolve(".claude/agents/analista-otro.md"), MD);

        PathDomainException e = failure(() -> descriptor.describe(stranger.toString(), project));

        assertEquals(AppErrorCode.ARTIFACT_NOT_CATALOGUED, e.code());
        assertTrue(e.location().endsWith("analista-otro.md"));
    }
}
