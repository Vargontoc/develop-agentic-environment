package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactParts;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;
import es.vargontoc.agentic.develop.cli.support.Console;

class ConfigureProviderTest {

    @TempDir
    Path tmp;

    private final ConfigureProvider configure = new ConfigureProvider();

    private Provider provider(ProviderType type) throws IOException {
        return new Provider(type, Files.createDirectories(tmp.resolve(type.id())));
    }

    @Test
    void unconfiguredProviderReceivesAgentsAndSkillsWithoutPrintingAnything() throws IOException {
        Provider claude = provider(ProviderType.CLAUDE);

        Console.Captured<OperationResult> run = Console.capture(() -> configure.execute(claude, true));

        assertEquals("", run.out());
        OperationResult result = run.value();
        assertTrue(result.created().contains("skills/sdd-design"));
        assertTrue(result.created().contains("agents/sdd-agent.md"));
        assertTrue(result.skipped().isEmpty());
        assertTrue(result.warnings().isEmpty());
        assertTrue(Files.isRegularFile(claude.getPath().resolve("agents/sdd-agent.md")));
        assertTrue(Files.isRegularFile(claude.getPath().resolve("skills/sdd-design/SKILL.md")));
    }

    @Test
    void secondRunOverwritesNothingAndReportsEverythingAsSkipped() throws IOException {
        Provider claude = provider(ProviderType.CLAUDE);
        OperationResult first = configure.execute(claude, true);
        Path agent = claude.getPath().resolve("agents/sdd-agent.md");
        Files.writeString(agent, "personalizado");

        OperationResult second = configure.execute(claude, true);

        assertTrue(second.created().isEmpty());
        assertEquals(first.created(), second.skipped());
        assertEquals("personalizado", Files.readString(agent));
    }

    @Test
    void codexGetsTomlAgentsWithEscapedBodyTheHelperAndTheProfile() throws IOException {
        Provider codex = provider(ProviderType.CODEX);

        OperationResult result = configure.execute(codex, true);

        assertTrue(result.created().contains("sdd.config.toml"));
        assertTrue(Files.isRegularFile(codex.getPath().resolve("skills/sdd-design/agents/openai.yaml")));

        String toml = Files.readString(codex.getPath().resolve("agents/sdd_agent.toml"));
        ArtifactParts parts = ArtifactFormat.CODEX_TOML.split(toml);
        String expectedBody = ClasspathResources.readText("/global/agents/shared/sdd-agent-body.md").orElseThrow();
        assertEquals(expectedBody, parts.body());
    }

    @Test
    void nothingIsInstalledWhenNotGlobal() throws IOException {
        Provider claude = provider(ProviderType.CLAUDE);

        OperationResult result = configure.execute(claude, false);

        assertTrue(result.created().isEmpty());
        assertTrue(result.skipped().isEmpty());
    }
}
