package es.vargontoc.agentic.develop.cli.infrastructure.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import es.vargontoc.agentic.develop.cli.domain.Provider;
import es.vargontoc.agentic.develop.cli.domain.ProviderType;

public class AppConfiguration {
    
    static final String CLAUDE_DIR = ".claude";
    static final String CODEX_DIR = ".codex";
    static final String GEMINI_DIR = ".gemini";
    static final String USERPROFILE_DIR = "USERPROFILE";

    private final Map<ProviderType, Path> globalPaths;

    public AppConfiguration() {
        String userProfile = System.getenv(USERPROFILE_DIR);
        if(userProfile == null || userProfile.isEmpty()) {
            globalPaths = Map.of();
            return;
        }

        globalPaths = Map.of(
            ProviderType.CLAUDE, Path.of(userProfile, CLAUDE_DIR),
            ProviderType.OPENCODE, Path.of(userProfile, ".config", "opencode"),
            ProviderType.CODEX, Path.of(userProfile, CODEX_DIR),
            ProviderType.GEMINI, Path.of(userProfile, GEMINI_DIR)
        );
    }


    public  boolean hasProvider(ProviderType type) {
        Path path = globalPaths.get(type);
        return path != null && path.toFile().exists();
    }

    public Path getPath(ProviderType type) {
        return globalPaths.get(type);
    }


    public boolean canRunApp(){
        return List.of(ProviderType.values()).stream().anyMatch(this::hasProvider);
    }


    public List<Provider> getAvailableProviders() {
        List<Provider> available = new ArrayList<>();
        List.of(ProviderType.values()).forEach(t -> {
            if(hasProvider(t)) available.add(new Provider(t, true));
        });
        return available;
    }
}
