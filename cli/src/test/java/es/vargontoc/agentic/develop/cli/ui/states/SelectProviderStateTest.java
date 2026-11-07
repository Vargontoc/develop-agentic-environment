package es.vargontoc.agentic.develop.cli.ui.states;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.Services;
import es.vargontoc.agentic.develop.cli.application.ConfigureProvider;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.ProviderStatus;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.ui.TerminalState;
import es.vargontoc.agentic.develop.cli.support.Console;

class SelectProviderStateTest {

    @TempDir
    Path tmp;

    private final TerminalState parent = new TerminalState() {
        @Override public String printMenu() { return ""; }
        @Override public TerminalState handleChoice(String choice) { return this; }
    };

    private String runOnce(Provider provider) {
        return runOnce(provider, new ConfigureProvider());
    }

    private String runOnce(Provider provider, ConfigureProvider configure) {
        ProviderCatalog catalog = new ProviderCatalog() {
            @Override public List<Provider> availableProviders() { return List.of(provider); }
            @Override public ProviderStatus status(ProviderType type) { throw new UnsupportedOperationException(); }
        };
        SelectProviderState state = new SelectProviderState(catalog, configure, parent);
        state.handleChoice("1");
        return Console.capturedOut(() -> assertSame(parent, state.handleChoice("")));
    }

    @Test
    void printsTheUsualMessagesForCreatedAndExistingArtifacts() throws IOException {
        Provider claude = new Provider(ProviderType.CLAUDE, Files.createDirectories(tmp.resolve("claude")));

        String first = runOnce(claude);
        String second = runOnce(claude);

        assertTrue(first.startsWith("Configurando Claude"), first);
        assertTrue(first.contains("Copied skill 'sdd-design'"), first);
        assertTrue(first.contains("Copied agent 'sdd-agent.md'"), first);
        assertTrue(second.contains("Skill 'sdd-design' exists"), second);
        assertTrue(second.contains("Agent 'sdd-agent.md' exists"), second);
        assertEquals(-1, second.indexOf("Copied"));
    }

    @Test
    void aHookIsLabelledAsAHookAndNotAsAProfile() throws IOException {
        var paths = new PathResolver(Map.of("HOME", tmp.toString(), "LOCALAPPDATA", tmp.toString()), PathResolver.Os.LINUX);
        ConfigureProvider withHooks = Services.of(paths).configureProvider();
        Provider claude = new Provider(ProviderType.CLAUDE, Files.createDirectories(tmp.resolve("claude")));

        String first = runOnce(claude, withHooks);
        String second = runOnce(claude, withHooks);

        assertTrue(first.contains("Copied hook 'sdd-specs-only.sh'"), first);
        assertTrue(second.contains("Hook 'sdd-specs-only.sh' exists"), second);
        assertEquals(-1, first.indexOf("profile"), first);
    }

    @Test
    void codexProfileUsesTheProfileMessages() throws IOException {
        Provider codex = new Provider(ProviderType.CODEX, Files.createDirectories(tmp.resolve("codex")));

        String first = runOnce(codex);
        String second = runOnce(codex);

        assertTrue(first.contains("Copied profile 'sdd.config.toml'"), first);
        assertTrue(second.contains("Profile 'sdd.config.toml' exists"), second);
    }
}
