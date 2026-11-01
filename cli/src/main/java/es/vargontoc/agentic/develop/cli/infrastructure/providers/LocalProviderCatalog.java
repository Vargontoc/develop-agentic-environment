package es.vargontoc.agentic.develop.cli.infrastructure.providers;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderCatalog;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;

public class LocalProviderCatalog implements ProviderCatalog {

    static final String CLAUDE_DIR = ".claude";
    static final String CODEX_DIR = ".codex";
    static final String USERPROFILE_DIR = "USERPROFILE";

    private final Map<ProviderType, Path> globalPaths;

    public LocalProviderCatalog() {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) {
            globalPaths = Map.of();
            return;
        }

        globalPaths = Map.of(
            ProviderType.CLAUDE, Path.of(userProfile, CLAUDE_DIR),
            ProviderType.OPENCODE, Path.of(userProfile, ".config", "opencode"),
            ProviderType.CODEX, Path.of(userProfile, CODEX_DIR),
            ProviderType.GEMINI, Path.of(userProfile, ".gemini", "config")
        );
    }

    @Override
    public List<Provider> availableProviders() {
        return List.of(ProviderType.values()).stream()
            .filter(this::isInstalled)
            .map(t -> new Provider(t, globalPaths.get(t)))
            .toList();
    }

    private boolean isInstalled(ProviderType type) {
        Path path = globalPaths.get(type);
        return path != null && path.toFile().exists();
    }
}
