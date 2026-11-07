package es.vargontoc.agentic.develop.cli.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactFormat;
import es.vargontoc.agentic.develop.cli.domain.format.ArtifactParts;
import es.vargontoc.agentic.develop.cli.domain.format.TargetOs;
import es.vargontoc.agentic.develop.cli.domain.artifact.ArtifactStatus;
import es.vargontoc.agentic.develop.cli.domain.metadata.LinkState;
import es.vargontoc.agentic.develop.cli.infrastructure.files.TextStore;
import es.vargontoc.agentic.develop.cli.infrastructure.metadata.MetadataStore;
import es.vargontoc.agentic.develop.cli.infrastructure.paths.PathResolver;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ClasspathResources;
import es.vargontoc.agentic.develop.cli.infrastructure.resources.ResourceArtifactCatalog;
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
    void theSameCatalogEntryGetsTheHeaderOfEachTargetSystem() throws IOException {
        Provider forWindows = new Provider(ProviderType.CLAUDE, Files.createDirectories(tmp.resolve("win")));
        Provider forLinux = new Provider(ProviderType.CLAUDE, Files.createDirectories(tmp.resolve("lin")));

        assertTrue(new ConfigureProvider(TargetOs.WINDOWS).execute(forWindows, true).warnings().isEmpty());
        assertTrue(new ConfigureProvider(TargetOs.LINUX).execute(forLinux, true).warnings().isEmpty());

        for(String agent : new String[] {"sdd-agent", "sdd-design", "sdd-requirements", "sdd-tasklist"}) {
            String win = Files.readString(forWindows.getPath().resolve("agents/" + agent + ".md"));
            String lin = Files.readString(forLinux.getPath().resolve("agents/" + agent + ".md"));
            assertTrue(win.contains("shell: powershell"), agent);
            assertTrue(win.contains("$env:USERPROFILE"), agent);
            assertTrue(lin.contains("shell: bash"), agent);
            assertTrue(lin.contains("\"$HOME/.claude/hooks/sdd-specs-only.sh\" specs"), agent);
            assertFalse(lin.toLowerCase().contains("powershell"), agent);
            assertFalse(lin.contains("USERPROFILE"), agent);
            assertFalse(lin.contains("{shell}") || lin.contains("{hook:"), agent);
        }
    }

    @Test
    void nothingIsInstalledWhenNotGlobal() throws IOException {
        Provider claude = provider(ProviderType.CLAUDE);

        OperationResult result = configure.execute(claude, false);

        assertTrue(result.created().isEmpty());
        assertTrue(result.skipped().isEmpty());
    }

    private ConfigureProvider tracking(MetadataStore[] store) {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path backups = tmp.resolve("backups");
        var catalog = new ResourceArtifactCatalog();
        store[0] = new MetadataStore(tmp.resolve("app-home/metadata.json"), new TextStore(paths, List.of(tmp), backups));
        return new ConfigureProvider(TargetOs.LINUX, catalog, new ArtifactInventory(paths, catalog, store[0], backups), store[0]);
    }

    @Test
    void installedArtifactsAreRegisteredWithGroupHashAndLinkedState() throws IOException {
        MetadataStore[] store = new MetadataStore[1];
        ConfigureProvider tracked = tracking(store);
        Provider codex = provider(ProviderType.CODEX);

        OperationResult result = tracked.execute(codex, true);

        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
        Path root = codex.getPath().toAbsolutePath().normalize();
        var agent = store[0].artifact(root.resolve("agents/sdd_agent.toml")).orElseThrow();
        assertEquals("global/agent/sdd-agent", agent.groupId());
        assertEquals(LinkState.LINKED, agent.state());
        assertTrue(store[0].artifact(root.resolve("skills/sdd-design")).isPresent());
        assertTrue(store[0].artifact(root.resolve("sdd.config.toml")).isPresent());
        assertEquals(ClasspathResources.readText("/global/agents/shared/sdd-agent-body.md").orElseThrow(),
            store[0].group("global/agent/sdd-agent").orElseThrow().sharedBody());
        var inventory = new ArtifactInventory(new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX),
            new ResourceArtifactCatalog(), store[0], tmp.resolve("backups"));
        assertTrue(inventory.scanGlobal(ProviderType.CODEX).artifacts().isEmpty()
            || inventory.scanGlobal(ProviderType.CODEX).artifacts().stream().noneMatch(a -> a.modifiedExternally()));
    }

    // --- hook sdd-specs-only ---

    private ConfigureProvider withHooks(MetadataStore[] store) {
        PathResolver paths = new PathResolver(Map.of("HOME", tmp.toString()), PathResolver.Os.LINUX);
        Path backups = tmp.resolve("backups");
        var catalog = new ResourceArtifactCatalog();
        store[0] = new MetadataStore(tmp.resolve("app-home/metadata.json"), new TextStore(paths, List.of(tmp), backups));
        return new ConfigureProvider(TargetOs.LINUX, catalog, new ArtifactInventory(paths, catalog, store[0], backups),
            store[0], new HookInstaller(paths, store[0]));
    }

    @Test
    void globalInstallLeavesTheHookInEverySelectedProvider() throws IOException {
        MetadataStore[] store = new MetadataStore[1];
        ConfigureProvider configureWithHooks = withHooks(store);
        Provider claude = provider(ProviderType.CLAUDE);
        Provider codex = provider(ProviderType.CODEX);

        OperationResult first = configureWithHooks.execute(claude, true);
        OperationResult second = configureWithHooks.execute(codex, true);

        assertTrue(Files.isRegularFile(claude.getPath().resolve("hooks/sdd-specs-only.sh")));
        assertTrue(Files.isRegularFile(codex.getPath().resolve("hooks/sdd-specs-only.sh")));
        assertFalse(Files.exists(claude.getPath().resolve("hooks/sdd-specs-only.bat")));
        assertTrue(first.created().contains("hooks/sdd-specs-only.sh"));
        assertTrue(second.created().contains("hooks/sdd-specs-only.sh"));
        // En un sistema de ficheros no POSIX el permiso de ejecución no se puede fijar y eso es lo único que avisa
        boolean posix = claude.getPath().getFileSystem().supportedFileAttributeViews().contains("posix");
        assertTrue(first.warnings().stream().allMatch(w -> !posix && w.contains("could not be made executable")),
            first.warnings().toString());
        assertEquals(2, store[0].artifactsOfGroup("global/hook/sdd-specs-only").size());
    }

    @Test
    void hookGoesAfterAgentsAndSkillsAndDoesNotDisturbTheirRegistration() throws IOException {
        MetadataStore[] store = new MetadataStore[1];
        Provider claude = provider(ProviderType.CLAUDE);

        OperationResult result = withHooks(store).execute(claude, true);

        assertEquals("hooks/sdd-specs-only.sh", result.created().get(result.created().size() - 1));
        Path root = claude.getPath().toAbsolutePath().normalize();
        assertTrue(store[0].artifact(root.resolve("agents/sdd-agent.md")).isPresent());
        assertTrue(store[0].artifact(root.resolve("skills/sdd-design")).isPresent());
    }

    @Test
    void notGlobalNeverInstallsTheHook() throws IOException {
        MetadataStore[] store = new MetadataStore[1];
        Provider claude = provider(ProviderType.CLAUDE);

        OperationResult result = withHooks(store).execute(claude, false);

        assertFalse(Files.exists(claude.getPath().resolve("hooks")));
        assertTrue(result.created().isEmpty());
        assertTrue(store[0].artifactsOfGroup("global/hook/sdd-specs-only").isEmpty());
    }

    @Test
    void theUsersOwnHookScriptsAreLeftUntouched() throws IOException {
        MetadataStore[] store = new MetadataStore[1];
        Provider claude = provider(ProviderType.CLAUDE);
        Path mine = Files.createDirectories(claude.getPath().resolve("hooks")).resolve("mi-script.sh");
        Files.writeString(mine, "propio\n");
        var before = Files.getLastModifiedTime(mine);

        withHooks(store).execute(claude, true);

        assertEquals("propio\n", Files.readString(mine));
        assertEquals(before, Files.getLastModifiedTime(mine));
        assertTrue(Files.exists(claude.getPath().resolve("hooks/sdd-specs-only.sh")));
    }

    @Test
    void withoutAHookInstallerTheGlobalInstallDoesNotTouchHooks() throws IOException {
        Provider claude = provider(ProviderType.CLAUDE);

        configure.execute(claude, true);

        assertFalse(Files.exists(claude.getPath().resolve("hooks")));
    }

    @Test
    void theCompositionRootWiresTheHookIntoTheGlobalInstallAndRepeatingItIsQuiet() throws IOException {
        var paths = new PathResolver(Map.of("HOME", tmp.toString(), "LOCALAPPDATA", tmp.toString()), PathResolver.Os.LINUX);
        var services = es.vargontoc.agentic.develop.cli.Services.of(paths);
        Provider claude = new Provider(ProviderType.CLAUDE, paths.providerHome(ProviderType.CLAUDE));

        OperationResult first = services.configureProvider().execute(claude, true);
        OperationResult again = services.configureProvider().execute(claude, true);

        assertTrue(Files.isRegularFile(paths.hookPath(ProviderType.CLAUDE)));
        assertTrue(first.created().contains("hooks/sdd-specs-only.sh"));
        assertTrue(again.created().isEmpty());
        assertTrue(again.skipped().contains("hooks/sdd-specs-only.sh"));
        assertTrue(again.warnings().isEmpty(), again.warnings().toString());
    }

    @Test
    void existingArtifactsAreNeitherOverwrittenNorRegisteredAndRepeatingChangesNothing() throws IOException {
        MetadataStore[] store = new MetadataStore[1];
        ConfigureProvider tracked = tracking(store);
        Provider claude = provider(ProviderType.CLAUDE);
        Path custom = Files.createDirectories(claude.getPath().resolve("agents")).resolve("sdd-agent.md");
        Files.writeString(custom, "personalizado");

        tracked.execute(claude, true);
        Path root = claude.getPath().toAbsolutePath().normalize();
        assertEquals("personalizado", Files.readString(custom));
        assertTrue(store[0].artifact(root.resolve("agents/sdd-agent.md")).isEmpty());
        assertTrue(store[0].artifact(root.resolve("agents/sdd-design.md")).isPresent());

        OperationResult again = tracked.execute(claude, true);
        assertTrue(again.created().isEmpty());
        assertTrue(again.warnings().isEmpty());
    }
}
